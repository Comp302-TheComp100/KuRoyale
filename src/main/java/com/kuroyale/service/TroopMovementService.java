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
            if (!troop.isAlive()) {
                toRemove.add(troop);
                continue;
            }

            // Update pathfinding cooldown
            troop.setPathfindingCooldown(troop.getPathfindingCooldown() - deltaTime);

            // Force retarget if current target is dead or null (sync with CombatService)
            ICombatant currentTarget = troop.getTarget();
            boolean hasNoTarget = (troop.getTargetWorldPosition() == null);
            boolean targetIsDead = (currentTarget != null && !currentTarget.isAlive());
            boolean cooldownReady = (troop.getPathfindingCooldown() <= 0);

            // Only check for retargeting if forced or cooldown is ready
            if (hasNoTarget || targetIsDead || (cooldownReady && shouldRetarget(state, troop))) {
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
            // Apply movement and/or separation
            // Note: Even attacking units need separation to avoid stacking
            if (troop.getUnitState() != UnitState.ATTACKING) {
                troop.setUnitState(UnitState.MOVING);
            }
            updateTroopPosition(deltaTime, troop, state);
        }
        // Cleanup: remove destroyed troops and notify others to retarget/move
        if (!toRemove.isEmpty()) {
            troops.removeAll(toRemove);
            for (Troop t : troops) {
                if (!t.isAlive())
                    continue;

                // If their target was removed, clear and allow retarget next frame
                ICombatant target = t.getTarget();
                if (target != null && !target.isAlive()) {
                    t.setTarget(null);
                    t.setTargetWorldPosition(null);
                    t.clearPath();
                    t.setUnitState(UnitState.IDLE);
                } else {
                    // Fallback for distance-based targeting if target entity isn't set
                    for (Troop dead : toRemove) {
                        Vector2 targetPos = t.getTargetWorldPosition();
                        Vector2 deadPos = dead.getWorldPosition();
                        if (targetPos != null && deadPos != null && targetPos.distanceTo(deadPos) < 1.0) {
                            t.setTargetWorldPosition(null);
                            t.clearPath();
                            t.setUnitState(UnitState.IDLE);
                            break;
                        }
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

            // Apply movement and/or separation
            // Note: Even attacking units need separation to avoid stacking
            if (troop.getUnitState() != UnitState.ATTACKING) {
                troop.setUnitState(UnitState.MOVING);
            }
            updatePvPTroopPosition(deltaTime, troop, state);
        }
    }

    private void updatePvPTroopPosition(double deltaTime, Troop troop, com.kuroyale.model.logic.PvPGameState state) {
        Vector2 currentPos = troop.getWorldPosition();
        Vector2 movement = Vector2.ZERO;

        // Calculate path movement only if not attacking and path exists
        if (troop.getUnitState() != UnitState.ATTACKING && !troop.getPath().isEmpty()) {
            Vector2 nextWaypoint = troop.getPath().peekFirst();
            Vector2 toWaypoint = nextWaypoint.subtract(currentPos);
            double distanceToWaypoint = toWaypoint.length();

            // Check if we've reached the waypoint
            if (distanceToWaypoint < WAYPOINT_THRESHOLD) {
                troop.getPath().pollFirst();
                if (!troop.getPath().isEmpty()) {
                    nextWaypoint = troop.getPath().peekFirst();
                    toWaypoint = nextWaypoint.subtract(currentPos);
                    distanceToWaypoint = toWaypoint.length();
                }
            }

            // Only move if we have a valid destination vector
            if (distanceToWaypoint > 1e-6) {
                Vector2 direction = toWaypoint.normalize();
                double moveDistance = troop.getMoveSpeed() * deltaTime;
                if (moveDistance > distanceToWaypoint) {
                    moveDistance = distanceToWaypoint;
                }
                movement = direction.multiply(moveDistance);
            }
        }

        Vector2 proposedPos = currentPos.add(movement);

        // Apply separation (Always applies, even if stationary/attacking)
        SpatialGrid spatialGrid = (state != null && state.getArena() != null) ? state.getArena().getSpatialGrid()
                : null;
        Vector2 finalPos = applySeparation(proposedPos, troop, spatialGrid);

        troop.setWorldPosition(finalPos);

        // Update SpatialGrid
        if (state != null && state.getArena() != null && state.getArena().getSpatialGrid() != null) {
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
    private static final double SEPARATION_RADIUS = 0.5;

    private void updateTroopPosition(double deltaTime, Troop troop, GameState state) {
        Vector2 currentPos = troop.getWorldPosition();
        Vector2 movement = Vector2.ZERO;

        // Calculate path movement only if not attacking and path exists
        if (troop.getUnitState() != UnitState.ATTACKING && !troop.getPath().isEmpty()) {
            Vector2 nextWaypoint = troop.getPath().peekFirst();
            Vector2 toWaypoint = nextWaypoint.subtract(currentPos);
            double distanceToWaypoint = toWaypoint.length();

            // Check if we've reached the waypoint
            if (distanceToWaypoint < WAYPOINT_THRESHOLD) {
                troop.getPath().pollFirst();
                if (!troop.getPath().isEmpty()) {
                    nextWaypoint = troop.getPath().peekFirst();
                    toWaypoint = nextWaypoint.subtract(currentPos);
                    distanceToWaypoint = toWaypoint.length();
                }
            }

            // Only move if we have a valid destination vector
            if (distanceToWaypoint > 1e-6) {
                // DON'T normalize if distance is tiny, but here we checked > 1e-6
                Vector2 direction = toWaypoint.normalize();
                double moveDistance = troop.getMoveSpeed() * deltaTime;
                if (moveDistance > distanceToWaypoint) {
                    moveDistance = distanceToWaypoint;
                }
                movement = direction.multiply(moveDistance);
            }
        }

        Vector2 proposedPos = currentPos.add(movement);

        // Apply separation (Always applies, even if stationary/attacking)
        SpatialGrid spatialGrid = (state != null && state.getArena() != null) ? state.getArena().getSpatialGrid()
                : null;
        Vector2 finalPos = applySeparation(proposedPos, troop, spatialGrid);

        troop.setWorldPosition(finalPos);

        // Update SpatialGrid
        if (state != null && state.getArena() != null && state.getArena().getSpatialGrid() != null) {
            state.getArena().getSpatialGrid().update(troop);
        }
    }

    // Simple separation steering to prevent troops from overlapping.
    private Vector2 applySeparation(Vector2 proposedPos, Troop self, SpatialGrid grid) {
        if (grid == null)
            return proposedPos;

        Vector2 separation = Vector2.ZERO;
        int count = 0;

        // Query only nearby entities using SpatialGrid (O(k) instead of O(N))
        List<ICombatant> nearby = grid.getNearby(proposedPos.toGridPosition(), SEPARATION_RADIUS + 1.0);

        for (ICombatant candidate : nearby) {
            if (!(candidate instanceof Troop other))
                continue;
            if (other == self || !other.isAlive())
                continue;

            Vector2 otherPos = other.getWorldPosition();
            if (otherPos == null)
                continue;

            double dist = proposedPos.distanceTo(otherPos);

            if (dist < SEPARATION_RADIUS) {
                Vector2 away;
                if (dist < 1e-3) {
                    // Exact overlap or very close: random push
                    double angle = Math.random() * 2 * Math.PI;
                    away = new Vector2(Math.cos(angle), Math.sin(angle));
                } else {
                    // Push away from other troop (radial)
                    away = proposedPos.subtract(otherPos).normalize();

                    // Add a tangential (perpendicular) component to encourage sliding/passing
                    // This prevents the "straight line push" where units get stuck head-to-head.
                    // By adding a perpendicular vector, we suggest a side to pass on.
                    Vector2 tangential = new Vector2(-away.getY(), away.getX());

                    // Mix in some tangential force (e.g., 0.5). This makes the push
                    // diagonal rather than straight back, helping units "flow" around each other.
                    away = away.add(tangential.multiply(0.5)).normalize();
                }

                // Strength increases as they get closer
                double strength = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
                // Increased push force (0.4) to effectively separate clustered units
                separation = separation.add(away.multiply(strength * 0.4));
                count++;
            }
        }

        if (count > 0) {
            return proposedPos.add(separation);
        }
        return proposedPos;
    }

}
