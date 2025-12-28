package com.kuroyale.service;

import com.kuroyale.model.entities.Building;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.entities.ICombatant;

public class CombatService {

    // Core single-target damage methods
    public void applyDamage(ICombatant attacker, ICombatant target) {
        if (attacker == null || target == null)
            return;
        double dmg = attacker.getDamage();
        target.takeDamage(dmg);
    }

    /**
     * Centralized Area Damage logic.
     * 
     * @param gameState      Reference to game state to access active entities.
     * @param center         Center point of the damage (e.g. projectile impact or
     *                       unit center).
     * @param radiusTiles    Radius in tiles.
     * @param damage         Amount of damage to deal.
     * @param targetType     Restrictions on what can be hit (AIR, GROUND, BOTH).
     * @param isPlayerSource True if the damage comes from the player (hurts
     *                       enemies), False otherwise.
     */
    public void applyAreaDamage(com.kuroyale.model.logic.GameState gameState,
            com.kuroyale.model.entities.GridPosition center,
            double radiusTiles,
            double damage,
            com.kuroyale.model.enums.TargetType targetType,
            boolean isPlayerSource) {

        if (gameState == null || center == null || radiusTiles <= 0 || damage <= 0)
            return;

        // Damage enemy troops
        java.util.List<Troop> troops = new java.util.ArrayList<>(gameState.getActiveTroops());
        for (Troop t : troops) {
            if (!t.isAlive())
                continue;
            // Don't hurt friendly troops
            if (t.isPlayerSide() == isPlayerSource)
                continue;

            // Validate Target Type
            if (targetType == com.kuroyale.model.enums.TargetType.GROUND && t.isAirUnit())
                continue;
            if (targetType == com.kuroyale.model.enums.TargetType.AIR && !t.isAirUnit())
                continue;
            if (targetType == com.kuroyale.model.enums.TargetType.NONE)
                continue;

            // Distance Check
            double dist = center.getEuclideanDistanceTo(t.getPosition());
            if (dist <= radiusTiles) {
                t.takeDamage((int) Math.round(damage));
            }
        }

        // Damage enemy buildings and towers (Ground only for now usually, or allow
        // TargetType check)
        // Buildings/Towers are typically GROUND targets.
        boolean canHitGround = (targetType != com.kuroyale.model.enums.TargetType.AIR
                && targetType != com.kuroyale.model.enums.TargetType.NONE);

        if (canHitGround) {
            // Check Buildings
            java.util.List<Building> buildings = new java.util.ArrayList<>(gameState.getActiveBuildings());
            for (Building b : buildings) {
                if (!b.isAlive())
                    continue;
                if (b.isPlayerSide() == isPlayerSource)
                    continue;

                // Use center position for fair range calculation
                com.kuroyale.model.entities.GridPosition bCenter = b.getCenterPosition();
                if (bCenter == null)
                    continue;

                double dist = center.getEuclideanDistanceTo(bCenter);
                if (dist <= radiusTiles) {
                    b.takeDamage(damage);
                    if (!b.isAlive()) {
                        gameState.getArena().freeFootprint(b);
                    }
                }
            }

            // Check Towers
            // Optimize by iterating unique towers instead of grid cells if possible.
            // Arena now has getAllTowers(), but GameState doesn't expose it directly yet?
            // GameState exposes Arena. Arena exposes getAllTowers().
            // But checking getAllTowers directly is safer and faster.
            java.util.Set<Tower> towers = gameState.getArena().getAllTowers();
            for (Tower t : towers) {
                if (!t.isAlive())
                    continue;
                if (t.isPlayerSide() == isPlayerSource)
                    continue; // Friendly fire check

                com.kuroyale.model.entities.GridPosition tCenter = t.getCenterPosition();
                if (tCenter == null)
                    continue;

                double dist = center.getEuclideanDistanceTo(tCenter);
                if (dist <= radiusTiles) {
                    t.takeDamage(damage);
                }
            }
        }

        // Add visual effect (this creates a dependency on GameState to add effect,
        // effectively mutating state. Is this side effect desired in Service?
        // Yes, CombatService mutates state (HP).)
        gameState.getActiveSpellEffects().add(
                new com.kuroyale.model.logic.GameState.SpellEffect(center, (int) Math.round(radiusTiles),
                        isPlayerSource,
                        0.3));
    }
}
