package com.kuroyale.event;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Tower;

/**
 * Interface for listening to game-related events.
 * Implements the Observer pattern to decouple GameState from secondary
 * services.
 */
public interface GameEventListener {
    default void onCardPlayed(boolean isPlayer, Card card) {
    }

    default void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
    }

    default void onElixirSpent(boolean isPlayer, int amount) {
    }
}
