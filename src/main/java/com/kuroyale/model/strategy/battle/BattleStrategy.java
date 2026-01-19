package com.kuroyale.model.strategy.battle;

import com.kuroyale.controller.BattleController;

/**
 * Strategy interface for battle initialization (GoF Strategy Pattern).
 * Promotes low coupling by isolating battle-type-specific logic.
 */
public interface BattleStrategy {

    /**
     * Initialize and start the battle with this strategy.
     * 
     * @param controller The BattleController to initialize
     */
    void initialize(BattleController controller);

    /**
     * @return true if this battle mode is fully implemented
     */
    boolean isImplemented();

    /**
     * @return message to show if mode is not implemented
     */
    String getNotImplementedMessage();
}
