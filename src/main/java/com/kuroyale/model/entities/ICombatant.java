package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

/**
 * Common interface for all structures that can attack or be attacked.
 * Unifies Towers and Buildings for easier combat logic.
 */
public interface ICombatant {
    // Identity & State
    boolean isPlayerSide();

    boolean isAlive();

    // Position & Geometry
    GridPosition getPosition(); // Top-left position

    GridPosition getCenterPosition(); // Center for range calculations

    int getWidth();

    int getHeight();

    // Combat Stats
    double getRange();

    int getDamage();

    double getHitSpeed(); // Seconds per hit

    double getAttackCooldown();

    void setAttackCooldown(double cooldown);

    // Targeting & Damage
    TargetType getTargetType();

    boolean canTarget(Troop troop);

    void takeDamage(int amount);

    // Optional capabilities
    default boolean isAreaEffect() {
        return false;
    }

    // Target tracking
    void setTarget(Troop troop);

    Troop getTarget();
}
