package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class TargetingService {
    public GridPosition findNearestEnemyOrObjective(GameState state, Troop troop) {
        List<GameState.PlacedCard> placed = state.getPlacedCards();
        GridPosition troopPos = troop.getPosition();
        GameState.PlacedCard best = null;
        double bestDist = Double.MAX_VALUE;
        for (GameState.PlacedCard pc : placed) {
            if (pc.isPlayer == troop.isPlayerSide()) continue;
            if (troop.isBuildingOnly() && pc.card.getType() != CardType.BUILDING) continue;
            if (!troop.isBuildingOnly() && pc.card.getType() == CardType.SPELL) continue;
            GridPosition pos = GridPosition.tryCreate(pc.x, pc.y);
            if (pos == null) continue;
            double dist = troopPos.getEuclideanDistanceTo(pos);
            if (dist < bestDist) { bestDist = dist; best = pc; }
        }
        if (best != null) return GridPosition.tryCreate(best.x, best.y);
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
