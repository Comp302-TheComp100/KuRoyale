package com.kuroyale.service.network;

import com.kuroyale.model.dto.NetworkGameStateSnapshot;
import com.kuroyale.model.dto.NetworkGameStateSnapshot.*;
import com.kuroyale.model.entities.Arena;

import java.util.ArrayList;
import java.util.List;

/**
 * PERSPECTIVE MAPPER
 * 
 * Maps the authoritative game state to each player's perspective.
 * 
 * The authoritative state uses a neutral coordinate system where:
 * - Player 1 (Host) is at the bottom (high Y values)
 * - Player 2 (Client) is at the top (low Y values)
 * 
 * Each client renders from their own POV:
 * - "My tower" is always at the bottom of their screen
 * - "Enemy tower" is at the top
 * 
 * For Player 1: No transformation needed (they see raw coordinates)
 * For Player 2: Y coordinates are mirrored, and player labels are swapped
 * 
 * This class provides clean mapping functions that don't mutate the original state.
 */
public class PerspectiveMapper {
    
    /**
     * Maps the authoritative state snapshot to a player's perspective.
     * 
     * @param snapshot The authoritative state
     * @param localPlayerId The player viewing the state (1 or 2)
     * @return A new snapshot transformed to the player's perspective
     */
    public static NetworkGameStateSnapshot mapToPerspective(
            NetworkGameStateSnapshot snapshot, int localPlayerId) {
        
        if (snapshot == null) return null;
        
        // Player 1 sees the raw state (no transformation)
        if (localPlayerId == 1) {
            return snapshot;
        }
        
        // Player 2 needs mirrored coordinates and swapped labels
        return transformForPlayer2(snapshot);
    }
    
    /**
     * Transforms the state for Player 2's perspective.
     * - Mirrors Y coordinates
     * - Swaps player/enemy labels
     * - Swaps scores (so "my score" is player 2's score)
     */
    private static NetworkGameStateSnapshot transformForPlayer2(NetworkGameStateSnapshot snapshot) {
        // Transform towers
        List<TowerSnapshot> transformedTowers = new ArrayList<>();
        for (TowerSnapshot tower : snapshot.getTowers()) {
            transformedTowers.add(transformTower(tower));
        }
        
        // Transform troops
        List<TroopSnapshot> transformedTroops = new ArrayList<>();
        for (TroopSnapshot troop : snapshot.getTroops()) {
            transformedTroops.add(transformTroop(troop));
        }
        
        // Transform buildings
        List<BuildingSnapshot> transformedBuildings = new ArrayList<>();
        for (BuildingSnapshot building : snapshot.getBuildings()) {
            transformedBuildings.add(transformBuilding(building));
        }
        
        // Transform projectiles
        List<ProjectileSnapshot> transformedProjectiles = new ArrayList<>();
        for (ProjectileSnapshot projectile : snapshot.getProjectiles()) {
            transformedProjectiles.add(transformProjectile(projectile));
        }
        
        // Swap scores and elixir (Player 2 sees their stats as "player 1")
        return new NetworkGameStateSnapshot(
            snapshot.getTick(),
            snapshot.getGameTime(),
            snapshot.getPlayer2Score(),  // Swapped: P2's score becomes "my score"
            snapshot.getPlayer1Score(),  // Swapped: P1's score becomes "enemy score"
            snapshot.isDoubleElixir(),
            snapshot.isGameOver(),
            snapshot.isTiebreakerMode(),
            transformWinner(snapshot.getWinner()),  // Swap winner perspective
            snapshot.getPlayer2Elixir(),  // Swapped: P2's elixir becomes "my elixir"
            snapshot.getPlayer1Elixir(),  // Swapped: P1's elixir becomes "enemy elixir"
            transformedTowers,
            transformedTroops,
            transformedBuildings,
            transformedProjectiles
        );
    }
    
