package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.*;

public class TroopMovementService {
    private final TargetingService targetingService = new TargetingService();
    private final PathfindingStrategy groundStrategy = new GroundPathfindingStrategy();
    private final PathfindingStrategy airStrategy = new AirDirectPathfindingStrategy();

    public void updateTroops(double deltaTime, IBattleState state, List<Troop> troops) {
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
            boolean targetIsDead = (currentTarget != null && !currentTarget.isAlive());
            boolean hasNoTargetPos = (troop.getTargetWorldPosition() == null);
            boolean cooldownReady = (troop.getPathfindingCooldown() <= 0);

            // Instant reaction if target is dead: clear path and reset state
            if (targetIsDead) {
                troop.setTarget(null);
                troop.setTargetWorldPosition(null);
                troop.clearPath();
                troop.setUnitState(UnitState.IDLE);
                // Force immediate retargeting logic below
            }

            // Check for retargeting if forced (dead/null) or periodic cooldown ready
            if (targetIsDead || hasNoTargetPos || (cooldownReady && shouldRetarget(state, troop))) {
                GridPosition newTargetGrid = targetingService.findNearestEnemyOrObjective(state, troop);

                // If target hasn't changed significantly, don't recompute path
                troop.setTargetWorldPosition(Vector2.fromGridPosition(newTargetGrid));
                troop.clearPath();
                if (newTargetGrid != null) {
                    PathfindingStrategy strategy = troop.isAirUnit() ? airStrategy : groundStrategy;
                    Deque<GridPosition> gridPath = strategy.computePath(arena, troop, newTargetGrid);
                    // Convert and smooth GridPosition path to Vector2 waypoints
                    Deque<Vector2> worldPath = convertPathToVector2(arena, gridPath, troop.isAirUnit());
                    troop.setPath(worldPath);
                }

                // Reset cooldown (randomize slightly to distribute load)
                troop.setPathfindingCooldown(0.25 + Math.random() * 0.1);
            }

            // Apply movement and/or separation
            if (troop.getUnitState() != UnitState.ATTACKING) {
                troop.setUnitState(UnitState.MOVING);
            }
            updateTroopPosition(deltaTime, troop, state);
        }

        // Cleanup: remove destroyed troops
        if (!toRemove.isEmpty()) {
            troops.removeAll(toRemove);
        }
    }

    private boolean shouldRetarget(IBattleState state, Troop troop) {
        return troop.getPath().isEmpty() || troop.getTargetWorldPosition() == null;
    }

    // Converts a GridPosition path to Vector2 waypoints and smooths it using string
    // pulling.
    private Deque<Vector2> convertPathToVector2(Arena arena, Deque<GridPosition> gridPath, boolean isAir) {
        if (gridPath == null || gridPath.isEmpty())
            return new ArrayDeque<>();

        List<GridPosition> original = new ArrayList<>(gridPath);
        Deque<Vector2> smoothedPath = new ArrayDeque<>();

        // Start from first node
        int current = 0;
        smoothedPath.add(Vector2.fromGridPosition(original.get(0)));

        while (current < original.size() - 1) {
            int furthestVisible = current + 1;
            // Look ahead to find the furthest node we can see in a straight line
            for (int next = current + 2; next < original.size(); next++) {
                if (isLineWalkable(arena, original.get(current), original.get(next), isAir)) {
                    furthestVisible = next;
                } else {
                    // Obstacle in the way, stop searching further
                    break;
                }
            }
            smoothedPath.add(Vector2.fromGridPosition(original.get(furthestVisible)));
            current = furthestVisible;
        }

        return smoothedPath;
    }

    // Checks if a straight line between two tiles is walkable (LOS check).
    private boolean isLineWalkable(Arena arena, GridPosition p1, GridPosition p2, boolean isAir) {
        if (isAir || arena == null)
            return true;

        // Use centers for ray casting
        double x1 = p1.getX() + 0.5;
        double y1 = p1.getY() + 0.5;
        double x2 = p2.getX() + 0.5;
        double y2 = p2.getY() + 0.5;

        double dist = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        if (dist < 1.0)
            return true;

        int samples = (int) Math.ceil(dist * 4); // 4 samples per tile for high accuracy
        for (int i = 1; i < samples; i++) {
            double t = (double) i / samples;
            int gx = (int) Math.floor(x1 + (x2 - x1) * t);
            int gy = (int) Math.floor(y1 + (y2 - y1) * t);

            GridCell cell = arena.getCell(gx, gy);
            if (cell == null || !cell.isWalkable())
                return false;
        }
        return true;
    }

    private static final double WAYPOINT_THRESHOLD = 0.1; // How close to waypoint before moving to next
    private static final double SEPARATION_RADIUS = 0.5;

    private void updateTroopPosition(double deltaTime, Troop troop, IBattleState state) {
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
