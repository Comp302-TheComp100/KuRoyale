package com.kuroyale.service;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.dto.NetworkGameStateSnapshot;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.util.NetworkConfig;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.Enumeration;
import java.util.function.BiConsumer;

/**
 * Service for handling network multiplayer connections.
 * Implements Host-Client model using Java Socket and ServerSocket classes.
 * 
 * Connection Architecture (as per spec):
 * - Host-Client model: One player hosts (acts as server), other player joins (acts as client)
 * - Host specifies a port number (default: 8080)
 * - Client connects using host's IP address and port
 * - Connection status indicator showing "Connected" / "Connecting" / "Disconnected"
 * 
 * Network Protocol Format:
 * MESSAGE_TYPE|player_id|data|timestamp
 * 
 * Examples:
 * CARD_PLACED|1|Knight|5.2,3.8|00:45
 * TOWER_DAMAGED|2|CrownLeft|450|01:23
 * ELIXIR_UPDATE|1|7|01:24
 */
public class NetworkService {
    
    public enum ConnectionMode {
        DIRECT,
        RELAY
    }
    
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
    private ConnectionMode connectionMode = ConnectionMode.DIRECT;
    
    // Server components (for host)
    private ServerSocket serverSocket;
    private Socket clientSocket;
    
    // Client components
    private Socket socket;
    
    // I/O streams
    private BufferedReader reader;
    private PrintWriter writer;
    private final BlockingQueue<String> outboundQueue = new LinkedBlockingQueue<>();
    private Future<?> writerTask;
    
    // Threading
    private ExecutorService executorService;
    private ScheduledExecutorService heartbeatScheduler;
    private volatile boolean running = false;
    
    // Relay
    private final RelayService relayService = RelayService.getInstance();
    
    // Callbacks
    private Consumer<NetworkMessage> onMessageReceived;
    private Consumer<ConnectionState> onStateChanged;
    private Consumer<String> onError;
    
    // Player info
    private int playerId; // 1 for host, 2 for client
    private String playerName;
    private String opponentName;
    private List<String> opponentDeck;
    
    // Arena layout (received from host for clients)
    private ArenaLayout hostArenaLayout;
    
    // Port being used
    private int hostedPort = -1;
    
    // Callbacks for connection status
    private BiConsumer<Boolean, String> onConnectionReady;
    
    // Reconnection tracking
    private String lastHostAddress;
    private int lastHostPort;
    
