package com.kuroyale.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kuroyale.model.Card;
import com.kuroyale.model.CardFactory;

/**
 * Service that manages the catalog of available cards in the game
 * Follows Information Expert GRASP pattern - CardCatalog knows about all available cards
 * Follows Indirection GRASP pattern - Provides interface between controllers and CardFactory
 * Acts as a facade to card creation logic
 */
public class CardCatalog {
    
    private final CardFactory cardFactory;
    private final Map<String, Card> cardsByName;
    private final List<Card> allCards;
    
    /**
     * Creates a new CardCatalog
     * Follows Creator pattern - CardCatalog aggregates Card references
     */
    public CardCatalog() {
        this.cardFactory = new CardFactory();
        this.allCards = cardFactory.getAllCards();
        this.cardsByName = new HashMap<>();
        
        // Build lookup map for quick card retrieval by name
        for (Card card : allCards) {
            cardsByName.put(card.getName(), card);
        }
    }
    
    /**
     * Gets all available cards in the game
     * @return List of all 28 cards
     */
    public List<Card> getAllCards() {
        return allCards;
    }
    
    /**
     * Finds a card by its name
     * Information Expert - CardCatalog knows all available cards
     * @param name The card name to search for
     * @return The Card if found, null otherwise
     */
    public Card getCardByName(String name) {
        return cardsByName.get(name);
    }
    
    /**
     * Checks if a card with the given name exists
     * @param name The card name to check
     * @return true if card exists, false otherwise
     */
    public boolean cardExists(String name) {
        return cardsByName.containsKey(name);
    }
    
    /**
     * Gets the total number of available cards
     * @return Number of cards in catalog
     */
    public int getCardCount() {
        return allCards.size();
    }
}


