package com.kuroyale.service.battle;

import com.kuroyale.controller.BattleController;

/**
 * Strategy for Network multiplayer battle.
 * Currently a stub - to be implemented in future.
 */
public class NetworkPvPBattleStrategy implements BattleStrategy {

    @Override
    public void initialize(BattleController controller) {
        // Not implemented yet
    }

    @Override
    public boolean isImplemented() {
        return false;
    }

    @Override
    public String getNotImplementedMessage() {
        return "Network Multiplayer mode is coming soon!\n\n" +
                "This mode will allow you to battle players online.";
    }
}
