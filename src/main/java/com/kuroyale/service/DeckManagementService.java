package com.kuroyale.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kuroyale.model.Card;
import com.kuroyale.model.Deck;
import com.kuroyale.model.User;
import com.kuroyale.repository.UserRepository;

/**
 * Service for managing deck operations
 * Follows Controller GRASP pattern - coordinates deck management operations
 * Follows High Cohesion - focused solely on deck management concerns
 * Follows Indirection - mediates between controllers and domain objects
 */
public class DeckManagementService {
    
    private final UserRepository userRepository;
    private final CardCatalog cardCatalog;
    
    /**
     * Creates a DeckManagementService
     * Follows Low Coupling via Dependency Injection
     * @param userRepository The repository for user persistence
     * @param cardCatalog The catalog of available cards
     */
    public DeckManagementService(UserRepository userRepository, CardCatalog cardCatalog) {
        this.userRepository = userRepository;
        this.cardCatalog = cardCatalog;
    }
    
    /**
     * Adds a card to a deck
     * Delegates to Deck domain object (Information Expert)
     * @param deck The deck to add to
     * @param card The card to add
     * @return true if card was added, false if deck is full or card already in deck
     */
    public boolean addCardToDeck(Deck deck, Card card) {
        return deck.addCard(card);
    }
    
    /**
     * Removes a card from a deck
     * Delegates to Deck domain object (Information Expert)
     * @param deck The deck to remove from
     * @param card The card to remove
     * @return true if card was removed, false if card wasn't in deck
     */
    public boolean removeCardFromDeck(Deck deck, Card card) {
        return deck.removeCard(card);
    }
    
    /**
     * Replaces a card in the deck with another card
     * Delegates to Deck domain object (Information Expert)
     * @param deck The deck to modify
     * @param oldCard The card to replace
     * @param newCard The card to add
     * @return true if replacement was successful
     */
    public boolean replaceCardInDeck(Deck deck, Card oldCard, Card newCard) {
        return deck.replaceCard(oldCard, newCard);
    }
    
    /**
     * Saves a user's deck to persistent storage
     * Follows Controller pattern - coordinates between Deck and User
     * @param user The user whose deck to save
     * @param deck The deck to save
     * @throws IOException If there's an error saving
     */
    public void saveDeck(User user, Deck deck) throws IOException {
        // Get card names from deck (Information Expert)
        List<String> cardNames = deck.getCardNames();
        
        // Update user's deck (Information Expert - User manages its own deck)
        user.updateDeck(cardNames);
        
        // Persist to repository
        userRepository.save(user);
    }
    
    /**
     * Loads a user's saved deck
     * Follows Creator pattern - creates Deck with initialization data from User
     * @param user The user whose deck to load
     * @return A Deck populated with the user's saved cards
     */
    public Deck loadUserDeck(User user) {
        Deck deck = new Deck();
        
        if (user.getDeck() == null || user.getDeck().isEmpty()) {
            return deck; // Return empty deck
        }
        
        // Load each card by name from catalog
        for (String cardName : user.getDeck()) {
            if (cardName != null && !cardName.isEmpty()) {
                Card card = cardCatalog.getCardByName(cardName);
                if (card != null) {
                    deck.addCard(card);
                }
            }
        }
        
        return deck;
    }
    
    /**
     * Loads a deck with specific slot positions preserved
     * Used when deck positions matter (e.g., UI deck slots)
     * @param user The user whose deck to load
     * @return Map of slot index to Card (for positioned loading)
     */
    public Map<Integer, Card> loadUserDeckWithPositions(User user) {
        Map<Integer, Card> deckMap = new HashMap<>();
        
        if (user.getDeck() == null || user.getDeck().isEmpty()) {
            return deckMap;
        }
        
        List<String> savedDeck = user.getDeck();
        for (int i = 0; i < savedDeck.size(); i++) {
            String cardName = savedDeck.get(i);
            if (cardName != null && !cardName.isEmpty()) {
                Card card = cardCatalog.getCardByName(cardName);
                if (card != null) {
                    deckMap.put(i, card);
                }
            }
        }
        
        return deckMap;
    }
    
    /**
     * Saves a deck with specific slot positions
     * Preserves empty slots as empty strings
     * @param user The user whose deck to save
     * @param slotCards Map of slot index to Card
     * @throws IOException If there's an error saving
     */
    public void saveDeckWithPositions(User user, Map<Integer, Card> slotCards) throws IOException {
        List<String> cardNames = new ArrayList<>();
        
        // Find max slot index to determine deck size
        int maxSlot = slotCards.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
        
        // Create list with empty strings for empty slots
        for (int i = 0; i <= maxSlot; i++) {
            Card card = slotCards.get(i);
            if (card != null) {
                cardNames.add(card.getName());
            } else {
                cardNames.add(""); // Empty slot
            }
        }
        
        // Update user's deck
        user.updateDeck(cardNames);
        
        // Persist to repository
        userRepository.save(user);
    }
    
    /**
     * Validates if a deck is complete and ready for play
     * @param deck The deck to validate
     * @return true if deck has exactly 8 cards
     */
    public boolean isDeckValid(Deck deck) {
        return deck.isValid();
    }
    
    /**
     * Gets the average elixir cost of a deck
     * @param deck The deck to analyze
     * @return Average elixir cost
     */
    public double getDeckAverageElixirCost(Deck deck) {
        return deck.getAverageElixirCost();
    }
}

