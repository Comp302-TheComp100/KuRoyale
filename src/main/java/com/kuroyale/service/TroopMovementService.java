package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.*;

public class TroopMovementService {
    private final TargetingService targetingService = new TargetingService();
    private final PathfindingStrategy groundStrategy = new GroundPathfindingStrategy();
    private final PathfindingStrategy airStrategy = new AirDirectPathfindingStrategy();

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
            // If in ATTACKING state, don't move
            if (troop.getUnitState() == UnitState.ATTACKING) {
                continue;
            }

            // Otherwise, move along path
            troop.setUnitState(UnitState.MOVING);
            advanceAlongPath(deltaTime, troop, state);
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

    /**
     * Update troops for PvP game state.
     */
    public void updateTroopsPvP(double deltaTime, com.kuroyale.model.logic.PvPGameState state, List<Troop> troops) {
        Arena arena = state.getArena();
        for (Troop troop : troops) {
            if (!troop.isAlive())
                continue;

            troop.setPathfindingCooldown(troop.getPathfindingCooldown() - deltaTime);

            // Only check for retargeting if cooldown is ready or no target
            if (troop.getTargetWorldPosition() == null
                    || (troop.getPathfindingCooldown() <= 0 && shouldRetargetPvP(state, troop))) {
                GridPosition newTargetGrid = targetingService.findNearestEnemyOrObjectivePvP(state, troop);

                // If target hasn't changed significantly, don't recompute path
                troop.setTargetWorldPosition(Vector2.fromGridPosition(newTargetGrid));
                troop.clearPath();
                if (newTargetGrid != null) {
                    PathfindingStrategy strategy = troop.isAirUnit() ? airStrategy : groundStrategy;
                    Deque<GridPosition> gridPath = strategy.computePath(arena, troop, newTargetGrid);
                    Deque<Vector2> worldPath = convertPathToVector2(gridPath);
                    troop.setPath(worldPath);
                }

                // Reset cooldown (randomize slightly to distribute load)
                troop.setPathfindingCooldown(0.25 + Math.random() * 0.1);
            }

            if (troop.getUnitState() == UnitState.ATTACKING) {
                continue;
            }

            troop.setUnitState(UnitState.MOVING);
            advanceAlongPathPvP(deltaTime, troop, state);
        }
    }

    private void advanceAlongPathPvP(double deltaTime, Troop troop, com.kuroyale.model.logic.PvPGameState state) {
        if (troop.getPath().isEmpty())
            return;

        Vector2 currentPos = troop.getWorldPosition();
        Vector2 nextWaypoint = troop.getPath().peekFirst();

        Vector2 toWaypoint = nextWaypoint.subtract(currentPos);
        double distanceToWaypoint = toWaypoint.length();

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
            return;
        }

        Vector2 direction = toWaypoint.normalize();
        double moveDistance = troop.getMoveSpeed() * deltaTime;

        if (moveDistance > distanceToWaypoint) {
            moveDistance = distanceToWaypoint;
        }

        Vector2 newPos = currentPos.add(direction.multiply(moveDistance));
        newPos = applySeparation(newPos, troop, state.getActiveTroops());
        troop.setWorldPosition(newPos);

        if (state.getArena() != null && state.getArena().getSpatialGrid() != null) {
            state.getArena().getSpatialGrid().update(troop);
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

    private boolean shouldRetargetPvP(com.kuroyale.model.logic.PvPGameState state, Troop troop) {
        // Instant retarget if new closer enemy appears inside attack range or path
        if (troop.getPath().isEmpty())
            return true;
        GridPosition nearestGrid = targetingService.findNearestEnemyOrObjectivePvP(state, troop);
        if (nearestGrid == null)
            return false;
        Vector2 nearest = Vector2.fromGridPosition(nearestGrid);
        Vector2 currentPos = troop.getWorldPosition();
        Vector2 currentTarget = troop.getTargetWorldPosition();
        if (currentTarget == null)
            return true;
        return currentPos.distanceTo(nearest) < currentPos.distanceTo(currentTarget);
    }

    // Converts a GridPosition path to Vector2 waypoints (centered on tiles).
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

        // Update SpatialGrid
        if (state != null && state.getArena() != null && state.getArena().getSpatialGrid() != null) {
            state.getArena().getSpatialGrid().update(troop);
        }
    }

    // Simple separation steering to prevent troops from overlapping.
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

}