    // Ping tracking
    private volatile int pingMs = 0;
    
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
     * Resets the service for a fresh connection attempt.
     * Call this before startHosting or connectToHost if the service was previously used.
     */
    public void reset() {
        System.out.println("[NetworkService] Resetting service...");
        
        // Force close any existing connections
        running = false;
        
        // Close direct socket connections
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (Exception e) { /* ignore */ }
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (Exception e) { /* ignore */ }
        try { if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close(); } catch (Exception e) { /* ignore */ }
        try { if (reader != null) reader.close(); } catch (Exception e) { /* ignore */ }
        try { if (writer != null) writer.close(); } catch (Exception e) { /* ignore */ }
        
        serverSocket = null;
        socket = null;
        clientSocket = null;
        reader = null;
        writer = null;
        outboundQueue.clear();
        if (writerTask != null) {
            writerTask.cancel(true);
            writerTask = null;
        }
        
        // Shutdown and recreate executor if needed
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newCachedThreadPool();
        }
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
            heartbeatScheduler = null;
        }
        
        // Reset state
        opponentName = null;
        opponentDeck = null;
        pingMs = 0;
        state = ConnectionState.DISCONNECTED;
        connectionMode = ConnectionMode.DIRECT;
        
        System.out.println("[NetworkService] Reset complete");
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
    
    private void closeClientSocket() {
        try { if (reader != null) reader.close(); } catch (Exception e) { /* ignore */ }
        try { if (writer != null) writer.close(); } catch (Exception e) { /* ignore */ }
        try { if (socket != null) socket.close(); } catch (Exception e) { /* ignore */ }
        try { if (clientSocket != null) clientSocket.close(); } catch (Exception e) { /* ignore */ }
        reader = null;
        writer = null;
        socket = null;
        clientSocket = null;
    }
    
    /**
     * Starts hosting a game on the specified port.
     * Uses Java ServerSocket as per spec requirements.
     * 
     * @param port The port to host on (default: 8080)
     * @param playerName The host player's name
     * @return true if hosting started successfully
     */
    public boolean startHosting(int port, String playerName) {
        // Reset any previous state first
        reset();
        
        this.connectionMode = ConnectionMode.DIRECT;
        this.role = Role.HOST;
        this.playerId = 1;
        this.playerName = playerName;
        this.hostedPort = port;
        
        setState(ConnectionState.CONNECTING);
        System.out.println("[NetworkService] Starting server on port " + port + "...");
        
        executorService.submit(() -> {
            try {
                // Create server socket with address reuse enabled
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true); // Allow immediate port reuse
                serverSocket.bind(new InetSocketAddress(port));
                serverSocket.setSoTimeout(0); // No timeout - wait indefinitely
                running = true;
                
                // Get local IP for display
                String localIP = getLocalIPAddress();
                System.out.println("[NetworkService] Server started! IP: " + localIP + ", Port: " + port);
                
                // Notify that hosting is ready
                if (onConnectionReady != null) {
                    javafx.application.Platform.runLater(() -> 
                        onConnectionReady.accept(true, localIP + ":" + port));
                }
                
                // Wait for client connection
                waitForClient();
                
            } catch (BindException e) {
                System.err.println("[NetworkService] Port " + port + " is already in use");
                handleError("Port " + port + " is already in use. Try a different port or wait a moment.");
                setState(ConnectionState.DISCONNECTED);
                if (onConnectionReady != null) {
                    javafx.application.Platform.runLater(() -> 
                        onConnectionReady.accept(false, null));
                }
            } catch (IOException e) {
                System.err.println("[NetworkService] Failed to start server: " + e.getMessage());
                handleError("Failed to start server: " + e.getMessage());
                setState(ConnectionState.DISCONNECTED);
                if (onConnectionReady != null) {
                    javafx.application.Platform.runLater(() -> 
                        onConnectionReady.accept(false, null));
                }
            }
        });
        
        return true;
    }
    
    /**
     * Connects to a host at the specified IP address and port.
     * Uses Java Socket as per spec requirements.
     * 
     * @param hostAddress The host's IP address (e.g., "192.168.1.100" or "127.0.0.1")
     * @param port The port the host is listening on
     * @param playerName The client player's name
     * @return true if connection attempt started
     */
    public boolean connectToHost(String hostAddress, int port, String playerName) {
        // Reset any previous state first
        reset();
        
        this.connectionMode = ConnectionMode.DIRECT;
        this.role = Role.CLIENT;
        this.playerId = 2;
        this.playerName = playerName;
        this.lastHostAddress = hostAddress;
        this.lastHostPort = port;
        
        setState(ConnectionState.CONNECTING);
        System.out.println("[NetworkService] Connecting to " + hostAddress + ":" + port + "...");
        
        executorService.submit(() -> {
            try {
                // Create socket and connect with timeout
                socket = new Socket();
                socket.connect(new InetSocketAddress(hostAddress, port), config.getConnectionTimeout());
                running = true;
                
                setupStreams(socket);
                setState(ConnectionState.CONNECTED);
                
                System.out.println("[NetworkService] Connected to host!");
                
                // Send connection request with player name
                send(NetworkMessage.connect(playerName));
                
                // Start receiving messages
                startReceiving();
                startWriter();
                startHeartbeat();
                
            } catch (SocketTimeoutException e) {
                System.err.println("[NetworkService] Connection timed out");
                handleError("Connection timed out. Check the IP address and port.");
                setState(ConnectionState.DISCONNECTED);
            } catch (IOException e) {
                System.err.println("[NetworkService] Connection failed: " + e.getMessage());
                handleError("Connection failed: " + e.getMessage());
                setState(ConnectionState.DISCONNECTED);
            }
        });
        
        return true;
    }
    
    /**
     * Waits for a client to connect (HOST only).
     */
    private void waitForClient() {
        try {
            System.out.println("[NetworkService] Waiting for client connection...");
            clientSocket = serverSocket.accept();
            
            setupStreams(clientSocket);
            setState(ConnectionState.CONNECTED);
            
            System.out.println("[NetworkService] Client connected from " + 
                    clientSocket.getInetAddress().getHostAddress());
            
            // Start receiving messages
            startReceiving();
            startWriter();
            startHeartbeat();
            
        } catch (IOException e) {
            if (running) {
                handleError("Error accepting client: " + e.getMessage());
            }
        }
    }
    
    /**
     * Sets up input/output streams for a socket.
     */
    private void setupStreams(Socket socket) throws IOException {
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }
    
    /**
     * Starts the message receiving loop in a separate thread.
     */
    private void startReceiving() {
        executorService.submit(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    final String message = line;
                    
                    if (message.length() > config.getMaxMessageSize()) {
                        System.err.println("[NetworkService] Oversized message (" + message.length() + " bytes) - continuing");
                    }
                    
                    // Parse and handle message
                    NetworkMessage netMsg = NetworkMessage.fromProtocolString(message);
                    if (netMsg != null) {
                        handleMessage(netMsg);
                    }
                }
                } catch (IOException e) {
                    if (running) {
                    System.err.println("[NetworkService] Connection lost: " + e.getMessage());
                        handleDisconnection();
                }
            }
        });
    }
    
    /**
     * Starts a dedicated writer loop to send queued messages without spawning a thread per send.
     */
    private void startWriter() {
        if (writerTask != null && !writerTask.isDone()) {
            return;
        }
        
        writerTask = executorService.submit(() -> {
            try {
                while (running) {
                    String msg = outboundQueue.poll(200, TimeUnit.MILLISECONDS);
                    if (msg == null) continue;
                    if (writer != null) {
                        writer.println(msg);
                        writer.flush();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (running) {
                    System.err.println("[NetworkService] Writer error: " + e.getMessage());
                    handleDisconnection();
                }
            }
        });
    }
    
    /**
     * Starts the heartbeat sender for connection keep-alive.
     */
    private void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (running && state == ConnectionState.CONNECTED) {
                send(NetworkMessage.heartbeat(playerId));
            }
        }, 1000, config.getHeartbeatInterval(), TimeUnit.MILLISECONDS);
    }

    // ==================== RELAY MODE (no setup required) ====================
    
    public boolean createRelayRoom(String playerName) {
        reset();
        this.connectionMode = ConnectionMode.RELAY;
        this.role = Role.HOST;
        this.playerId = 1;
        this.playerName = playerName;
        
        setState(ConnectionState.CONNECTING);
        relayService.reset();
        
        relayService.setOnMessageReceived(message -> {
            NetworkMessage netMsg = NetworkMessage.fromProtocolString(message);
            if (netMsg != null) {
                handleMessage(netMsg);
            }
        });
        
        relayService.setOnPlayerJoined(joinedPlayerId -> {
            running = true;
            setState(ConnectionState.CONNECTED);
        });
        
        relayService.setOnError(this::handleError);
        
        relayService.createRoom().whenComplete((roomCode, error) -> {
            if (error != null || roomCode == null) {
                handleError("Failed to create room: " + (error != null ? error.getMessage() : "Unknown error"));
                setState(ConnectionState.DISCONNECTED);
                if (onConnectionReady != null) {
                    javafx.application.Platform.runLater(() -> onConnectionReady.accept(false, null));
                }
            } else {
                running = true;
                if (onConnectionReady != null) {
                    javafx.application.Platform.runLater(() -> onConnectionReady.accept(true, "ROOM: " + roomCode));
                }
            }
        });
        
        return true;
    }
    
    public boolean joinRelayRoom(String roomCode, String playerName) {
        reset();
        this.connectionMode = ConnectionMode.RELAY;
        this.role = Role.CLIENT;
        this.playerId = 2;
        this.playerName = playerName;
        
        setState(ConnectionState.CONNECTING);
        relayService.reset();
        
        relayService.setOnMessageReceived(message -> {
            NetworkMessage netMsg = NetworkMessage.fromProtocolString(message);
            if (netMsg != null) {
                handleMessage(netMsg);
            }
        });
        
        relayService.setOnError(this::handleError);
        
        relayService.joinRoom(roomCode).whenComplete((success, error) -> {
            if (error != null || !success) {
                handleError("Failed to join room: " + (error != null ? error.getMessage() : "Room not found"));
                setState(ConnectionState.DISCONNECTED);
            } else {
                running = true;
                setState(ConnectionState.CONNECTED);
                send(NetworkMessage.connect(playerName));
            }
        });
        
        return true;
    }
    
    /**
     * Handles an incoming network message.
     */
    private void handleMessage(NetworkMessage message) {
        System.out.println("[NetworkService] Received: " + message.getType());
        
        switch (message.getType()) {
            case CONNECT:
                // Client is connecting - data contains player name
                opponentName = message.getData();
                System.out.println("[NetworkService] Opponent name: " + opponentName);
                // Send acknowledgment with our name
                send(NetworkMessage.connectAck(playerName));
                // Notify listener so lobby can update UI
                if (onMessageReceived != null) {
                    javafx.application.Platform.runLater(() -> onMessageReceived.accept(message));
                }
                break;
                
            case CONNECT_ACK:
                // Host acknowledged our connection
                String ackName = message.getData();
                if (ackName != null && !ackName.isEmpty()) {
                    opponentName = ackName;
                }
                // Notify listener so lobby can update UI
                if (onMessageReceived != null) {
                    javafx.application.Platform.runLater(() -> onMessageReceived.accept(message));
                }
                break;
                
            case PLAYER_INFO:
                // Opponent info received (name + deck)
                String[] info = message.parsePlayerInfo();
                if (info != null && info.length >= 2) {
                    if (opponentName == null || opponentName.isEmpty()) {
                        opponentName = info[0];
                    }
                    String deckStr = info[1];
                    opponentDeck = (deckStr == null || deckStr.isEmpty())
                            ? List.of()
                            : Arrays.asList(deckStr.split(","));
                }
                if (onMessageReceived != null) {
                    javafx.application.Platform.runLater(() -> onMessageReceived.accept(message));
                }
                break;
                
            case HEARTBEAT:
                // Keep-alive received, update ping and respond if needed
                String hbData = message.getData();
                if (hbData != null) {
                    if (hbData.startsWith("PING:")) {
                        try {
                            long pingSentAt = Long.parseLong(hbData.substring("PING:".length()));
                            send(NetworkMessage.heartbeatPong(playerId, pingSentAt));
                        } catch (NumberFormatException ignored) {
                        }
                    } else if (hbData.startsWith("PONG:")) {
                        try {
                            long pingSentAt = Long.parseLong(hbData.substring("PONG:".length()));
                            int rtt = (int) Math.max(0, System.currentTimeMillis() - pingSentAt);
                            // Smooth ping to avoid jitter
                            pingMs = (pingMs == 0) ? rtt : (int) (pingMs * 0.7 + rtt * 0.3);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                break;
                
            case DISCONNECT:
                System.out.println("[NetworkService] Opponent disconnected gracefully");
                handleDisconnection();
                break;
                
            case ARENA_LAYOUT:
                // Client receives arena layout from host
                if (role == Role.CLIENT) {
                    hostArenaLayout = message.parseArenaLayout();
                    System.out.println("[NetworkService] Received arena layout from host");
                }
                // Fall through to notify listener
                
            default:
                // Forward to listener (game logic)
                if (onMessageReceived != null) {
                    javafx.application.Platform.runLater(() -> onMessageReceived.accept(message));
                }
                break;
        }
    }
    
    /**
     * Handles disconnection - attempts reconnection as per spec.
     */
    private void handleDisconnection() {
        if (state == ConnectionState.DISCONNECTED) return;
        
        setState(ConnectionState.RECONNECTING);
        long deadline = System.currentTimeMillis() + 5000; // 5 seconds as per spec
        
        executorService.submit(() -> {
            if (role == Role.HOST) {
                attemptHostReaccept(deadline);
            } else {
                attemptClientReconnect(deadline);
            }
        });
    }
    
    private void attemptHostReaccept(long deadline) {
        closeClientSocket();
        System.out.println("[NetworkService] Waiting for client reconnection...");
        while (System.currentTimeMillis() < deadline && state == ConnectionState.RECONNECTING) {
            try {
                if (serverSocket == null || serverSocket.isClosed()) {
                    break;
                }
                serverSocket.setSoTimeout(1000);
                clientSocket = serverSocket.accept();
                setupStreams(clientSocket);
                running = true;
                setState(ConnectionState.CONNECTED);
                startReceiving();
                startWriter();
                startHeartbeat();
                System.out.println("[NetworkService] Client reconnected!");
                return;
            } catch (SocketTimeoutException ignored) {
                // Retry until deadline
            } catch (IOException e) {
                System.out.println("[NetworkService] Reaccept failed: " + e.getMessage());
                break;
            }
        }
        finalizeDisconnect();
    }
    
    private void attemptClientReconnect(long deadline) {
        closeClientSocket();
        System.out.println("[NetworkService] Attempting to reconnect to host...");
        while (System.currentTimeMillis() < deadline && state == ConnectionState.RECONNECTING) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(lastHostAddress, lastHostPort), 1000);
                setupStreams(socket);
                running = true;
                setState(ConnectionState.CONNECTED);
                startReceiving();
                startWriter();
                startHeartbeat();
                send(NetworkMessage.connect(playerName));
                System.out.println("[NetworkService] Reconnected to host!");
                return;
            } catch (IOException e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) { }
            }
        }
        finalizeDisconnect();
    }
    
    private void finalizeDisconnect() {
        System.out.println("[NetworkService] Reconnection window expired");
        setState(ConnectionState.DISCONNECTED);
        running = false;
        // Notify about opponent disconnection
        if (onMessageReceived != null) {
            javafx.application.Platform.runLater(() -> 
                onMessageReceived.accept(NetworkMessage.opponentDisconnected()));
        }
    }
    
    /**
     * Sends a message to the connected peer.
     */
    public void send(NetworkMessage message) {
        if (state != ConnectionState.CONNECTED && state != ConnectionState.RECONNECTING) {
            return;
        }
        
        String protocolString = message.toProtocolString();
        if (protocolString.length() > config.getMaxMessageSize()) {
            System.err.println("[NetworkService] Oversized outgoing message (" + protocolString.length() + " bytes) - sending anyway");
        }
        if (connectionMode == ConnectionMode.RELAY) {
            relayService.send(protocolString);
        } else {
            outboundQueue.offer(protocolString);
        }
    }
    
    /**
     * Sends a game state snapshot to the connected peer.
     */
    public void sendGameStateSnapshot(NetworkGameStateSnapshot snapshot) {
        send(NetworkMessage.gameStateSync(snapshot));
    }
    
    /**
     * Sends the arena layout to the client (HOST only).
     */
    public void sendArenaLayout(ArenaLayout layout) {
        if (role == Role.HOST) {
            send(NetworkMessage.arenaLayout(layout));
        }
    }
    
    /**
     * Sends ready status to the opponent.
     */
    public void sendReadyStatus(boolean ready) {
        send(NetworkMessage.readyStatus(playerId, ready));
    }
    
    /**
     * Sends match start signal (HOST only).
     */
    public void sendMatchStart() {
        if (role == Role.HOST) {
            send(NetworkMessage.matchStart());
        }
    }
    
    /**
     * Gracefully disconnects from the game.
     * Notifies opponent before closing connection (as per spec).
     */
    public void disconnect() {
        if (state == ConnectionState.DISCONNECTED) return;
        
        System.out.println("[NetworkService] Disconnecting gracefully...");
        
        // Notify opponent
        send(NetworkMessage.disconnect(playerId, "Leaving game"));
        
        // Give time for message to be sent
        try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
        
        running = false;
        
        // Close socket resources
        closeClientSocket();
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception e) { /* ignore */ }
        if (connectionMode == ConnectionMode.RELAY) {
            relayService.disconnect();
        }
        
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        if (writerTask != null) {
            writerTask.cancel(true);
            writerTask = null;
        }
        outboundQueue.clear();
        
        setState(ConnectionState.DISCONNECTED);
    }
    
    /**
     * Gets the local IP address for display.
     */
    public String getLocalIPAddress() {
        try {
            // Try to find a non-loopback address
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
            // Fallback to localhost
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
    
    /**
     * Fetches the public IP address asynchronously (for internet play).
     */
    public void fetchPublicIPAsync(Consumer<String> callback) {
        executorService.submit(() -> {
            try {
                URI uri = new URI("https://api.ipify.org");
                BufferedReader reader = new BufferedReader(new InputStreamReader(uri.toURL().openStream()));
                String publicIP = reader.readLine();
                reader.close();
                callback.accept(publicIP);
            } catch (Exception e) {
                callback.accept(null);
            }
        });
    }
    
    // ==================== State Management ====================
    
    private void setState(ConnectionState newState) {
        this.state = newState;
        if (onStateChanged != null) {
            javafx.application.Platform.runLater(() -> onStateChanged.accept(newState));
        }
    }
    
    private void handleError(String error) {
        if (onError != null) {
            javafx.application.Platform.runLater(() -> onError.accept(error));
        }
    }
    
    // ==================== Getters and Setters ====================
    
    public ConnectionState getState() { return state; }
    public Role getRole() { return role; }
    public boolean isHost() { return role == Role.HOST; }
    public boolean isConnected() { return state == ConnectionState.CONNECTED; }
    public int getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getOpponentName() { return opponentName; }
    public ArenaLayout getHostArenaLayout() { return hostArenaLayout; }
    public int getHostedPort() { return hostedPort; }
    public int getPing() {
        return isConnected() ? pingMs : 0;
    }
    
    public List<String> getOpponentDeck() {
        return opponentDeck;
    }
    
    // ==================== Callback Setters ====================
    
    public void setOnMessageReceived(Consumer<NetworkMessage> callback) {
        this.onMessageReceived = callback;
    }
    
    public void setOnStateChanged(Consumer<ConnectionState> callback) {
        this.onStateChanged = callback;
    }
    
    public void setOnError(Consumer<String> callback) {
        this.onError = callback;
    }
    
    public void setOnConnectionReady(BiConsumer<Boolean, String> callback) {
        this.onConnectionReady = callback;
    }
}
