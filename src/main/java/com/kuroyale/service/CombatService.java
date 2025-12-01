package com.kuroyale.service;

import com.kuroyale.model.Building;
import com.kuroyale.model.Troop;
import com.kuroyale.model.Tower;

public class CombatService {
    public void applyDamage(Troop attacker, Troop target) {
        int dmg = attacker.getCombatStats().getDamage();
        target.takeDamage(dmg);
    }

    public void applyDamage(Building attacker, Troop target) {
        int dmg = attacker.getDamage();
        target.takeDamage(dmg);
    }

    public void applyDamage(Troop attacker, Building target) {
        int dmg = attacker.getCombatStats().getDamage();
        target.takeDamage(dmg);
    }

    public void applyDamage(Troop attacker, Tower target) {
        int dmg = attacker.getCombatStats().getDamage();
        target.takeDamage(dmg);
    }

    public void applyDamage(Tower attacker, Troop target) {
        int dmg = (int)Math.round(attacker.getDamage());
        target.takeDamage(dmg);
    }
}
