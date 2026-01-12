package com.kuroyale.service;

import com.kuroyale.model.entities.Building;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.entities.Card;

public class CombatService {

    // Core single-target damage methods
    public void applyDamage(ICombatant attacker, ICombatant target) {
        if (attacker == null || target == null)
            return;
        int dmg = attacker.getDamage();
        target.takeDamage(dmg);
    }

    // Orchestrates combat for all entities in the game state.
    public void update(double deltaTime, com.kuroyale.model.logic.IBattleState state) {
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
                processBuildingProduction(building, state, deltaTime);
                processCombatant(building, state, deltaTime);
            } else {
                // Cleanup building footprint
                state.getArena().freeFootprint(building);

                // Death Spawn handling (e.g. Tombstone)
                Card base = building.getBaseCard();
                if (base != null && base.getDeathSpawnUnitName() != null) {
                    com.kuroyale.model.entities.GridPosition spawnPos = state.getFrontPosition(building);
                    if (spawnPos == null) {
                        spawnPos = building.getPosition();
                    }

                    Card unitCard = state.getCardByName(base.getDeathSpawnUnitName());
                    if (unitCard != null) {
                        state.spawnTroopDirectly(building.isPlayerSide(), unitCard,
                                spawnPos.getX(), spawnPos.getY(), base.getDeathSpawnUnitCount());
                    }
                }
            }
        }
        // 3. Towers
        java.util.Set<Tower> towers = state.getArena().getAllTowers();
        for (Tower tower : towers) {
            processCombatant(tower, state, deltaTime);
        }

        // 4. Projectiles (Orphaned or Active)
        java.util.List<com.kuroyale.model.entities.Projectile> projectiles = state.getProjectiles();
        java.util.Iterator<com.kuroyale.model.entities.Projectile> it = projectiles.iterator();
        while (it.hasNext()) {
            com.kuroyale.model.entities.Projectile p = it.next();
            p.update(deltaTime);
            if (!p.isActive()) {
                // Hit target
                if (p.isAreaEffect()) {
                    com.kuroyale.model.entities.GridPosition impactPos = com.kuroyale.model.entities.GridPosition
                            .tryCreate((int) p.getPosition().getX(), (int) p.getPosition().getY());
                    if (impactPos != null) {
                        applyAreaDamage(state, impactPos, 1.0, p.getDamage(),
                                p.getTargetType(), p.isPlayerSide(), false, 0.0);
                    }
                } else {
                    if (p.getTarget() != null && p.getTarget().isAlive()) {
                        p.getTarget().takeDamage(p.getDamage());
                    }
                }
                it.remove();
            }
        }
    }

    private void processBuildingProduction(Building b, com.kuroyale.model.logic.IBattleState state, double deltaTime) {
        if (b.isStunned()) {
            return;
        }
        if (b.getProductionResource() != null && b.isAlive()) {
            double newTimer = b.getProductionTimer() - deltaTime;
            if (newTimer <= 0) {
                // Producing
                if ("ELIXIR".equals(b.getProductionResource())) {
                    state.getElixirManager(b.isPlayerSide()).addElixir(b.getProductionAmount());
                    // Visual feedback
                    com.kuroyale.event.GameEventBus.getInstance().publishBuildingProduction(b, "ELIXIR",
                            b.getProductionAmount());
                }
                newTimer = b.getProductionInterval();

            }
            b.setProductionTimer(newTimer);
        }
    }

    private void processCombatant(ICombatant attacker, com.kuroyale.model.logic.IBattleState state, double deltaTime) {
        if (!attacker.isAlive())
            return;

        // Update status effects (stun, freeze, etc.)
        attacker.updateStatus(deltaTime);

        // If stunned, skip all combat logic
        if (attacker.isStunned()) {
            if (attacker instanceof Troop t) {
                t.setUnitState(com.kuroyale.model.enums.UnitState.STUNNED);
            }
            return;
        }

        // 1. Update Cooldown
        double cd = attacker.getAttackCooldown() - deltaTime;

        // 2. Target Handling
        ICombatant target = attacker.getTarget();
        ICombatant previousTarget = target;
        boolean targetValid = (target != null && target.isAlive() && isInAttackRange(attacker, target));

        // STICKY TARGETING: Only search for new target if current is invalid
        if (!targetValid) {
            target = findNearestTarget(attacker, state);
            attacker.setTarget(target);

            // Fix: Troops should wait for attack speed before first attack
            if (previousTarget == null && target != null && cd <= 0) {
                cd = attacker.getHitSpeed();
                attacker.setAttackCooldown(cd);
            }
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

            // Building periodic spawning
            if (attacker instanceof Building b && cd <= 0) {
                Card base = b.getBaseCard();
                if (base != null && base.getSpawnUnitName() != null) {
                    spawnUnitsFromBuilding(b, state);
                    // Ensure we don't spam if hitSpeed is 0
                    double nextCooldown = Math.max(b.getHitSpeed(), 0.5);
                    b.setAttackCooldown(nextCooldown);
                }
            }
        }
    }

    private void spawnUnitsFromBuilding(Building b, com.kuroyale.model.logic.IBattleState state) {
        Card base = b.getBaseCard();
        if (base == null || base.getSpawnUnitName() == null)
            return;

        com.kuroyale.model.entities.GridPosition spawnPos = state.getFrontPosition(b);
        if (spawnPos == null)
            return;

        Card unitCard = state.getCardByName(base.getSpawnUnitName());
        if (unitCard != null) {
            state.spawnTroopDirectly(b.isPlayerSide(), unitCard,
                    spawnPos.getX(), spawnPos.getY(), base.getSpawnUnitCount());
        }
    }

    private void performAttack(ICombatant attacker, ICombatant target, com.kuroyale.model.logic.IBattleState state) {
        boolean isMelee = false;

        if (attacker instanceof Troop t) {
            if (t.isMelee()) {
                isMelee = true;
            }
        }

        if (isMelee) {
            // Instant Damage
            if (attacker.isAreaEffect()) {
                com.kuroyale.model.entities.GridPosition targetPos = target.getPosition();
                if (target instanceof Tower || target instanceof Building) {
                    targetPos = target.getCenterPosition();
                }

                applyAreaDamage(state, targetPos, 1.0, attacker.getDamage(),
                        attacker.getTargetType(), attacker.isPlayerSide(), false, 0.0);
            } else {
                applyDamage(attacker, target);
            }
        } else {
            // Ranged / Projectile
            state.addProjectile(new com.kuroyale.model.entities.Projectile(attacker, target));
        }
    }

    private ICombatant findNearestTarget(ICombatant attacker, com.kuroyale.model.logic.IBattleState state) {
        ICombatant best = null;
        double bestDist = Double.MAX_VALUE;
        com.kuroyale.model.entities.GridPosition center = attacker.getCenterPosition();
        if (center == null)
            return null;

        double range = attacker.getRange();

        if (attacker instanceof Troop troop) {
            com.kuroyale.model.entities.CombatStats stats = troop.getCombatStats();
            if (stats != null && stats.getAttackType() == com.kuroyale.model.entities.CombatStats.AttackType.MELEE) {
                range = Math.max(range, com.kuroyale.util.GameConstants.MELEE_ATTACK_BUFFER);
            }
        }

        // Optimize with SpatialGrid
        com.kuroyale.model.logic.SpatialGrid grid = state.getArena().getSpatialGrid();
        if (grid == null)
            return null;

        java.util.List<ICombatant> candidates = grid.getNearby(center, range);

        for (ICombatant candidate : candidates) {
            if (!candidate.isAlive() || candidate.isPlayerSide() == attacker.isPlayerSide())
                continue;

            // Check targeting rules (Ground/Air/Buildings)
            if (!attacker.canTarget(candidate))
                continue;

            double dist = getDistanceToTarget(attacker, candidate);
            // Check Blind Spot (Min Range)
            if (attacker instanceof Building && dist < ((Building) attacker).getMinRange()) {
                continue;
            }
            if (attacker instanceof Troop) {
                // Troops usually don't have min range but check card just in case
                Troop t = (Troop) attacker;
                if (t.getBaseCard() != null && dist < t.getBaseCard().getMinRange()) {
                    continue;
                }
            }

            if (dist <= range && dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        return best;
    }

    private boolean isInAttackRange(ICombatant attacker, ICombatant target) {
        return com.kuroyale.model.logic.CombatUtils.isInRange(attacker, target);
    }

    private double getDistanceToTarget(ICombatant attacker, ICombatant target) {
        return com.kuroyale.model.logic.CombatUtils.getDistance(attacker, target);
    }

    // Centralized Area Damage logic.
    public void applyAreaDamage(com.kuroyale.model.logic.IBattleState gameState,
            com.kuroyale.model.entities.GridPosition center,
            double radiusTiles,
            double damage,
            com.kuroyale.model.enums.TargetType targetType,
            boolean isPlayerSource,
            boolean isSpell,
            double stunDuration) {

        if (gameState == null || center == null || radiusTiles <= 0)
            return;

        int intDamage = (int) Math.round(damage);

        // Damage enemy troops
        // Use SpatialGrid to efficiently find all potential targets in the blast area
        com.kuroyale.model.logic.SpatialGrid grid = gameState.getArena().getSpatialGrid();
        if (grid == null)
            return;

        java.util.List<ICombatant> candidates = grid.getNearby(center, radiusTiles);

        for (ICombatant candidate : candidates) {
            if (!candidate.isAlive())
                continue;
            // Don't hurt friendly units
            if (candidate.isPlayerSide() == isPlayerSource)
                continue;

            // Handle Target Types
            if (candidate instanceof Troop t) {
                if (targetType == com.kuroyale.model.enums.TargetType.GROUND && t.isAirUnit())
                    continue;
                if (targetType == com.kuroyale.model.enums.TargetType.AIR && !t.isAirUnit())
                    continue;
                if (targetType == com.kuroyale.model.enums.TargetType.NONE)
                    continue;
            } else if ((candidate instanceof Building || candidate instanceof Tower)) {
                // Buildings/Towers are always "Ground" for targeting purposes usually,
                // but checking canHitGround is good practice.
                boolean canHitGround = (targetType != com.kuroyale.model.enums.TargetType.AIR
                        && targetType != com.kuroyale.model.enums.TargetType.NONE);
                if (!canHitGround)
                    continue;
            }

            // Precise Distance Check
            com.kuroyale.model.entities.GridPosition candidatePos = candidate.getPosition();
            // For structures, use center for better splash approximation or keep simple pos
            if (candidate instanceof Building || candidate instanceof Tower) {
                candidatePos = candidate.getCenterPosition();
            }

            if (candidatePos != null) {
                double dist = center.getEuclideanDistanceTo(candidatePos);
                if (dist <= radiusTiles) {

                    int finalDamage = intDamage;
                    if (isSpell && candidate instanceof Tower) {
                        finalDamage = (int) Math.round(damage * 0.4);
                    }

                    // Apply Damage
                    candidate.takeDamage(finalDamage);

                    // Apply Stun
                    if (stunDuration > 0) {
                        candidate.stun(stunDuration);
                    }

                    // Check for destroyed buildings to free footprint immediately
                    if (!candidate.isAlive() && candidate instanceof Building b) {
                        gameState.getArena().freeFootprint(b);
                    }
                }
            }
        }

        // Broadcast visual effect via Event Bus
        com.kuroyale.event.GameEventBus.getInstance().publishAreaEffect(isPlayerSource, center, radiusTiles, 0.3);
    }
}
