package com.kuroyale.model.enums;

/*Enum defining the 5 challenge types in Challenge Mode.
 * Each challenge has unique rules and victory conditions.*/
public enum ChallengeType {
    SWARM_MASTER("Swarm Master", 250),
    SPELL_BARRAGE("Spell Barrage", 300),
    NO_BUILDINGS("No Buildings Allowed", 200),
    BUDGET_BATTLE("Budget Battle", 250),
    TANK_RUSH("Tank Rush", 300);

    private final String displayName;
    private final int goldReward;

    ChallengeType(String displayName, int goldReward) {
        this.displayName = displayName;
        this.goldReward = goldReward;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getGoldReward() {
        return goldReward;
    }
}
