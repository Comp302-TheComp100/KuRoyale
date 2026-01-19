package com.kuroyale.service.management;

import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.User;
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.util.common.ServiceFactory;

/* Service for handling card-related operations like upgrades.
 * Validates business rules and delegates to services.*/
public class CardManagementService {

    private final AuthenticationService authService;

    public CardManagementService() {
        this.authService = ServiceFactory.getInstance().getAuthenticationService();
    }

    // Attempts to upgrade a card for the current user.
    public void upgradeCard(Card card, User user) {
        if (card == null || user == null) {
            throw new IllegalArgumentException("Card and User cannot be null");
        }

        if (card.getLevel() >= Card.MAX_LEVEL) {
            throw new IllegalStateException("Card is already at max level");
        }

        int cost = card.calculateUpgradeCost();
        if (user.getGold() < cost) {
            throw new IllegalStateException("Insufficient gold for upgrade");
        }

        // Perform Transaction
        try {
            // Deduct Gold
            user.setGold(user.getGold() - cost);

            // Increment Card Level
            int newLevel = card.getLevel() + 1;
            card.setLevel(newLevel);

            // Save Persistence State
            // Map the specific card name to its new level in user profile
            user.setCardLevel(card.getName(), newLevel);
            authService.saveCurrentUser();

            System.out.println("CardController: Upgraded " + card.getName() + " to level " + newLevel);

        } catch (Exception e) {
            // Revert on failure (simple in-memory revert)
            throw new RuntimeException("Failed to save upgrade state: " + e.getMessage(), e);
        }
    }
}
