package com.kuroyale.model.state;

import com.kuroyale.model.entities.Deck;

/**
 * Singleton session to hold deck state across navigation between screens.
 * Memento Pattern: Preserves deck state when navigating to/from deck builder.
 * Mediator Pattern: Acts as intermediary for inter-screen communication.
 */
public class PvPDeckBuilderSession {

    private static PvPDeckBuilderSession instance;

    private Deck player1Deck;
    private Deck player2Deck;
    private int activePlayer; // 1 or 2
    private boolean pvpMode; // Whether in PvP deck building mode

    private PvPDeckBuilderSession() {
        reset();
    }

    public static PvPDeckBuilderSession getInstance() {
        if (instance == null) {
            instance = new PvPDeckBuilderSession();
        }
        return instance;
    }

    /**
     * Start a PvP deck building session.
     */
    public void startSession() {
        this.pvpMode = true;
    }

    /**
     * End the PvP session and reset state.
     */
    public void endSession() {
        reset();
    }

    /**
     * Reset all session state.
     */
    public void reset() {
        this.player1Deck = null;
        this.player2Deck = null;
        this.activePlayer = 0;
        this.pvpMode = false;
    }

    /**
     * Set the active player before navigating to deck builder.
     */
    public void setActivePlayer(int playerNumber) {
        this.activePlayer = playerNumber;
    }

    public int getActivePlayer() {
        return activePlayer;
    }

    /**
     * Save the built deck for the active player.
     */
    public void saveBuiltDeck(Deck deck) {
        if (activePlayer == 1) {
            player1Deck = deck;
        } else if (activePlayer == 2) {
            player2Deck = deck;
        }
    }

    public Deck getPlayer1Deck() {
        return player1Deck;
    }

    public void setPlayer1Deck(Deck deck) {
        this.player1Deck = deck;
    }

    public Deck getPlayer2Deck() {
        return player2Deck;
    }

    public void setPlayer2Deck(Deck deck) {
        this.player2Deck = deck;
    }

    public boolean isPvpMode() {
        return pvpMode;
    }

    /**
     * Check if both players have complete decks.
     */
    public boolean areBothDecksReady() {
        return player1Deck != null && player1Deck.isValid()
                && player2Deck != null && player2Deck.isValid();
    }
}
