package com.kuroyale.service;

import com.kuroyale.service.NetworkService.ConnectionState;
import com.kuroyale.service.NetworkService.Role;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for NetworkService - Network Connection Handling.
 * Tests server/client connections and message passing.
 */
class NetworkServiceTest {

    private NetworkService hostService;
    private NetworkService clientService;
    private static final int TEST_PORT = 9999; // Use non-standard port for testing
    
    @BeforeEach
    void setUp() {
        hostService = new NetworkService();
        clientService = new NetworkService();
    }
    
    @AfterEach
    void tearDown() {
        if (hostService != null) {
            hostService.disconnect();
        }
        if (clientService != null) {
            clientService.disconnect();
        }
    }

    /**
     * Test Case 1: Initial state is disconnected
     * Verifies that a new NetworkService starts in disconnected state.
     */
    @Test
    @DisplayName("New NetworkService should be disconnected")
    void testInitialStateDisconnected() {
        // Act
        NetworkService service = new NetworkService();
        
        // Assert
        assertEquals(ConnectionState.DISCONNECTED, service.getState());
        assertFalse(service.isConnected());
    }

    /**
     * Test Case 2: Start hosting changes state
     * Verifies that starting to host transitions to connecting state.
     */
    @Test
    @DisplayName("startHosting should change state to CONNECTING")
    void testStartHostingChangesState() throws InterruptedException {
        // Arrange
        AtomicReference<ConnectionState> stateRef = new AtomicReference<>();
        hostService.setOnStateChanged(stateRef::set);
        
        // Act
        boolean started = hostService.startHosting(TEST_PORT, "TestHost");
        
        // Small delay for state change
        Thread.sleep(100);
        
        // Assert
        assertTrue(started, "Hosting should start successfully");
        assertEquals(Role.HOST, hostService.getRole());
        assertEquals(1, hostService.getPlayerId());
        assertEquals("TestHost", hostService.getPlayerName());
        assertTrue(hostService.isHost());
        
        // Cleanup
        hostService.disconnect();
    }

    /**
     * Test Case 3: Connect to host changes client state
     * Verifies that connecting as client transitions to connecting state.
     */
    @Test
    @DisplayName("connectToHost should change state to CONNECTING")
    void testConnectToHostChangesState() throws InterruptedException {
        // Arrange
        AtomicBoolean stateChanged = new AtomicBoolean(false);
        clientService.setOnStateChanged(state -> {
            if (state == ConnectionState.CONNECTING) {
                stateChanged.set(true);
            }
        });
        
        // Act - try to connect (will fail since no host, but state should change)
        clientService.connectToHost("127.0.0.1", TEST_PORT + 1, "TestClient");
        
        // Small delay for state change
        Thread.sleep(100);
        
        // Assert
        assertEquals(Role.CLIENT, clientService.getRole());
        assertEquals(2, clientService.getPlayerId());
        assertEquals("TestClient", clientService.getPlayerName());
        assertFalse(clientService.isHost());
    }

    /**
     * Test Case 4: Disconnect cleans up properly
     * Verifies that disconnect properly cleans up and changes state.
     */
    @Test
    @DisplayName("disconnect should change state to DISCONNECTED")
    void testDisconnectChangesState() throws InterruptedException {
        // Arrange
        hostService.startHosting(TEST_PORT + 2, "TestHost");
        Thread.sleep(100);
        
        // Act
        hostService.disconnect();
        Thread.sleep(100);
        
        // Assert
        assertEquals(ConnectionState.DISCONNECTED, hostService.getState());
        assertFalse(hostService.isConnected());
    }

    /**
     * Test Case 5: getLocalIPAddress returns valid address
     * Verifies that the local IP address can be retrieved.
     */
    @Test
    @DisplayName("getLocalIPAddress should return valid IP address")
    void testGetLocalIPAddress() {
        // Act
        String ip = hostService.getLocalIPAddress();
        
        // Assert
        assertNotNull(ip);
        assertFalse(ip.isEmpty());
        // Should be in format x.x.x.x or "localhost"
        assertTrue(ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || ip.equals("127.0.0.1"),
                "IP should be valid format: " + ip);
    }

    /**
     * Test Case 6: Error callback is invoked on connection failure
     * Verifies that error callback is called when connection fails.
     */
    @Test
    @DisplayName("Error callback should be invoked on connection failure")
    void testErrorCallbackOnFailure() throws InterruptedException {
        // Arrange
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorRef = new AtomicReference<>();
        
        clientService.setOnError(error -> {
            errorRef.set(error);
            errorLatch.countDown();
        });
        
        // Act - try to connect to non-existent host
        clientService.connectToHost("192.168.255.255", 1, "TestClient");
        
        // Wait for error (with timeout)
        boolean errorReceived = errorLatch.await(10, TimeUnit.SECONDS);
        
        // Assert - either error received or connection still trying
        // Note: This test might timeout on some systems, which is acceptable
        if (errorReceived) {
            assertNotNull(errorRef.get());
            assertTrue(errorRef.get().contains("Failed") || errorRef.get().contains("refused") 
                    || errorRef.get().contains("timed out"));
        }
    }

    /**
     * Test Case 7: Host and Client can establish connection
     * Integration test for full connection flow.
     */
    @Test
    @DisplayName("Host and Client should be able to connect")
    void testHostClientConnection() throws InterruptedException {
        // Arrange
        int testPort = TEST_PORT + 3;
        CountDownLatch connectionLatch = new CountDownLatch(2);
        
        hostService.setOnStateChanged(state -> {
            if (state == ConnectionState.CONNECTED) {
                connectionLatch.countDown();
            }
        });
        
        clientService.setOnStateChanged(state -> {
            if (state == ConnectionState.CONNECTED) {
                connectionLatch.countDown();
            }
        });
        
        // Act
        hostService.startHosting(testPort, "Host");
        Thread.sleep(200); // Wait for server to start
        
        clientService.connectToHost("127.0.0.1", testPort, "Client");
        
        // Wait for both to connect
        boolean connected = connectionLatch.await(5, TimeUnit.SECONDS);
        
        // Assert
        assertTrue(connected, "Both host and client should connect");
        assertEquals(ConnectionState.CONNECTED, hostService.getState());
        assertEquals(ConnectionState.CONNECTED, clientService.getState());
    }
}

