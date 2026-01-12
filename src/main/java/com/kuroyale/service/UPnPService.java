package com.kuroyale.service;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for automatic port forwarding using UPnP (Universal Plug and Play).
 * This allows the game to work over the internet without manual router configuration.
 * 
 * UPnP Support:
 * - Most modern home routers support UPnP
 * - Automatically opens ports on the router when hosting
 * - Automatically closes ports when done
 * - Falls back gracefully if UPnP is not available
 */
public class UPnPService {
    
    private static UPnPService instance;
    
    private GatewayDevice gateway;
    private boolean upnpAvailable = false;
    private boolean initialized = false;
    private String externalIP;
    private String localIP;
    
    private int mappedPort = -1;
    private static final String PORT_MAPPING_DESCRIPTION = "KU Royale Game";
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    
    private UPnPService() {
        // Private constructor for singleton
    }
    
    public static synchronized UPnPService getInstance() {
        if (instance == null) {
            instance = new UPnPService();
        }
        return instance;
    }
    
    /**
     * Initializes UPnP by discovering the gateway device.
     * This should be called early in the application lifecycle.
     * @return CompletableFuture that completes when discovery is done
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            if (initialized) {
                return upnpAvailable;
            }
            
            try {
                System.out.println("[UPnP] Discovering gateway devices...");
                
                GatewayDiscover discover = new GatewayDiscover();
                discover.setTimeout(5000); // 5 second timeout
                
                Map<InetAddress, GatewayDevice> gateways = discover.discover();
                
                if (gateways == null || gateways.isEmpty()) {
                    System.out.println("[UPnP] No UPnP gateway devices found");
                    initialized = true;
                    upnpAvailable = false;
                    return false;
                }
                
                gateway = discover.getValidGateway();
                
                if (gateway == null) {
                    System.out.println("[UPnP] No valid gateway found");
                    initialized = true;
                    upnpAvailable = false;
                    return false;
                }
                
                localIP = gateway.getLocalAddress().getHostAddress();
                externalIP = gateway.getExternalIPAddress();
                
                System.out.println("[UPnP] Gateway found: " + gateway.getFriendlyName());
                System.out.println("[UPnP] Local IP: " + localIP);
                System.out.println("[UPnP] External IP: " + externalIP);
                
                initialized = true;
                upnpAvailable = true;
                return true;
                
            } catch (Exception e) {
                System.err.println("[UPnP] Error during discovery: " + e.getMessage());
                initialized = true;
                upnpAvailable = false;
                return false;
            }
        }, executor);
    }
    
    /**
     * Opens a port on the router for incoming connections.
     * @param port The port to open
     * @return CompletableFuture with true if successful, false otherwise
     */
    public CompletableFuture<Boolean> openPort(int port) {
        return CompletableFuture.supplyAsync(() -> {
            if (!upnpAvailable || gateway == null) {
                System.out.println("[UPnP] UPnP not available, cannot open port");
                return false;
            }
            
            try {
                // Check if port is already mapped
                PortMappingEntry portMapping = new PortMappingEntry();
                if (gateway.getSpecificPortMappingEntry(port, "TCP", portMapping)) {
                    // Port already mapped - check if it's ours
                    if (PORT_MAPPING_DESCRIPTION.equals(portMapping.getPortMappingDescription())) {
                        System.out.println("[UPnP] Port " + port + " already mapped by us");
                        mappedPort = port;
                        return true;
                    } else {
                        System.out.println("[UPnP] Port " + port + " already mapped by: " + 
                                portMapping.getPortMappingDescription());
                        return false;
                    }
                }
                
                // Add port mapping
                boolean success = gateway.addPortMapping(
                        port,                    // External port
                        port,                    // Internal port
                        localIP,                 // Internal IP
                        "TCP",                   // Protocol
                        PORT_MAPPING_DESCRIPTION // Description
                );
                
                if (success) {
                    mappedPort = port;
                    System.out.println("[UPnP] Successfully opened port " + port);
                    System.out.println("[UPnP] External address: " + externalIP + ":" + port);
                    return true;
                } else {
                    System.err.println("[UPnP] Failed to open port " + port);
                    return false;
                }
                
            } catch (Exception e) {
                System.err.println("[UPnP] Error opening port: " + e.getMessage());
                return false;
            }
        }, executor);
    }
    
    /**
     * Closes a previously opened port.
     * @param port The port to close
     * @return CompletableFuture with true if successful
     */
    public CompletableFuture<Boolean> closePort(int port) {
        return CompletableFuture.supplyAsync(() -> {
            if (!upnpAvailable || gateway == null) {
                return false;
            }
            
            try {
                boolean success = gateway.deletePortMapping(port, "TCP");
                if (success) {
                    System.out.println("[UPnP] Successfully closed port " + port);
                    if (mappedPort == port) {
                        mappedPort = -1;
                    }
                }
                return success;
            } catch (Exception e) {
                System.err.println("[UPnP] Error closing port: " + e.getMessage());
                return false;
            }
        }, executor);
    }
    
    /**
     * Closes the currently mapped port if any.
     */
    public void closeCurrentPort() {
        if (mappedPort > 0) {
            closePort(mappedPort);
        }
    }
    
    /**
     * Gets the external (public) IP address.
     * @return The external IP, or null if not available
     */
    public String getExternalIP() {
        return externalIP;
    }
    
    /**
     * Gets the local IP address.
     * @return The local IP, or null if not available
     */
    public String getLocalIP() {
        return localIP;
    }
    
    /**
     * Checks if UPnP is available on this network.
     * @return true if UPnP is available
     */
    public boolean isUPnPAvailable() {
        return upnpAvailable;
    }
    
    /**
     * Checks if initialization is complete.
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the currently mapped port.
     * @return The mapped port, or -1 if none
     */
    public int getMappedPort() {
        return mappedPort;
    }
    
    /**
     * Gets the gateway device name.
     * @return The gateway name, or null if not available
     */
    public String getGatewayName() {
        return gateway != null ? gateway.getFriendlyName() : null;
    }
    
    /**
     * Shuts down the service and closes any open ports.
     */
    public void shutdown() {
        closeCurrentPort();
        executor.shutdown();
    }
    
    /**
     * Gets a connection string that can be shared with other players.
     * @param port The port being used
     * @return A string like "123.45.67.89:8080" or null if not available
     */
    public String getConnectionString(int port) {
        if (externalIP != null) {
            return externalIP + ":" + port;
        }
        return null;
    }
}
