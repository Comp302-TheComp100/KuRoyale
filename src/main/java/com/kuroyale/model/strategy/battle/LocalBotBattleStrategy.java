package com.kuroyale.model.strategy.battle;

import com.kuroyale.controller.BattleController;

/**
 * Strategy for Local Player vs Bot battle (current implementation).
 * Extracts existing battle logic into Strategy pattern.
 */
public class LocalBotBattleStrategy implements BattleStrategy {

    @Override
    public void initialize(BattleController controller) {
        // Uses existing startGame() logic which handles bot opponent
        controller.startGame();
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public String getNotImplementedMessage() {
        return null; // Always implemented
    }
}
