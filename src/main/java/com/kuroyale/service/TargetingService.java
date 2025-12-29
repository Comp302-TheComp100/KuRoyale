package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.ArrayList;

public class TargetingService {
    private static final int BASE_DETECTION_RADIUS = 5; // tiles

    public GridPosition findNearestEnemyOrObjective(GameState state, Troop troop) {
        Vector2 troopWorldPos = troop.getWorldPosition();
        GridPosition troopPos = troop.getPosition(); // Fallback for grid-based queries
        double bestDist = Double.MAX_VALUE;
        GridPosition bestPos = null;

        // Compute detection radius: ranged cards use range+2, melee use base radius
        int detectionRadius = BASE_DETECTION_RADIUS;
        double cardRange = troop.getBaseCard().getRange();
        if (cardRange > 0) {
            detectionRadius = (int) Math.floor(cardRange + 2);
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
        return findNearestEnemyTower(state.getArena(), troop);
    }

    /**
     * Find nearest enemy or objective for PvP game state.
     */
    public GridPosition findNearestEnemyOrObjectivePvP(com.kuroyale.model.logic.PvPGameState state, Troop troop) {
        Vector2 troopWorldPos = troop.getWorldPosition();
        GridPosition troopPos = troop.getPosition();
        double bestDist = Double.MAX_VALUE;
        GridPosition bestPos = null;

        int detectionRadius = BASE_DETECTION_RADIUS;
        double cardRange = troop.getBaseCard().getRange();
        if (cardRange > 0) {
            detectionRadius = (int) Math.floor(cardRange + 2);
        }

        com.kuroyale.model.logic.SpatialGrid grid = state.getArena().getSpatialGrid();
        java.util.List<ICombatant> candidates;

        if (grid != null) {
            candidates = grid.getNearby(troopPos, detectionRadius);
        } else {
            candidates = new ArrayList<>();
            candidates.addAll(state.getActiveTroops());
            candidates.addAll(state.getActiveBuildings());
        }

        for (ICombatant candidate : candidates) {
            if (!candidate.isAlive())
                continue;
            if (candidate.isPlayerSide() == troop.isPlayerSide())
                continue;

            if (candidate instanceof Troop other) {
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
        GridPosition towerTarget = findNearestEnemyTower(state.getArena(), troop);
        if (towerTarget == null) {
            System.out.println("[DEBUG] PvP Targeting: No target found for troop at " + troop.getPosition() +
                    ", isPlayerSide=" + troop.isPlayerSide() + ", towers=" + state.getArena().getAllTowers().size());
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
        // Check tiles around footprint borders (one tile outward)
        for (int dx = -1; dx <= w; dx++) {
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
            GridPosition closestTile = closestFootprintTile(tower, troopPos);
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

    private GridPosition closestFootprintTile(ICombatant combatant, GridPosition from) {
        GridPosition pos = combatant.getPosition();
        if (pos == null)
            return null;

        int x0 = pos.getX();
        int y0 = pos.getY();
        int w = combatant.getWidth();
        int h = combatant.getHeight();

        // Clamp 'from' coordinates to footprint bounds
        int closestX = Math.max(x0, Math.min(from.getX(), x0 + w - 1));
        int closestY = Math.max(y0, Math.min(from.getY(), y0 + h - 1));

        return GridPosition.tryCreate(closestX, closestY);
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
