package com.kuroyale.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NetworkConfig - Network Configuration Loading.
 * Tests configuration loading and default value handling.
 */
class NetworkConfigTest {

    private NetworkConfig config;
    
    @BeforeEach
    void setUp() {
        config = NetworkConfig.getInstance();
    }

    /**
     * Test Case 1: Singleton pattern works correctly
     * Verifies that getInstance always returns the same instance.
     */
    @Test
    @DisplayName("NetworkConfig should implement Singleton pattern")
    void testSingletonPattern() {
        // Act
        NetworkConfig instance1 = NetworkConfig.getInstance();
        NetworkConfig instance2 = NetworkConfig.getInstance();
        
        // Assert
        assertNotNull(instance1);
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    /**
     * Test Case 2: Default port is valid
     * Verifies that the default port is within acceptable range.
     */
    @Test
    @DisplayName("Default port should be in valid range")
    void testDefaultPortValid() {
        // Act
        int port = config.getDefaultPort();
        
        // Assert
        assertTrue(port >= 1024 && port <= 65535, 
                "Port should be between 1024 and 65535, was: " + port);
    }

    /**
     * Test Case 3: Connection timeout is reasonable
     * Verifies that connection timeout is within acceptable bounds.
     */
    @Test
    @DisplayName("Connection timeout should be in valid range")
    void testConnectionTimeoutValid() {
        // Act
        int timeout = config.getConnectionTimeout();
        
        // Assert
        assertTrue(timeout >= 1000 && timeout <= 30000,
                "Timeout should be between 1000 and 30000ms, was: " + timeout);
    }

    /**
     * Test Case 4: Reconnect attempts is reasonable
     * Verifies that reconnect attempts is within acceptable bounds.
     */
    @Test
    @DisplayName("Reconnect attempts should be in valid range")
    void testReconnectAttemptsValid() {
        // Act
        int attempts = config.getReconnectAttempts();
        
        // Assert
        assertTrue(attempts >= 1 && attempts <= 10,
                "Reconnect attempts should be between 1 and 10, was: " + attempts);
    }

    /**
     * Test Case 5: Sync interval is reasonable
     * Verifies that sync interval allows for smooth gameplay.
     */
    @Test
    @DisplayName("Sync interval should be in valid range for smooth gameplay")
    void testSyncIntervalValid() {
        // Act
        int interval = config.getSyncInterval();
        
        // Assert
        assertTrue(interval >= 50 && interval <= 500,
                "Sync interval should be between 50 and 500ms, was: " + interval);
    }

    /**
     * Test Case 6: All configuration values are initialized
     * Verifies that no configuration value is left uninitialized.
     */
    @Test
    @DisplayName("All configuration values should be initialized")
    void testAllValuesInitialized() {
        // Assert - all values should be positive (default or loaded)
        assertTrue(config.getDefaultPort() > 0, "Port should be positive");
        assertTrue(config.getConnectionTimeout() > 0, "Timeout should be positive");
        assertTrue(config.getReconnectAttempts() > 0, "Reconnect attempts should be positive");
        assertTrue(config.getSyncInterval() > 0, "Sync interval should be positive");
        assertTrue(config.getMaxMessageSize() > 0, "Max message size should be positive");
        assertTrue(config.getReconnectWait() > 0, "Reconnect wait should be positive");
        assertTrue(config.getHeartbeatInterval() > 0, "Heartbeat interval should be positive");
    }

    /**
     * Test Case 7: toString provides useful information
     * Verifies that toString returns a meaningful string.
     */
    @Test
    @DisplayName("toString should return meaningful configuration summary")
    void testToString() {
        // Act
        String str = config.toString();
        
        // Assert
        assertNotNull(str);
        assertTrue(str.contains("NetworkConfig"));
        assertTrue(str.contains("port"));
        assertTrue(str.contains("timeout"));
    }

    /**
     * Test Case 8: Max message size is sufficient for game messages
     * Verifies that max message size can accommodate typical game messages.
     */
    @Test
    @DisplayName("Max message size should be sufficient for typical messages")
    void testMaxMessageSizeSufficient() {
        // A typical long message might be:
        // CARD_PLACED|1|ElixirCollector|12.345678,28.901234|59:59
        // This is about 60 characters
        
        int maxSize = config.getMaxMessageSize();
        
        // Should accommodate at least 512 bytes for safety
        assertTrue(maxSize >= 512, "Max message size should be at least 512 bytes");
    }
}

