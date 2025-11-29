package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class TroopMovementService {
    private final TargetingService targetingService = new TargetingService();
    private final PathfindingStrategy groundStrategy = new GroundPathfindingStrategy();
    private final PathfindingStrategy airStrategy = new AirDirectPathfindingStrategy();

    public void updateTroops(double deltaTime, GameState state, List<Troop> troops) {
        Arena arena = state.getArena();
        for (Troop troop : troops) {
            if (!troop.isAlive()) continue;
            if (troop.getTargetPosition() == null || shouldRetarget(state, troop)) {
                GridPosition newTarget = targetingService.findNearestEnemyOrObjective(state, troop);
                troop.setTargetPosition(newTarget);
                troop.clearPath();
                if (newTarget != null) {
                    PathfindingStrategy strategy = troop.isAirUnit() ? airStrategy : groundStrategy;
                    Deque<GridPosition> path = strategy.computePath(arena, troop, newTarget);
                    troop.setPath(path);
                }
            }
            advanceAlongPath(deltaTime, troop);
        }
    }

    private boolean shouldRetarget(GameState state, Troop troop) {
        // Instant retarget if new closer enemy appears inside attack range or path empty
        if (troop.getPath().isEmpty()) return true;
        GridPosition nearest = targetingService.findNearestEnemyOrObjective(state, troop);
        if (nearest == null) return false;
        double dist = troop.getPosition().getEuclideanDistanceTo(nearest);
        return dist < troop.getPosition().getEuclideanDistanceTo(troop.getTargetPosition());
    }

    private void advanceAlongPath(double deltaTime, Troop troop) {
        if (troop.getPath().isEmpty()) return;
        double deltaCells = troop.getMoveSpeed() * deltaTime;
        troop.addMoveProgress(deltaCells);
        while (troop.getMoveProgress() >= 1.0 && !troop.getPath().isEmpty()) {
            GridPosition next = troop.getPath().peekFirst();
            if (next.equals(troop.getPosition())) {
                troop.getPath().pollFirst();
                continue;
            }
            troop.setPosition(next);
            troop.getPath().pollFirst();
            troop.consumeMoveProgress(1.0);
        }
    }
}