    /**
     * Transforms a tower for Player 2's perspective.
     */
    private static TowerSnapshot transformTower(TowerSnapshot tower) {
        return new TowerSnapshot(
            tower.getId(),
            tower.getType(),
            !tower.isPlayerSide(),  // Swap player/enemy
            tower.getHealth(),
            tower.getMaxHealth(),
            tower.getX(),
            mirrorY(tower.getY())
        );
    }
    
    /**
     * Transforms a troop for Player 2's perspective.
     */
    private static TroopSnapshot transformTroop(TroopSnapshot troop) {
        return new TroopSnapshot(
            troop.getId(),
            troop.getCardName(),
            !troop.isPlayerSide(),  // Swap player/enemy
            troop.getHealth(),
            troop.getMaxHealth(),
            troop.getX(),
            mirrorY(troop.getY()),
            troop.getTargetX(),
            mirrorY(troop.getTargetY())
        );
    }
    
    /**
     * Transforms a building for Player 2's perspective.
     */
    private static BuildingSnapshot transformBuilding(BuildingSnapshot building) {
        return new BuildingSnapshot(
            building.getId(),
            building.getCardName(),
            !building.isPlayerSide(),  // Swap player/enemy
            building.getHealth(),
            building.getMaxHealth(),
            building.getX(),
            mirrorY(building.getY()),
            building.getRemainingLifetime()
        );
    }
    
    /**
     * Transforms a projectile for Player 2's perspective.
     */
    private static ProjectileSnapshot transformProjectile(ProjectileSnapshot projectile) {
        return new ProjectileSnapshot(
            projectile.getId(),
            !projectile.isPlayerSide(),  // Swap player/enemy
            projectile.getX(),
            mirrorY(projectile.getY()),
            projectile.getTargetX(),
            mirrorY(projectile.getTargetY())
        );
    }
    
    /**
     * Mirrors a Y coordinate for the opposite perspective.
     */
    private static double mirrorY(double y) {
        return (Arena.HEIGHT - 1) - y;
    }
    
    /**
     * Transforms winner value for Player 2's perspective.
     * 0 = none, 1 = player1 won, 2 = player2 won, 3 = draw
     */
    private static int transformWinner(int winner) {
        return switch (winner) {
            case 1 -> 2;  // P1 won -> from P2's view, enemy won
            case 2 -> 1;  // P2 won -> from P2's view, I won
            default -> winner;  // 0 (none) or 3 (draw) stay the same
        };
    }
    
    // ==================== Input Coordinate Transformation ====================
    
    /**
     * Transforms input coordinates from a player's local space to authoritative space.
     * 
     * @param x X coordinate in player's local space
     * @param y Y coordinate in player's local space
     * @param playerId The player sending the input (1 or 2)
     * @return Coordinates in authoritative space [x, y]
     */
    public static int[] transformInputCoordinates(int x, int y, int playerId) {
        if (playerId == 1) {
            // Player 1's coordinates are already in authoritative space
            return new int[] { x, y };
        } else {
            // Player 2 needs Y mirrored
            return new int[] { x, (Arena.HEIGHT - 1) - y };
        }
    }
    
    /**
     * Checks if a position is valid for a player to deploy units.
     * Each player can only deploy on their half of the arena.
     * 
     * @param x X coordinate in authoritative space
     * @param y Y coordinate in authoritative space
     * @param playerId The player trying to deploy (1 or 2)
     * @return true if the position is valid for deployment
     */
    public static boolean isValidDeployPosition(int x, int y, int playerId) {
        if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
            return false;
        }
        
        int midY = Arena.HEIGHT / 2;
        
        if (playerId == 1) {
            // Player 1 deploys on bottom half (high Y values)
            return y >= midY;
        } else {
            // Player 2 deploys on top half (low Y values) in authoritative space
            // Note: In their local space, they also deploy on "bottom" but that's mirrored
            return y < midY;
        }
    }
}
