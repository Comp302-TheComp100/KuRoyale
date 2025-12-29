package com.kuroyale.service;

import com.kuroyale.model.entities.Building;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.entities.Card;

public class CombatService {
    public static final double MELEE_ATTACK_BUFFER = 1.5;

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
    }

    private void processBuildingProduction(Building b, com.kuroyale.model.logic.IBattleState state, double deltaTime) {
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

        // 1. Update Cooldown
        double cd = attacker.getAttackCooldown() - deltaTime;

        // 2. Target Handling
        ICombatant target = attacker.getTarget();
        boolean targetValid = (target != null && target.isAlive() && isInAttackRange(attacker, target));

        // STICKY TARGETING: Only search for new target if current is invalid
        if (!targetValid) {
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
        if (attacker.isAreaEffect()) {
            // Splash radius is usually 1.0 tiles for units/buildings unless specified
            com.kuroyale.model.entities.GridPosition targetPos = target.getPosition();
            if (target instanceof Tower || target instanceof Building) {
                targetPos = target.getCenterPosition();
            }

            applyAreaDamage(state, targetPos, 1.0, attacker.getDamage(),
                    attacker.getTargetType(), attacker.isPlayerSide());
        } else {
            applyDamage(attacker, target);
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
                range = Math.max(range, MELEE_ATTACK_BUFFER);
            }
        }

        // Structures (Towers/Buildings) only target Troops
        // Troops target Troops, Buildings, and Towers

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
        if (attacker == null || target == null)
            return false;
        double range = attacker.getRange();

        // Special Melee handling (copied from TroopMovementService)
        if (attacker instanceof Troop troop) {
            com.kuroyale.model.entities.CombatStats stats = troop.getCombatStats();
            if (stats != null && stats.getAttackType() == com.kuroyale.model.entities.CombatStats.AttackType.MELEE) {
                range = Math.max(range, 1.0);
                double threshold = Math.max(MELEE_ATTACK_BUFFER, range);
                return getDistanceToTarget(attacker, target) <= threshold;
            }
        }

        double dist = getDistanceToTarget(attacker, target); // Renaming for clarity

        // Check Min Range (Blind Spot)
        double minRange = 0;
        if (attacker instanceof Building) {
            minRange = ((Building) attacker).getMinRange();
        } else if (attacker instanceof Troop) {
            Troop t = (Troop) attacker;
            if (t.getBaseCard() != null) {
                minRange = t.getBaseCard().getMinRange();
            }
        }

        if (dist < minRange) {
            return false;
        }

        return dist <= range;
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

    // Centralized Area Damage logic.
    public void applyAreaDamage(com.kuroyale.model.logic.IBattleState gameState,
            com.kuroyale.model.entities.GridPosition center,
            double radiusTiles,
            double damage,
            com.kuroyale.model.enums.TargetType targetType,
            boolean isPlayerSource) {

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
                    // Apply Damage
                    candidate.takeDamage(intDamage);

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
