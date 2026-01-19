package com.kuroyale.model.core.enums;

/* Enum defining all permanent achievements.
 * Each achievement has a target value, gold reward, name, and description.*/
public enum AchievementType {
    FIRST_BLOOD(1, 500, "First Blood", "Win your first match"),
    TOWER_HUNTER(50, 750, "Tower Hunter", "Destroy 50 Crown Towers total"),
    CHALLENGE_MASTER(5, 1500, "Challenge Master", "Complete all 5 challenges"),
    THREE_STAR_HERO(1, 600, "Three-Star Hero", "Get 3 stars on any challenge"),
    LEGENDARY_COLLECTOR(1, 1000, "Legendary Collector", "Upgrade a Legendary card to Level 3"),
    NETWORK_WARRIOR(10, 800, "Network Warrior", "Win 10 network multiplayer matches"),
    ARMY_BUILDER(100, 700, "Army Builder", "Deploy 100 swarm troops total"),
    SPELL_MASTER(10000, 800, "Spell Master", "Deal 10,000 damage with spells total"),
    GOLD_HOARDER(5000, 500, "Gold Hoarder", "Accumulate 5,000 total gold earned"),
    VETERAN_PLAYER(50, 600, "Veteran Player", "Play 50 matches"),
    COMBO_EXPERT(25, 750, "Combo Expert", "Trigger 25 card combos"),
    UNDEFEATED(5, 1000, "Undefeated", "Win 5 matches in a row");

    private final int targetValue;
    private final int goldReward;
    private final String displayName;
    private final String description;

    AchievementType(int targetValue, int goldReward, String displayName, String description) {
        this.targetValue = targetValue;
        this.goldReward = goldReward;
        this.displayName = displayName;
        this.description = description;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
