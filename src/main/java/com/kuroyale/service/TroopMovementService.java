package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import java.util.*;

public class TroopMovementService {
    private final TargetingService targetingService = new TargetingService();
    private final PathfindingStrategy groundStrategy = new GroundPathfindingStrategy();
    private final PathfindingStrategy airStrategy = new AirDirectPathfindingStrategy();

    private static final double WAYPOINT_THRESHOLD = com.kuroyale.util.GameConstants.WAYPOINT_THRESHOLD;

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

            // Apply movement
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
        Arena arena = (state != null) ? state.getArena() : null;
        SpatialGrid spatialGrid = (arena != null) ? arena.getSpatialGrid() : null;
        Vector2 finalPos = applySeparation(proposedPos, troop, spatialGrid, deltaTime, arena);

        troop.setWorldPosition(finalPos);

        // Update SpatialGrid
        if (state != null && state.getArena() != null && state.getArena().getSpatialGrid() != null) {
            state.getArena().getSpatialGrid().update(troop);
        }
    }

    // Simple separation steering to prevent troops from overlapping.
    private Vector2 applySeparation(Vector2 proposedPos, Troop self, SpatialGrid grid, double deltaTime, Arena arena) {
        if (grid == null)
            return proposedPos;

        Vector2 separation = Vector2.ZERO;
        int count = 0;

        // Query nearby entities using SpatialGrid
        // Use a safe search radius (max possible combined radius is roughly 1.8 for 2
        // giants, plus buffer)
        double searchRadius = 3.0;
        List<ICombatant> nearby = grid.getNearby(proposedPos.toGridPosition(), searchRadius);

        for (ICombatant candidate : nearby) {
            if (!(candidate instanceof Troop other))
                continue;
            if (other == self || !other.isAlive())
                continue;

            Vector2 otherPos = other.getWorldPosition();
            if (otherPos == null)
                continue;

            double dist = proposedPos.distanceTo(otherPos);
            double combinedRadius = self.getCollisionRadius() + other.getCollisionRadius();

            // Use a slight buffer (e.g., 80% of radius) to allow some visual overlap but
            // prevent stacking
            // or stick to 100% for hard collision. Let's use 90%.
            double separationThreshold = combinedRadius * 0.9;

            if (dist < separationThreshold) {
                Vector2 away;
                if (dist < 1e-3) {
                    // Exact overlap or very close: random push
                    double angle = Math.random() * 2 * Math.PI;
                    away = new Vector2(Math.cos(angle), Math.sin(angle));
                } else {
                    // Push away from other troop (radial)
                    away = proposedPos.subtract(otherPos).normalize();

                    // Tangential force (Parallel Slide)
                    // We reduce this slightly to rely more on the new Wall Sliding logic below
                    Vector2 tangential = new Vector2(-away.getY(), away.getX());

                    // Consistent slide direction
                    if (System.identityHashCode(self) < System.identityHashCode(other)) {
                        tangential = tangential.multiply(-1);
                    }

                    // weighted: 80% Push, 20% Slide
                    away = away.add(tangential.multiply(0.2)).normalize();
                }

                // Strength increases as they get closer (0.0 to 1.0)
                double strength = (separationThreshold - dist) / separationThreshold;
                separation = separation.add(away.multiply(strength));
                count++;
            }
        }

        if (count > 0) {
            // Average the direction
            separation = separation.multiply(1.0 / count);

            double urgency = separation.length();
            if (urgency > 1.0)
                urgency = 1.0;

            // Normalize direction but scale speed by urgency
            Vector2 pushDir = separation.normalize();
            double separationSpeed = 2.0;
            Vector2 push = pushDir.multiply(separationSpeed * urgency * deltaTime);

            Vector2 finalPos = proposedPos.add(push);

            // WALL SLIDING / COLLISION CHECK
            if (arena != null) {
                // 1. Try full move
                GridPosition gpFixed = finalPos.toGridPosition();
                if (isWalkable(arena, gpFixed.getX(), gpFixed.getY())) {
                    return finalPos;
                }

                // 2. Try moving X only (Side-to-side slide)
                Vector2 posXOnly = new Vector2(finalPos.getX(), proposedPos.getY());
                GridPosition gpX = posXOnly.toGridPosition();
                if (isWalkable(arena, gpX.getX(), gpX.getY())) {
                    return posXOnly;
                }

                // 3. Try moving Y only (Up-down slide)
                Vector2 posYOnly = new Vector2(proposedPos.getX(), finalPos.getY());
                GridPosition gpY = posYOnly.toGridPosition();
                if (isWalkable(arena, gpY.getX(), gpY.getY())) {
                    return posYOnly;
                }

                return proposedPos;
            }
            return finalPos;
        }
        return proposedPos;
    }
}