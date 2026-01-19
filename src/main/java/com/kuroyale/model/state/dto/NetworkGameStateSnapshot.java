package com.kuroyale.model.state.dto;

import com.kuroyale.model.core.entities.Arena;
import com.kuroyale.model.core.entities.Building;
import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.GridPosition;
import com.kuroyale.model.core.entities.ICombatant;
import com.kuroyale.model.core.entities.Projectile;
import com.kuroyale.model.core.entities.Tower;
import com.kuroyale.model.core.entities.Troop;
import com.kuroyale.model.core.entities.Vector2;
import com.kuroyale.model.logic.GameState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Immutable snapshot of the complete game state for network synchronization.
 * Used by the host to broadcast authoritative game state to all clients.
 * 
 * This ensures both players see the exact same game state at any given moment.
 * The host is the single source of truth - clients only render what the host
 * tells them.
 * 
 * Protocol format for transmission:
 * GAME_STATE_SYNC|tick|gameTime|p1Score|p2Score|isDoubleElixir|isGameOver|winner|p1Elixir|p2Elixir|towers|troops|buildings|projectiles
 */
public class NetworkGameStateSnapshot {

    // Game timing
    private final long tick; // Authoritative tick number
    private final double gameTime; // Remaining game time in seconds

    // Scores
    private final int player1Score;
    private final int player2Score;

    // Game state flags
    private final boolean isDoubleElixir;
    private final boolean isGameOver;
    private final boolean isTiebreakerMode;
    private final int winner; // 0 = none, 1 = player1, 2 = player2, 3 = draw

    // Elixir
    private final double player1Elixir;
    private final double player2Elixir;

    // Entity snapshots
    private final List<TowerSnapshot> towers;
    private final List<TroopSnapshot> troops;
    private final List<BuildingSnapshot> buildings;
    private final List<ProjectileSnapshot> projectiles;

    /**
     * Creates a snapshot from the current game state.
     * Should only be called by the host.
     */
    public NetworkGameStateSnapshot(GameState gameState, long tick) {
        this.tick = tick;
        this.gameTime = gameState.getGameTime();
        this.player1Score = gameState.getPlayerScore();
        this.player2Score = gameState.getBotScore(); // "Bot" is player 2 in network mode
        this.isDoubleElixir = gameState.isDoubleElixir();
        this.isGameOver = gameState.isGameOver();
        this.isTiebreakerMode = gameState.isTiebreakerMode();

        // Determine winner
        if (gameState.isGameOver()) {
            if (gameState.isDraw()) {
                this.winner = 3;
            } else if (gameState.isPlayerWinner()) {
                this.winner = 1;
            } else {
                this.winner = 2;
            }
        } else {
            this.winner = 0;
        }

        this.player1Elixir = gameState.getPlayerElixir().getCurrentElixir();
        this.player2Elixir = gameState.getBotElixir().getCurrentElixir();

        // Snapshot all entities
        this.towers = snapshotTowers(gameState.getArena());
        this.troops = snapshotTroops(gameState.getTroops());
        this.buildings = snapshotBuildings(gameState.getBuildings());
        this.projectiles = snapshotProjectiles(gameState.getProjectiles());
    }

    /**
     * Creates a snapshot from parsed network data.
     */
    public NetworkGameStateSnapshot(
            long tick, double gameTime,
            int player1Score, int player2Score,
            boolean isDoubleElixir, boolean isGameOver, boolean isTiebreakerMode, int winner,
            double player1Elixir, double player2Elixir,
            List<TowerSnapshot> towers, List<TroopSnapshot> troops,
            List<BuildingSnapshot> buildings, List<ProjectileSnapshot> projectiles) {
        this.tick = tick;
        this.gameTime = gameTime;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.isDoubleElixir = isDoubleElixir;
        this.isGameOver = isGameOver;
        this.isTiebreakerMode = isTiebreakerMode;
        this.winner = winner;
        this.player1Elixir = player1Elixir;
        this.player2Elixir = player2Elixir;
        this.towers = towers != null ? towers : new ArrayList<>();
        this.troops = troops != null ? troops : new ArrayList<>();
        this.buildings = buildings != null ? buildings : new ArrayList<>();
        this.projectiles = projectiles != null ? projectiles : new ArrayList<>();
    }

