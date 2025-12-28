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
        int dmg = attacker.getDamage();
        target.takeDamage(dmg);
    }

    /**
     * Orchestrates combat for all entities in the game state.
     */
    public void update(double deltaTime, com.kuroyale.model.logic.GameState state) {
        if (state == null)
            return;

        // Process all potential attackers
        // 1. Troops
        java.util.List<Troop> troops = new java.util.ArrayList<>(state.getActiveTroops());
        for (Troop troop : troops) {
            processCombatant(troop, state, deltaTime);
        }

        // 2. Buildings
        java.util.List<Building> buildings = new java.util.ArrayList<>(state.getActiveBuildings());
        for (Building building : buildings) {
            if (building.isAlive()) {
                processCombatant(building, state, deltaTime);
            }

            // Death Damage handling (e.g. Bomb Tower)
            if (!building.isAlive()) {
                if (building.isAreaEffect()) {
                    com.kuroyale.model.entities.GridPosition center = building.getCenterPosition();
                    if (center != null) {
                        applyAreaDamage(state, center, 1.0, building.getDamage(),
                                building.getTargetType(), building.isPlayerSide());
                    }
                }
                // Cleanup building footprint
                state.getArena().freeFootprint(building);
            }
        }

        // 3. Towers
        java.util.Set<Tower> towers = state.getArena().getAllTowers();
        for (Tower tower : towers) {
            processCombatant(tower, state, deltaTime);
        }
    }

    private void processCombatant(ICombatant attacker, com.kuroyale.model.logic.GameState state, double deltaTime) {
        if (!attacker.isAlive())
            return;

        // 1. Update Cooldown
        double cd = attacker.getAttackCooldown() - deltaTime;

        // 2. Target Handling
        ICombatant target = attacker.getTarget();
        if (target == null || !target.isAlive() || !isInAttackRange(attacker, target)) {
            target = findNearestTarget(attacker, state);
            attacker.setTarget(target);
        }

        // 3. State Management (for Troops)
        if (attacker instanceof Troop troop) {
            if (target != null) {
                troop.setUnitState(com.kuroyale.model.enums.UnitState.ATTACKING);
                troop.setTarget(target);
            } else {
                // Only reset if it was attacking before
                if (troop.getUnitState() == com.kuroyale.model.enums.UnitState.ATTACKING) {
                    troop.setUnitState(com.kuroyale.model.enums.UnitState.MOVING);
                    troop.setTarget(null);
                }
            }
        }

        // 4. Combat Application
        if (target != null) {
            if (cd <= 0) {
                performAttack(attacker, target, state);
                attacker.setAttackCooldown(attacker.getHitSpeed());
            } else {
                attacker.setAttackCooldown(cd);
            }
        } else {
            // No target, just tick down cooldown
            attacker.setAttackCooldown(Math.max(0, cd));
        }
    }

    private void performAttack(ICombatant attacker, ICombatant target, com.kuroyale.model.logic.GameState state) {
        if (attacker.isAreaEffect()) {
            // Splash radius is usually 1.0 tiles for units/buildings unless specified
            applyAreaDamage(state, target.getPosition(), 1.0, attacker.getDamage(),
                    attacker.getTargetType(), attacker.isPlayerSide());
        } else {
            applyDamage(attacker, target);
        }
    }

    private ICombatant findNearestTarget(ICombatant attacker, com.kuroyale.model.logic.GameState state) {
        ICombatant best = null;
        double bestDist = Double.MAX_VALUE;
        com.kuroyale.model.entities.GridPosition center = attacker.getCenterPosition();
        if (center == null)
            return null;

        double range = attacker.getRange();

        // Structures (Towers/Buildings) only target Troops
        // Troops target Troops, Buildings, and Towers

        // 1. Check Troops
        for (Troop t : state.getActiveTroops()) {
            if (!t.isAlive() || t.isPlayerSide() == attacker.isPlayerSide())
                continue;
            if (!attacker.canTarget(t))
                continue;

            double dist = getDistanceToTarget(attacker, t);
            if (dist <= range && dist < bestDist) {
                bestDist = dist;
                best = t;
            }
        }

        // 2. Troops also target Buildings and Towers
        if (attacker instanceof Troop) {
            // Buildings
            for (Building b : state.getActiveBuildings()) {
                if (!b.isAlive() || b.isPlayerSide() == attacker.isPlayerSide())
                    continue;
                // Note: Troop's canTarget(Troop) doesn't cover buildings, but normally they hit
                // buildings
                // unless they have specific logic. Building-only units are handled by
                // canTarget(Troop) returning false for troops.

                double dist = getDistanceToTarget(attacker, b);
                if (dist <= range && dist < bestDist) {
                    bestDist = dist;
                    best = b;
                }
            }

            // Towers
            for (Tower t : state.getArena().getAllTowers()) {
                if (!t.isAlive() || t.isPlayerSide() == attacker.isPlayerSide())
                    continue;

                double dist = getDistanceToTarget(attacker, t);
                if (dist <= range && dist < bestDist) {
                    bestDist = dist;
                    best = t;
                }
            }
        }

        return best;
    }

    private boolean isInAttackRange(ICombatant attacker, ICombatant target) {
        if (attacker == null || target == null)
            return false;
        double range = attacker.getRange();

        // Special Melee handling (copied from TroopMovementService)
        if (attacker instanceof Troop troop) {
            com.kuroyale.model.entities.CombatStats stats = troop.getCombatStats();
            if (stats != null && stats.getAttackType() == com.kuroyale.model.entities.CombatStats.AttackType.MELEE) {
                range = Math.max(range, 1.0);
                double threshold = Math.max(1.5, range);
                return getDistanceToTarget(attacker, target) <= threshold;
            }
        }

        return getDistanceToTarget(attacker, target) <= range;
    }

    private double getDistanceToTarget(ICombatant attacker, ICombatant target) {
        com.kuroyale.model.entities.GridPosition from = attacker.getPosition(); // Usually attacker's position
        if (attacker instanceof Tower || attacker instanceof Building) {
            from = attacker.getCenterPosition();
        }

        if (target instanceof Tower || target instanceof Building) {
            // For structures, calculate distance to their perimeter
            return distanceToCombatantPerimeter(from, target);
        } else {
            // For troops, standard euclidean distance
            return from.getEuclideanDistanceTo(target.getPosition());
        }
    }

    private double distanceToCombatantPerimeter(com.kuroyale.model.entities.GridPosition from, ICombatant combatant) {
        com.kuroyale.model.entities.GridPosition pos = combatant.getPosition();
        if (pos == null)
            return Double.MAX_VALUE;

        int x0 = pos.getX();
        int y0 = pos.getY();
        int w = Math.max(1, combatant.getWidth());
        int h = Math.max(1, combatant.getHeight());

        double best = Double.MAX_VALUE;
        for (int dx = 0; dx < w; dx++) {
            com.kuroyale.model.entities.GridPosition top = com.kuroyale.model.entities.GridPosition.tryCreate(x0 + dx,
                    y0);
            com.kuroyale.model.entities.GridPosition bottom = com.kuroyale.model.entities.GridPosition
                    .tryCreate(x0 + dx, y0 + h - 1);
            if (top != null)
                best = Math.min(best, from.getEuclideanDistanceTo(top));
            if (bottom != null)
                best = Math.min(best, from.getEuclideanDistanceTo(bottom));
        }
        for (int dy = 0; dy < h; dy++) {
            com.kuroyale.model.entities.GridPosition left = com.kuroyale.model.entities.GridPosition.tryCreate(x0,
                    y0 + dy);
            com.kuroyale.model.entities.GridPosition right = com.kuroyale.model.entities.GridPosition
                    .tryCreate(x0 + w - 1, y0 + dy);
            if (left != null)
                best = Math.min(best, from.getEuclideanDistanceTo(left));
            if (right != null)
                best = Math.min(best, from.getEuclideanDistanceTo(right));
        }
        return best;
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

        int intDamage = (int) Math.round(damage);

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
                t.takeDamage(intDamage);
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
                    b.takeDamage(intDamage);
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
                    t.takeDamage(intDamage);
                }
            }
        }

        // Broadcast visual effect via Event Bus
        com.kuroyale.event.GameEventBus.getInstance().publishAreaEffect(isPlayerSource, center, radiusTiles, 0.3);
    }
}
