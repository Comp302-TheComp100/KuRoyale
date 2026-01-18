package com.kuroyale.service.network;

/**
 * PLAYER INPUT
 * 
 * Represents an input intent from a player (e.g., deploying a card).
 * This is a simple data structure that can be serialized over the network.
 * 
 * Key fields:
 * - sequence: Monotonically increasing ID for ordering and dedup
 * - playerId: Which player sent this (1 or 2)
 * - type: What kind of input (CARD_DEPLOY, etc.)
 * - cardName, x, y: Payload for card deployment
 * - clientTime: Optional client timestamp for latency measurement
 */
public class PlayerInput {
    
    public enum InputType {
        CARD_DEPLOY,    // Deploy a card at a position
        EMOTE,          // Send an emote (future)
        PAUSE_REQUEST,  // Request to pause (future)
        FORFEIT         // Forfeit the match
    }
    
    private final long sequence;        // Monotonically increasing sequence number
    private final int playerId;         // 1 = host, 2 = client
    private final InputType type;
    private final String cardName;      // For CARD_DEPLOY
    private final int x, y;             // Grid position for CARD_DEPLOY
    private final long clientTime;      // Client timestamp (for latency measurement)
    private final long serverReceiveTime; // When server received this
    
    // Private constructor - use factory methods
    private PlayerInput(long sequence, int playerId, InputType type, 
                       String cardName, int x, int y, long clientTime) {
        this.sequence = sequence;
        this.playerId = playerId;
        this.type = type;
        this.cardName = cardName;
        this.x = x;
        this.y = y;
        this.clientTime = clientTime;
        this.serverReceiveTime = System.currentTimeMillis();
    }
    
    // ==================== Factory Methods ====================
    
    /**
     * Creates a card deploy input.
     */
    public static PlayerInput cardDeploy(long sequence, int playerId, String cardName, int x, int y) {
        return new PlayerInput(sequence, playerId, InputType.CARD_DEPLOY, cardName, x, y, 0);
    }
    
    /**
     * Creates a card deploy input with client timestamp.
     */
    public static PlayerInput cardDeploy(long sequence, int playerId, String cardName, int x, int y, long clientTime) {
        return new PlayerInput(sequence, playerId, InputType.CARD_DEPLOY, cardName, x, y, clientTime);
    }
    
    /**
     * Creates a forfeit input.
     */
    public static PlayerInput forfeit(long sequence, int playerId) {
        return new PlayerInput(sequence, playerId, InputType.FORFEIT, null, 0, 0, 0);
    }
    
    // ==================== Serialization ====================
    
    /**
     * Serializes to network format.
     * Format: seq:playerId:type:cardName:x:y:clientTime
     */
    public String serialize() {
        return sequence + ":" + playerId + ":" + type.name() + ":" + 
               (cardName != null ? cardName : "") + ":" + x + ":" + y + ":" + clientTime;
    }
    
    /**
     * Deserializes from network format.
     */
    public static PlayerInput deserialize(String data) {
        if (data == null || data.isEmpty()) return null;
        
        try {
            String[] parts = data.split(":", -1);
            if (parts.length < 7) return null;
            
            long sequence = Long.parseLong(parts[0]);
            int playerId = Integer.parseInt(parts[1]);
            InputType type = InputType.valueOf(parts[2]);
            String cardName = parts[3].isEmpty() ? null : parts[3];
            int x = Integer.parseInt(parts[4]);
            int y = Integer.parseInt(parts[5]);
            long clientTime = Long.parseLong(parts[6]);
            
            return new PlayerInput(sequence, playerId, type, cardName, x, y, clientTime);
        } catch (Exception e) {
            System.err.println("[PlayerInput] Failed to deserialize: " + data);
            return null;
        }
    }
    
    // ==================== Getters ====================
    
    public long getSequence() { return sequence; }
    public int getPlayerId() { return playerId; }
    public InputType getType() { return type; }
    public String getCardName() { return cardName; }
    public int getX() { return x; }
    public int getY() { return y; }
    public long getClientTime() { return clientTime; }
    public long getServerReceiveTime() { return serverReceiveTime; }
    
    /**
     * Calculates latency (time from client send to server receive).
     * Only meaningful if clientTime was set.
     */
    public long getLatencyMs() {
        if (clientTime <= 0) return -1;
        return serverReceiveTime - clientTime;
    }
    
    @Override
    public String toString() {
        return "PlayerInput{seq=" + sequence + ", player=" + playerId + 
               ", type=" + type + ", card=" + cardName + ", pos=(" + x + "," + y + ")}";
    }
}
