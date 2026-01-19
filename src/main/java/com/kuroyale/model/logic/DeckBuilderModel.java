package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.DeckManagementService;
import com.kuroyale.util.ServiceFactory;

/*The Model component for the Deck Builder screen.
 * Encapsulates business logic for deck operations and card management.*/
public class DeckBuilderModel {

    private final AuthenticationService authService;
    private final DeckManagementService deckService;
    private final com.kuroyale.model.entities.CardCatalog cardCatalog;

    public DeckBuilderModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.deckService = factory.getDeckManagementService();
        this.cardCatalog = factory.getCardCatalog();
    }

    // Gets the current logged-in user
    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    // Loads the user's saved deck with slot positions preserved
    public Map<Integer, Card> loadUserDeckWithPositions() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return java.util.Collections.emptyMap();
        }
        return deckService.loadUserDeckWithPositions(currentUser);
    }

    // Saves a deck with specific slot positions
    public void saveDeckWithPositions(Map<Integer, Card> slotCards) throws IOException {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return; // No user logged in
        }
        deckService.saveDeckWithPositions(currentUser, slotCards);
    }

    // Gets all available cards from the catalog
    public List<Card> getAllCards() {
        return cardCatalog.getAllCards();
    }

    // Gets the average elixir cost of a deck
    public double getDeckAverageElixirCost(Deck deck) {
        return deckService.getDeckAverageElixirCost(deck);
    }

    // Replaces a card in the deck with another card
    public boolean replaceCardInDeck(Deck deck, Card oldCard, Card newCard) {
        return deckService.replaceCardInDeck(deck, oldCard, newCard);
    }
}
