package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.ArrayList;

public class TargetingService {

    /**
     * Finds the nearest valid enemy target or objective for a troop.
     * 
     * @requires state != null && troop != null && troop.getPosition() != null &&
     *           state.getArena() != null
     * @modifies None
     * @effects
     *          Returns the GridPosition of the nearest valid target (Troop,
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
    public GridPosition findNearestEnemyOrObjective(IBattleState state, Troop troop) {
        Vector2 troopWorldPos = troop.getWorldPosition();
        GridPosition troopPos = troop.getPosition(); // Fallback for grid-based queries
        double bestDist = Double.MAX_VALUE;
        GridPosition bestPos = null;

        // Compute detection radius: ranged cards use range+2, melee use base radius
        double detectionRadius = com.kuroyale.util.GameConstants.BASE_DETECTION_RADIUS;
        double cardRange = troop.getBaseCard().getRange();
        if (cardRange > 0) {
            detectionRadius = cardRange + 2.0;
        }

        // Optimize with SpatialGrid for detection radius query
        com.kuroyale.model.logic.SpatialGrid grid = state.getArena().getSpatialGrid();
        java.util.List<ICombatant> candidates;

        if (grid != null) {
            candidates = grid.getNearby(troopPos, detectionRadius);
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
                double dist = (troopWorldPos != null && otherWorldPos != null)
                        ? troopWorldPos.distanceTo(otherWorldPos)
                        : troopPos.getEuclideanDistanceTo(other.getPosition());

                if (dist <= detectionRadius && dist < bestDist) {
                    bestDist = dist;
                    bestPos = other.getPosition();
                }
            } else if (candidate instanceof Building b) {
                // Building checks
                // Use CombatUtils to find the nearest perimeter tile
                GridPosition perimeter = CombatUtils.getNearestPerimeterTile(state.getArena(), b, troopPos);
                if (perimeter == null)
                    continue;

                double dist = troopPos.getEuclideanDistanceTo(perimeter);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestPos = perimeter;
                }
            }
        }

        if (bestPos != null)
            return bestPos;

        // fallback to nearest enemy tower cell
        GridPosition towerTarget = findNearestEnemyTower(state.getArena(), troop);
        return towerTarget;
    }

    // Find the nearest walkable, unoccupied perimeter tile adjacent to the building
    // footprint

    private GridPosition findNearestEnemyTower(Arena arena, Troop troop) {
        GridPosition bestPos = null;
        double bestDist = Double.MAX_VALUE;
        GridPosition troopPos = troop.getPosition();

        for (Tower tower : arena.getAllTowers()) {
            if (!tower.isAlive())
                continue;
            if (tower.isPlayerSide() == troop.isPlayerSide())
                continue;

            // Find the closest tile within the tower's footprint
            GridPosition closestTile = CombatUtils.getNearestPerimeterTile(arena, tower, troopPos);
            if (closestTile == null)
                continue;

            double dist = troopPos.getEuclideanDistanceTo(closestTile);
            if (dist < bestDist) {
                bestDist = dist;
                bestPos = closestTile;
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
