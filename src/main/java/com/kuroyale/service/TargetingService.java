package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.ArrayList;

public class TargetingService {

    /**
     * Finds the nearest valid enemy target or objective for a troop.
     * 
     * @requires state != null && troop != null && troop.getWorldPosition() != null
     *           &&
     *           state.getArena() != null
     * @modifies None
     * @effects
     *          Returns the Vector2 world position of the nearest valid target
     *          (Troop,
     *          Building, or Tower) within detection radius. Returns null if no
     *          valid target is found within range and fallback fails.
     * 
     *          <pre>
     *          Target selection priority:
     *          1. Nearest enemy Troop or Building within detection radius (taking
     *          into account target type filters).
     *          2. Closest enemy Tower if no immediate targets found.
     *          </pre>
     */
    public Vector2 findNearestEnemyOrObjective(IBattleState state, Troop troop) {
        Vector2 troopWorldPos = troop.getWorldPosition();
        if (troopWorldPos == null)
            return null;

        double bestDist = Double.MAX_VALUE;
        Vector2 bestPos = null;

        // Compute detection radius: ranged cards use range+2, melee use base radius
        double detectionRadius = com.kuroyale.util.GameConstants.BASE_DETECTION_RADIUS;
        double cardRange = troop.getBaseCard().getRange();
        if (cardRange > 0) {
            detectionRadius = cardRange + 2.0;
        }

        // Optimize with SpatialGrid for detection radius query (using Vector2)
        com.kuroyale.model.logic.SpatialGrid grid = state.getArena().getSpatialGrid();
        java.util.List<ICombatant> candidates;

        if (grid != null) {
            candidates = grid.getNearby(troopWorldPos, detectionRadius);
        } else {
            // Fallback (shouldn't happen)
            candidates = new ArrayList<>();
            candidates.addAll(state.getActiveTroops());
            candidates.addAll(state.getActiveBuildings());
        }

        for (ICombatant candidate : candidates) {
            // Filter invalid targets
            if (!candidate.isAlive())
                continue;
            if (candidate.isPlayerSide() == troop.isPlayerSide())
                continue;

            if (candidate instanceof Troop other) {
                // Troop checks
                if (troop.isBuildingOnly())
                    continue;
                if (troop.getBaseCard().getTarget() == TargetType.GROUND && other.isAirUnit())
                    continue;

                Vector2 otherWorldPos = other.getWorldPosition();
                if (otherWorldPos == null)
                    continue;

                double dist = troopWorldPos.distanceTo(otherWorldPos);

                if (dist <= detectionRadius && dist < bestDist) {
                    bestDist = dist;
                    bestPos = otherWorldPos;
                }
            } else if (candidate instanceof Building b) {
                // Building checks - use center world position
                Vector2 buildingCenter = b.getCenterWorldPosition();
                if (buildingCenter == null)
                    continue;

                double dist = troopWorldPos.distanceTo(buildingCenter);
                if (dist < bestDist) {
                    bestDist = dist;
                    // Return perimeter position for pathfinding (still needed for ground units)
                    GridPosition perimeter = CombatUtils.getNearestPerimeterTile(state.getArena(), b,
                            troop.getPosition());
                    if (perimeter != null) {
                        bestPos = Vector2.fromGridPosition(perimeter);
                    } else {
                        bestPos = buildingCenter;
                    }
                }
            }
        }

        if (bestPos != null)
            return bestPos;

        // fallback to nearest enemy tower
        return findNearestEnemyTower(state.getArena(), troop);
    }

    /**
     * Finds the nearest enemy tower position for a troop.
     */
    private Vector2 findNearestEnemyTower(Arena arena, Troop troop) {
        Vector2 troopWorldPos = troop.getWorldPosition();
        if (troopWorldPos == null)
            return null;

        Vector2 bestPos = null;
        double bestDist = Double.MAX_VALUE;

        for (Tower tower : arena.getAllTowers()) {
            if (!tower.isAlive())
                continue;
            if (tower.isPlayerSide() == troop.isPlayerSide())
                continue;

            // Find the closest perimeter tile for pathfinding
            GridPosition closestTile = CombatUtils.getNearestPerimeterTile(arena, tower, troop.getPosition());
            if (closestTile == null)
                continue;

            Vector2 tilePos = Vector2.fromGridPosition(closestTile);
            double dist = troopWorldPos.distanceTo(tilePos);
            if (dist < bestDist) {
                bestDist = dist;
                bestPos = tilePos;
            }
        }
        return bestPos;
    }

    public boolean isValidTarget(Troop attacker, Troop candidate) {
        if (!candidate.isAlive())
            return false;
        if (attacker.isBuildingOnly())
            return false; // building-only troops ignore enemy troops
        if (attacker.getBaseCard().getTarget() == TargetType.GROUND && candidate.isAirUnit())
            return false;
        return true;
    }

    public boolean isInRange(Troop attacker, Troop target) {
        return CombatUtils.isInRange(attacker, target);
    }
}
