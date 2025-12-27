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
            if (!troop.isAlive())
                continue;

            // Update pathfinding cooldown
            troop.setPathfindingCooldown(troop.getPathfindingCooldown() - deltaTime);

            // Only check for retargeting if cooldown is ready or no target
            if (troop.getTargetWorldPosition() == null
                    || (troop.getPathfindingCooldown() <= 0 && shouldRetarget(state, troop))) {
                GridPosition newTargetGrid = targetingService.findNearestEnemyOrObjective(state, troop);

                // If target hasn't changed significantly, don't recompute path
                troop.setTargetWorldPosition(Vector2.fromGridPosition(newTargetGrid));
                troop.clearPath();
                if (newTargetGrid != null) {
                    PathfindingStrategy strategy = troop.isAirUnit() ? airStrategy : groundStrategy;
                    Deque<GridPosition> gridPath = strategy.computePath(arena, troop, newTargetGrid);
                    // Convert GridPosition path to Vector2 waypoints
                    Deque<Vector2> worldPath = convertPathToVector2(gridPath);
                    troop.setPath(worldPath);
                }

                // Reset cooldown (randomize slightly to distribute load)
                troop.setPathfindingCooldown(0.25 + Math.random() * 0.1);
            }
            // If in attack range, handle combat; else move
            Troop targetTroop = findNearestEnemyTroopInRange(state, troop);
            Building targetBuilding = null;
            Tower targetTower = null;
            boolean canAttack = targetTroop != null;
            if (!canAttack) {
                targetBuilding = findNearestEnemyBuildingInRange(state, troop);
                canAttack = targetBuilding != null;
            }
            if (!canAttack) {
                targetTower = findNearestEnemyTowerInRange(state, troop);
                canAttack = targetTower != null;
            }
            if (canAttack && targetTroop != null) {
                troop.setUnitState(UnitState.ATTACKING);
                handleAttack(deltaTime, state, troop, targetTroop);
                if (!targetTroop.isAlive()) {
                    targetTroop.setUnitState(UnitState.DESTROYED);
                    toRemove.add(targetTroop);
                }
            } else if (canAttack && targetBuilding != null) {
                troop.setUnitState(UnitState.ATTACKING);
                handleAttack(deltaTime, state, troop, targetBuilding);
            } else if (canAttack && targetTower != null) {
                troop.setUnitState(UnitState.ATTACKING);
                handleAttack(deltaTime, state, troop, targetTower);
            } else {
                troop.setUnitState(UnitState.MOVING);
                advanceAlongPath(deltaTime, troop, state);
            }
        }
        // Cleanup: remove destroyed troops and notify others to retarget/move
        if (!toRemove.isEmpty()) {
            troops.removeAll(toRemove);
            for (Troop t : troops) {
                if (!t.isAlive())
                    continue;
                // If their target was removed, clear and allow retarget
                for (Troop dead : toRemove) {
                    Vector2 targetPos = t.getTargetWorldPosition();
                    Vector2 deadPos = dead.getWorldPosition();
                    if (targetPos != null && deadPos != null && targetPos.distanceTo(deadPos) < 1.0) {
                        t.setTargetWorldPosition(null);
                        t.clearPath();
                        t.setUnitState(UnitState.IDLE);
                    }
                }
            }
        }
    }

    private boolean shouldRetarget(GameState state, Troop troop) {
        // Instant retarget if new closer enemy appears inside attack range or path
        if (troop.getPath().isEmpty())
            return true;
        GridPosition nearestGrid = targetingService.findNearestEnemyOrObjective(state, troop);
        if (nearestGrid == null)
            return false;
        Vector2 nearest = Vector2.fromGridPosition(nearestGrid);
        Vector2 currentPos = troop.getWorldPosition();
        Vector2 currentTarget = troop.getTargetWorldPosition();
        if (currentTarget == null)
            return true;
        return currentPos.distanceTo(nearest) < currentPos.distanceTo(currentTarget);
    }

    /**
     * Converts a GridPosition path to Vector2 waypoints (centered on tiles).
     */
    private Deque<Vector2> convertPathToVector2(Deque<GridPosition> gridPath) {
        Deque<Vector2> worldPath = new ArrayDeque<>();
        for (GridPosition gp : gridPath) {
            worldPath.add(Vector2.fromGridPosition(gp));
        }
        return worldPath;
    }

    private static final double WAYPOINT_THRESHOLD = 0.1; // How close to waypoint before moving to next
    private static final double SEPARATION_RADIUS = 0.4; // Minimum distance between troops

    private void advanceAlongPath(double deltaTime, Troop troop, GameState state) {
        if (troop.getPath().isEmpty())
            return;

        Vector2 currentPos = troop.getWorldPosition();
        Vector2 nextWaypoint = troop.getPath().peekFirst();

        // Calculate direction to next waypoint
        Vector2 toWaypoint = nextWaypoint.subtract(currentPos);
        double distanceToWaypoint = toWaypoint.length();

        // Check if we've reached the waypoint
        if (distanceToWaypoint < WAYPOINT_THRESHOLD) {
            troop.getPath().pollFirst();
            if (troop.getPath().isEmpty()) {
                return;
            }
            nextWaypoint = troop.getPath().peekFirst();
            toWaypoint = nextWaypoint.subtract(currentPos);
            distanceToWaypoint = toWaypoint.length();
        }

        if (distanceToWaypoint < 1e-6) {
            return; // Already at destination
        }

        // Normalize direction and calculate movement
        Vector2 direction = toWaypoint.normalize();
        double moveDistance = troop.getMoveSpeed() * deltaTime;

        // Don't overshoot the waypoint
        if (moveDistance > distanceToWaypoint) {
            moveDistance = distanceToWaypoint;
        }

        // Calculate new position
        Vector2 newPos = currentPos.add(direction.multiply(moveDistance));

        // Apply simple separation from other troops
        newPos = applySeparation(newPos, troop, state.getActiveTroops());

        troop.setWorldPosition(newPos);
    }

    /**
     * Simple separation steering to prevent troops from overlapping.
     */
    private Vector2 applySeparation(Vector2 proposedPos, Troop self, List<Troop> troops) {
        Vector2 separation = Vector2.ZERO;
        int count = 0;

        for (Troop other : troops) {
            if (other == self || !other.isAlive())
                continue;

            Vector2 otherPos = other.getWorldPosition();
            double dist = proposedPos.distanceTo(otherPos);

            if (dist < SEPARATION_RADIUS && dist > 1e-6) {
                // Push away from other troop
                Vector2 away = proposedPos.subtract(otherPos).normalize();
                double strength = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
                separation = separation.add(away.multiply(strength * 0.1));
                count++;
            }
        }

        if (count > 0) {
            return proposedPos.add(separation);
        }
        return proposedPos;
    }

    private boolean isTileFree(GridPosition position, Troop self, java.util.List<Troop> troops) {
        for (Troop t : troops) {
            if (t == self)
                continue;
            if (!t.isAlive())
                continue;
            if (t.getPosition().equals(position)) {
                return false;
            }
        }
        return true;
    }

    private Troop findEnemyTroopAt(GameState state, GridPosition pos, boolean isPlayer) {
        if (pos == null)
            return null;
        for (Troop t : state.getActiveTroops()) {
            if (t.isPlayerSide() == isPlayer)
                continue;
            if (t.getPosition().equals(pos))
                return t;
        }
        return null;
    }

    private Troop findNearestEnemyTroopInRange(GameState state, Troop self) {
        Troop best = null;
        double bestDist = Double.MAX_VALUE;
        for (Troop t : state.getActiveTroops()) {
            if (!t.isAlive())
                continue;
            if (t.isPlayerSide() == self.isPlayerSide())
                continue;
            if (!targetingService.isValidTarget(self, t))
                continue;
            double dist = self.getPosition().getEuclideanDistanceTo(t.getPosition());
            if (targetingService.isInRange(self, t) && dist < bestDist) {
                bestDist = dist;
                best = t;
            }
        }
        return best;
    }

    private void handleAttack(double deltaTime, GameState state, Troop attacker, Troop target) {
        double cd = attacker.getAttackCooldown() - deltaTime;
        if (cd <= 0) {
            if (attacker.getBaseCard() != null && attacker.getBaseCard().isAreaEffect()) {
                state.applyAreaDamageFromTroop(attacker, target);
            } else {
                combatService.applyDamage(attacker, target);
            }
            attacker.setAttackCooldown(attacker.getCombatStats().getHitSpeedSeconds());
        } else {
            attacker.setAttackCooldown(cd);
        }
    }

    private void handleAttack(double deltaTime, GameState state, Troop attacker, Building target) {
        double cd = attacker.getAttackCooldown() - deltaTime;
        if (cd <= 0) {
            if (attacker.getBaseCard() != null && attacker.getBaseCard().isAreaEffect()) {
                state.applyAreaDamageFromTroop(attacker, target);
            } else {
                combatService.applyDamage(attacker, target);
            }
            attacker.setAttackCooldown(attacker.getCombatStats().getHitSpeedSeconds());
        } else {
            attacker.setAttackCooldown(cd);
        }
    }

    private void handleAttack(double deltaTime, GameState state, Troop attacker, Tower target) {
        double cd = attacker.getAttackCooldown() - deltaTime;
        if (cd <= 0) {
            if (attacker.getBaseCard() != null && attacker.getBaseCard().isAreaEffect()) {
                state.applyAreaDamageFromTroop(attacker, target);
            } else {
                combatService.applyDamage(attacker, target);
            }
            attacker.setAttackCooldown(attacker.getCombatStats().getHitSpeedSeconds());
        } else {
            attacker.setAttackCooldown(cd);
        }
    }

    private Building findNearestEnemyBuildingInRange(GameState state, Troop self) {
        Building best = null;
        double bestDist = Double.MAX_VALUE;
        for (Building b : state.getActiveBuildings()) {
            if (!b.isAlive())
                continue;
            if (b.isPlayerSide() == self.isPlayerSide())
                continue;
            if (self.getBaseCard().getTarget() == TargetType.AIR)
                continue;
            // Measure distance to nearest perimeter tile of building footprint
            double dist = distanceToBuildingPerimeter(state.getArena(), self.getPosition(), b);
            double rangeTiles = self.getCombatStats() != null ? self.getCombatStats().getRangeTiles()
                    : self.getAttackRange();
            boolean inRange;
            if (self.getCombatStats() != null
                    && self.getCombatStats().getAttackType() == CombatStats.AttackType.MELEE) {
                rangeTiles = Math.max(rangeTiles, 1);
                double threshold = Math.max(1.5, rangeTiles);
                inRange = dist <= threshold;
            } else {
                inRange = dist <= rangeTiles;
            }
            if (inRange && dist < bestDist) {
                bestDist = dist;
                best = b;
            }
        }
        return best;
    }

    private Tower findNearestEnemyTowerInRange(GameState state, Troop self) {
        Tower best = null;
        double bestDist = Double.MAX_VALUE;
        Arena arena = state.getArena();
        // Compute groups of tower footprints to measure distance to perimeter
        java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType t = cell.getTileType();
            boolean enemyTowerTile = self.isPlayerSide()
                    ? (t == TileType.PRINCESS_TOWER_COMPUTER || t == TileType.KING_TOWER_COMPUTER)
                    : (t == TileType.PRINCESS_TOWER_USER || t == TileType.KING_TOWER_USER);
            if (!enemyTowerTile)
                continue;
            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null || tower.getCurrentHealth() <= 0)
                continue;
            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }
        for (java.util.Map.Entry<Tower, java.util.List<GridCell>> e : groups.entrySet()) {
            Tower tower = e.getKey();
            double dist = distanceToTowerPerimeter(self.getPosition(), e.getValue());
            double rangeTiles = self.getCombatStats() != null ? self.getCombatStats().getRangeTiles()
                    : self.getAttackRange();
            boolean inRange;
            if (self.getCombatStats() != null
                    && self.getCombatStats().getAttackType() == CombatStats.AttackType.MELEE) {
                rangeTiles = Math.max(rangeTiles, 1);
                double threshold = Math.max(1.5, rangeTiles);
                inRange = dist <= threshold;
            } else {
                inRange = dist <= rangeTiles;
            }
            if (inRange && dist < bestDist) {
                bestDist = dist;
                best = tower;
            }
        }
        return best;
    }

    private double distanceToBuildingPerimeter(Arena arena, GridPosition from, Building b) {
        int x0 = b.getPosition().getX();
        int y0 = b.getPosition().getY();
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());
        double best = Double.MAX_VALUE;
        // distance to footprint boundary cells (edge of the rectangle)
        for (int dx = 0; dx < w; dx++) {
            GridPosition top = GridPosition.tryCreate(x0 + dx, y0);
            GridPosition bottom = GridPosition.tryCreate(x0 + dx, y0 + h - 1);
            if (top != null)
                best = Math.min(best, from.getEuclideanDistanceTo(top));
            if (bottom != null)
                best = Math.min(best, from.getEuclideanDistanceTo(bottom));
        }
        for (int dy = 0; dy < h; dy++) {
            GridPosition left = GridPosition.tryCreate(x0, y0 + dy);
            GridPosition right = GridPosition.tryCreate(x0 + w - 1, y0 + dy);
            if (left != null)
                best = Math.min(best, from.getEuclideanDistanceTo(left));
            if (right != null)
                best = Math.min(best, from.getEuclideanDistanceTo(right));
        }
        return best;
    }

    private double distanceToTowerPerimeter(GridPosition from, java.util.List<GridCell> cells) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (GridCell c : cells) {
            int x = c.getPosition().getX();
            int y = c.getPosition().getY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        double best = Double.MAX_VALUE;
        // distance to footprint boundary cells (edge of the rectangle)
        for (int x = minX; x <= maxX; x++) {
            GridPosition top = GridPosition.tryCreate(x, minY);
            GridPosition bottom = GridPosition.tryCreate(x, maxY);
            if (top != null)
                best = Math.min(best, from.getEuclideanDistanceTo(top));
            if (bottom != null)
                best = Math.min(best, from.getEuclideanDistanceTo(bottom));
        }
        for (int y = minY; y <= maxY; y++) {
            GridPosition left = GridPosition.tryCreate(minX, y);
            GridPosition right = GridPosition.tryCreate(maxX, y);
            if (left != null)
                best = Math.min(best, from.getEuclideanDistanceTo(left));
            if (right != null)
                best = Math.min(best, from.getEuclideanDistanceTo(right));
        }
        return best;
    }
}