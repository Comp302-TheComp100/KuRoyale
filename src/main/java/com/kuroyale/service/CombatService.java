package com.kuroyale.service;

import com.kuroyale.model.Troop;

public class CombatService {
    public void applyDamage(Troop attacker, Troop target) {
        int dmg = attacker.getCombatStats().getDamage();
        target.takeDamage(dmg);
    }
}
