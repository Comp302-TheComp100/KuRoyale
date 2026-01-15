package com.kuroyale.service.battle;

import com.kuroyale.controller.BattleController;

/**
 * Strategy for Network multiplayer battle.
 * Implements Host-Client model for online multiplayer gameplay.
 * 
 * GRASP Patterns:
 * - Strategy Pattern: Encapsulates network battle initialization logic
 * - Low Coupling: Isolated from other battle modes
 * 
 * Network Architecture:
 * - Host acts as server, Client connects to host
 * - Uses Java Sockets for TCP communication
 * - Synchronizes: card placements, unit movements, tower damage, elixir, timer
 */
public class NetworkPvPBattleStrategy implements BattleStrategy {

    @Override
    public void initialize(BattleController controller) {
        // Note: For network mode, we navigate to network-lobby.fxml first
        // The actual battle initialization happens in NetworkBattleController
        // This method is called but the navigation is handled by BattleModeSelectionController
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public String getNotImplementedMessage() {
        return ""; // Not used since isImplemented() returns true
    }
}
