package com.kuroyale.util.game;

import com.kuroyale.model.entities.*;

/**
 * Utility class for spatial and combat math.
 * Centralizes logic for distance calculations and range checks.
 */
public class CombatUtils {

    /**
     * Calculates the effective distance between two combatants using world
     * coordinates.
     * Handles differences between Point-to-Point (Troop vs Troop)
     * and Point-to-Perimeter (Troop vs Building).
     */
    public static double getDistance(ICombatant a, ICombatant b) {
        if (a == null || b == null)
            return Double.MAX_VALUE;

        // If one is a structure and the other is a troop
        if ((isStructure(a) && b instanceof Troop) || (a instanceof Troop && isStructure(b))) {
            return distanceToPerimeter(a, b);
        }

        // Use world coordinates for sub-tile precision
        Vector2 posA = a.getCenterWorldPosition();
        Vector2 posB = b.getCenterWorldPosition();

        if (posA == null || posB == null)
            return Double.MAX_VALUE;

        return posA.distanceTo(posB);
    }

    /**
     * Calculates distance from one combatant (usually a troop) to the nearest
     * perimeter of another (usually a building) using world coordinates.
     * If both are troops, falls back to standard distance.
     */
    public static double distanceToPerimeter(ICombatant source, ICombatant target) {
        // If neither is a structure, just use point distance
        if (!isStructure(source) && !isStructure(target)) {
            Vector2 srcPos = source.getCenterWorldPosition();
            Vector2 tgtPos = target.getCenterWorldPosition();
            if (srcPos == null || tgtPos == null)
                return Double.MAX_VALUE;
            return srcPos.distanceTo(tgtPos);
        }

        // Identify which one is the structure (or both).
        // Logic: Measure from Source (Point) to Target (Rect).
        // If Source is also a Rect, use its CENTER as the 'From' point.
        Vector2 from = source.getCenterWorldPosition();
        ICombatant rectTarget = target;

        if (!isStructure(target) && isStructure(source)) {
            // Swap: We measure from Target(Troop) to Source(Building) perimeter
            from = target.getCenterWorldPosition();
            rectTarget = source;
        }

        if (from == null)
            return Double.MAX_VALUE;

        return calculateDistanceToRectWorld(from, rectTarget);
    }

    /**
     * Calculates distance from a world position to the nearest point on a
     * structure's footprint.
     */
    private static double calculateDistanceToRectWorld(Vector2 from, ICombatant rect) {
        GridPosition pos = rect.getPosition();
        if (pos == null || from == null)
            return Double.MAX_VALUE;

        double x0 = pos.getX();
        double y0 = pos.getY();
        double w = Math.max(1, rect.getWidth());
        double h = Math.max(1, rect.getHeight());

        // Clamp point to rect bounds for shortest distance
        double nearestX = Math.max(x0, Math.min(from.getX(), x0 + w));
        double nearestY = Math.max(y0, Math.min(from.getY(), y0 + h));

        return from.distanceTo(new Vector2(nearestX, nearestY));
    }

    public static boolean isInRange(ICombatant attacker, ICombatant target) {
        if (attacker == null || target == null)
            return false;

        double dist = getDistance(attacker, target);
        double range = attacker.getRange();

        // Melee buffering
        if (attacker instanceof Troop troop && troop.isMelee()) {
            range = Math.max(range, com.kuroyale.util.config.GameConstants.MELEE_MIN_RANGE);
            double threshold = Math.max(com.kuroyale.util.config.GameConstants.MELEE_ATTACK_BUFFER, range);
            return dist <= threshold;
        }

        // Min Range (Blind Spot) check
        double minRange = getMinRange(attacker);
        if (dist < minRange) {
            return false;
        }

        return dist <= range;
    }

    public static double getMinRange(ICombatant c) {
        if (c instanceof Building b) {
            return b.getMinRange();
        } else if (c instanceof Troop t) {
            if (t.getBaseCard() != null) {
                return t.getBaseCard().getMinRange();
            }
        }
        return 0.0;
    }

