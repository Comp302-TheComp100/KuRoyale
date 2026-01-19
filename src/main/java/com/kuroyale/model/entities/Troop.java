package com.kuroyale.model.entities;

import java.util.ArrayDeque;
import java.util.Deque;

import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.arena.Vector2;
import com.kuroyale.model.enums.*;

public class Troop implements ICombatant {
    private final Card baseCard;
    private Vector2 worldPosition, targetWorldPosition;
    private final boolean isPlayer, isAirUnit, buildingOnly;
    private int currentHealth;
    private double moveSpeed;
    private final double attackRange;
    private final Deque<Vector2> path;
    // Combat
    private CombatStats combatStats;
    private double attackCooldown;
    private UnitState unitState = UnitState.IDLE;
    private ICombatant currentTarget;

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
        this.combatStats = new CombatStats(card.getDamage(), card.getHitSpeed(), card.getRange(), at);
        this.attackCooldown = card.getHitSpeed();
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

    /**
     * Sets target world position from x, y coordinates (used for network sync).
     */
    public void setTargetWorldPosition(double x, double y) {
        this.targetWorldPosition = new Vector2(x, y);
    }

    public Vector2 getTargetWorldPosition() {
        return targetWorldPosition;
    }

    @Deprecated
    public void setTargetPosition(GridPosition target) {
        this.targetWorldPosition = Vector2.fromGridPosition(target);
    }

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

    public boolean isBuildingOnly() {
        return buildingOnly;
    }

    public Card getBaseCard() {
        return baseCard;
    }

    @Override
    public Vector2 getWorldPosition() {
        return worldPosition;
    }

    public void setWorldPosition(Vector2 pos) {
        this.worldPosition = pos;
    }

    /**
     * Sets world position from x, y coordinates (used for network sync).
     */
    public void setWorldPosition(double x, double y) {
        this.worldPosition = new Vector2(x, y);
    }

    public GridPosition getPosition() {
        return worldPosition != null ? worldPosition.toGridPosition() : null;
    }

    @Deprecated
    public void setPosition(GridPosition pos) {
        this.worldPosition = Vector2.fromGridPosition(pos);
    }

    @Override
    public boolean isPlayerSide() {
        return isPlayer;
    }

    @Override
    public boolean isAirUnit() {
        return isAirUnit;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    /**
     * Sets current health directly (used for network sync).
     */
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, health);
    }

    public void takeDamage(int amount) {
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

    public double getCollisionRadius() {
        return baseCard != null ? baseCard.getWidth() / 2.0 : 0.5;
    }

    // ICombatant Implementation

    @Override
    public GridPosition getCenterPosition() {
        return getPosition();
    }

    @Override
    public Vector2 getCenterWorldPosition() {
        return worldPosition; // Troops are point entities, center = position
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

    public int getDamage() {
        return combatStats != null ? combatStats.getDamage() : 0;
    }

    public double getHitSpeed() {
        return combatStats != null ? combatStats.getHitSpeedSeconds() : 1.0;
    }

    public TargetType getTargetType() {
        return baseCard != null ? baseCard.getTarget() : TargetType.GROUND;
    }

    @Override
    public boolean canTarget(ICombatant target) {
        if (target == null || !target.isAlive())
            return false;
        TargetType tt = getTargetType();

        // Specific logic: if building only, only target Buildings or Towers
        if (tt == TargetType.BUILDINGS) {
            return target instanceof Building || target instanceof Tower;
        }

        // Standard Target Checks
        if (tt == TargetType.GROUND && target.isAirUnit())
            return false;
        if (tt == TargetType.AIR && !target.isAirUnit())
            return false;

        return true;
    }

    @Override
    public void setTarget(ICombatant target) {
        this.currentTarget = target;
    }

    @Override
    public ICombatant getTarget() {
        return currentTarget;
    }

    @Override
    public boolean isAreaEffect() {
        return baseCard != null && baseCard.isAreaEffect();
    }

    private double stunTimer = 0.0;
    private double retargetCooldown = 0.0;
    private static final double RETARGET_COOLDOWN_DURATION = 0.5; // seconds

    @Override
    public void stun(double duration) {
        this.stunTimer = Math.max(this.stunTimer, duration);
    }

    @Override
    public boolean isStunned() {
        return stunTimer > 0;
    }

    @Override
    public void updateStatus(double deltaTime) {
        if (stunTimer > 0) {
            stunTimer -= deltaTime;
        }
        if (retargetCooldown > 0) {
            retargetCooldown -= deltaTime;
        }
    }

    public boolean canRetarget() {
        return retargetCooldown <= 0;
    }

    public void triggerRetargetCooldown() {
        this.retargetCooldown = RETARGET_COOLDOWN_DURATION;
    }

    public void heal(int amount) {
        currentHealth += amount;
    }

    public void modifySpeed(double multiplier) {
        this.moveSpeed *= multiplier;
    }

    public void buffDamage(double percent) {
        if (this.combatStats != null) {
            int newDamage = (int) (this.combatStats.getDamage() * (1.0 + percent));
            this.combatStats = new CombatStats(newDamage, this.combatStats.getHitSpeedSeconds(),
                    this.combatStats.getRangeTiles(), this.combatStats.getAttackType());
        }
    }

    public boolean isMelee() {
        return combatStats != null && combatStats.getAttackType() == CombatStats.AttackType.MELEE;
    }
}
