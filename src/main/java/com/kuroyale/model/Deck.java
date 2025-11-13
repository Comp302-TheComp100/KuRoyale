package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages a deck of 8 cards
 * Follows Information Expert GRASP pattern - Deck knows about its own cards
 * and has responsibility for operations on the deck
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
    
    // Information Expert: Deck knows if it's valid
    /**
     * Checks if the deck is valid (has exactly 8 cards)
     * @return true if deck has 8 cards, false otherwise
     */
    public boolean isValid() {
        return cards.size() == MAX_CARDS;
    }
    
    // Information Expert: Deck handles its own card replacement logic
    /**
     * Replaces an old card with a new card in the deck, maintaining position
     * @param oldCard The card to replace
     * @param newCard The new card to add
     * @return true if replacement was successful, false if oldCard wasn't in deck
     */
    public boolean replaceCard(Card oldCard, Card newCard) {
        int index = cards.indexOf(oldCard);
        if (index == -1) {
            return false;
        }
        cards.set(index, newCard);
        return true;
    }
    
    // Information Expert: Deck can provide its card names
    /**
     * Returns a list of card names in the deck
     * Empty strings are used for positions that should remain empty
     * @return List of card names (or empty strings for empty positions)
     */
    public List<String> getCardNames() {
        return cards.stream()
                .map(Card::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the maximum number of cards allowed in a deck
     * @return Maximum deck size
     */
    public static int getMaxCards() {
        return MAX_CARDS;
    }
}