    /**
     * Finds the nearest walkable perimeter position adjacent to a structure's
     * footprint.
     * Returns Vector2 for sub-tile precision in approach paths.
     */
    public static Vector2 getNearestPerimeterPosition(Arena arena, ICombatant combatant, Vector2 from) {
        if (from == null)
            return null;
        GridPosition pos = combatant.getPosition();
        if (pos == null)
            return null;

        double x0 = pos.getX();
        double y0 = pos.getY();
        double w = Math.max(1, combatant.getWidth());
        double h = Math.max(1, combatant.getHeight());

        // Calculate the nearest point on the perimeter (just outside the footprint)
        // Clamp to the footprint bounds, then offset to perimeter
        double nearestX = Math.max(x0, Math.min(from.getX(), x0 + w));
        double nearestY = Math.max(y0, Math.min(from.getY(), y0 + h));

        // Determine which edge is closest and offset to just outside
        double distToLeft = from.getX() - (x0 - 0.5);
        double distToRight = (x0 + w + 0.5) - from.getX();
        double distToTop = from.getY() - (y0 - 0.5);
        double distToBottom = (y0 + h + 0.5) - from.getY();

        double minDist = Math.min(Math.min(distToLeft, distToRight), Math.min(distToTop, distToBottom));

        if (minDist == distToLeft) {
            nearestX = x0 - 0.5;
            nearestY = Math.max(y0, Math.min(from.getY(), y0 + h));
        } else if (minDist == distToRight) {
            nearestX = x0 + w + 0.5;
            nearestY = Math.max(y0, Math.min(from.getY(), y0 + h));
        } else if (minDist == distToTop) {
            nearestX = Math.max(x0, Math.min(from.getX(), x0 + w));
            nearestY = y0 - 0.5;
        } else {
            nearestX = Math.max(x0, Math.min(from.getX(), x0 + w));
            nearestY = y0 + h + 0.5;
        }

        // Verify the tile is walkable
        GridPosition tilePos = new Vector2(nearestX, nearestY).toGridPosition();
        if (tilePos != null && arena.getCell(tilePos) != null) {
            return new Vector2(nearestX, nearestY);
        }

        // Fallback to discrete tile search if direct calculation fails
        GridPosition gridResult = getNearestPerimeterTile(arena, combatant, from.toGridPosition());
        return gridResult != null ? Vector2.fromGridPosition(gridResult) : null;
    }

    /**
     * @deprecated Use getNearestPerimeterPosition(Arena, ICombatant, Vector2) for
     *             sub-tile precision.
     */
    @Deprecated
    public static GridPosition getNearestPerimeterTile(Arena arena, ICombatant combatant, GridPosition from) {
        GridPosition pos = combatant.getPosition();
        if (pos == null)
            return null;
        int x0 = pos.getX();
        int y0 = pos.getY();
        int w = Math.max(1, combatant.getWidth());
        int h = Math.max(1, combatant.getHeight());
        GridPosition best = null;
        double bestDist = Double.MAX_VALUE;
        // Check tiles around footprint borders (faces only, no corners)
        for (int dx = 0; dx < w; dx++) {
            // top perimeter
            GridPosition p1 = GridPosition.tryCreate(x0 + dx, y0 - 1);
            best = pickIfBetter(arena, from, best, p1, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
            // bottom perimeter
            GridPosition p2 = GridPosition.tryCreate(x0 + dx, y0 + h);
            best = pickIfBetter(arena, from, best, p2, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
        }
        for (int dy = 0; dy < h; dy++) {
            // left perimeter
            GridPosition p3 = GridPosition.tryCreate(x0 - 1, y0 + dy);
            best = pickIfBetter(arena, from, best, p3, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
            // right perimeter
            GridPosition p4 = GridPosition.tryCreate(x0 + w, y0 + dy);
            best = pickIfBetter(arena, from, best, p4, bestDist);
            if (best != null)
                bestDist = from.getEuclideanDistanceTo(best);
        }
        return best;
    }

    private static GridPosition pickIfBetter(Arena arena, GridPosition from, GridPosition currentBest,
            GridPosition candidate,
            double currentBestDist) {
        if (candidate == null)
            return currentBest;
        GridCell cell = arena.getCell(candidate);
        if (cell == null)
            return currentBest;
        double dist = from.getEuclideanDistanceTo(candidate);
        if (currentBest == null || dist < currentBestDist) {
            return candidate;
        }
        return currentBest;
    }

    private static boolean isStructure(ICombatant c) {
        return c instanceof Building || c instanceof Tower;
    }
}
