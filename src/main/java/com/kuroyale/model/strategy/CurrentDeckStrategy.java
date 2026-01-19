package com.kuroyale.model.strategy;

import java.util.List;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.CardCatalog;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.entities.User;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.ServiceFactory;

/**
 * Strategy Pattern: Loads the current user's saved deck.
 * GRASP: Information Expert - delegates to User who knows their deck.
 */
public class CurrentDeckStrategy implements DeckBuildingStrategy {

    private final AuthenticationService authService;
    private final CardCatalog cardCatalog;

    public CurrentDeckStrategy() {
        this.authService = ServiceFactory.getInstance().getAuthenticationService();
        this.cardCatalog = ServiceFactory.getInstance().getCardCatalog();
    }

    @Override
    public Deck buildDeck() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            // Fallback to random if no user
            return new RandomDeckStrategy().buildDeck();
        }

        List<String> cardNames = currentUser.getDeck();
        Deck deck = new Deck();

        for (String cardName : cardNames) {
            if (cardName != null && !cardName.isEmpty()) {
                int level = currentUser.getCardLevel(cardName);
                Card card = cardCatalog.createCardWithLevel(cardName, level);
                if (card != null) {
                    deck.addCard(card);
                }
            }
        }
        return deck;
    }

    @Override
    public String getDisplayName() {
        return "Current Deck";
    }

    @Override
    public boolean requiresUserInput() {
        return false;
    }
}
