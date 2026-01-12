package com.kuroyale.service;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    
    // FREE public MQTT broker - no account needed!
    private static final String BROKER_HOST = "broker.hivemq.com";
    private static final int BROKER_PORT = 1883;
    private static final String TOPIC_PREFIX = "kuroyale/game/";
    
    private static RelayService instance;
    
    private Mqtt5AsyncClient client;
    private String roomCode;
    private String playerId;
    private boolean isHost;
    private boolean connected = false;
    
    // Callbacks
    private Consumer<String> onMessageReceived;
    private Consumer<Boolean> onConnectionChanged;
    private Consumer<String> onPlayerJoined;
    private Consumer<String> onError;
    
    private RelayService() {
        this.playerId = UUID.randomUUID().toString().substring(0, 8);
    }
    
    public static synchronized RelayService getInstance() {
        if (instance == null) {
            instance = new RelayService();
        }
        return instance;
    }
    
    /**
     * Creates a new game room and returns the room code.
     * Share this code with your friend to let them join.
     */
    public CompletableFuture<String> createRoom() {
        this.isHost = true;
        this.roomCode = generateRoomCode();
        
        return connect().thenApply(success -> {
            if (success) {
                subscribeToRoom();
                return roomCode;
            }
            return null;
        });
    }
    
    /**
     * Joins an existing game room using a room code.
     */
    public CompletableFuture<Boolean> joinRoom(String code) {
        this.isHost = false;
        this.roomCode = code.toUpperCase().trim();
        
        return connect().thenCompose(success -> {
            if (success) {
                subscribeToRoom();
                // Notify host that we joined
                sendSystemMessage("PLAYER_JOINED:" + playerId);
                return CompletableFuture.completedFuture(true);
            }
            return CompletableFuture.completedFuture(false);
        });
    }
    
    /**
     * Connects to the free public MQTT broker.
     */
    private CompletableFuture<Boolean> connect() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        try {
            System.out.println("[Relay] Connecting to relay server...");
            
            client = MqttClient.builder()
                    .useMqttVersion5()
                    .serverHost(BROKER_HOST)
                    .serverPort(BROKER_PORT)
                    .identifier("kuroyale-" + playerId)
                    .buildAsync();
            
            client.connect()
                    .whenComplete((connAck, throwable) -> {
                        if (throwable != null) {
                            System.err.println("[Relay] Connection failed: " + throwable.getMessage());
                            handleError("Connection failed: " + throwable.getMessage());
                            future.complete(false);
                        } else {
                            System.out.println("[Relay] Connected to relay server!");
                            connected = true;
                            notifyConnectionChanged(true);
                            future.complete(true);
                        }
                    });
            
        } catch (Exception e) {
            System.err.println("[Relay] Error: " + e.getMessage());
            handleError("Error: " + e.getMessage());
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
        if (client != null && connected) {
            System.out.println("[Relay] Disconnecting...");
            client.disconnect();
            connected = false;
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