    // ==================== Snapshot Helpers ====================

    private List<TowerSnapshot> snapshotTowers(Arena arena) {
        List<TowerSnapshot> snapshots = new ArrayList<>();
        for (Tower tower : arena.getAllTowers()) {
            snapshots.add(new TowerSnapshot(tower));
        }
        return snapshots;
    }

    private List<TroopSnapshot> snapshotTroops(List<Troop> troops) {
        List<TroopSnapshot> snapshots = new ArrayList<>();
        for (Troop troop : troops) {
            if (troop.isAlive()) {
                snapshots.add(new TroopSnapshot(troop));
            }
        }
        return snapshots;
    }

    private List<BuildingSnapshot> snapshotBuildings(List<Building> buildings) {
        List<BuildingSnapshot> snapshots = new ArrayList<>();
        for (Building building : buildings) {
            if (building.isAlive()) {
                snapshots.add(new BuildingSnapshot(building));
            }
        }
        return snapshots;
    }

    private List<ProjectileSnapshot> snapshotProjectiles(List<Projectile> projectiles) {
        List<ProjectileSnapshot> snapshots = new ArrayList<>();
        for (Projectile proj : projectiles) {
            snapshots.add(new ProjectileSnapshot(proj));
        }
        return snapshots;
    }

    // ==================== Serialization ====================

    /**
     * Serializes this snapshot to a compact string for network transmission.
     * Format uses ~ as field separator, ^ as list separator, and : as sub-field
     * separator
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();

        // Core state
        sb.append(tick).append("~");
        sb.append(String.format(Locale.US, "%.2f", gameTime)).append("~");
        sb.append(player1Score).append("~");
        sb.append(player2Score).append("~");
        sb.append(isDoubleElixir ? "1" : "0").append("~");
        sb.append(isGameOver ? "1" : "0").append("~");
        sb.append(isTiebreakerMode ? "1" : "0").append("~");
        sb.append(winner).append("~");
        sb.append(String.format(Locale.US, "%.2f", player1Elixir)).append("~");
        sb.append(String.format(Locale.US, "%.2f", player2Elixir)).append("~");

        // Towers
        sb.append(serializeTowers()).append("~");

        // Troops
        sb.append(serializeTroops()).append("~");

        // Buildings
        sb.append(serializeBuildings()).append("~");

        // Projectiles
        sb.append(serializeProjectiles());

        return sb.toString();
    }

    private String serializeTowers() {
        if (towers.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < towers.size(); i++) {
            if (i > 0)
                sb.append("^");
            sb.append(towers.get(i).serialize());
        }
        return sb.toString();
    }

    private String serializeTroops() {
        if (troops.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < troops.size(); i++) {
            if (i > 0)
                sb.append("^");
            sb.append(troops.get(i).serialize());
        }
        return sb.toString();
    }

    private String serializeBuildings() {
        if (buildings.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buildings.size(); i++) {
            if (i > 0)
                sb.append("^");
            sb.append(buildings.get(i).serialize());
        }
        return sb.toString();
    }

    private String serializeProjectiles() {
        if (projectiles.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < projectiles.size(); i++) {
            if (i > 0)
                sb.append("^");
            sb.append(projectiles.get(i).serialize());
        }
        return sb.toString();
    }

    /**
     * Deserializes a snapshot from network data.
     */
    public static NetworkGameStateSnapshot deserialize(String data) {
        if (data == null || data.isEmpty())
            return null;

        try {
            String[] parts = data.split("~", -1);
            if (parts.length < 14)
                return null;

            long tick = Long.parseLong(parts[0]);
            double gameTime = Double.parseDouble(parts[1]);
            int p1Score = Integer.parseInt(parts[2]);
            int p2Score = Integer.parseInt(parts[3]);
            boolean isDoubleElixir = "1".equals(parts[4]);
            boolean isGameOver = "1".equals(parts[5]);
            boolean isTiebreakerMode = "1".equals(parts[6]);
            int winner = Integer.parseInt(parts[7]);
            double p1Elixir = Double.parseDouble(parts[8]);
            double p2Elixir = Double.parseDouble(parts[9]);

            List<TowerSnapshot> towers = deserializeTowers(parts[10]);
            List<TroopSnapshot> troops = deserializeTroops(parts[11]);
            List<BuildingSnapshot> buildings = deserializeBuildings(parts[12]);
            List<ProjectileSnapshot> projectiles = deserializeProjectiles(parts[13]);

            return new NetworkGameStateSnapshot(
                    tick, gameTime, p1Score, p2Score,
                    isDoubleElixir, isGameOver, isTiebreakerMode, winner,
                    p1Elixir, p2Elixir, towers, troops, buildings, projectiles);
        } catch (Exception e) {
            System.err.println("[NetworkGameStateSnapshot] Failed to deserialize: " + e.getMessage());
            return null;
        }
    }

