package com.kuroyale.model.entities;

import com.kuroyale.model.enums.TargetType;

public class Projectile {
    private final ICombatant owner;
    private final ICombatant target;
    private final int damage;
    private final boolean areaEffect;
    private final TargetType targetType;
    private boolean isPlayerSide;

    private Vector2 position;
    private Vector2 targetPosSnapshot; // Where it's going (in case target moves/dies)
    private double speed;
    private boolean active = true;

    /**
     * Standard constructor for combat-created projectiles.
     */
    public Projectile(ICombatant owner, ICombatant target) {
        this.owner = owner;
        this.target = target;
        this.damage = owner.getDamage();
        this.areaEffect = owner.isAreaEffect();
        this.targetType = owner.getTargetType();
        this.isPlayerSide = owner.isPlayerSide();

        // safe start position
        GridPosition start = owner.getCenterPosition();
        if (start != null) {
            this.position = new Vector2(start.getX() + 0.5, start.getY() + 0.5);
        } else {
            this.position = new Vector2(0, 0);
        }
        updateTargetSnapshot();

        // Calculate speed such that travel time = owner's hit speed
        // Speed = Distance / Time
        double dist = owner.getRange();
        double hitSpeed = Math.max(0.1, owner.getHitSpeed()); // Sanity check: min 0.1s
        this.speed = Math.max(1.0, dist / hitSpeed); // Ensure it actually moves (min speed 1.0)
    }
    
    /**
     * Simplified constructor for network sync (CLIENT side rendering only).
     * These projectiles are just for visual display, not for combat logic.
     */
    public Projectile(Vector2 position, Vector2 targetPosition, boolean isPlayerSide) {
        this.owner = null;
        this.target = null;
        this.damage = 0;
        this.areaEffect = false;
        this.targetType = TargetType.BOTH;
        this.isPlayerSide = isPlayerSide;
        this.position = position;
        this.targetPosSnapshot = targetPosition;
        this.speed = 5.0; // Default speed for rendering
    }

    public void update(double deltaTime) {
        if (!active)
            return;

        // Update target position if target is still alive
        if (target != null && target.isAlive()) {
            updateTargetSnapshot();
        }

        // Move towards target
        double dist = position.distanceTo(targetPosSnapshot);
        double move = speed * deltaTime;

        if (move >= dist) {
            // Hit!
            this.active = false; // logic will handle damage application
        } else {
            Vector2 dir = targetPosSnapshot.subtract(position).normalize();
            position = position.add(dir.multiply(move));
        }
    }

    private void updateTargetSnapshot() {
        if (target == null)
            return;
        GridPosition p = target.getCenterPosition();

        if (p != null) {
            this.targetPosSnapshot = new Vector2(p.getX() + 0.5, p.getY() + 0.5);
        }
    }

    public boolean isActive() {
        return active;
    }

    public Vector2 getPosition() {
        return position;
    }
    
    /**
     * Sets position directly (used for network sync).
     */
    public void setPosition(Vector2 pos) {
        this.position = pos;
    }

    public ICombatant getOwner() {
        return owner;
    }

    public ICombatant getTarget() {
        return target;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isAreaEffect() {
        return areaEffect;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public boolean isPlayerSide() {
        return isPlayerSide;
    }
}
