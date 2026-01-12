package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

public class Tower implements ICombatant {
    private final TowerType type;
    private final boolean playerSide; // Now explicitly tracked
    private int maxHealth;
    private int currentHealth;
    private int damage;
    private double hitSpeed;
    private double range;
    private TargetType targetType;
    private double attackCooldown;

    public enum TowerType {
        PRINCESS, KING
    }

    public Tower(TowerType type, boolean playerSide) {
        this.type = type;
        this.playerSide = playerSide;
        initializeStats();
    }

    private void initializeStats() {
        if (type == TowerType.PRINCESS) {
            this.maxHealth = 1400;
            this.damage = 50;
            this.hitSpeed = 0.8;
            this.range = 7.5;
        } else if (type == TowerType.KING) {
            this.maxHealth = 2400;
            this.damage = 50;
            this.hitSpeed = 1.0;
            this.range = 7.0;
        }
        this.currentHealth = this.maxHealth;
        this.targetType = TargetType.BOTH;
        this.attackCooldown = this.hitSpeed;
    }

    @Override
    public void takeDamage(int amount) {
        if (amount > 0 && currentHealth > 0) {
            this.currentHealth -= amount;
            if (this.currentHealth < 0) {
                this.currentHealth = 0;
            }
            // Publish damage event
            com.kuroyale.event.GameEventBus.getInstance().publishTowerDamaged(this);
        }
    }

    @Override
    public boolean isAlive() {
        return currentHealth > 0;
    }

    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, (int) maxHealth));
    }

    // Getters implementation for ICombatant
    @Override
    public boolean isPlayerSide() {
        return playerSide;
    }

    @Override
    public boolean isAirUnit() {
        return false;
    }

    @Override
    public GridPosition getPosition() {
        return this.position;
    }

    // We need to add position field to Tower to support ICombatant properly
    private GridPosition position;

    public void setPosition(GridPosition position) {
        this.position = position;
    }

    @Override
    public GridPosition getCenterPosition() {
        if (position == null)
            return null;
        int w = getWidth();
        int h = getHeight();
        // Center = topLeft + (size-1)/2
        int cx = position.getX() + (w > 0 ? (w - 1) / 2 : 0);
        int cy = position.getY() + (h > 0 ? (h - 1) / 2 : 0);
        return GridPosition.tryCreate(cx, cy);
    }

    // Actually, can't check Arena.java easily here inside the class without passing
    // it.
    public int getWidthTiles() { // Renamed helper for clarity
        return type == TowerType.KING ? 4 : 3;
    }

    @Override
    public int getHeight() {
        return getWidthTiles();
    }

    // Fix conflict with ICombatant vs methods
    @Override
    public int getWidth() {
        return getWidthTiles();
    }

    public double getHealthPercentage() {
        return (double) currentHealth / maxHealth;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public int getDamage() {
        return damage;
    }

    public double getHitSpeed() {
        return hitSpeed;
    }

    public double getRange() {
        return range;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public TowerType getType() {
        return type;
    }

    public double getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(double cd) {
        this.attackCooldown = cd;
    }

    @Override
    public boolean canTarget(ICombatant target) {
        if (target == null || !target.isAlive())
            return false;
        if (targetType == TargetType.BOTH)
            return true;
        if (targetType == TargetType.GROUND && !target.isAirUnit())
            return true;
        if (targetType == TargetType.AIR && target.isAirUnit())
            return true;
        return false;
    }

    // Target tracking for MVC
    private ICombatant target;

    @Override
    public void setTarget(ICombatant target) {
        this.target = target;
    }

    @Override
    public ICombatant getTarget() {
        return target;
    }

    private double stunTimer = 0.0;

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
    }
}
