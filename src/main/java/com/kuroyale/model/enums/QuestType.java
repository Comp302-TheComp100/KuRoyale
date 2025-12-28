package com.kuroyale.model.enums;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.logic.*;

/**
 * Enum defining all possible daily quest types.
 * Each quest has a target value, gold reward, and description template.
 */
public enum QuestType {
    WIN_MATCHES(3, 250, "Win %d matches"),
    DESTROY_CROWN_TOWERS(5, 200, "Destroy %d Crown Towers"),
    PLAY_SPELL_CARDS(10, 150, "Play %d spell cards"),
    DEPLOY_TROOP_CARDS(15, 175, "Deploy %d troop cards"),
    SPEND_ELIXIR(100, 100, "Spend %d total Elixir"),
    WIN_WITHOUT_LOSING_TOWER(1, 300, "Win a match without losing a Crown Tower"),
    PLAY_BUILDING_CARDS(5, 150, "Play %d building cards"),
    DEAL_SPELL_DAMAGE(3000, 200, "Deal %d damage with spells"),
    WIN_WITH_COMMON_CARDS(1, 250, "Win using only common cards"),
    COMPLETE_CHALLENGES(2, 300, "Complete %d challenges"),
    WIN_NETWORK_MATCH(1, 200, "Win a network multiplayer match"),
    PLAY_CARDS_IN_SINGLE_MATCH(20, 150, "Play %d cards in a single match"),
    WIN_MATCHES_IN_ROW(2, 300, "Win %d matches in a row"),
    DESTROY_KING_TOWER(1, 350, "Destroy an enemy King Tower"),
    WIN_PVP_MATCH(1, 200, "Win a PvP match");

    private final int targetValue;
    private final int goldReward;
    private final String descriptionTemplate;

    QuestType(int targetValue, int goldReward, String descriptionTemplate) {
        this.targetValue = targetValue;
        this.goldReward = goldReward;
        this.descriptionTemplate = descriptionTemplate;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public String getDescription() {
        return String.format(descriptionTemplate, targetValue);
    }

    public String getDescriptionTemplate() {
        return descriptionTemplate;
    }
}