    private static List<TowerSnapshot> deserializeTowers(String data) {
        List<TowerSnapshot> list = new ArrayList<>();
        if (data == null || data.isEmpty())
            return list;

        String[] parts = data.split("\\^");
        for (String part : parts) {
            TowerSnapshot snapshot = TowerSnapshot.deserialize(part);
            if (snapshot != null)
                list.add(snapshot);
        }
        return list;
    }

    private static List<TroopSnapshot> deserializeTroops(String data) {
        List<TroopSnapshot> list = new ArrayList<>();
        if (data == null || data.isEmpty())
            return list;

        String[] parts = data.split("\\^");
        for (String part : parts) {
            TroopSnapshot snapshot = TroopSnapshot.deserialize(part);
            if (snapshot != null)
                list.add(snapshot);
        }
        return list;
    }

    private static List<BuildingSnapshot> deserializeBuildings(String data) {
        List<BuildingSnapshot> list = new ArrayList<>();
        if (data == null || data.isEmpty())
            return list;

        String[] parts = data.split("\\^");
        for (String part : parts) {
            BuildingSnapshot snapshot = BuildingSnapshot.deserialize(part);
            if (snapshot != null)
                list.add(snapshot);
        }
        return list;
    }

    private static List<ProjectileSnapshot> deserializeProjectiles(String data) {
        List<ProjectileSnapshot> list = new ArrayList<>();
        if (data == null || data.isEmpty())
            return list;

        String[] parts = data.split("\\^");
        for (String part : parts) {
            ProjectileSnapshot snapshot = ProjectileSnapshot.deserialize(part);
            if (snapshot != null)
                list.add(snapshot);
        }
        return list;
    }

    // ==================== Getters ====================

    public long getTick() {
        return tick;
    }

