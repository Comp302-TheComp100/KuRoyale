package com.kuroyale.model.core.entities;

public class CombatStats {
    private final int damage;
    private final double hitSpeedSeconds;
    private final double rangeTiles;
    private final AttackType attackType;

    public enum AttackType {
        MELEE,
        RANGED
    }

    public CombatStats(int damage, double hitSpeedSeconds, double rangeTiles, AttackType attackType) {
        this.damage = damage;
        this.hitSpeedSeconds = hitSpeedSeconds;
        this.rangeTiles = rangeTiles;
        this.attackType = attackType;
    }

    public int getDamage() {
        return damage;
    }

    public double getHitSpeedSeconds() {
        return hitSpeedSeconds;
    }

    public double getRangeTiles() {
        return rangeTiles;
    }

    public AttackType getAttackType() {
        return attackType;
    }
}
