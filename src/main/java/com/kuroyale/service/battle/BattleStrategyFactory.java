package com.kuroyale.service.battle;

import com.kuroyale.model.enums.BattleMode;

/**
 * Factory for creating BattleStrategy instances (GoF Factory Method Pattern).
 * Centralizes strategy creation logic and promotes low coupling.
 */
public class BattleStrategyFactory {

    /**
     * Creates the appropriate strategy for the given battle mode.
     * 
     * @param mode The selected battle mode
     * @return The corresponding BattleStrategy implementation
     */
    public static BattleStrategy createStrategy(BattleMode mode) {
        return switch (mode) {
            case LOCAL_VS_BOT -> new LocalBotBattleStrategy();
            case LOCAL_PVP -> new LocalPvPBattleStrategy();
            case NETWORK_PVP -> new NetworkPvPBattleStrategy();
        };
    }
}
