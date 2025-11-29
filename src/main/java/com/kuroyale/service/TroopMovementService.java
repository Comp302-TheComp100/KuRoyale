package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class TroopMovementService {
    private final TargetingService targetingService = new TargetingService();
    private final PathfindingStrategy groundStrategy = new GroundPathfindingStrategy();
    private final PathfindingStrategy airStrategy = new AirDirectPathfindingStrategy();
    private final CombatService combatService = new CombatService();

    public void updateTroops(double deltaTime, GameState state, List<Troop> troops) {
        Arena arena = state.getArena();
        java.util.List<Troop> toRemove = new java.util.ArrayList<>();
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
            // If in attack range, handle combat; else move
            Troop targetTroop = findNearestEnemyTroopInRange(state, troop);
            boolean canAttack = targetTroop != null;
            if (canAttack) {
                troop.setUnitState(UnitState.ATTACKING);
                handleAttack(deltaTime, troop, targetTroop);
                if (!targetTroop.isAlive()) {
                    targetTroop.setUnitState(UnitState.DESTROYED);
                    toRemove.add(targetTroop);
                }
            } else {
                troop.setUnitState(UnitState.MOVING);
                advanceAlongPath(deltaTime, troop, state);
            }
        }
        // Cleanup: remove destroyed troops and notify others to retarget/move
        if (!toRemove.isEmpty()) {
            troops.removeAll(toRemove);
            for (Troop t : troops) {
                if (!t.isAlive()) continue;
                // If their target was removed, clear and allow retarget
                for (Troop dead : toRemove) {
                    if (t.getTargetPosition() != null && t.getTargetPosition().equals(dead.getPosition())) {
                        t.setTargetPosition(null);
                        t.clearPath();
                        t.setUnitState(UnitState.IDLE);
                    }
                }
            }
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

    private void advanceAlongPath(double deltaTime, Troop troop, GameState state) {
        if (troop.getPath().isEmpty()) return;
        double deltaCells = troop.getMoveSpeed() * deltaTime;
        troop.addMoveProgress(deltaCells);
        while (troop.getMoveProgress() >= 1.0 && !troop.getPath().isEmpty()) {
            GridPosition next = troop.getPath().peekFirst();
            if (next.equals(troop.getPosition())) {
                troop.getPath().pollFirst();
                continue;
            }
            // Collision avoidance: do not move into a tile occupied by another troop
            if (isTileFree(next, troop, state.getActiveTroops())) {
                troop.setPosition(next);
                troop.getPath().pollFirst();
                troop.consumeMoveProgress(1.0);
            } else {
                // Stop advancing this tick; optionally could try alternative paths
                break;
            }
        }
    }

    private boolean isTileFree(GridPosition position, Troop self, java.util.List<Troop> troops) {
        for (Troop t : troops) {
            if (t == self) continue;
            if (!t.isAlive()) continue;
            if (t.getPosition().equals(position)) {
                return false;
            }
        }
        return true;
    }

    private Troop findEnemyTroopAt(GameState state, GridPosition pos, boolean isPlayer) {
        if (pos == null) return null;
        for (Troop t : state.getActiveTroops()) {
            if (t.isPlayerSide() == isPlayer) continue;
            if (t.getPosition().equals(pos)) return t;
        }
        return null;
    }

    private Troop findNearestEnemyTroopInRange(GameState state, Troop self) {
        Troop best = null;
        double bestDist = Double.MAX_VALUE;
        for (Troop t : state.getActiveTroops()) {
            if (!t.isAlive()) continue;
            if (t.isPlayerSide() == self.isPlayerSide()) continue;
            if (!targetingService.isValidTarget(self, t)) continue;
            double dist = self.getPosition().getEuclideanDistanceTo(t.getPosition());
            if (targetingService.isInRange(self, t) && dist < bestDist) {
                bestDist = dist;
                best = t;
            }
        }
        return best;
    }

    private void handleAttack(double deltaTime, Troop attacker, Troop target) {
        double cd = attacker.getAttackCooldown() - deltaTime;
        if (cd <= 0) {
            combatService.applyDamage(attacker, target);
            attacker.setAttackCooldown(attacker.getCombatStats().getHitSpeedSeconds());
        } else {
            attacker.setAttackCooldown(cd);
        }
    }
}
