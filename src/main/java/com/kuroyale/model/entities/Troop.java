package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.logic.*;

import java.util.ArrayDeque;
import java.util.Deque;

public class Troop implements ICombatant {
    private final Card baseCard;
    private Vector2 worldPosition; // Continuous position in world-space
    private Vector2 targetWorldPosition; // Continuous target position
    private final boolean isPlayer;
    private final boolean isAirUnit;
    private final boolean buildingOnly;
    private double currentHealth;
    private final double moveSpeed; // tiles per second
    private final double attackRange; // in tiles
    private final Deque<Vector2> path; // Continuous waypoints
    // Combat
    private CombatStats combatStats;
    private double attackCooldown;
    private UnitState unitState = UnitState.IDLE;
    private ICombatant currentTarget; // The specific entity this unit is attacking

    public void setCurrentTarget(ICombatant target) {
        this.currentTarget = target;
    }

    public ICombatant getCurrentTarget() {
        return currentTarget;
    }

    public Troop(Card card, GridPosition spawn, boolean isPlayer) {
        this.baseCard = card;
        this.worldPosition = Vector2.fromGridPosition(spawn);
        this.targetWorldPosition = null;
        this.isPlayer = isPlayer;
        this.isAirUnit = card.isAirUnit();
        this.buildingOnly = card.getTarget() == TargetType.BUILDINGS;
        this.currentHealth = card.getHp();
        this.moveSpeed = mapSpeed(card.getSpeed());
        this.attackRange = card.getRange();
        this.path = new ArrayDeque<>();
        CombatStats.AttackType at = card.getRange() > 1.5 ? CombatStats.AttackType.RANGED
                : CombatStats.AttackType.MELEE;
        this.combatStats = new CombatStats(card.getDamage(), card.getHitSpeed(), (int) Math.round(card.getRange()), at);
        this.attackCooldown = 0.0;
    }

    private double mapSpeed(SpeedType speedType) {
        switch (speedType) {
            case VERY_SLOW:
                return 0.8;
            case SLOW:
                return 1.0;
            case MEDIUM:
                return 1.3;
            case FAST:
                return 1.6;
            case VERY_FAST:
                return 2.0;
            default:
                return 0.0;
        }
    }

    public void setTargetWorldPosition(Vector2 target) {
        this.targetWorldPosition = target;
    }

    public Vector2 getTargetWorldPosition() {
        return targetWorldPosition;
    }

    /**
     * @deprecated Use setTargetWorldPosition instead
     */
    @Deprecated
    public void setTargetPosition(GridPosition target) {
        this.targetWorldPosition = Vector2.fromGridPosition(target);
    }

    /**
     * @deprecated Use getTargetWorldPosition instead
     */
    @Deprecated
    public GridPosition getTargetPosition() {
        return targetWorldPosition != null ? targetWorldPosition.toGridPosition() : null;
    }

    public void clearPath() {
        path.clear();
    }

    public void setPath(Deque<Vector2> newPath) {
        path.clear();
        path.addAll(newPath);
    }

    public Deque<Vector2> getPath() {
        return path;
    }

    public boolean isAirUnit() {
        return isAirUnit;
    }

    public boolean isBuildingOnly() {
        return buildingOnly;
    }

    public Card getBaseCard() {
        return baseCard;
    }

    /**
     * Returns the continuous world position.
     */
    public Vector2 getWorldPosition() {
        return worldPosition;
    }

    public void setWorldPosition(Vector2 pos) {
        this.worldPosition = pos;
    }

    /**
     * Returns the grid cell this troop is currently in.
     */
    public GridPosition getPosition() {
        return worldPosition != null ? worldPosition.toGridPosition() : null;
    }

    /**
     * @deprecated Use setWorldPosition instead
     */
    @Deprecated
    public void setPosition(GridPosition pos) {
        this.worldPosition = Vector2.fromGridPosition(pos);
    }

    public boolean isPlayerSide() {
        return isPlayer;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void takeDamage(double amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public double getMoveSpeed() {
        return moveSpeed;
    }

    public double getAttackRange() {
        return attackRange;
    }

    // moveProgress removed - continuous movement no longer needs it

    // Combat getters/setters
    public CombatStats getCombatStats() {
        return combatStats;
    }

    public double getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(double attackCooldown) {
        this.attackCooldown = attackCooldown;
    }

    public UnitState getUnitState() {
        return unitState;
    }

    public void setUnitState(UnitState unitState) {
        this.unitState = unitState;
    }

    // ICombatant Implementation

    public GridPosition getCenterPosition() {
        return getPosition(); // For 1x1 units, center is same as position
    }

    public int getWidth() {
        return 1;
    }

    public int getHeight() {
        return 1;
    }

    public double getRange() {
        return attackRange;
    }

    public double getDamage() {
        return combatStats != null ? combatStats.getDamage() : 0;
    }

    public double getHitSpeed() {
        return combatStats != null ? combatStats.getHitSpeedSeconds() : 1.0;
    }

    public TargetType getTargetType() {
        return baseCard != null ? baseCard.getTarget() : TargetType.GROUND;
    }

    public boolean canTarget(Troop t) {
        if (t == null || !t.isAlive())
            return false;
        TargetType tt = getTargetType();
        if (tt == TargetType.BUILDINGS)
            return false; // Troops can't target troops if building-only
        if (tt == TargetType.GROUND && t.isAirUnit())
            return false;
        if (tt == TargetType.AIR && !t.isAirUnit())
            return false; // Usually implies AIR-ONLY? Or BOTH?
        // Note: TargetType.AIR usually means targets AIR (and maybe GROUND?).
        // In this game, usually TargetType is GROUND, AIR, BUILDINGS, or BOTH (implies
        // all).
        // Let's assume standard logic.
        return true;
    }

    // Pathfinding optimization
    private double pathfindingCooldown = 0.0;

    public double getPathfindingCooldown() {
        return pathfindingCooldown;
    }

    public void setPathfindingCooldown(double val) {
        this.pathfindingCooldown = val;
    }

    @Override
    public void setTarget(Troop troop) {
        this.currentTarget = troop;
    }

    @Override
    public Troop getTarget() {
        if (currentTarget instanceof Troop) {
            return (Troop) currentTarget;
        }
        return null;
    }
}
