package com.kuroyale.model.logic;

import com.kuroyale.model.dto.*;
import com.kuroyale.model.entities.*;
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.service.game.GameSaveService;
import com.kuroyale.util.common.ServiceFactory;

import java.util.List;

/*The Model component for the Saved Games screen.
 * Encapsulates logic for fetching and managing saved game states.*/
public class SavedGamesModel {

    private final GameSaveService gameSaveService;
    private final AuthenticationService authService;

    public SavedGamesModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.gameSaveService = factory.getGameSaveService();
        this.authService = factory.getAuthenticationService();
    }

    // Loads saved games, filtered by the current logged-in user if available.
    public List<SavedGameState> loadSavedGamesForDisplay() {
        User currentUser = authService.getCurrentUser();

        if (currentUser != null) {
            return gameSaveService.loadSavedGamesForPlayer(currentUser.getUsername());
        } else {
            return gameSaveService.loadAllSavedGames();
        }
    }

    // Attempts to delete a saved game by its ID.
    public boolean deleteGame(String saveId) {
        return gameSaveService.deleteSavedGame(saveId);
    }
}
