package com.kuroyale.model.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages turn-based gameplay for PvP battles (GoF State Pattern).
 * Tracks whose turn it is and notifies listeners on turn changes.
 */
public class TurnManager {

    /**
     * Enum representing which player's turn it currently is.
     */
    public enum Turn {
        PLAYER_1("Player 1"),
        PLAYER_2("Player 2");

        private final String displayName;

        Turn(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Listener interface for turn change events (Observer Pattern).
     */
    public interface TurnChangeListener {
        void onTurnChanged(Turn newTurn);
    }

    private Turn currentTurn;
    private final List<TurnChangeListener> listeners;
    private int turnCount;

    public TurnManager() {
        this.currentTurn = Turn.PLAYER_1;
        this.listeners = new ArrayList<>();
        this.turnCount = 1;
    }

    /**
     * End the current player's turn and switch to the other player.
     */
    public void endTurn() {
        currentTurn = (currentTurn == Turn.PLAYER_1) ? Turn.PLAYER_2 : Turn.PLAYER_1;
        turnCount++;
        notifyListeners();
    }

    /**
     * Check if it's the specified player's turn.
     */
    public boolean isCurrentTurn(Turn player) {
        return currentTurn == player;
    }

    /**
     * Check if it's Player 1's turn.
     */
    public boolean isPlayer1Turn() {
        return currentTurn == Turn.PLAYER_1;
    }

    /**
     * Check if it's Player 2's turn.
     */
    public boolean isPlayer2Turn() {
        return currentTurn == Turn.PLAYER_2;
    }

    public Turn getCurrentTurn() {
        return currentTurn;
    }

    public int getTurnCount() {
        return turnCount;
    }

    /**
     * Add a listener to be notified when the turn changes.
     */
    public void addTurnChangeListener(TurnChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a turn change listener.
     */
    public void removeTurnChangeListener(TurnChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (TurnChangeListener listener : listeners) {
            listener.onTurnChanged(currentTurn);
        }
    }

    /**
     * Reset the turn manager to initial state.
     */
    public void reset() {
        currentTurn = Turn.PLAYER_1;
        turnCount = 1;
        notifyListeners();
    }
}
