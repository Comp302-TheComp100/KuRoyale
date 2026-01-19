package com.kuroyale.service.network;

import com.kuroyale.model.core.entities.ArenaLayout;
import com.kuroyale.model.state.dto.NetworkMessage;
import com.kuroyale.util.config.NetworkConfig;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.Enumeration;
import java.util.function.BiConsumer;

/**
 * Service for handling network multiplayer connections.
 * Implements Host-Client model using Java Sockets.
 * 
 * GRASP Patterns:
 * - Pure Fabrication: Not a domain concept, created to handle network concerns
 * - Low Coupling: Isolated network logic from game logic
 * - Controller: Manages network communication flow
 * 
 * Cross-Network Support:
 * - Detects public IP for internet play
 * - Supports UPnP for automatic port forwarding
 * - Provides fallback instructions for manual port forwarding
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

    // Arena layout (received from host for clients)
    private ArenaLayout hostArenaLayout;

    // UPnP support
    private final UPnPService upnpService;
    private boolean upnpPortOpened = false;
    private int hostedPort = -1;

    // Relay support (works on ANY network, NO setup needed)
    private final RelayService relayService;
    private boolean usingRelay = false;

    // Callback for connection status updates
    private BiConsumer<Boolean, String> onUPnPStatusChanged;
    private BiConsumer<Boolean, String> onConnectionReady;

    public NetworkService() {
        this.config = NetworkConfig.getInstance();
        this.executorService = Executors.newCachedThreadPool();
        this.upnpService = UPnPService.getInstance();
        this.relayService = RelayService.getInstance();

        // Add shutdown hook to ensure cleanup on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[NetworkService] Shutdown hook triggered");
            forceCloseAllSockets();
            // Close relay connection
            if (usingRelay) {
                relayService.disconnect();
            }
            // Close UPnP port mapping
            if (upnpPortOpened) {
                upnpService.closeCurrentPort();
            }
        }));

        // Setup relay callbacks
        setupRelayCallbacks();
    }

    /**
     * Sets up callbacks for the relay service.
     */
    private void setupRelayCallbacks() {
        relayService.setOnMessageReceived(message -> {
            System.out.println("[NetworkService] Relay message: " + message);
            NetworkMessage netMsg = NetworkMessage.fromProtocolString(message);
            if (netMsg != null) {
                handleMessage(netMsg);
            }
        });

        relayService.setOnPlayerJoined(joinedPlayerId -> {
            System.out.println("[NetworkService] Player joined: " + joinedPlayerId);
            setState(ConnectionState.CONNECTED);
            // Send our info to the new player
            send(NetworkMessage.connectAck(playerName));
        });

        relayService.setOnConnectionChanged(connected -> {
            if (!connected && state != ConnectionState.DISCONNECTED) {
                handleDisconnection();
            }
        });

        relayService.setOnError(error -> {
            handleError(error);
        });
    }

    /**
     * Force closes all sockets without sending messages (for shutdown hook).
     */
    private void forceCloseAllSockets() {
        running = false;
        try {
            if (serverSocket != null)
                serverSocket.close();
        } catch (Exception e) {
            /* ignore */ }
        try {
            if (socket != null)
                socket.close();
        } catch (Exception e) {
            /* ignore */ }
        try {
            if (clientSocket != null)
                clientSocket.close();
        } catch (Exception e) {
            /* ignore */ }
        if (executorService != null)
            executorService.shutdownNow();
        if (heartbeatScheduler != null)
            heartbeatScheduler.shutdownNow();
    }

    /**
     * Starts hosting a game using the FREE relay service.
     * Works on ANY network without any setup!
     * 
     * @param port       Unused (kept for compatibility)
     * @param playerName The host player's name
     * @return true if hosting started successfully
     */
    public boolean startHosting(int port, String playerName) {
        this.role = Role.HOST;
        this.playerId = 1;
        this.playerName = playerName;
        this.hostedPort = port;
        this.usingRelay = true;

        setState(ConnectionState.CONNECTING);
        System.out.println("[NetworkService] Creating game room via relay...");

        // Reset relay service for fresh connection
        relayService.reset();

        // Create room using the FREE relay service
        relayService.createRoom().thenAccept(roomCode -> {
            if (roomCode != null) {
                System.out.println("[NetworkService] Room created! Code: " + roomCode);
                if (onConnectionReady != null) {
                    onConnectionReady.accept(true, roomCode);
                }
            } else {
                handleError("Failed to create room. Please try again.");
                setState(ConnectionState.DISCONNECTED);
            }
        });

        return true;
    }

    /**
     * Attempts to open a port via UPnP for internet connectivity.
     * This runs asynchronously and updates the UI when complete.
     */
    private void openPortViaUPnP(int port) {
        if (!upnpService.isInitialized()) {
            // Wait for UPnP initialization then try
            upnpService.initialize().thenCompose(available -> {
                if (available) {
                    return upnpService.openPort(port);
                }
                return CompletableFuture.completedFuture(false);
            }).thenAccept(success -> {
                upnpPortOpened = success;
                handleUPnPResult(success, port);
            });
        } else if (upnpService.isUPnPAvailable()) {
            upnpService.openPort(port).thenAccept(success -> {
                upnpPortOpened = success;
                handleUPnPResult(success, port);
            });
        } else {
            System.out.println("[NetworkService] UPnP not available on this network");
            // Fallback to showing local IP for same-network play
            notifyConnectionReady(false, "LOCAL_ONLY", port);
        }
    }

    /**
     * Handles the UPnP result and notifies appropriate callbacks.
     */
    private void handleUPnPResult(boolean success, int port) {
        if (success) {
            String externalIP = upnpService.getExternalIP();
            String connStr = upnpService.getConnectionString(port);

            // Check for CGNAT (private IP ranges as "external" IP)
            if (externalIP != null && isCGNAT(externalIP)) {
                System.out.println("[NetworkService] CGNAT detected - internet play may not work");
                notifyConnectionReady(false, "CGNAT_DETECTED", port);
            } else {
                System.out.println("[NetworkService] UPnP port opened! Share: " + connStr);
                notifyConnectionReady(true, connStr, port);
            }
        } else {
            System.out.println("[NetworkService] UPnP port opening failed");
            notifyConnectionReady(false, "UPNP_FAILED", port);
        }
    }

    /**
     * Checks if the IP address indicates CGNAT (Carrier-Grade NAT).
     * CGNAT uses private IP ranges as the "external" IP.
     */
    private boolean isCGNAT(String ip) {
        if (ip == null)
            return false;
        // CGNAT typically uses 100.64.0.0/10 range, but ISPs also use 10.x.x.x
        return ip.startsWith("10.") ||
                ip.startsWith("100.64.") || ip.startsWith("100.65.") || ip.startsWith("100.66.") ||
                ip.startsWith("100.67.") || ip.startsWith("100.68.") || ip.startsWith("100.69.") ||
                ip.startsWith("100.70.") || ip.startsWith("100.71.") || ip.startsWith("100.72.") ||
                ip.startsWith("100.73.") || ip.startsWith("100.74.") || ip.startsWith("100.75.") ||
                ip.startsWith("100.76.") || ip.startsWith("100.77.") || ip.startsWith("100.78.") ||
                ip.startsWith("100.79.") || ip.startsWith("100.80.") || ip.startsWith("100.81.") ||
                ip.startsWith("100.82.") || ip.startsWith("100.83.") || ip.startsWith("100.84.") ||
                ip.startsWith("100.85.") || ip.startsWith("100.86.") || ip.startsWith("100.87.") ||
                ip.startsWith("100.88.") || ip.startsWith("100.89.") || ip.startsWith("100.90.") ||
                ip.startsWith("100.91.") || ip.startsWith("100.92.") || ip.startsWith("100.93.") ||
                ip.startsWith("100.94.") || ip.startsWith("100.95.") || ip.startsWith("100.96.") ||
                ip.startsWith("100.97.") || ip.startsWith("100.98.") || ip.startsWith("100.99.") ||
                ip.startsWith("100.100.") || ip.startsWith("100.101.") || ip.startsWith("100.102.") ||
                ip.startsWith("100.103.") || ip.startsWith("100.104.") || ip.startsWith("100.105.") ||
                ip.startsWith("100.106.") || ip.startsWith("100.107.") || ip.startsWith("100.108.") ||
                ip.startsWith("100.109.") || ip.startsWith("100.110.") || ip.startsWith("100.111.") ||
                ip.startsWith("100.112.") || ip.startsWith("100.113.") || ip.startsWith("100.114.") ||
                ip.startsWith("100.115.") || ip.startsWith("100.116.") || ip.startsWith("100.117.") ||
                ip.startsWith("100.118.") || ip.startsWith("100.119.") || ip.startsWith("100.120.") ||
                ip.startsWith("100.121.") || ip.startsWith("100.122.") || ip.startsWith("100.123.") ||
                ip.startsWith("100.124.") || ip.startsWith("100.125.") || ip.startsWith("100.126.") ||
                ip.startsWith("100.127.") ||
                ip.startsWith("192.168.") || ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") || ip.startsWith("172.19.") || ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") || ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") || ip.startsWith("172.25.") || ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") || ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") || ip.startsWith("172.31.");
    }

    /**
     * Notifies callbacks about connection readiness.
     */
    private void notifyConnectionReady(boolean success, String status, int port) {
        if (onConnectionReady != null) {
            onConnectionReady.accept(success, status);
        }
        if (onUPnPStatusChanged != null) {
            onUPnPStatusChanged.accept(success, status);
        }
    }

    /**
     * Connects to a host using a room code via the FREE relay service.
     * 
     * @param roomCode   The room code shared by the host
     * @param port       Unused (kept for compatibility)
     * @param playerName The client player's name
     * @return true if connection started
     */
    public boolean connectToHost(String roomCode, int port, String playerName) {
        this.role = Role.CLIENT;
        this.playerId = 2;
        this.playerName = playerName;
        this.usingRelay = true;

        setState(ConnectionState.CONNECTING);
        System.out.println("[NetworkService] Joining room: " + roomCode);

        // Reset relay service for fresh connection
        relayService.reset();

        // Join room using the FREE relay service
        relayService.joinRoom(roomCode).thenAccept(success -> {
            if (success) {
                System.out.println("[NetworkService] Joined room successfully!");
                setState(ConnectionState.CONNECTED);

                // Send connection message via relay
                send(NetworkMessage.connect(playerName));

            } else {
                handleError("Failed to join room. Check the code and try again.");
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

            case ARENA_LAYOUT:
                // Client receives arena layout from host
                if (role == Role.CLIENT) {
                    hostArenaLayout = message.parseArenaLayout();
                    System.out.println("[NetworkService] Received host arena layout: " +
                            (hostArenaLayout != null ? hostArenaLayout.getName() : "null"));
                }
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
        if (state == ConnectionState.DISCONNECTED)
            return;

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
     * 
     * @param message The message to send
     */
    public void send(NetworkMessage message) {
        String protocolStr = message.toProtocolString();

        if (usingRelay) {
            // Send via relay service
            relayService.send(protocolStr);
            System.out.println("[NetworkService] SENT via relay: " + protocolStr);
        } else if (writer != null && state == ConnectionState.CONNECTED) {
            // Send via direct socket
            writer.println(protocolStr);
            writer.flush();
            System.out.println("[NetworkService] SENT: " + protocolStr);
        } else {
            System.err.println("[NetworkService] Cannot send - not connected");
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
     * Sends arena layout to the client (host only).
     * This should be called before sendMatchStart to ensure the client
     * receives the layout before the battle starts.
     */
    public void sendArenaLayout(ArenaLayout layout) {
        if (role == Role.HOST && layout != null) {
            send(NetworkMessage.arenaLayout(layout));
            System.out.println("[NetworkService] Sent arena layout: " + layout.getName());
        }
    }

    /**
     * Gets the arena layout received from the host.
     * 
     * @return The host's arena layout, or null if not received or if this is the
     *         host
     */
    public ArenaLayout getHostArenaLayout() {
        return hostArenaLayout;
    }

    /**
     * Clears the stored host arena layout.
     */
    public void clearHostArenaLayout() {
        this.hostArenaLayout = null;
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

        // Close relay connection if active
        if (usingRelay) {
            System.out.println("[NetworkService] Closing relay connection...");
            relayService.disconnect();
            usingRelay = false;
        }

        // Close UPnP port mapping if we opened one
        if (upnpPortOpened && hostedPort > 0) {
            System.out.println("[NetworkService] Closing UPnP port mapping...");
            upnpService.closePort(hostedPort);
            upnpPortOpened = false;
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
        } catch (IOException e) {
            /* ignore */ }

        try {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        } catch (Exception e) {
            /* ignore */ }

        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            /* ignore */ }

        try {
            if (clientSocket != null) {
                clientSocket.close();
                clientSocket = null;
            }
        } catch (IOException e) {
            /* ignore */ }

        // Shutdown executor service
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = Executors.newCachedThreadPool(); // Create fresh one for next use
        }

        hostedPort = -1;
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
     * Gets the local IP address for LAN play.
     * This is the IP other devices on the same network can use.
     */
    public String getLocalIPAddress() {
        try {
            // Try to find a non-loopback, non-virtual network interface
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Skip loopback and down interfaces
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                    continue;
                }

                // Skip common virtual interface names
                String name = iface.getName().toLowerCase();
                if (name.contains("docker") || name.contains("veth") ||
                        name.contains("vmnet") || name.contains("vbox")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    // Prefer IPv4, non-loopback addresses
                    if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        // Prefer private network addresses
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") ||
                                ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
                                ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
                                ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
                                ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
                                ip.startsWith("172.24.") || ip.startsWith("172.25.") ||
                                ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
                                ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
                                ip.startsWith("172.30.") || ip.startsWith("172.31.")) {
                            return ip;
                        }
                    }
                }
            }

            // Fallback to getLocalHost
            return InetAddress.getLocalHost().getHostAddress();

        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // Cached public IP
    private String cachedPublicIP = null;
    private boolean publicIPFetched = false;

    /**
     * Gets the public IP address for internet play.
     * Uses external services to determine the public-facing IP.
     * Returns null if unable to determine (e.g., no internet connection).
     */
    public String getPublicIPAddress() {
        if (publicIPFetched) {
            return cachedPublicIP;
        }

        // Try multiple services for reliability
        String[] services = {
                "https://api.ipify.org",
                "https://checkip.amazonaws.com",
                "https://icanhazip.com",
                "https://ipinfo.io/ip"
        };

        for (String service : services) {
            try {
                URI uri = new URI(service);
                URL url = uri.toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestProperty("User-Agent", "KU-Royale/1.0");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String ip = reader.readLine().trim();
                    reader.close();
                    conn.disconnect();

                    // Validate IP format
                    if (ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                        cachedPublicIP = ip;
                        publicIPFetched = true;
                        return ip;
                    }
                }
                conn.disconnect();

            } catch (Exception e) {
                // Try next service
                System.out.println("[NetworkService] Failed to get public IP from " + service + ": " + e.getMessage());
            }
        }

        publicIPFetched = true;
        cachedPublicIP = null;
        return null;
    }

    /**
     * Fetches the public IP address asynchronously.
     * 
     * @param callback Called with the public IP when found, or null if not
     *                 available
     */
    public void fetchPublicIPAsync(Consumer<String> callback) {
        executorService.submit(() -> {
            String publicIP = getPublicIPAddress();
            if (callback != null) {
                callback.accept(publicIP);
            }
        });
    }

    /**
     * Gets connection info string for display.
     */
    public String getConnectionInfoString(int port) {
        StringBuilder sb = new StringBuilder();
        sb.append("Local Network IP: ").append(getLocalIPAddress()).append("\n");

        String publicIP = cachedPublicIP;
        if (publicIP != null) {
            sb.append("Public IP (Internet): ").append(publicIP).append("\n");
        } else {
            sb.append("Public IP: Fetching...\n");
        }

        sb.append("Port: ").append(port);

        return sb.toString();
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

    /**
     * Sets callback for UPnP status changes.
     * 
     * @param callback BiConsumer with (success, message)
     */
    public void setOnUPnPStatusChanged(BiConsumer<Boolean, String> callback) {
        this.onUPnPStatusChanged = callback;
    }

    /**
     * Sets callback for when connection is ready (ngrok tunnel created or UPnP port
     * opened).
     * 
     * @param callback BiConsumer with (success, shareableAddress or status message)
     */
    public void setOnConnectionReady(BiConsumer<Boolean, String> callback) {
        this.onConnectionReady = callback;
    }

    // Relay related getters

    /**
     * Gets the relay service.
     */
    public RelayService getRelayService() {
        return relayService;
    }

    /**
     * Checks if using relay for communication.
     */
    public boolean isUsingRelay() {
        return usingRelay;
    }

    /**
     * Gets the current room code.
     */
    public String getRoomCode() {
        return relayService.getRoomCode();
    }

    // UPnP related getters

    /**
     * Checks if UPnP is available on this network.
     */
    public boolean isUPnPAvailable() {
        return upnpService.isUPnPAvailable();
    }

    /**
     * Checks if a port was successfully opened via UPnP.
     */
    public boolean isUPnPPortOpened() {
        return upnpPortOpened;
    }

    /**
     * Gets the shareable room code for multiplayer.
     * 
     * @return Room code like "ABC123" or null
     */
    public String getShareableConnectionString() {
        if (usingRelay) {
            return relayService.getRoomCode();
        }
        return null;
    }

    /**
     * Gets the UPnP service instance for advanced usage.
     */
    public UPnPService getUPnPService() {
        return upnpService;
    }
}
