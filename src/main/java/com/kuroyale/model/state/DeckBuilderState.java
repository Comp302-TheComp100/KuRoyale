package com.kuroyale.model.state;

import java.util.ArrayList;
import java.util.List;

import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.Deck;

/**
 * Observer Pattern: Manages deck building state and notifies listeners of
 * changes.
 * GRASP: Information Expert - knows about deck state and can notify when
 * complete.
 */
public class DeckBuilderState {

    private final Deck deck;
    private final List<DeckStateListener> listeners;

    /**
     * Observer interface for deck state changes.
     */
    public interface DeckStateListener {
        void onDeckChanged(Deck deck);

        void onDeckComplete(boolean complete);
    }

    public DeckBuilderState() {
        this.deck = new Deck();
        this.listeners = new ArrayList<>();
    }

    /**
     * Add a listener for deck state changes.
     */
    public void addListener(DeckStateListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener.
     */
    public void removeListener(DeckStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Add a card to the deck.
     * 
     * @return true if card was added successfully.
     */
    public boolean addCard(Card card) {
        if (deck.addCard(card)) {
            notifyDeckChanged();
            return true;
        }
        return false;
    }

    /**
     * Remove a card from the deck.
     * 
     * @return true if card was removed successfully.
     */
    public boolean removeCard(Card card) {
        if (deck.removeCard(card)) {
            notifyDeckChanged();
            return true;
        }
        return false;
    }

    /**
     * Check if deck contains a card.
     */
    public boolean contains(Card card) {
        return deck.contains(card);
    }

    /**
     * Get the current deck.
     */
    public Deck getDeck() {
        return deck;
    }

    /**
     * Get current card count.
     */
    public int getCardCount() {
        return deck.size();
    }

    /**
     * Check if deck is complete (8 cards).
     */
    public boolean isComplete() {
        return deck.isValid();
    }

    /**
     * Clear the deck and notify listeners.
     */
    public void clear() {
        deck.clear();
        notifyDeckChanged();
    }

    /**
     * Set the deck from an existing deck (for preloading).
     */
    public void setDeck(Deck sourceDeck) {
        deck.clear();
        if (sourceDeck != null) {
            for (Card card : sourceDeck.getCards()) {
                deck.addCard(card);
            }
        }
        notifyDeckChanged();
    }

    private void notifyDeckChanged() {
        for (DeckStateListener listener : listeners) {
            listener.onDeckChanged(deck);
            listener.onDeckComplete(isComplete());
        }
    }
}
