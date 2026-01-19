package com.kuroyale.model.core.entities;

import com.kuroyale.model.core.enums.BattleMode;
import com.kuroyale.model.strategy.battle.*;

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
