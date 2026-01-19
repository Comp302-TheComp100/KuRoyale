package com.kuroyale.service.management;

import java.util.ArrayList;
import java.util.List;

import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.User;

/*Service for validating game start conditions.
 * High Cohesion - focused solely on validation logic.
 * Information Expert - knows the rules for valid game configurations.*/
public class GameStartValidator {

    /*
     * Validates all game start conditions and returns a list of error messages.
     * If the list is empty, all validations passed.
     */
    public List<String> validateGameStart(User user) {
        List<String> errors = new ArrayList<>();

        // Validate deck
        if (!validateDeck(user)) {
            errors.add("Your deck must have exactly 8 cards. Please build your deck first.");
        }

        // Validate arena layout
        if (!user.hasArenaLayout()) {
            errors.add("You must design an arena first. Please create an arena layout.");
        } else {
            ArenaLayout layout = user.getArenaLayout();

            // Validate tower counts
            String towerError = validateTowers(layout);
            if (towerError != null) {
                errors.add(towerError);
            }

            // Validate bridge count
            String bridgeError = validateBridges(layout);
            if (bridgeError != null) {
                errors.add(bridgeError);
            }
        }

        return errors;
    }

    // Validates that the user has a complete deck of 8 cards.
    private boolean validateDeck(User user) {
        if (user == null || user.getDeck() == null) {
            return false;
        }

        // Count non-empty cards
        long cardCount = user.getDeck().stream().filter(card -> card != null && !card.trim().isEmpty()).count();
        return cardCount == 8;
    }

    // Validates that the arena has exactly 2 princess towers and 1 king tower.
    private String validateTowers(ArenaLayout layout) {
        if (layout == null) {
            return "Arena layout is missing.";
        }

        // Check princess towers
        int princessTowerCount = layout.getPrincessTowerPositions() != null ? layout.getPrincessTowerPositions().size()
                : 0;

        if (princessTowerCount != 2) {
            return String.format("Arena must have exactly 2 Princess Towers (found %d). Please redesign your arena.",
                    princessTowerCount);
        }

        // Check king tower
        if (layout.getKingTowerPosition() == null) {
            return "Arena must have exactly 1 King Tower (found 0). Please redesign your arena.";
        }

        return null; // All tower validations passed
    }

    // Validates that the arena has 1, 2 or 3 bridges.
    private String validateBridges(ArenaLayout layout) {
        if (layout == null) {
            return "Arena layout is missing.";
        }

        int bridgeTileCount = layout.getBridgePositions() != null ? layout.getBridgePositions().size() : 0;

        // Each bridge consists of 4 tiles (2x2)
        int bridgeCount = bridgeTileCount / 4;

        if (bridgeCount < 1 || bridgeCount > 3) {
            return String.format("Arena must have at least 1 bridge (found %d). Please redesign your arena.",
                    bridgeCount);
        }

        return null; // Bridge validation passed
    }
}
