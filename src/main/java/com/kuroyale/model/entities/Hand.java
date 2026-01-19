package com.kuroyale.model.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

//Manages the 4-card hand drawn from the 8-card deck. Handles cycling cards as they are played.
public class Hand {
    public static final int HAND_SIZE = 4;

    private final List<Card> currentHand;
    private final Queue<Card> drawPile;
    private Card nextCard;

    public Hand(Deck deck) {
        this.currentHand = new ArrayList<>(HAND_SIZE);
        this.drawPile = new LinkedList<>();
        initializeHand(deck);
    }

    private void initializeHand(Deck deck) {
        List<Card> allCards = new ArrayList<>(deck.getCards());
        Collections.shuffle(allCards);

        // Fill hand
        for (int i = 0; i < Math.min(HAND_SIZE, allCards.size()); i++) {
            currentHand.add(allCards.get(i));
        }

        // Fill draw pile with remaining
        for (int i = HAND_SIZE; i < allCards.size(); i++) {
            drawPile.offer(allCards.get(i));
        }

        // Set next card
        updateNextCard();
    }

    // Plays a card from the hand and draws a new one.
    public Card playCard(int index) {
        if (index < 0 || index >= currentHand.size()) {
            return null;
        }

        Card playedCard = currentHand.get(index);

        // Add played card to bottom of draw pile (cycling)
        drawPile.offer(playedCard);

        // Replace played card with the next card from draw pile
        if (!drawPile.isEmpty()) {
            currentHand.set(index, drawPile.poll());
        }

        updateNextCard();
        return playedCard;
    }

    private void updateNextCard() {
        nextCard = drawPile.peek();
    }

    public List<Card> getCards() {
        return new ArrayList<>(currentHand);
    }

    public Card getCard(int index) {
        if (index >= 0 && index < currentHand.size()) {
            return currentHand.get(index);
        }
        return null;
    }

    public Card getNextCard() {
        return nextCard;
    }

    /*
     * Restores the hand and draw pile from saved data.
     * Used when loading a saved game to restore the exact card state.
     */
    public void restoreFromSaved(List<Card> handCards, List<Card> drawPileCards) {
        // Clear current state
        currentHand.clear();
        drawPile.clear();

        // Restore hand cards
        for (Card card : handCards) {
            if (card != null) {
                currentHand.add(card);
            }
        }

        // Restore draw pile cards
        for (Card card : drawPileCards) {
            if (card != null) {
                drawPile.offer(card);
            }
        }

        // Update next card
        updateNextCard();
    }

    // Gets a copy of the draw pile as a list (for saving).
    public List<Card> getDrawPileCards() {
        return new ArrayList<>(drawPile);
    }
}
