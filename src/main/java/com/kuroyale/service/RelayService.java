package com.kuroyale.service;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Relay service using FREE public MQTT broker for multiplayer connectivity.
 * 
 * This works on ANY network without ANY setup:
 * - No accounts needed
 * - No port forwarding needed
 * - No router configuration needed
 * - Works through any firewall/NAT
 * 
 * How it works:
 * 1. Host creates a room (generates a room code)
 * 2. Host shares the room code with friend
 * 3. Friend joins using the room code
 * 4. All messages are relayed through the free public broker
 */
public class RelayService {
    
    // FREE public MQTT brokers - multiple for fallback
    private static final String[] BROKER_HOSTS = {
        "broker.hivemq.com",
        "test.mosquitto.org",
        "broker.emqx.io"
    };
    private static final int[] BROKER_PORTS = {1883, 1883, 1883};
    private static final String TOPIC_PREFIX = "kuroyale/game/";
    
    // Retry configuration
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    
    private static RelayService instance;
    
    private Mqtt5AsyncClient client;
    private String roomCode;
    private String playerId;
    private boolean isHost;
    private boolean connected = false;
    private int currentBrokerIndex = 0;
    
    // Callbacks
    private Consumer<String> onMessageReceived;
    private Consumer<Boolean> onConnectionChanged;
    private Consumer<String> onPlayerJoined;
    private Consumer<String> onError;
    
    private RelayService() {
        // Generate a unique player ID for this session
        regeneratePlayerId();
    }
    
