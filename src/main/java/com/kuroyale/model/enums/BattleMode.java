package com.kuroyale.model.enums;

/**
 * Defines the available battle modes for multiplayer gameplay.
 * Uses GRASP Information Expert - enum knows its own display data.
 */
public enum BattleMode {
    LOCAL_VS_BOT("Local Player vs Bot", "Battle against AI opponent"),
    LOCAL_PVP("Local Player vs Player", "Two players on same device"),
    NETWORK_PVP("Players through Network", "Online multiplayer battle");

    private final String displayName;
    private final String description;

    BattleMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
