package com.kuroyale.model.entities;

import com.kuroyale.model.enums.TargetType;

public class Projectile {
    private final ICombatant owner;
    private final ICombatant target;
    private final int damage;
    private final boolean areaEffect;
    private final TargetType targetType;
    private final boolean isPlayerSide;

    private Vector2 position;
    private Vector2 targetPosSnapshot; // Where it's going (in case target moves/dies)
    private final double speed;
    private boolean active = true;

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
