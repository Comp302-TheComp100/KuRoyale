package com.kuroyale.util;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Loads and provides network configuration settings.
 * Implements Singleton pattern for global access.
 * Uses GRASP Information Expert - knows how to load and provide its own settings.
 */
public class NetworkConfig {
    private static NetworkConfig instance;
    
    // Default values
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_RECONNECT_ATTEMPTS = 3;
    private static final int DEFAULT_SYNC_INTERVAL = 100;
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 1024;
    private static final int DEFAULT_RECONNECT_WAIT = 1000;
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 2000;
    
    // Configuration values
    private int defaultPort;
    private int connectionTimeout;
    private int reconnectAttempts;
    private int syncInterval;
    private int maxMessageSize;
    private int reconnectWait;
    private int heartbeatInterval;
    
    private static final String CONFIG_FILE_NAME = "network_config.txt";
    
    private NetworkConfig() {
        loadConfiguration();
    }
    
    /**
     * Gets the singleton instance of NetworkConfig.
     * @return The NetworkConfig instance
     */
    public static synchronized NetworkConfig getInstance() {
        if (instance == null) {
            instance = new NetworkConfig();
        }
        return instance;
    }
    
    /**
     * Reloads configuration from file.
     */
    public void reload() {
        loadConfiguration();
    }
    
    private void loadConfiguration() {
        // Set defaults first
        setDefaults();
        
        // Try to load from resources
        try (InputStream is = getClass().getResourceAsStream("/" + CONFIG_FILE_NAME)) {
            if (is != null) {
                loadFromStream(is);
                System.out.println("[NetworkConfig] Loaded configuration from resources");
                return;
            }
        } catch (IOException e) {
            System.err.println("[NetworkConfig] Error loading from resources: " + e.getMessage());
        }
        
        // Try to load from user home directory
        Path userConfigPath = Paths.get(System.getProperty("user.home"), ".kuroyale", CONFIG_FILE_NAME);
        if (Files.exists(userConfigPath)) {
            try (InputStream is = Files.newInputStream(userConfigPath)) {
                loadFromStream(is);
                System.out.println("[NetworkConfig] Loaded configuration from: " + userConfigPath);
                return;
            } catch (IOException e) {
                System.err.println("[NetworkConfig] Error loading from user home: " + e.getMessage());
            }
        }
        
        System.out.println("[NetworkConfig] Using default configuration values");
    }
    
    private void setDefaults() {
        this.defaultPort = DEFAULT_PORT;
        this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        this.reconnectAttempts = DEFAULT_RECONNECT_ATTEMPTS;
        this.syncInterval = DEFAULT_SYNC_INTERVAL;
        this.maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
        this.reconnectWait = DEFAULT_RECONNECT_WAIT;
        this.heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
    }
    
    private void loadFromStream(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);
        
        this.defaultPort = parseIntWithDefault(props, "DEFAULT_PORT", DEFAULT_PORT, 1024, 65535);
        this.connectionTimeout = parseIntWithDefault(props, "CONNECTION_TIMEOUT", DEFAULT_CONNECTION_TIMEOUT, 1000, 30000);
        this.reconnectAttempts = parseIntWithDefault(props, "RECONNECT_ATTEMPTS", DEFAULT_RECONNECT_ATTEMPTS, 1, 10);
        this.syncInterval = parseIntWithDefault(props, "SYNC_INTERVAL", DEFAULT_SYNC_INTERVAL, 50, 500);
        this.maxMessageSize = parseIntWithDefault(props, "MAX_MESSAGE_SIZE", DEFAULT_MAX_MESSAGE_SIZE, 512, 4096);
        this.reconnectWait = parseIntWithDefault(props, "RECONNECT_WAIT", DEFAULT_RECONNECT_WAIT, 500, 5000);
        this.heartbeatInterval = parseIntWithDefault(props, "HEARTBEAT_INTERVAL", DEFAULT_HEARTBEAT_INTERVAL, 500, 5000);
    }
    
    private int parseIntWithDefault(Properties props, String key, int defaultValue, int min, int max) {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < min || parsed > max) {
                System.err.println("[NetworkConfig] " + key + " out of range (" + min + "-" + max + "), using default: " + defaultValue);
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException e) {
            System.err.println("[NetworkConfig] Invalid " + key + " value, using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    // Getters
    public int getDefaultPort() {
        return defaultPort;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
    
    public int getSyncInterval() {
        return syncInterval;
    }
    
    public int getMaxMessageSize() {
        return maxMessageSize;
    }
    
    public int getReconnectWait() {
        return reconnectWait;
    }
    
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    @Override
    public String toString() {
        return String.format("NetworkConfig{port=%d, timeout=%d, reconnectAttempts=%d, syncInterval=%d}",
                defaultPort, connectionTimeout, reconnectAttempts, syncInterval);
    }
}

