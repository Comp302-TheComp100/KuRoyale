package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.ArrayList;

public class TargetingService {
    private static final double BASE_DETECTION_RADIUS = 5.0; // tiles (world units)

    /**
     * Finds the nearest valid enemy target or objective for a troop.
     * 
     * @requires state != null && troop != null && troop.getPosition() != null &&
     *           state.getArena() != null
     * @modifies None
     * @effects Returns the GridPosition of the nearest valid target (Troop,
     *          Building, or Tower) within detection radius. Returns null if no
     *          valid target is found within range and fallback fails.
     *          <p>
     *          Target selection priority:
     *          1. Nearest enemy Troop or Building within detection radius (taking
     *          into account target type filters).
     *          2. Closest enemy Tower if no immediate targets found.
     *          </p>
     */
    public GridPosition findNearestEnemyOrObjective(IBattleState state, Troop troop) {
        Vector2 troopWorldPos = troop.getWorldPosition();
        GridPosition troopPos = troop.getPosition(); // Fallback for grid-based queries
        double bestDist = Double.MAX_VALUE;
        GridPosition bestPos = null;

        // Compute detection radius: ranged cards use range+2, melee use base radius
        double detectionRadius = BASE_DETECTION_RADIUS;
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
                // Use nearest perimeter tile around the building footprint as target
                GridPosition perimeter = nearestPerimeterTile(state.getArena(), b, troopPos);
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
        if (towerTarget == null) {
            // Log if even the tower cannot be found (usually only happens if king tower is
            // destroyed but game hasn't ended)
            System.out.println("[DEBUG] Targeting: No target found for troop at " + troop.getPosition() +
                    ", side=" + (troop.isPlayerSide() ? "Player" : "Opponent"));
        }
        return towerTarget;
    }

    // Find the nearest walkable, unoccupied perimeter tile adjacent to the building
    // footprint
    private GridPosition nearestPerimeterTile(Arena arena, ICombatant combatant, GridPosition from) {
        GridPosition pos = combatant.getPosition();
        if (pos == null)
            return null;
        int x0 = pos.getX();
        int y0 = pos.getY();
        int w = Math.max(1, combatant.getWidth());
        int h = Math.max(1, combatant.getHeight());
        GridPosition best = null;
        double bestDist = Double.MAX_VALUE;
        // Check tiles around footprint borders (faces only, no corners)
        for (int dx = 0; dx < w; dx++) {
            // top perimeter
            GridPosition p1 = GridPosition.tryCreate(x0 + dx, y0 - 1);
            best = pickIfBetter(arena, from, best, p1, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
            // bottom perimeter
            GridPosition p2 = GridPosition.tryCreate(x0 + dx, y0 + h);
            best = pickIfBetter(arena, from, best, p2, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
        }
        for (int dy = 0; dy < h; dy++) {
            // left perimeter
            GridPosition p3 = GridPosition.tryCreate(x0 - 1, y0 + dy);
            best = pickIfBetter(arena, from, best, p3, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
            // right perimeter
            GridPosition p4 = GridPosition.tryCreate(x0 + w, y0 + dy);
            best = pickIfBetter(arena, from, best, p4, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
        }
        return best;
    }

    private GridPosition pickIfBetter(Arena arena, GridPosition from, GridPosition currentBest, GridPosition candidate,
            double currentBestDist) {
        if (candidate == null)
            return currentBest;
        GridCell cell = arena.getCell(candidate);
        if (cell == null || !cell.canPlaceUnit() || cell.isOccupied())
            return currentBest;
        double dist = from.getEuclideanDistanceTo(candidate);
        if (currentBest == null || dist < currentBestDist) {
            return candidate;
        }
        return currentBest;
    }

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
            GridPosition closestTile = nearestPerimeterTile(arena, tower, troopPos);
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
        // Use Vector2 for accurate distance calculation
        Vector2 attackerPos = attacker.getWorldPosition();
        Vector2 targetPos = target.getWorldPosition();

        double dist;
        if (attackerPos != null && targetPos != null) {
            dist = attackerPos.distanceTo(targetPos);
        } else {
            // Fallback to grid-based calculation
            dist = attacker.getPosition().getEuclideanDistanceTo(target.getPosition());
        }

        double rangeTiles = attacker.getCombatStats() != null ? attacker.getCombatStats().getRangeTiles()
                : attacker.getAttackRange();
        if (attacker.getCombatStats() != null
                && attacker.getCombatStats().getAttackType() == CombatStats.AttackType.MELEE) {
            // Allow melee to hit adjacent including diagonals
            rangeTiles = Math.max(rangeTiles, 1);
            double threshold = Math.max(1.5, rangeTiles);
            return dist <= threshold;
        }
        return dist <= rangeTiles;
    }
}
