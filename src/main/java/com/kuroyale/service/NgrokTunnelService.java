package com.kuroyale.service;

import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Tunnel;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Service for creating ngrok tunnels to enable internet multiplayer.
 * This allows ANY two players to connect regardless of their network setup.
 * 
 * How it works:
 * 1. Host starts the game server locally
 * 2. NgrokTunnelService creates a public tunnel to that port
 * 3. Host shares the public URL (e.g., tcp://0.tcp.ngrok.io:12345)
 * 4. Friend connects using that URL
 * 5. ngrok routes the traffic - works through ANY firewall/NAT
 * 
 * First-time setup:
 * - User needs a free ngrok account (ngrok.com)
 * - User enters their auth token once (saved for future use)
 */
public class NgrokTunnelService {
    
    private static NgrokTunnelService instance;
    
    private NgrokClient ngrokClient;
    private Tunnel activeTunnel;
    private boolean initialized = false;
    private String authToken;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Path configPath;
    
    // Callbacks
    private Consumer<String> onTunnelCreated;
    private Consumer<String> onError;
    private Consumer<String> onStatusUpdate;
    
    private NgrokTunnelService() {
        // Store config in user's home directory
        configPath = Paths.get(System.getProperty("user.home"), ".kuroyale", "ngrok_config.txt");
        loadAuthToken();
    }
    
    public static synchronized NgrokTunnelService getInstance() {
        if (instance == null) {
            instance = new NgrokTunnelService();
        }
        return instance;
    }
    
    /**
     * Loads the auth token from saved config.
     */
    private void loadAuthToken() {
        try {
            if (Files.exists(configPath)) {
                authToken = Files.readString(configPath).trim();
                if (!authToken.isEmpty()) {
                    System.out.println("[Ngrok] Auth token loaded from config");
                }
            }
        } catch (IOException e) {
            System.err.println("[Ngrok] Failed to load auth token: " + e.getMessage());
        }
    }
    
    /**
     * Saves the auth token for future use.
     */
    public void saveAuthToken(String token) {
        this.authToken = token;
        try {
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, token);
            System.out.println("[Ngrok] Auth token saved");
        } catch (IOException e) {
            System.err.println("[Ngrok] Failed to save auth token: " + e.getMessage());
        }
    }
    
    /**
     * Checks if an auth token is configured.
     */
    public boolean hasAuthToken() {
        return authToken != null && !authToken.isEmpty();
    }
    
    /**
     * Gets the current auth token.
     */
    public String getAuthToken() {
        return authToken;
    }
    
    /**
     * Initializes the ngrok client.
     * @return CompletableFuture that completes when ready
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            if (initialized && ngrokClient != null) {
                return true;
            }
            
            try {
                updateStatus("Initializing ngrok...");
                
                JavaNgrokConfig.Builder configBuilder = new JavaNgrokConfig.Builder();
                
                if (hasAuthToken()) {
                    configBuilder.withAuthToken(authToken);
                }
                
                ngrokClient = new NgrokClient.Builder()
                        .withJavaNgrokConfig(configBuilder.build())
                        .build();
                
                initialized = true;
                updateStatus("Ngrok ready");
                System.out.println("[Ngrok] Client initialized successfully");
                return true;
                
            } catch (Exception e) {
                String error = "Failed to initialize ngrok: " + e.getMessage();
                System.err.println("[Ngrok] " + error);
                handleError(error);
                return false;
            }
        }, executor);
    }
    
    /**
     * Creates a TCP tunnel to the specified port.
     * @param port The local port to tunnel
     * @return CompletableFuture with the public URL
     */
    public CompletableFuture<String> createTunnel(int port) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Initialize if needed
                if (!initialized || ngrokClient == null) {
                    updateStatus("Setting up ngrok...");
                    
                    JavaNgrokConfig.Builder configBuilder = new JavaNgrokConfig.Builder();
                    if (hasAuthToken()) {
                        configBuilder.withAuthToken(authToken);
                    }
                    
                    ngrokClient = new NgrokClient.Builder()
                            .withJavaNgrokConfig(configBuilder.build())
                            .build();
                    initialized = true;
                }
                
                // Close existing tunnel if any
                if (activeTunnel != null) {
                    closeTunnel();
                }
                
                updateStatus("Creating tunnel to port " + port + "...");
                System.out.println("[Ngrok] Creating TCP tunnel to port " + port);
                
                // Create TCP tunnel
                CreateTunnel createTunnel = new CreateTunnel.Builder()
                        .withProto(Proto.TCP)
                        .withAddr(port)
                        .withName("kuroyale-game")
                        .build();
                
                activeTunnel = ngrokClient.connect(createTunnel);
                
                String publicUrl = activeTunnel.getPublicUrl();
                System.out.println("[Ngrok] Tunnel created: " + publicUrl);
                updateStatus("Tunnel active!");
                
                if (onTunnelCreated != null) {
                    onTunnelCreated.accept(publicUrl);
                }
                
                return publicUrl;
                
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                System.err.println("[Ngrok] Failed to create tunnel: " + errorMsg);
                
                // Check for common errors and provide helpful messages
                if (errorMsg != null && errorMsg.contains("authtoken")) {
                    handleError("AUTH_TOKEN_REQUIRED");
                } else if (errorMsg != null && errorMsg.contains("ERR_NGROK")) {
                    handleError("Ngrok error: " + errorMsg);
                } else {
                    handleError("Failed to create tunnel: " + errorMsg);
                }
                
                return null;
            }
        }, executor);
    }
    
    /**
     * Closes the active tunnel.
     */
    public void closeTunnel() {
        if (activeTunnel != null && ngrokClient != null) {
            try {
                System.out.println("[Ngrok] Closing tunnel...");
                ngrokClient.disconnect(activeTunnel.getPublicUrl());
                activeTunnel = null;
                updateStatus("Tunnel closed");
            } catch (Exception e) {
                System.err.println("[Ngrok] Error closing tunnel: " + e.getMessage());
            }
        }
    }
    
    /**
     * Shuts down the ngrok client completely.
     */
    public void shutdown() {
        closeTunnel();
        if (ngrokClient != null) {
            try {
                ngrokClient.kill();
            } catch (Exception e) {
                // Ignore
            }
            ngrokClient = null;
        }
        initialized = false;
        executor.shutdown();
    }
    
    /**
     * Gets the public URL of the active tunnel.
     */
    public String getPublicUrl() {
        return activeTunnel != null ? activeTunnel.getPublicUrl() : null;
    }
    
    /**
     * Checks if a tunnel is currently active.
     */
    public boolean isTunnelActive() {
        return activeTunnel != null;
    }
    
    /**
     * Parses a ngrok URL into host and port.
     * @param ngrokUrl URL like "tcp://0.tcp.ngrok.io:12345"
     * @return String array with [host, port] or null if invalid
     */
    public static String[] parseNgrokUrl(String ngrokUrl) {
        if (ngrokUrl == null) return null;
        
        // Remove protocol prefix
        String url = ngrokUrl.replace("tcp://", "").replace("http://", "").replace("https://", "");
        
        // Split host and port
        String[] parts = url.split(":");
        if (parts.length == 2) {
            return parts;
        }
        
        return null;
    }
    
    /**
     * Formats the public URL for display (removes tcp:// prefix).
     */
    public String getShareableAddress() {
        if (activeTunnel == null) return null;
        return activeTunnel.getPublicUrl().replace("tcp://", "");
    }
    
    // Callback setters
    
    public void setOnTunnelCreated(Consumer<String> callback) {
        this.onTunnelCreated = callback;
    }
    
    public void setOnError(Consumer<String> callback) {
        this.onError = callback;
    }
    
    public void setOnStatusUpdate(Consumer<String> callback) {
        this.onStatusUpdate = callback;
    }
    
    private void handleError(String error) {
        if (onError != null) {
            onError.accept(error);
        }
    }
    
    private void updateStatus(String status) {
        System.out.println("[Ngrok] " + status);
        if (onStatusUpdate != null) {
            onStatusUpdate.accept(status);
        }
    }
}
