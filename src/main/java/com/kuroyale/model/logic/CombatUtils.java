package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;

/**
 * Utility class for spatial and combat math.
 * Centralizes logic for distance calculations and range checks.
 */
public class CombatUtils {

    /**
     * Calculates the effective distance between two combatants.
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

        // Structure vs Structure or Troop vs Troop (usually center to center or
        // specialized)
        // For Structure vs Structure this might technically be perimeter-to-perimeter
        // in advanced logic,
        // but typically standard distance or center-based is fine for spells.
        // However, keeping consistent with old logic:

        GridPosition posA = (isStructure(a)) ? a.getCenterPosition() : a.getPosition();
        GridPosition posB = (isStructure(b)) ? b.getCenterPosition() : b.getPosition();

        if (posA == null || posB == null)
            return Double.MAX_VALUE;

        return posA.getEuclideanDistanceTo(posB);
    }

    /**
     * Calculates distance from one combatant (usually a troop) to the nearest
     * perimeter tile of another (usually a building).
     * If both are troops, falls back to standard distance.
     */
    public static double distanceToPerimeter(ICombatant source, ICombatant target) {
        // If neither is a structure, just use point distance
        if (!isStructure(source) && !isStructure(target)) {
            return source.getPosition().getEuclideanDistanceTo(target.getPosition());
        }

        // Identify which one is the structure (or both).
        // Logic: Measure from Source (Point) to Target (Rect).
        // If Source is also a Rect, we simplified in old code to use its CENTER as the
        // 'From' point.

        GridPosition from = source.getPosition();
        ICombatant rectTarget = target;

        if (isStructure(source)) {
            from = source.getCenterPosition();
        }

        if (!isStructure(target) && isStructure(source)) {
            // Swap: We measure from Target(Troop) to Source(Building) perimeter
            from = target.getPosition();
            rectTarget = source;
        }

        return calculateDistanceToRect(from, rectTarget);
    }

    private static double calculateDistanceToRect(GridPosition from, ICombatant rect) {
        GridPosition pos = rect.getPosition();
        if (pos == null || from == null)
            return Double.MAX_VALUE;

        int x0 = pos.getX();
        int y0 = pos.getY();
        int w = Math.max(1, rect.getWidth());
        int h = Math.max(1, rect.getHeight());

        double best = Double.MAX_VALUE;

        // Check horizontal edges
        for (int dx = 0; dx < w; dx++) {
            GridPosition top = GridPosition.tryCreate(x0 + dx, y0);
            GridPosition bottom = GridPosition.tryCreate(x0 + dx, y0 + h - 1);
            if (top != null)
                best = Math.min(best, from.getEuclideanDistanceTo(top));
            if (bottom != null)
                best = Math.min(best, from.getEuclideanDistanceTo(bottom));
        }

        // Check vertical edges
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

    public static boolean isInRange(ICombatant attacker, ICombatant target) {
        if (attacker == null || target == null)
            return false;

        double dist = getDistance(attacker, target);
        double range = attacker.getRange();

        // Melee buffering
        if (attacker instanceof Troop troop && troop.isMelee()) {
            range = Math.max(range, com.kuroyale.util.GameConstants.MELEE_MIN_RANGE);
            double threshold = Math.max(com.kuroyale.util.GameConstants.MELEE_ATTACK_BUFFER, range);
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