    /**
     * Regenerates the player ID to avoid connection collisions.
     */
    private void regeneratePlayerId() {
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static synchronized RelayService getInstance() {
        if (instance == null) {
            instance = new RelayService();
        }
        return instance;
    }
    
    /**
     * Resets the service for a fresh connection attempt.
     */
    public void reset() {
        disconnect();
        regeneratePlayerId();
        currentBrokerIndex = 0;
    }
    
    /**
     * Creates a new game room and returns the room code.
     * Share this code with your friend to let them join.
     * Room code format: First char = broker index, rest = random code
     */
    public CompletableFuture<String> createRoom() {
        this.isHost = true;
        this.roomCode = generateRoomCode();
        regeneratePlayerId(); // Fresh ID for each room
        
        return connectWithRetry(0).thenApply(success -> {
            if (success) {
                subscribeToRoom();
                // Return room code with broker index prefix so client connects to same broker
                return currentBrokerIndex + roomCode;
            }
            return null;
        });
    }
    
    /**
     * Joins an existing game room using a room code.
     * Room code format: First char = broker index (0-2), rest = actual room code
     */
    public CompletableFuture<Boolean> joinRoom(String code) {
        this.isHost = false;
        code = code.toUpperCase().trim();
        
        // Extract broker index from first character
        if (code.length() >= 1) {
            char brokerChar = code.charAt(0);
            if (brokerChar >= '0' && brokerChar <= '2') {
                currentBrokerIndex = brokerChar - '0';
                this.roomCode = code.substring(1); // Actual room code without broker prefix
            } else {
                this.roomCode = code; // Legacy format
            }
        } else {
            this.roomCode = code;
        }
        
        regeneratePlayerId(); // Fresh ID for each join attempt
        
        // Connect directly to the same broker as the host
        return connectToBroker(BROKER_HOSTS[currentBrokerIndex], BROKER_PORTS[currentBrokerIndex])
            .thenCompose(success -> {
                if (success) {
                    subscribeToRoom();
                    // Notify host that we joined
                    sendSystemMessage("PLAYER_JOINED:" + playerId);
                    return CompletableFuture.completedFuture(true);
                }
                // If the specific broker fails, try others
                return connectWithRetry(0).thenCompose(retrySuccess -> {
                    if (retrySuccess) {
                        subscribeToRoom();
                        sendSystemMessage("PLAYER_JOINED:" + playerId);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
    }
    
    /**
     * Connects to the MQTT broker with retry logic and broker fallback.
     */
    private CompletableFuture<Boolean> connectWithRetry(int attempt) {
        if (attempt >= MAX_RETRY_ATTEMPTS * BROKER_HOSTS.length) {
            // Tried all brokers multiple times
            handleError("All connection attempts failed. Please try again later.");
            return CompletableFuture.completedFuture(false);
        }
        
        // Calculate which broker to try
        currentBrokerIndex = (attempt / MAX_RETRY_ATTEMPTS) % BROKER_HOSTS.length;
        String brokerHost = BROKER_HOSTS[currentBrokerIndex];
        int brokerPort = BROKER_PORTS[currentBrokerIndex];
        
        // Calculate delay with exponential backoff
        int retryWithinBroker = attempt % MAX_RETRY_ATTEMPTS;
        long delay = retryWithinBroker == 0 ? 0 : INITIAL_RETRY_DELAY_MS * (1L << (retryWithinBroker - 1));
        
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        // Apply delay if this is a retry
        if (delay > 0) {
            System.out.println("[Relay] Waiting " + delay + "ms before retry...");
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.complete(false);
                return future;
            }
        }
        
        // Try to connect
        connectToBroker(brokerHost, brokerPort)
            .whenComplete((success, throwable) -> {
                if (throwable != null || !success) {
                    String error = throwable != null ? throwable.getMessage() : "Connection failed";
                    System.out.println("[Relay] Attempt " + (attempt + 1) + " failed: " + error);
                    
                    // Try next attempt
                    connectWithRetry(attempt + 1)
                        .whenComplete((retrySuccess, retryError) -> {
                            future.complete(retrySuccess);
                        });
                } else {
                    future.complete(true);
                }
            });
        
        return future;
    }
    
    /**
     * Connects to a specific MQTT broker.
     */
    private CompletableFuture<Boolean> connectToBroker(String host, int port) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            System.out.println("[Relay] Connecting to " + host + ":" + port + "...");
            
            // Disconnect existing client if any
            if (client != null) {
                try {
                    client.disconnect().get(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Ignore disconnect errors
                }
                client = null;
            }
            
            // Create a unique client ID with timestamp to avoid collisions
            String clientId = "kuroyale-" + playerId + "-" + System.currentTimeMillis() % 100000;
            
            client = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost(host)
                    .serverPort(port)
                    .identifier(clientId)
                    .automaticReconnect()
                        .initialDelay(1, TimeUnit.SECONDS)
                        .maxDelay(30, TimeUnit.SECONDS)
                        .applyAutomaticReconnect()
                    .buildAsync();
            
            client.connect()
                    .orTimeout(10, TimeUnit.SECONDS)
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            System.err.println("[Relay] Connection failed: " + throwable.getMessage());
                            future.complete(false);
                        } else {
                            System.out.println("[Relay] Connected to " + host + "!");
                            connected = true;
                            notifyConnectionChanged(true);
                            future.complete(true);
                        }
                    });
            
        } catch (Exception e) {
            System.err.println("[Relay] Error: " + e.getMessage());
            future.complete(false);
        }
        
        return future;
    }
    
    /**
     * Subscribes to the room topic to receive messages.
     */
    private void subscribeToRoom() {
        String topic = TOPIC_PREFIX + roomCode;
        System.out.println("[Relay] Subscribing to room: " + roomCode);
        
        client.subscribeWith()
                .topicFilter(topic)
                .callback(this::handleMessage)
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("[Relay] Subscribe failed: " + throwable.getMessage());
                    } else {
                        System.out.println("[Relay] Subscribed to room " + roomCode);
                    }
                });
    }
    
    /**
     * Handles incoming messages from the relay.
     */
    private void handleMessage(Mqtt5Publish publish) {
        String payload = new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8);
        
        // Parse message format: "SENDER_ID|MESSAGE"
        int separatorIndex = payload.indexOf('|');
        if (separatorIndex == -1) return;
        
        String senderId = payload.substring(0, separatorIndex);
        String message = payload.substring(separatorIndex + 1);
        
        // Ignore our own messages
        if (senderId.equals(playerId)) return;
        
        System.out.println("[Relay] Received: " + message);
        
        // Handle system messages
        if (message.startsWith("PLAYER_JOINED:")) {
            String joinedPlayerId = message.substring("PLAYER_JOINED:".length());
            if (onPlayerJoined != null) {
                onPlayerJoined.accept(joinedPlayerId);
            }
            return;
        }
        
        // Forward to callback
        if (onMessageReceived != null) {
            onMessageReceived.accept(message);
        }
    }
    
    /**
     * Sends a game message to the other player.
     */
    public void send(String message) {
        if (!connected || client == null || roomCode == null) {
            System.err.println("[Relay] Cannot send - not connected");
            return;
        }
        
        String topic = TOPIC_PREFIX + roomCode;
        String payload = playerId + "|" + message;
        
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes(StandardCharsets.UTF_8))
                .send()
                .whenComplete((pubAck, throwable) -> {
                    if (throwable != null) {
                        System.err.println("[Relay] Send failed: " + throwable.getMessage());
                    }
                });
    }
    
    /**
     * Sends a system message (for connection management).
     */
    private void sendSystemMessage(String message) {
        send(message);
    }
    
    /**
     * Disconnects from the relay.
     */
    public void disconnect() {
        connected = false;
        
        if (client != null) {
            System.out.println("[Relay] Disconnecting...");
            try {
                client.disconnect().get(2, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Ignore disconnect errors
            }
            client = null;
            notifyConnectionChanged(false);
        }
        roomCode = null;
    }
    
    /**
     * Generates a random 6-character room code.
     */
    private String generateRoomCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Removed confusing chars like 0/O, 1/I
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }
    
    // Getters
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getPlayerId() {
        return playerId;
    }
    
    // Callback setters
    
    public void setOnMessageReceived(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }
    
    public void setOnConnectionChanged(Consumer<Boolean> callback) {
        this.onConnectionChanged = callback;
    }
    
    public void setOnPlayerJoined(Consumer<String> callback) {
        this.onPlayerJoined = callback;
    }
    
    public void setOnError(Consumer<String> callback) {
        this.onError = callback;
    }
    
    private void notifyConnectionChanged(boolean connected) {
        if (onConnectionChanged != null) {
            onConnectionChanged.accept(connected);
        }
    }
    
    private void handleError(String error) {
        if (onError != null) {
            onError.accept(error);
        }
    }
}
