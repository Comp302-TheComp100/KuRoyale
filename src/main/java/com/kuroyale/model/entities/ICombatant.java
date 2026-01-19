package com.kuroyale.model.entities;

import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.arena.Vector2;
import com.kuroyale.model.enums.*;

/* Common interface for all structures that can attack or be attacked.
 * Unifies Towers and Buildings for easier combat logic.*/
public interface ICombatant {
    // Identity & State
    boolean isPlayerSide();

    boolean isAlive();

    boolean isAirUnit();

    // Position & Geometry
    GridPosition getPosition(); // Top-left position (discrete grid)

    GridPosition getCenterPosition(); // Center for range calculations (discrete grid)

    // World coordinates (continuous, sub-tile precision)
    Vector2 getWorldPosition(); // World position of entity

    Vector2 getCenterWorldPosition(); // Center in world coordinates

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

    boolean canTarget(ICombatant target);

    void takeDamage(int amount);

    // Optional capabilities
    default boolean isAreaEffect() {
        return false;
    }

    // Target tracking
    void setTarget(ICombatant target);

    ICombatant getTarget();

    // Status effects
    void stun(double duration);

    boolean isStunned();

    void updateStatus(double deltaTime);
}
