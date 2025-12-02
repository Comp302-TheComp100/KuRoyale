package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class TargetingService {
    private static final int BASE_DETECTION_RADIUS = 5; // tiles
    public GridPosition findNearestEnemyOrObjective(GameState state, Troop troop) {
        GridPosition troopPos = troop.getPosition();
        double bestDist = Double.MAX_VALUE;
        GridPosition bestPos = null;

        // Compute detection radius: ranged cards use range+2, melee use base radius
        int detectionRadius = BASE_DETECTION_RADIUS;
        double cardRange = troop.getBaseCard().getRange();
        if (cardRange > 0) {
            detectionRadius = (int) Math.floor(cardRange + 2);
        }

        // Consider active enemy troops within detection radius
        for (Troop other : state.getActiveTroops()) {
            if (other.isPlayerSide() == troop.isPlayerSide()) continue;
            // Building only troops ignore enemy troops
            if (troop.isBuildingOnly()) continue;
            // Ground troops cannot target air only enemies if target type is ground
            if (troop.getBaseCard().getTarget() == TargetType.GROUND && other.isAirUnit()) continue;
            // Air only attackers cannot hit ground only if target type is air; handled by both
            GridPosition pos = other.getPosition();
            double dist = troopPos.getEuclideanDistanceTo(pos);
            if (dist <= detectionRadius && dist < bestDist) { bestDist = dist; bestPos = pos; }
        }

        // Consider active enemy buildings
        for (Building b : state.getActiveBuildings()) {
            if (!b.isAlive()) continue;
            if (b.isPlayerSide() == troop.isPlayerSide()) continue;
            if (troop.isBuildingOnly()) {
                // building-only troops prioritize buildings
            }
            // Use nearest perimeter tile around the building footprint as target
            GridPosition perimeter = nearestPerimeterTile(state.getArena(), b, troopPos);
            if (perimeter == null) continue;
            double dist = troopPos.getEuclideanDistanceTo(perimeter);
            if (dist < bestDist) { bestDist = dist; bestPos = perimeter; }
        }

        if (bestPos != null) return bestPos;
        // fallback to nearest enemy tower cell
        return findNearestEnemyTower(state.getArena(), troop);
    }

    private GridPosition buildingCenter(Building b) {
        if (b == null || b.getPosition() == null) return null;
        int cx = b.getPosition().getX() + Math.max(0, b.getWidth() - 1) / 2;
        int cy = b.getPosition().getY() + Math.max(0, b.getHeight() - 1) / 2;
        return GridPosition.tryCreate(cx, cy);
    }

    // Find the nearest walkable, unoccupied perimeter tile adjacent to the building footprint
    private GridPosition nearestPerimeterTile(Arena arena, Building b, GridPosition from) {
        int x0 = b.getPosition().getX();
        int y0 = b.getPosition().getY();
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        GridPosition best = null;
        double bestDist = Double.MAX_VALUE;
        // Check tiles around footprint borders (one tile outward)
        for (int dx = -1; dx <= w; dx++) {
            // top perimeter
            GridPosition p1 = GridPosition.tryCreate(x0 + dx, y0 - 1);
            best = pickIfBetter(arena, from, best, p1, bestDist);
            if (best != null) bestDist = from.getEuclideanDistanceTo(best);
            // bottom perimeter
            GridPosition p2 = GridPosition.tryCreate(x0 + dx, y0 + h);
            best = pickIfBetter(arena, from, best, p2, bestDist);
            if (best != null) bestDist = from.getEuclideanDistanceTo(best);
        }
        for (int dy = 0; dy < h; dy++) {
            // left perimeter
            GridPosition p3 = GridPosition.tryCreate(x0 - 1, y0 + dy);
            best = pickIfBetter(arena, from, best, p3, bestDist);
            if (best != null) bestDist = from.getEuclideanDistanceTo(best);
            // right perimeter
            GridPosition p4 = GridPosition.tryCreate(x0 + w, y0 + dy);
            best = pickIfBetter(arena, from, best, p4, bestDist);
            if (best != null) bestDist = from.getEuclideanDistanceTo(best);
        }
        return best;
    }

    private GridPosition pickIfBetter(Arena arena, GridPosition from, GridPosition currentBest, GridPosition candidate, double currentBestDist) {
        if (candidate == null) return currentBest;
        GridCell cell = arena.getCell(candidate);
        if (cell == null || !cell.canPlaceUnit() || cell.isOccupied()) return currentBest;
        double dist = from.getEuclideanDistanceTo(candidate);
        if (currentBest == null || dist < currentBestDist) {
            return candidate;
        }
        return currentBest;
    }

    private GridPosition findNearestEnemyTower(Arena arena, Troop troop) {
        GridPosition bestPos = null;
        double bestDist = Double.MAX_VALUE;
        for (GridCell cell : arena.getAllCells()) {
            TileType t = cell.getTileType();
            boolean enemyTower = troop.isPlayerSide() ? (t == TileType.PRINCESS_TOWER_COMPUTER || t == TileType.KING_TOWER_COMPUTER)
                    : (t == TileType.PRINCESS_TOWER_USER || t == TileType.KING_TOWER_USER);
            if (enemyTower) {
                double dist = troop.getPosition().getEuclideanDistanceTo(cell.getPosition());
                if (dist < bestDist) { bestDist = dist; bestPos = cell.getPosition(); }
            }
        }
        return bestPos;
    }

    public boolean isValidTarget(Troop attacker, Troop candidate) {
        if (!candidate.isAlive()) return false;
        if (attacker.isBuildingOnly()) return false; // building-only troops ignore enemy troops
        if (attacker.getBaseCard().getTarget() == TargetType.GROUND && candidate.isAirUnit()) return false;
        return true;
    }

    public boolean isInRange(Troop attacker, Troop target) {
        GridPosition a = attacker.getPosition();
        GridPosition b = target.getPosition();
        double dist = a.getEuclideanDistanceTo(b);
        double rangeTiles = attacker.getCombatStats() != null ? attacker.getCombatStats().getRangeTiles() : attacker.getAttackRange();
        if (attacker.getCombatStats() != null && attacker.getCombatStats().getAttackType() == CombatStats.AttackType.MELEE) {
            // Allow melee to hit adjacent including diagonals
            rangeTiles = Math.max(rangeTiles, 1);
            double threshold = Math.max(1.5, rangeTiles);
            return dist <= threshold;
        }
        return dist <= rangeTiles;
    }
}