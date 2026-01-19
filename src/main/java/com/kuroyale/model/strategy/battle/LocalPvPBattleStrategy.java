package com.kuroyale.model.strategy.battle;

import com.kuroyale.controller.BattleController;
import com.kuroyale.util.SceneLoader;

/**
 * Strategy for Local Player vs Player battle (same device).
 * Navigates to PvP deck selection screen.
 */
public class LocalPvPBattleStrategy implements BattleStrategy {

    @Override
    public void initialize(BattleController controller) {
        // This strategy navigates to PvP deck selection first
        // The actual game initialization happens in PvPBattleController
        try {
            SceneLoader sceneLoader = new SceneLoader();
            sceneLoader.load(controller.getArenaContainer(), "/fxml/pvp-deck-selection.fxml",
                    "KU Royale - PvP Deck Selection", null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public String getNotImplementedMessage() {
        return null; // Implemented
    }
}
