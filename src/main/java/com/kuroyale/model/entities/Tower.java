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
        this.attackCooldown = 0.0;
    }

    @Override
    public void takeDamage(int amount) {
        this.currentHealth -= amount;
        if (this.currentHealth < 0) {
            this.currentHealth = 0;
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
    public GridPosition getPosition() {
        // Tower itself doesn't store its position in the Model currently,
        // it acts as a Flyweight or is stored in the Map<GridPosition, Tower>.
        // However, the interface requires getPosition().
        // Refactoring Note: usage of getPosition() for Tower in previous code
        // relied on iterating the map or was null.
        // We need the position to be stored in the Tower or passed in.
        // Since Tower instances are unique per location (created in initializeGrid),
        // we should probably assign the position at creation time to satisfy this
        // interface
        // without breaking the API.
        // Let's add a position field.
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
    // But we know from Arena.java lines 89 (4x4) and 58 (3x3).
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
    public boolean canTarget(Troop troop) {
        if (troop == null || !troop.isAlive())
            return false;
        if (targetType == TargetType.BOTH)
            return true;
        if (targetType == TargetType.GROUND && !troop.isAirUnit())
            return true;
        if (targetType == TargetType.AIR && troop.isAirUnit())
            return true;
        return false;
    }

    // Target tracking for MVC
    private Troop target;

    public void setTarget(Troop target) {
        this.target = target;
    }

    public Troop getTarget() {
        return target;
    }
}
