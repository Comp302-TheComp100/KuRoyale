package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.logic.*;

public class CombatStats {
    private final int damage;
    private final double hitSpeedSeconds;
    private final int rangeTiles;
    private final AttackType attackType;

    public enum AttackType {
        MELEE,
        RANGED
    }

    public CombatStats(int damage, double hitSpeedSeconds, int rangeTiles, AttackType attackType) {
        this.damage = damage;
        this.hitSpeedSeconds = hitSpeedSeconds;
        this.rangeTiles = rangeTiles;
        this.attackType = attackType;
    }

    public int getDamage() { return damage; }
    public double getHitSpeedSeconds() { return hitSpeedSeconds; }
    public int getRangeTiles() { return rangeTiles; }
    public AttackType getAttackType() { return attackType; }
}
