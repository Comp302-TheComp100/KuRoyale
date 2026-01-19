package com.kuroyale.model.logic;

import java.util.List;

import com.kuroyale.model.core.entities.*;
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.service.management.GameStartValidator;
import com.kuroyale.util.common.ServiceFactory;

/*The Model component for the Main Menu.
 * Encapsulates the application logic and state related to the main menu operations.*/
public class MenuModel {

    // Dependencies (using ServiceFactory to match the existing code structure)
    private final AuthenticationService authService;
    private final GameStartValidator gameValidator;

    public MenuModel() {
        // Initialize dependencies via the ServiceFactory
        this.authService = ServiceFactory.getInstance().getAuthenticationService();
        this.gameValidator = new GameStartValidator();
    }

    // Checks if a user is logged in and if their current game state (e.g., deck) is
    // valid to start a match.
    public List<String> validateAndPrepareMatchStart() {
        User currentUser = authService.getCurrentUser();

        if (currentUser == null) {
            return List.of("You must be logged in to start a match.");
        }

        // Delegate the complex domain validation to the GameStartValidator
        List<String> validationErrors = gameValidator.validateGameStart(currentUser);

        return validationErrors;
    }

    public boolean hasSavedGames() {
        return false;
    }
}
