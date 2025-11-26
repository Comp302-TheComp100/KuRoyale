package com.kuroyale.model;

public class Tower {
    private final TowerType type;
    private double maxHealth;
    private double currentHealth;
    private double damage;
    private double hitSpeed;
    private double range;
    private TargetType targetType;

    public enum TowerType {
        PRINCESS, KING
    }

    public Tower(TowerType type) {
        this.type = type;
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
    }

    public void takeDamage(double amount) {
        this.currentHealth -= amount;
        if (this.currentHealth < 0) {
            this.currentHealth = 0;
        }
    }

    public double getHealthPercentage() {
        return currentHealth / maxHealth;
    }

    // Getters
    public double getMaxHealth() {
        return maxHealth;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public double getDamage() {
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
}
