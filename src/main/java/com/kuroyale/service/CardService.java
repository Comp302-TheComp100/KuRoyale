package com.kuroyale.service;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.User;
import com.kuroyale.util.ServiceFactory;

/**
 * Service for handling card-related operations like upgrades.
 * Validates business rules and delegates to services.
 */
public class CardService {

    private final AuthenticationService authService;

    public CardService() {
        this.authService = ServiceFactory.getInstance().getAuthenticationService();
    }

    /**
     * Attempts to upgrade a card for the current user.
     * 
     * @param card The card to upgrade.
     * @param user The user performing the upgrade.
     * @throws IllegalStateException    If validation fails (insufficient gold, max
     *                                  level).
     * @throws IllegalArgumentException If parameters are null.
     */
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
            // In a real DB, transaction rollback would handle this.
            // Here we just re-throw to let UI know.
            throw new RuntimeException("Failed to save upgrade state: " + e.getMessage(), e);
        }
    }
}
