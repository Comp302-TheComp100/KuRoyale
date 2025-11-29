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

        // 1) Consider active enemy troops within detection radius
        for (Troop other : state.getActiveTroops()) {
            if (other.isPlayerSide() == troop.isPlayerSide()) continue;
            // Building-only troops ignore enemy troops
            if (troop.isBuildingOnly()) continue;
            // Ground troops cannot target air-only enemies if target type is GROUND
            if (troop.getBaseCard().getTarget() == TargetType.GROUND && other.isAirUnit()) continue;
            // Air-only attackers cannot hit ground-only if target type is AIR (edge-case); handled by BOTH
            GridPosition pos = other.getPosition();
            double dist = troopPos.getEuclideanDistanceTo(pos);
            if (dist <= detectionRadius && dist < bestDist) { bestDist = dist; bestPos = pos; }
        }

        // 2) Consider placed enemy buildings (and spells ignored)
        for (GameState.PlacedCard pc : state.getPlacedCards()) {
            if (pc.isPlayer == troop.isPlayerSide()) continue;
            if (pc.card.getType() == CardType.SPELL) continue; // ignore spells as targets
            if (troop.isBuildingOnly() && pc.card.getType() != CardType.BUILDING) continue;
            GridPosition pos = GridPosition.tryCreate(pc.x, pc.y);
            if (pos == null) continue;
            double dist = troopPos.getEuclideanDistanceTo(pos);
            if (dist < bestDist) { bestDist = dist; bestPos = pos; }
        }

        if (bestPos != null) return bestPos;
        // fallback to nearest enemy tower cell
        return findNearestEnemyTower(state.getArena(), troop);
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
}
