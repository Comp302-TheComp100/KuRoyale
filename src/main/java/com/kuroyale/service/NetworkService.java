package com.kuroyale.service;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.util.NetworkConfig;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Service for handling network multiplayer connections.
 * Implements Host-Client model using Java Sockets.
 * 
 * GRASP Patterns:
 * - Pure Fabrication: Not a domain concept, created to handle network concerns
 * - Low Coupling: Isolated network logic from game logic
 * - Controller: Manages network communication flow
 */
public class NetworkService {
    
    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }
    
    public enum Role {
        HOST,
        CLIENT
    }
    
    private final NetworkConfig config;
    private Role role;
    private ConnectionState state = ConnectionState.DISCONNECTED;
    
    // Server components (for host)
    private ServerSocket serverSocket;
    private Socket clientSocket;
    
    // Client components
    private Socket socket;
    
    // I/O streams
    private BufferedReader reader;
    private PrintWriter writer;
    
    // Threading
    private ExecutorService executorService;
    private ScheduledExecutorService heartbeatScheduler;
    private volatile boolean running = false;
    
    // Callbacks
    private Consumer<NetworkMessage> onMessageReceived;
    private Consumer<ConnectionState> onStateChanged;
    private Consumer<String> onError;
    
    // Player info
    private int playerId; // 1 for host, 2 for client
    private String playerName;
    private String opponentName;
    
    public NetworkService() {
        this.config = NetworkConfig.getInstance();
        this.executorService = Executors.newCachedThreadPool();
        
        // Add shutdown hook to ensure cleanup on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[NetworkService] Shutdown hook triggered");
            forceCloseAllSockets();
        }));
    }
    
    /**
     * Force closes all sockets without sending messages (for shutdown hook).
     */
    private void forceCloseAllSockets() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) { /* ignore */ }
        try { if (socket != null) socket.close(); } catch (Exception e) { /* ignore */ }
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception e) { /* ignore */ }
        if (executorService != null) executorService.shutdownNow();
        if (heartbeatScheduler != null) heartbeatScheduler.shutdownNow();
    }
    
    /**
     * Starts hosting a game on the specified port.
     * @param port The port to listen on
     * @param playerName The host player's name
     * @return true if hosting started successfully
     */
    public boolean startHosting(int port, String playerName) {
        this.role = Role.HOST;
        this.playerId = 1;
        this.playerName = playerName;
        
        try {
            serverSocket = new ServerSocket();
            // Allow port reuse - fixes "Address already in use" after restart
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
            serverSocket.setSoTimeout(0); // No timeout for accepting connections
            setState(ConnectionState.CONNECTING);
            
            System.out.println("[NetworkService] Hosting on port " + port);
            
            // Accept client connection in background
            executorService.submit(this::waitForClient);
            return true;
            
        } catch (IOException e) {
            handleError("Failed to start hosting: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Connects to a host as a client.
     * @param hostAddress The host's IP address
     * @param port The port to connect to
     * @param playerName The client player's name
     * @return true if connection started
     */
    public boolean connectToHost(String hostAddress, int port, String playerName) {
        this.role = Role.CLIENT;
        this.playerId = 2;
        this.playerName = playerName;
        
        setState(ConnectionState.CONNECTING);
        
        // Connect in background
        executorService.submit(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostAddress, port), config.getConnectionTimeout());
                
                setupStreams(socket);
                setState(ConnectionState.CONNECTED);
                
                // Send connection message
                send(NetworkMessage.connect(playerName));
                
                // Start receiving messages
                startReceiving();
                startHeartbeat();
                
                System.out.println("[NetworkService] Connected to host at " + hostAddress + ":" + port);
                
            } catch (IOException e) {
                handleError("Failed to connect: " + e.getMessage());
                setState(ConnectionState.DISCONNECTED);
            }
        });
        
        return true;
    }
    
    private void waitForClient() {
        try {
            System.out.println("[NetworkService] Waiting for client connection...");
            clientSocket = serverSocket.accept();
            
            setupStreams(clientSocket);
            setState(ConnectionState.CONNECTED);
            
            // Start receiving messages
            startReceiving();
            startHeartbeat();
            
            System.out.println("[NetworkService] Client connected from " + 
                    clientSocket.getInetAddress().getHostAddress());
            
        } catch (IOException e) {
            if (running) {
                handleError("Error accepting client: " + e.getMessage());
            }
        }
    }
    
    private void setupStreams(Socket socket) throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        running = true;
    }
    
    private void startReceiving() {
        executorService.submit(() -> {
            while (running && state == ConnectionState.CONNECTED) {
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        // Connection closed
                        handleDisconnection();
                        break;
                    }
                    
                    System.out.println("[NetworkService] RECEIVED: " + line);
                    NetworkMessage message = NetworkMessage.fromProtocolString(line);
                    if (message != null) {
                        handleMessage(message);
                    } else {
                        System.err.println("[NetworkService] Failed to parse message: " + line);
                    }
                    
                } catch (SocketTimeoutException e) {
                    // Timeout is ok, continue reading
                } catch (IOException e) {
                    if (running) {
                        handleDisconnection();
                    }
                    break;
                }
            }
        });
    }
    
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (state == ConnectionState.CONNECTED) {
                send(NetworkMessage.heartbeat(playerId));
            }
        }, config.getHeartbeatInterval(), config.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }
    
    private void handleMessage(NetworkMessage message) {
        // Handle internal messages
        switch (message.getType()) {
            case CONNECT:
                opponentName = message.getData();
                // Host responds with acknowledgment
                if (role == Role.HOST) {
                    send(NetworkMessage.connectAck(playerName));
                    send(NetworkMessage.playerInfo(playerId, playerName, ""));
                }
                break;
                
            case CONNECT_ACK:
                opponentName = message.getData();
                send(NetworkMessage.playerInfo(playerId, playerName, ""));
                break;
                
            case HEARTBEAT:
                // Heartbeat received, connection is alive
                break;
                
            case DISCONNECT:
                handleDisconnection();
                break;
                
            default:
                // Forward to callback
                break;
        }
        
        // Always forward to callback for UI updates
        if (onMessageReceived != null) {
            onMessageReceived.accept(message);
        }
    }
    
    private void handleDisconnection() {
        if (state == ConnectionState.DISCONNECTED) return;
        
        System.out.println("[NetworkService] Connection lost, attempting reconnection...");
        setState(ConnectionState.RECONNECTING);
        
        // Notify about disconnection
        if (onMessageReceived != null) {
            onMessageReceived.accept(NetworkMessage.opponentDisconnected());
        }
        
        // Try to reconnect
        executorService.submit(() -> {
            for (int i = 0; i < config.getReconnectAttempts(); i++) {
                try {
                    Thread.sleep(config.getReconnectWait());
                    
                    if (role == Role.HOST) {
                        // Host waits for client to reconnect
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            serverSocket.setSoTimeout(config.getConnectionTimeout());
                            try {
                                clientSocket = serverSocket.accept();
                                setupStreams(clientSocket);
                                setState(ConnectionState.CONNECTED);
                                startReceiving();
                                System.out.println("[NetworkService] Client reconnected");
                                return;
                            } catch (SocketTimeoutException e) {
                                // Continue trying
                            }
                        }
                    } else {
                        // Client tries to reconnect to host
                        // Would need stored host address
                    }
                    
                } catch (InterruptedException | IOException e) {
                    // Continue trying
                }
            }
            
            // Reconnection failed
            System.out.println("[NetworkService] Reconnection failed");
            setState(ConnectionState.DISCONNECTED);
        });
    }
    
    /**
     * Sends a message to the connected player.
     * @param message The message to send
     */
    public void send(NetworkMessage message) {
        if (writer != null && state == ConnectionState.CONNECTED) {
            String protocolStr = message.toProtocolString();
            writer.println(protocolStr);
            writer.flush(); // Ensure message is sent immediately
            System.out.println("[NetworkService] SENT: " + protocolStr);
        } else {
            System.err.println("[NetworkService] Cannot send - writer=" + (writer != null) + ", state=" + state);
        }
    }
    
    /**
     * Sends a card placement message.
     */
    public void sendCardPlaced(String cardName, double x, double y) {
        send(NetworkMessage.cardPlaced(playerId, cardName, x, y));
    }
    
    /**
     * Sends a tower damage message.
     */
    public void sendTowerDamaged(String towerName, int damage) {
        send(NetworkMessage.towerDamaged(playerId, towerName, damage));
    }
    
    /**
     * Sends an elixir update.
     */
    public void sendElixirUpdate(double elixir) {
        send(NetworkMessage.elixirUpdate(playerId, elixir));
    }
    
    /**
     * Sends ready status.
     */
    public void sendReadyStatus(boolean isReady) {
        send(NetworkMessage.readyStatus(playerId, isReady));
    }
    
    /**
     * Sends match start signal (host only).
     */
    public void sendMatchStart() {
        if (role == Role.HOST) {
            send(NetworkMessage.matchStart());
        }
    }
    
    /**
     * Gracefully disconnects and releases all resources.
     */
    public void disconnect() {
        System.out.println("[NetworkService] Disconnecting...");
        
        // Set running to false first to stop loops
        running = false;
        
        // Send disconnect message before closing (if connected)
        if (writer != null && state == ConnectionState.CONNECTED) {
            try {
                writer.println(NetworkMessage.disconnect(playerId, "User disconnected").toProtocolString());
                writer.flush();
            } catch (Exception e) {
                // Ignore errors during disconnect
            }
        }
        
        // Shutdown heartbeat scheduler
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
        
        // Close server socket FIRST to interrupt accept() blocking call
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            serverSocket = null;
        }
        
        // Close client connections
        try {
            if (reader != null) {
                reader.close();
                reader = null;
            }
        } catch (IOException e) { /* ignore */ }
        
        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (Exception e) { /* ignore */ }
        
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) { /* ignore */ }
        
        try {
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
        } catch (IOException e) { /* ignore */ }
        
        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = Executors.newCachedThreadPool(); // Create fresh one for next use
        }
        
        setState(ConnectionState.DISCONNECTED);
        System.out.println("[NetworkService] Disconnected successfully");
    }
    
    private void setState(ConnectionState newState) {
        this.state = newState;
        if (onStateChanged != null) {
            onStateChanged.accept(newState);
        }
    }
    
    private void handleError(String error) {
        System.err.println("[NetworkService] Error: " + error);
        if (onError != null) {
            onError.accept(error);
        }
    }
    
    // Getters and setters
    
    public ConnectionState getState() {
        return state;
    }
    
    public Role getRole() {
        return role;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getOpponentName() {
        return opponentName;
    }
    
    public boolean isHost() {
        return role == Role.HOST;
    }
    
    public boolean isConnected() {
        return state == ConnectionState.CONNECTED;
    }
    
    /**
     * Gets the local IP address for hosting.
     */
    public String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
    
    // Callback setters
    
    public void setOnMessageReceived(Consumer<NetworkMessage> callback) {
        this.onMessageReceived = callback;
    }
    
    public void setOnStateChanged(Consumer<ConnectionState> callback) {
        this.onStateChanged = callback;
    }
    
    public void setOnError(Consumer<String> callback) {
        this.onError = callback;
    }
}