    public double getGameTime() {
        return gameTime;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public boolean isDoubleElixir() {
        return isDoubleElixir;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isTiebreakerMode() {
        return isTiebreakerMode;
    }

    public int getWinner() {
        return winner;
    }

    public double getPlayer1Elixir() {
        return player1Elixir;
    }

    public double getPlayer2Elixir() {
        return player2Elixir;
    }

    public List<TowerSnapshot> getTowers() {
        return towers;
    }

    public List<TroopSnapshot> getTroops() {
        return troops;
    }

    public List<BuildingSnapshot> getBuildings() {
        return buildings;
    }

    public List<ProjectileSnapshot> getProjectiles() {
        return projectiles;
    }

    // ==================== Inner Snapshot Classes ====================

    /**
     * Snapshot of a tower's state.
     * 
     * IMPORTANT: Tower IDs use absolute player numbers (P1, P2) not relative
     * "player/enemy".
     * This ensures consistent identification across perspectives:
     * - P1 = Host's tower (always at bottom for host, top for client after mapping)
     * - P2 = Client's tower (always at top for host, bottom for client after
     * mapping)
     */
    public static class TowerSnapshot {
        private final String id; // Unique identifier using absolute player (P1, P2)
        private final String type; // KING, PRINCESS_LEFT, PRINCESS_RIGHT
        private final boolean isPlayerSide; // Relative to viewer (swapped by perspective mapper)
        private final int health;
        private final int maxHealth;
        private final double x, y;

        public TowerSnapshot(Tower tower) {
            // Use absolute player ID: P1 for host's towers, P2 for client's towers
            // On host: isPlayerSide=true means P1, isPlayerSide=false means P2
            String absPlayer = tower.isPlayerSide() ? "P1" : "P2";
            this.id = tower.getType().name() + "_" + absPlayer;
            this.type = tower.getType().name();
            this.isPlayerSide = tower.isPlayerSide();
            this.health = tower.getCurrentHealth();
            this.maxHealth = tower.getMaxHealth();
            GridPosition pos = tower.getPosition();
            this.x = pos != null ? pos.getX() : 0;
            this.y = pos != null ? pos.getY() : 0;
        }

        public TowerSnapshot(String id, String type, boolean isPlayerSide, int health, int maxHealth, double x,
                double y) {
            this.id = id;
            this.type = type;
            this.isPlayerSide = isPlayerSide;
            this.health = health;
            this.maxHealth = maxHealth;
            this.x = x;
            this.y = y;
        }

        public String serialize() {
            return id + ":" + type + ":" + (isPlayerSide ? "1" : "0") + ":" +
                    health + ":" + maxHealth + ":" + String.format(Locale.US, "%.1f", x) + ":"
                    + String.format(Locale.US, "%.1f", y);
        }

        public static TowerSnapshot deserialize(String data) {
            try {
                String[] parts = data.split(":");
                if (parts.length < 7)
                    return null;
                return new TowerSnapshot(
                        parts[0], parts[1], "1".equals(parts[2]),
                        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                        Double.parseDouble(parts[5]), Double.parseDouble(parts[6]));
            } catch (Exception e) {
                return null;
            }
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public boolean isPlayerSide() {
            return isPlayerSide;
        }

        public int getHealth() {
            return health;
        }

        public int getMaxHealth() {
            return maxHealth;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    /**
     * Snapshot of a troop's state.
     */
    public static class TroopSnapshot {
        private final int id;
        private final String cardName;
        private final boolean isPlayerSide;
        private final int health;
        private final int maxHealth;
        private final double x, y;
        private final double targetX, targetY;

        public TroopSnapshot(Troop troop) {
            this.id = System.identityHashCode(troop);
            this.cardName = troop.getBaseCard() != null ? troop.getBaseCard().getName() : "Unknown";
            this.isPlayerSide = troop.isPlayerSide();
            this.health = troop.getCurrentHealth();
            Card baseCard = troop.getBaseCard();
            this.maxHealth = baseCard != null ? baseCard.getHp() : troop.getCurrentHealth();
            Vector2 pos = troop.getWorldPosition();
            this.x = pos != null ? pos.getX() : 0;
            this.y = pos != null ? pos.getY() : 0;
            Vector2 target = troop.getTargetWorldPosition();
            this.targetX = target != null ? target.getX() : this.x;
            this.targetY = target != null ? target.getY() : this.y;
        }

        public TroopSnapshot(int id, String cardName, boolean isPlayerSide, int health, int maxHealth,
                double x, double y, double targetX, double targetY) {
            this.id = id;
            this.cardName = cardName;
            this.isPlayerSide = isPlayerSide;
            this.health = health;
            this.maxHealth = maxHealth;
            this.x = x;
            this.y = y;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public String serialize() {
            return id + ":" + cardName + ":" + (isPlayerSide ? "1" : "0") + ":" +
                    health + ":" + maxHealth + ":" +
                    String.format(Locale.US, "%.2f", x) + ":" + String.format(Locale.US, "%.2f", y) + ":" +
                    String.format(Locale.US, "%.2f", targetX) + ":" + String.format(Locale.US, "%.2f", targetY);
        }

        public static TroopSnapshot deserialize(String data) {
            try {
                String[] parts = data.split(":");
                if (parts.length < 9)
                    return null;
                return new TroopSnapshot(
                        Integer.parseInt(parts[0]), parts[1], "1".equals(parts[2]),
                        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                        Double.parseDouble(parts[5]), Double.parseDouble(parts[6]),
                        Double.parseDouble(parts[7]), Double.parseDouble(parts[8]));
            } catch (Exception e) {
                return null;
            }
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getCardName() {
            return cardName;
        }

        public boolean isPlayerSide() {
            return isPlayerSide;
        }

        public int getHealth() {
            return health;
        }

        public int getMaxHealth() {
            return maxHealth;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getTargetX() {
            return targetX;
        }

        public double getTargetY() {
            return targetY;
        }
    }

    /**
     * Snapshot of a building's state.
     */
    public static class BuildingSnapshot {
        private final int id;
        private final String cardName;
        private final boolean isPlayerSide;
        private final int health;
        private final int maxHealth;
        private final double x, y;
        private final double remainingLifetime;

        public BuildingSnapshot(Building building) {
            this.id = System.identityHashCode(building);
            this.cardName = building.getBaseCard() != null ? building.getBaseCard().getName()
                    : (building.getCardName() != null ? building.getCardName() : "Unknown");
            this.isPlayerSide = building.isPlayerSide();
            this.health = building.getCurrentHealth();
            this.maxHealth = building.getMaxHealth();
            GridPosition pos = building.getPosition();
            this.x = pos != null ? pos.getX() : 0;
            this.y = pos != null ? pos.getY() : 0;
            this.remainingLifetime = building.getRemainingLifetime();
        }

        public BuildingSnapshot(int id, String cardName, boolean isPlayerSide, int health, int maxHealth,
                double x, double y, double remainingLifetime) {
            this.id = id;
            this.cardName = cardName;
            this.isPlayerSide = isPlayerSide;
            this.health = health;
            this.maxHealth = maxHealth;
            this.x = x;
            this.y = y;
            this.remainingLifetime = remainingLifetime;
        }

        public String serialize() {
            return id + ":" + cardName + ":" + (isPlayerSide ? "1" : "0") + ":" +
                    health + ":" + maxHealth + ":" +
                    String.format(Locale.US, "%.2f", x) + ":" + String.format(Locale.US, "%.2f", y) + ":" +
                    String.format(Locale.US, "%.2f", remainingLifetime);
        }

        public static BuildingSnapshot deserialize(String data) {
            try {
                String[] parts = data.split(":");
                if (parts.length < 8)
                    return null;
                return new BuildingSnapshot(
                        Integer.parseInt(parts[0]), parts[1], "1".equals(parts[2]),
                        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]),
                        Double.parseDouble(parts[5]), Double.parseDouble(parts[6]),
                        Double.parseDouble(parts[7]));
            } catch (Exception e) {
                return null;
            }
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getCardName() {
            return cardName;
        }

        public boolean isPlayerSide() {
            return isPlayerSide;
        }

        public int getHealth() {
            return health;
        }

        public int getMaxHealth() {
            return maxHealth;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getRemainingLifetime() {
            return remainingLifetime;
        }
    }

    /**
     * Snapshot of a projectile's state.
     */
    public static class ProjectileSnapshot {
        private final int id;
        private final boolean isPlayerSide;
        private final double x, y;
        private final double targetX, targetY;

        public ProjectileSnapshot(Projectile projectile) {
            this.id = System.identityHashCode(projectile);
            this.isPlayerSide = projectile.isPlayerSide();
            Vector2 pos = projectile.getPosition();
            this.x = pos != null ? pos.getX() : 0;
            this.y = pos != null ? pos.getY() : 0;
            // Get target position from the target entity
            ICombatant target = projectile.getTarget();
            if (target != null && target.getCenterPosition() != null) {
                GridPosition targetPos = target.getCenterPosition();
                this.targetX = targetPos.getX();
                this.targetY = targetPos.getY();
            } else {
                this.targetX = this.x;
                this.targetY = this.y;
            }
        }

        public ProjectileSnapshot(int id, boolean isPlayerSide, double x, double y, double targetX, double targetY) {
            this.id = id;
            this.isPlayerSide = isPlayerSide;
            this.x = x;
            this.y = y;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        public String serialize() {
            return id + ":" + (isPlayerSide ? "1" : "0") + ":" +
                    String.format(Locale.US, "%.2f", x) + ":" + String.format(Locale.US, "%.2f", y) + ":" +
                    String.format(Locale.US, "%.2f", targetX) + ":" + String.format(Locale.US, "%.2f", targetY);
        }

        public static ProjectileSnapshot deserialize(String data) {
            try {
                String[] parts = data.split(":");
                if (parts.length < 6)
                    return null;
                return new ProjectileSnapshot(
                        Integer.parseInt(parts[0]), "1".equals(parts[1]),
                        Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                        Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
            } catch (Exception e) {
                return null;
            }
        }

        // Getters
        public int getId() {
            return id;
        }

        public boolean isPlayerSide() {
            return isPlayerSide;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getTargetX() {
            return targetX;
        }

        public double getTargetY() {
            return targetY;
        }
    }
}
