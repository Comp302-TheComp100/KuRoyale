package com.kuroyale.model.core.entities;

import com.kuroyale.model.core.enums.*;

public class Building implements ICombatant {
    private final GridPosition position;
    private final int width, height;
    private final boolean playerSide;
    private int maxHealth, currentHealth;
    private final String imagePath;
    private String cardName;
    private final int lifetimeSeconds;
    private double remainingLifetime;
    private double accumulatedDecay = 0;
    private int damage;
    private double hitSpeedSeconds, rangeTiles;
    private TargetType targetType = TargetType.GROUND;
    private double attackCooldown;
    private boolean areaEffect = false;
    private Card baseCard;

    // Advanced Combat
    private double minRange = 0;

    // Inferno Tower Logic
    private boolean isInfernoTower = false;
    private double currentAttackDuration = 0.0;
    private int minDamage = 20;
    private int maxDamage = 400;
    private static final double RAMP_UP_TIME = 4.0; // Seconds to reach max damage

    // Production
    private String productionResource;
    private int productionAmount;
    private double productionInterval, productionTimer;

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

        if ("Inferno Tower".equals(cardName)) {
            this.isInfernoTower = true;
            this.maxDamage = card.getDamage(); // 400
            this.minDamage = 20;
            this.damage = minDamage; // Start at low damage
        }

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
        // Center = topLeft + size/2 (this gives proper center for both odd and even
        // widths)
        int cx = position.getX() + (width > 0 ? width / 2 : 0);
        int cy = position.getY() + (height > 0 ? height / 2 : 0);
        return GridPosition.tryCreate(cx, cy);
    }

    @Override
    public Vector2 getWorldPosition() {
        if (position == null)
            return null;
        return Vector2.fromGridPosition(position);
    }

    @Override
    public Vector2 getCenterWorldPosition() {
        if (position == null)
            return null;
        // Precise center using floating point
        return new Vector2(
                position.getX() + width / 2.0,
                position.getY() + height / 2.0);
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

    /**
     * Sets current health directly (used for network sync).
     */
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(maxHealth, health));
    }

    public void setRemainingLifetime(double lifetime) {
        this.remainingLifetime = Math.max(0, lifetime);
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
        if (this.target != target) {
            // Target changed or lost, reset ramp
            if (isInfernoTower) {
                this.currentAttackDuration = 0;
                this.damage = minDamage;
            }
        }
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

        // Inferno Ramping Logic
        if (isInfernoTower && target != null && target.isAlive()) {
            // Only ramp if actually engaging (within range)
            // We can check range loosely here, or rely on CombatService calling this only
            // when relevant?
            // Actually CombatService calls updateStatus every frame for alive buildings.
            // But we only want to ramp if we are ATTACKING.
            // CombatService sets target if found.
            // We assume if we have a target, we are attacking or trying to.

            // Check distance to be sure we are locked on?
            // Ideally simply having a target means we are "locked on" for the Inferno.

            currentAttackDuration += deltaTime;

            if (currentAttackDuration > 0.5) { // Small buffer before ramping starts effectively
                double progress = Math.min(1.0, currentAttackDuration / RAMP_UP_TIME);
                // Linear ramp
                // int ramped = (int) (minDamage + (maxDamage - minDamage) * progress);

                // Exponential/Tiered-like ramp feels better for Inferno
                // Let's stick to linear for now as requested "increases by time"
                this.damage = (int) (minDamage + (maxDamage - minDamage) * progress);
            } else {
                this.damage = minDamage;
            }
        } else if (isInfernoTower) {
            // No target, reset
            currentAttackDuration = 0;
            this.damage = minDamage;
        }
    }

    public void heal(int amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    public void buffRange(double tiles) {
        this.rangeTiles += tiles;
        // Also update min range? No, default is fine.
    }
}
