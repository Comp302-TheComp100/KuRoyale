package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

public class Building implements ICombatant {
    private final GridPosition position; // top-left grid position
    private final int width;
    private final int height;
    private final boolean playerSide; // owner
    private double maxHealth;
    private double currentHealth;
    private final String imagePath; // optional image path from card
    private String cardName; // Store the card name for save/load functionality
    // Lifetime tracking for depreciation
    private final int lifetimeSeconds; // Total lifetime in seconds
    private double remainingLifetime; // Remaining lifetime in seconds
    // Combat
    private int damage;
    private double hitSpeedSeconds;
    private int rangeTiles;
    private TargetType targetType = TargetType.GROUND; // default
    private double attackCooldown;
    private boolean areaEffect = false;

    public Building(GridPosition position, int width, int height, boolean playerSide, double maxHealth,
            String imagePath) {
        this(position, width, height, playerSide, maxHealth, imagePath, 0);
    }

    public Building(GridPosition position, int width, int height, boolean playerSide, double maxHealth,
            String imagePath, int lifetimeSeconds) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.playerSide = playerSide;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.imagePath = imagePath;
        this.lifetimeSeconds = lifetimeSeconds;
        this.remainingLifetime = lifetimeSeconds;
        this.damage = 0;
        this.hitSpeedSeconds = 0.0;
        this.rangeTiles = 1;
        this.attackCooldown = 0.0;
    }

    // Configure combat from a base card
    public void configureCombatFromCard(Card card) {
        if (card == null)
            return;
        this.cardName = card.getName();
        this.damage = card.getDamage();
        this.hitSpeedSeconds = card.getHitSpeed();
        this.rangeTiles = (int) Math.round(card.getRange());
        this.targetType = card.getTarget();
        this.areaEffect = card.isAreaEffect();
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    @Override
    public GridPosition getPosition() {
        return position;
    }

    @Override
    public GridPosition getCenterPosition() {
        if (position == null)
            return null;
        // Center = topLeft + (size-1)/2
        int cx = position.getX() + (width > 0 ? (width - 1) / 2 : 0);
        int cy = position.getY() + (height > 0 ? (height - 1) / 2 : 0);
        return GridPosition.tryCreate(cx, cy);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isPlayerSide() {
        return playerSide;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getDamage() {
        return damage;
    }

    // ICombatant getter for hit speed
    public double getHitSpeed() {
        return hitSpeedSeconds;
    }

    public double getHitSpeedSeconds() {
        return hitSpeedSeconds;
    }

    // ICombatant getter for range
    public double getRange() {
        return rangeTiles;
    }

    public int getRangeTiles() {
        return rangeTiles;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public boolean isAreaEffect() {
        return areaEffect;
    }

    public double getAttackCooldown() {
        return attackCooldown;
    }

    public void setAttackCooldown(double cd) {
        this.attackCooldown = cd;
    }

    public int getLifetimeSeconds() {
        return lifetimeSeconds;
    }

    public double getRemainingLifetime() {
        return remainingLifetime;
    }

    // Update building state (lifetime depreciation)
    public void update(double deltaTime) {
        if (lifetimeSeconds > 0) {
            remainingLifetime -= deltaTime;

            // Calculate decay amount per second: maxHealth / lifetimeSeconds
            double decayPerSecond = maxHealth / lifetimeSeconds;
            double decayAmount = decayPerSecond * deltaTime;

            // Apply decay
            currentHealth = Math.max(0, currentHealth - decayAmount);

            if (remainingLifetime <= 0) {
                // Building expired, ensure health is 0
                currentHealth = 0;
            }
        }
    }

    public void takeDamage(double amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public boolean canTargetTroop(Troop t) {
        return canTarget(t);
    }

    @Override
    public boolean canTarget(Troop t) {
        if (t == null || !t.isAlive())
            return false;
        if (this.targetType == TargetType.GROUND && t.isAirUnit())
            return false;
        if (this.targetType == TargetType.AIR && !t.isAirUnit())
            return false;
        return true;
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
