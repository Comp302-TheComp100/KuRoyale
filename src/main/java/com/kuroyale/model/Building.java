package com.kuroyale.model;

public class Building {
    private final GridPosition position; // top-left grid position
    private final int width; // tiles
    private final int height; // tiles
    private final boolean playerSide; // owner
    private double maxHealth;
    private double currentHealth;
    private final String imagePath; // optional image path from card
    // Combat
    private int damage;
    private double hitSpeedSeconds;
    private int rangeTiles;
    private TargetType targetType = TargetType.GROUND; // default
    private double attackCooldown;

    public Building(GridPosition position, int width, int height, boolean playerSide, double maxHealth, String imagePath) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.playerSide = playerSide;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.imagePath = imagePath;
        this.damage = 0;
        this.hitSpeedSeconds = 0.0;
        this.rangeTiles = 1;
        this.attackCooldown = 0.0;
    }

    // Configure combat from a base card
    public void configureCombatFromCard(Card card) {
        if (card == null) return;
        this.damage = card.getDamage();
        this.hitSpeedSeconds = card.getHitSpeed();
        this.rangeTiles = (int) Math.round(card.getRange());
        this.targetType = card.getTarget();
    }

    public GridPosition getPosition() { return position; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isPlayerSide() { return playerSide; }
    public double getMaxHealth() { return maxHealth; }
    public double getCurrentHealth() { return currentHealth; }
    public String getImagePath() { return imagePath; }
    public int getDamage() { return damage; }
    public double getHitSpeedSeconds() { return hitSpeedSeconds; }
    public int getRangeTiles() { return rangeTiles; }
    public TargetType getTargetType() { return targetType; }
    public double getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(double cd) { this.attackCooldown = cd; }

    public void takeDamage(double amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public boolean isAlive() { return currentHealth > 0; }

    public boolean canTargetTroop(Troop t) {
        if (t == null || !t.isAlive()) return false;
        if (this.targetType == TargetType.GROUND && t.isAirUnit()) return false;
        return true;
    }
}
