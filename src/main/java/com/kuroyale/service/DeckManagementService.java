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

/*Service for managing deck operations
 * Controller: coordinates deck management operations
 * High Cohesion: focused solely on deck management concerns*/
public class DeckManagementService {
    
    private final UserRepository userRepository;
    private final CardCatalog cardCatalog;
    
    //Creates a DeckManagementService: Low Coupling
    public DeckManagementService(UserRepository userRepository, CardCatalog cardCatalog) {
        this.userRepository = userRepository;
        this.cardCatalog = cardCatalog;
    }
    
    //Adds a card to a deck. Delegates to Deck domain object (Information Expert)
    public boolean addCardToDeck(Deck deck, Card card) {
        return deck.addCard(card);
    }
    
    //Removes a card from a dec.Delegates to Deck domain object (Information Expert)
    public boolean removeCardFromDeck(Deck deck, Card card) {
        return deck.removeCard(card);
    }
    
    //Replaces a card in the deck with another card. Delegates to Deck domain object (Information Expert)
    public boolean replaceCardInDeck(Deck deck, Card oldCard, Card newCard) {
        return deck.replaceCard(oldCard, newCard);
    }
    
    /*Saves a user's deck to persistent storage
     * Controller pattern: coordinates between Deck and User*/
    public void saveDeck(User user, Deck deck) throws IOException {
        // Get card names from deck (Information Expert)
        List<String> cardNames = deck.getCardNames();
        
        // Update user's deck (Information Expert - User manages its own deck)
        user.updateDeck(cardNames);
        
        // Persist to repository
        userRepository.save(user);
    }
    
    // Loads a user's saved deck / Creator pattern - creates Deck with initialization data from User
    public Deck loadUserDeck(User user) {
        Deck deck = new Deck();
        
        if (user.getDeck() == null || user.getDeck().isEmpty()) {
            return deck; // Return empty deck
        }
        
        // Load each card by name from catalog
        for (String cardName : user.getDeck()) {
            if (cardName != null && !cardName.isEmpty()) {
                int level = user.getCardLevel(cardName);
                Card card = cardCatalog.createCardWithLevel(cardName, level);
                if (card != null) {
                    deck.addCard(card);
                }
            }
        }
        
        return deck;
    }
    
    //Loads a deck with specific slot positions preserved
    public Map<Integer, Card> loadUserDeckWithPositions(User user) {
        Map<Integer, Card> deckMap = new HashMap<>();
        
        if (user.getDeck() == null || user.getDeck().isEmpty()) {
            return deckMap;
        }
        
        List<String> savedDeck = user.getDeck();
        for (int i = 0; i < savedDeck.size(); i++) {
            String cardName = savedDeck.get(i);
            if (cardName != null && !cardName.isEmpty()) {
                int level = user.getCardLevel(cardName);
                Card card = cardCatalog.createCardWithLevel(cardName, level);
                if (card != null) {
                    deckMap.put(i, card);
                }
            }
        }
        
        return deckMap;
    }
    
    //Saves a deck with specific slot positions & Preserves empty slots as empty strings
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
    
    //Validates if a deck is complete and ready for play
    public boolean isDeckValid(Deck deck) {
        return deck.isValid();
    }
    
    //Gets the average elixir cost of a deck
    public double getDeckAverageElixirCost(Deck deck) {
        return deck.getAverageElixirCost();
    }
}