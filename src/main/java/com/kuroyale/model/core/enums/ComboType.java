package com.kuroyale.model.core.enums;

public enum ComboType {
    TANK_SUPPORT("Tank + Support", "Ranged damage +15%"),
    SPELL_SYNERGY("Spell Synergy", "Elixir Refunded!"),
    SWARM_ATTACK("Swarm Attack", "Move Speed +10%"),
    BUILDING_DEFENSE("Building Defense", "HP +20%"),
    AIR_ASSAULT("Air Assault", "Air Damage +15%"),
    ROYAL_COMBO("Royal Combo", "Knight HP +100"),
    SIEGE_MODE("Siege Mode", "Range +2 Tiles"),
    RUSH_ATTACK("Rush Attack", "Hog Speed +20%");

    private final String displayName;
    private final String effectDescription;

    ComboType(String displayName, String effectDescription) {
        this.displayName = displayName;
        this.effectDescription = effectDescription;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEffectDescription() {
        return effectDescription;
    }
}
