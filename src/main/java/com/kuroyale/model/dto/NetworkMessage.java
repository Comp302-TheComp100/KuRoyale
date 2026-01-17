package com.kuroyale.model.dto;

import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.enums.NetworkMessageType;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Represents a message sent over the network between players.
 * Implements the network protocol format: MESSAGE_TYPE|player_id|data|timestamp
 * Data fields use ';' as internal delimiter to avoid conflict with '|'.
 * 
 * Examples:
 * - CARD_PLACED|1|Knight;5.2,3.8|00:45
 * - TOWER_DAMAGED|2|CrownLeft;450|01:23
 * - ELIXIR_UPDATE|1|7|01:24
 */
public class NetworkMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DELIMITER = "|";
    private static final String DATA_DELIMITER = ";";  // Use different delimiter for data fields
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("mm:ss");
    
    private final NetworkMessageType type;
    private final int playerId;
    private final String data;
    private final String timestamp;
    
    /**
     * Creates a new network message.
     * @param type The message type
     * @param playerId The player ID (1 for host, 2 for client)
     * @param data The message data (format depends on message type)
     */
    public NetworkMessage(NetworkMessageType type, int playerId, String data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = LocalTime.now().format(TIME_FORMAT);
    }
    
    /**
     * Creates a network message from a protocol string.
     * @param protocolString The string in format MESSAGE_TYPE|player_id|data|timestamp
     * @return The parsed NetworkMessage, or null if invalid
     */
    public static NetworkMessage fromProtocolString(String protocolString) {
        if (protocolString == null || protocolString.isEmpty()) {
            return null;
        }
        
        String[] parts = protocolString.split("\\|", 4);
        if (parts.length < 3) {
            return null;
        }
        
        try {
            NetworkMessageType type = NetworkMessageType.valueOf(parts[0]);
            int playerId = Integer.parseInt(parts[1]);
            String data = parts.length > 2 ? parts[2] : "";
            String timestamp = parts.length > 3 ? parts[3] : LocalTime.now().format(TIME_FORMAT);
            
            NetworkMessage msg = new NetworkMessage(type, playerId, data);
            return msg;
        } catch (IllegalArgumentException e) {
            System.err.println("[NetworkMessage] Failed to parse: " + protocolString);
            return null;
        }
    }
    
    /**
     * Converts this message to the protocol string format.
     * @return The protocol string
     */
    public String toProtocolString() {
        return type.name() + DELIMITER + playerId + DELIMITER + data + DELIMITER + timestamp;
    }
    
    // Factory methods for common message types
    
    public static NetworkMessage cardPlaced(int playerId, String cardName, double x, double y) {
        return new NetworkMessage(NetworkMessageType.CARD_PLACED, playerId, 
                cardName + DATA_DELIMITER + x + "," + y);
    }
    
    public static NetworkMessage towerDamaged(int playerId, String towerName, int damage) {
        return new NetworkMessage(NetworkMessageType.TOWER_DAMAGED, playerId,
                towerName + DATA_DELIMITER + damage);
    }
    
    public static NetworkMessage towerDestroyed(int playerId, String towerName) {
        return new NetworkMessage(NetworkMessageType.TOWER_DESTROYED, playerId, towerName);
    }
    
    public static NetworkMessage elixirUpdate(int playerId, double elixir) {
        return new NetworkMessage(NetworkMessageType.ELIXIR_UPDATE, playerId, 
                String.valueOf(elixir));
    }
    
    public static NetworkMessage timerSync(double gameTime) {
        return new NetworkMessage(NetworkMessageType.TIMER_SYNC, 0, 
                String.valueOf(gameTime));
    }
    
    public static NetworkMessage playerInfo(int playerId, String playerName, String deckCards) {
        return new NetworkMessage(NetworkMessageType.PLAYER_INFO, playerId,
                playerName + DATA_DELIMITER + deckCards);
    }
    
    public static NetworkMessage readyStatus(int playerId, boolean isReady) {
        return new NetworkMessage(NetworkMessageType.READY_STATUS, playerId,
                String.valueOf(isReady));
    }
    
    public static NetworkMessage connect(String playerName) {
        return new NetworkMessage(NetworkMessageType.CONNECT, 2, playerName);
    }
    
    public static NetworkMessage connectAck(String hostName) {
        return new NetworkMessage(NetworkMessageType.CONNECT_ACK, 1, hostName);
    }
    
    public static NetworkMessage disconnect(int playerId, String reason) {
        return new NetworkMessage(NetworkMessageType.DISCONNECT, playerId, reason);
    }
    
    public static NetworkMessage heartbeat(int playerId) {
        return new NetworkMessage(NetworkMessageType.HEARTBEAT, playerId, "");
    }
    
    public static NetworkMessage matchStart() {
        return new NetworkMessage(NetworkMessageType.MATCH_START, 0, "");
    }
    
    /**
     * Creates an arena layout message to sync the host's arena design to the client.
     * Format: name#bridge1X,bridge1Y:bridge2X,bridge2Y:...#princess1X,princess1Y:princess2X,princess2Y#kingX,kingY
     */
    public static NetworkMessage arenaLayout(ArenaLayout layout) {
        StringBuilder sb = new StringBuilder();
        
        // Name
        sb.append(layout.getName() != null ? layout.getName() : "Arena");
        sb.append("#");
        
        // Bridge positions
        List<GridPosition> bridges = layout.getBridgePositions();
        for (int i = 0; i < bridges.size(); i++) {
            if (i > 0) sb.append(":");
            sb.append(bridges.get(i).getX()).append(",").append(bridges.get(i).getY());
        }
        sb.append("#");
        
        // Princess tower positions
        List<GridPosition> princess = layout.getPrincessTowerPositions();
        for (int i = 0; i < princess.size(); i++) {
            if (i > 0) sb.append(":");
            sb.append(princess.get(i).getX()).append(",").append(princess.get(i).getY());
        }
        sb.append("#");
        
        // King tower position
        GridPosition king = layout.getKingTowerPosition();
        if (king != null) {
            sb.append(king.getX()).append(",").append(king.getY());
        }
        
        return new NetworkMessage(NetworkMessageType.ARENA_LAYOUT, 1, sb.toString());
    }
    
    /**
     * Creates a full game state sync message (host-authoritative).
     * This is the core of the authoritative game loop - sent by host every tick.
     */
    public static NetworkMessage gameStateSync(NetworkGameStateSnapshot snapshot) {
        return new NetworkMessage(NetworkMessageType.GAME_STATE_SYNC, 1, snapshot.serialize());
    }
    
    /**
     * Creates a client input message for card placement request.
     * Client sends this to host; host validates and applies if valid.
     */
    public static NetworkMessage clientInput(int playerId, String cardName, double x, double y) {
        return new NetworkMessage(NetworkMessageType.CLIENT_INPUT, playerId,
                cardName + DATA_DELIMITER + x + "," + y);
    }
    
    /**
     * Creates a player input message (new format for authoritative model).
     * Format: seq:playerId:type:cardName:x:y:clientTime
     */
    public static NetworkMessage playerInput(com.kuroyale.service.network.PlayerInput input) {
        return new NetworkMessage(NetworkMessageType.PLAYER_INPUT, input.getPlayerId(), input.serialize());
    }
    
    /**
     * Creates a request for a full state snapshot (for client resync).
     */
    public static NetworkMessage requestSnapshot(int playerId) {
        return new NetworkMessage(NetworkMessageType.REQUEST_SNAPSHOT, playerId, "");
    }
    
    public static NetworkMessage victory(int playerId) {
        return new NetworkMessage(NetworkMessageType.VICTORY, playerId, "");
    }
    
    public static NetworkMessage defeat(int playerId) {
        return new NetworkMessage(NetworkMessageType.DEFEAT, playerId, "");
    }
    
    public static NetworkMessage error(String errorMessage) {
        return new NetworkMessage(NetworkMessageType.ERROR, 0, errorMessage);
    }
    
    public static NetworkMessage opponentDisconnected() {
        return new NetworkMessage(NetworkMessageType.OPPONENT_DISCONNECTED, 0, "");
    }
    
    // Getters
    
    public NetworkMessageType getType() {
        return type;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public String getData() {
        return data;
    }
    
    public String getTimestamp() {
        return timestamp;
    }
    
    /**
     * Parses card placement data from the message.
     * @return String array with [cardName, x, y] or null if invalid
     */
    public String[] parseCardPlacement() {
        if (type != NetworkMessageType.CARD_PLACED) return null;
        String[] parts = data.split(";");
        if (parts.length < 2) return null;
        String[] coords = parts[1].split(",");
        if (coords.length < 2) return null;
        return new String[] { parts[0], coords[0], coords[1] };
    }
    
    /**
     * Parses player info from the message.
     * @return String array with [playerName, deckCards] or null if invalid
     */
    public String[] parsePlayerInfo() {
        if (type != NetworkMessageType.PLAYER_INFO) return null;
        String[] parts = data.split(";", 2);
        return parts.length >= 2 ? parts : null;
    }
    
    /**
     * Parses client input data (card placement request).
     * @return String array with [cardName, x, y] or null if invalid
     */
    public String[] parseClientInput() {
        if (type != NetworkMessageType.CLIENT_INPUT) return null;
        String[] parts = data.split(";");
        if (parts.length < 2) return null;
        String[] coords = parts[1].split(",");
        if (coords.length < 2) return null;
        return new String[] { parts[0], coords[0], coords[1] };
    }
    
    /**
     * Parses player input data (new authoritative model format).
     * @return PlayerInput or null if invalid
     */
    public com.kuroyale.service.network.PlayerInput parsePlayerInput() {
        if (type != NetworkMessageType.PLAYER_INPUT) return null;
        return com.kuroyale.service.network.PlayerInput.deserialize(data);
    }
    
    /**
     * Parses full game state sync data.
     * @return NetworkGameStateSnapshot or null if invalid
     */
    public NetworkGameStateSnapshot parseGameStateSync() {
        if (type != NetworkMessageType.GAME_STATE_SYNC) return null;
        return NetworkGameStateSnapshot.deserialize(data);
    }
    
    /**
     * Parses arena layout from the message.
     * @return ArenaLayout or null if invalid
     */
    public ArenaLayout parseArenaLayout() {
        if (type != NetworkMessageType.ARENA_LAYOUT) return null;
        
        try {
            String[] parts = data.split("#", -1);
            if (parts.length < 4) return null;
            
            String name = parts[0];
            ArenaLayout layout = new ArenaLayout(name);
            
            // Parse bridge positions
            if (!parts[1].isEmpty()) {
                String[] bridges = parts[1].split(":");
                for (String bridge : bridges) {
                    String[] coords = bridge.split(",");
                    if (coords.length == 2) {
                        layout.addBridgePosition(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    }
                }
            }
            
            // Parse princess tower positions
            if (!parts[2].isEmpty()) {
                String[] princess = parts[2].split(":");
                for (String p : princess) {
                    String[] coords = p.split(",");
                    if (coords.length == 2) {
                        layout.addPrincessTowerPosition(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                    }
                }
            }
            
            // Parse king tower position
            if (!parts[3].isEmpty()) {
                String[] coords = parts[3].split(",");
                if (coords.length == 2) {
                    layout.setKingTowerPosition(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]));
                }
            }
            
            return layout;
        } catch (Exception e) {
            System.err.println("[NetworkMessage] Failed to parse arena layout: " + e.getMessage());
            return null;
        }
    }
    
    // ==================== Effect Message Factory Methods ====================
    
    /**
     * Creates an area effect message (e.g., spell explosion, splash damage).
     */
    public static NetworkMessage effectArea(int playerId, boolean isPlayerSource, double centerX, double centerY, double radius, double duration) {
        return new NetworkMessage(NetworkMessageType.EFFECT_AREA, playerId,
                (isPlayerSource ? "1" : "0") + DATA_DELIMITER + centerX + DATA_DELIMITER + centerY + DATA_DELIMITER + radius + DATA_DELIMITER + duration);
    }
    
    /**
     * Creates a projectile hit effect message.
     */
    public static NetworkMessage effectProjectileHit(int playerId, double x, double y, boolean isPlayerSide) {
        return new NetworkMessage(NetworkMessageType.EFFECT_PROJECTILE_HIT, playerId,
                x + DATA_DELIMITER + y + DATA_DELIMITER + (isPlayerSide ? "1" : "0"));
    }
    
    /**
     * Parses area effect data: [isPlayerSource, centerX, centerY, radius, duration]
     */
    public double[] parseEffectArea() {
        if (type != NetworkMessageType.EFFECT_AREA) return null;
        try {
            String[] parts = data.split(DATA_DELIMITER);
            if (parts.length < 5) return null;
            return new double[] {
                "1".equals(parts[0]) ? 1.0 : 0.0,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4])
            };
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Parses projectile hit effect data: [x, y, isPlayerSide]
     */
    public double[] parseEffectProjectileHit() {
        if (type != NetworkMessageType.EFFECT_PROJECTILE_HIT) return null;
        try {
            String[] parts = data.split(DATA_DELIMITER);
            if (parts.length < 3) return null;
            return new double[] {
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                "1".equals(parts[2]) ? 1.0 : 0.0
            };
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String toString() {
        return toProtocolString();
    }
}

