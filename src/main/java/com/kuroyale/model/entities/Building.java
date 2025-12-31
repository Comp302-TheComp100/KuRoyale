package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

public class Building implements ICombatant {
    private final GridPosition position; // top-left grid position
    private final int width;
    private final int height;
    private final boolean playerSide; // owner
    private int maxHealth;
    private int currentHealth;
    private final String imagePath; // optional image path from card
    private String cardName; // Store the card name for save/load functionality
    // Lifetime tracking for depreciation
    private final int lifetimeSeconds; // Total lifetime in seconds
    private double remainingLifetime; // Remaining lifetime in seconds
    private double accumulatedDecay = 0; // Tracks fractional damage due to lifetime decay
    // Combat
    private int damage;
    private double hitSpeedSeconds;
    private double rangeTiles;
    private TargetType targetType = TargetType.GROUND; // default
    private double attackCooldown;
    private boolean areaEffect = false;
    private Card baseCard; // Reference to original card for spawning and other properties

    // Advanced Combat
    private double minRange = 0;

    // Production
    private String productionResource;
    private int productionAmount;
    private double productionInterval;
    private double productionTimer;

    public Building(GridPosition position, int width, int height, boolean playerSide, int maxHealth,
            String imagePath) {
        this(position, width, height, playerSide, maxHealth, imagePath, 0);
    }

    public Building(GridPosition position, int width, int height, boolean playerSide, int maxHealth,
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
        this.baseCard = card;
        this.cardName = card.getName();
        this.damage = card.getDamage();
        this.hitSpeedSeconds = card.getHitSpeed();
        this.rangeTiles = card.getRange();
        this.targetType = card.getTarget();
        this.areaEffect = card.isAreaEffect();
        this.minRange = card.getMinRange();
        this.attackCooldown = this.hitSpeedSeconds;

        this.productionResource = card.getProductionResource();
        this.productionAmount = card.getProductionAmount();
        this.productionInterval = card.getProductionInterval();
        // Initialize timer to full interval so it produces after the first interval
        this.productionTimer = this.productionInterval;
    }

    public Card getBaseCard() {
        return baseCard;
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

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getCurrentHealth() {
        return currentHealth;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getDamage() {
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

    public double getRangeTiles() {
        return rangeTiles;
    }

    public double getMinRange() {
        return minRange;
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

            // Apply decay (using floating point for precision during calculation, but
            // applying as int)
            double decayPerSecond = (double) maxHealth / lifetimeSeconds;
            double decayAmount = decayPerSecond * deltaTime;

            // Accumulate decay
            accumulatedDecay += decayAmount;

            if (accumulatedDecay >= 1.0) {
                int damage = (int) accumulatedDecay;
                currentHealth = Math.max(0, currentHealth - damage);
                accumulatedDecay -= damage;
            }

            if (remainingLifetime <= 0) {
                // Building expired, ensure health is 0
                currentHealth = 0;
            }
        }
    }

    public void takeDamage(int amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public boolean isAlive() {
        return currentHealth > 0;
    }

    public boolean canTargetTroop(Troop t) {
        return canTarget(t);
    }

    @Override
    public boolean isAirUnit() {
        return false;
    }

    @Override
    public boolean canTarget(ICombatant target) {
        if (target == null || !target.isAlive())
            return false;
        if (this.targetType == TargetType.BOTH)
            return true;
        if (this.targetType == TargetType.GROUND && target.isAirUnit())
            return false;
        if (this.targetType == TargetType.AIR && !target.isAirUnit())
            return false;
        return true;
    }

    // Target tracking for MVC
    private ICombatant target;

    public void setTarget(ICombatant target) {
        this.target = target;
    }

    public ICombatant getTarget() {
        return target;
    }

    // Production getters/state
    public String getProductionResource() {
        return productionResource;
    }

    public double getProductionTimer() {
        return productionTimer;
    }

    public void setProductionTimer(double timer) {
        this.productionTimer = timer;
    }

    public int getProductionAmount() {
        return productionAmount;
    }

    public double getProductionInterval() {
        return productionInterval;
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
