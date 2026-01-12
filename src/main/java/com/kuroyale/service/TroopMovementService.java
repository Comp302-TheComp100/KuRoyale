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

            // If stunned, skip movement and retargeting
            if (troop.isStunned()) {
                troop.setUnitState(UnitState.STUNNED);
                continue;
            }

            // retarget if current target is dead or null
            ICombatant currentTarget = troop.getTarget();
            boolean targetIsDead = (currentTarget != null && !currentTarget.isAlive());
            boolean hasNoTargetPos = (troop.getTargetWorldPosition() == null);

            if (targetIsDead) {
                troop.setTarget(null);
                troop.setTargetWorldPosition(null);
                troop.clearPath();
                troop.setUnitState(UnitState.IDLE);
            }

            if (targetIsDead || hasNoTargetPos || shouldRetarget(state, troop)) {
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
        // 1. Sticky Targeting: If already attacking, do not switch (CombatService
        // handles invalid/dead targets)
        if (troop.getUnitState() == UnitState.ATTACKING)
            return false;

        // 2. Navigation: If path is finished or lost, we must find a target
        if (troop.getPath().isEmpty() || troop.getTargetWorldPosition() == null)
            return true;

        // 3. Opportunistic Targeting:
        // If we are moving check if a NEW enemy has entered our
        // immediate attack range.
        GridPosition nearestGrid = targetingService.findNearestEnemyOrObjective(state, troop);
        if (nearestGrid == null)
            return false;

        Vector2 nearestPos = Vector2.fromGridPosition(nearestGrid);
        double distToNearest = troop.getWorldPosition().distanceTo(nearestPos);
        double distToCurrentTarget = troop.getTargetWorldPosition().distanceTo(nearestPos);

        // If the nearest enemy is closer than our current target, switch
        // 0.5 is a buffer to avoid switching targets too often
        if (distToCurrentTarget > distToNearest + 0.5) {
            return true;
        }

        return false;
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
        double x1 = p1.getX() + 0.5, y1 = p1.getY() + 0.5;
        double x2 = p2.getX() + 0.5, y2 = p2.getY() + 0.5;

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

            // Ensure we aren't clipping a corner by checking immediate non-diagonal
            // neighbors.

            // If so, treat this point as "tight" and potentially unsafe for string pulling
            // if we want strictly wide paths.
            // But for corner clipping specifically: if (x,y) is walkable, we just need to
            // ensure we didn't skip over a corner. The discrete sampling handles skipping.
            // To handle *width*, we check if adjacent cells are walls.

            // Check 4-neighbors for walls. If a neighbor is a wall, we might be too close.
            // We only check if the point is *very* close to the boundary of that neighbor.

            double px = x1 + (x2 - x1) * t;
            double py = y1 + (y2 - y1) * t;

            // Check right neighbor if we are close to right edge
            if (px - gx > 0.7 && !isWalkable(arena, gx + 1, gy))
                return false;
            // Check left neighbor if we are close to left edge
            if (px - gx < 0.3 && !isWalkable(arena, gx - 1, gy))
                return false;
            // Check bottom neighbor if we are close to bottom edge
            if (py - gy > 0.7 && !isWalkable(arena, gx, gy + 1))
                return false;
            // Check top neighbor if we are close to top edge
            if (py - gy < 0.3 && !isWalkable(arena, gx, gy - 1))
                return false;
        }
        return true;
    }

    private boolean isWalkable(Arena arena, int x, int y) {
        GridCell cell = arena.getCell(x, y);
        return cell != null && cell.isWalkable();
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

                    // Tangential force to encourage sliding but avoid swirling (Parallel Slide)
                    // Standard perpendicular vector
                    Vector2 tangential = new Vector2(-away.getY(), away.getX());

                    // We want both units to slide in the SAME world direction to avoid
                    // rotation/swirling.
                    // Since 'away' vectors are opposite for the two units, the default tangential
                    // vectors are also opposite (causing rotation).
                    if (System.identityHashCode(self) < System.identityHashCode(other)) {
                        tangential = tangential.multiply(-1);
                    }

                    // Add tangential component (weighted) -> Mix 70% Push, 30% Slide
                    away = away.add(tangential.multiply(0.3)).normalize();
                }

                // Strength increases as they get closer
                double strength = (SEPARATION_RADIUS - dist) / SEPARATION_RADIUS;
                // Increased push force (0.5) to effectively separate clustered units
                separation = separation.add(away.multiply(strength * 0.5));
                count++;
            }
        }

        if (count > 0) {
            return proposedPos.add(separation);
        }
        return proposedPos;
    }
}