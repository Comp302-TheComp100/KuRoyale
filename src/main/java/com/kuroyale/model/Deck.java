package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a deck of 8 cards
 */
public class Deck {
    private static final int MAX_CARDS = 8;
    private final List<Card> cards;
    
    public Deck() {
        this.cards = new ArrayList<>();
    }
    
    /**
     * Adds a card to the deck if there's space
     * @return true if card was added, false if deck is full
     */
    public boolean addCard(Card card) {
        if (isFull() || cards.contains(card)) {
            return false;
        }
        cards.add(card);
        return true;
    }
    
    /**
     * Removes a card from the deck
     * @return true if card was removed, false if card wasn't in deck
     */
    public boolean removeCard(Card card) {
        return cards.remove(card);
    }
    
    /**
     * Checks if the deck is full (8 cards)
     */
    public boolean isFull() {
        return cards.size() >= MAX_CARDS;
    }
    
    /**
     * Checks if a card is in the deck
     */
    public boolean contains(Card card) {
        return cards.contains(card);
    }
    
    /**
     * Returns a copy of the cards list
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }
    
    /**
     * Returns the number of cards in the deck
     */
    public int size() {
        return cards.size();
    }
    
    /**
     * Clears all cards from the deck
     */
    public void clear() {
        cards.clear();
    }
    
    /**
     * Returns the average elixir cost of the deck
     */
    public double getAverageElixirCost() {
        if (cards.isEmpty()) return 0;
        return cards.stream()
                .mapToInt(Card::getCost)
                .average()
                .orElse(0);
    }
}

