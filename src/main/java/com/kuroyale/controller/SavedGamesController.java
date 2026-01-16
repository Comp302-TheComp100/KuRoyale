package com.kuroyale.controller;

import com.kuroyale.model.dto.SavedGameState;
import com.kuroyale.model.logic.SavedGamesModel; // Import the new Model
import com.kuroyale.util.SceneLoader; // Assuming SceneLoader is available
import com.kuroyale.util.SoundEffectUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;

// Controller for the Saved Games screen. Implements MVC pattern.
public class SavedGamesController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label infoLabel;
    @FXML
    private ScrollPane savedGamesScrollPane;
    @FXML
    private VBox savedGamesContainer;

    // 1. MVC: Instantiate the Model
    private final SavedGamesModel model = new SavedGamesModel();
    // 2. Scene Management: Instantiate the SceneLoader
    private final SceneLoader sceneLoader = new SceneLoader();

    // Dependencies (Service classes removed from fields, now used only by the
    // Model)

    public SavedGamesController() {
    }

    @FXML
    private void initialize() {
        // Apply styles
        root.getStyleClass().add("main-menu-background");

        // Load saved games
        loadSavedGames();
    }

    // Data Loading (Delegated to Model)

    private void loadSavedGames() {
        savedGamesContainer.getChildren().clear();

        // 1. MVC: Get data from the Model
        List<SavedGameState> savedGames = model.loadSavedGamesForDisplay();

        if (savedGames.isEmpty()) {
            // View construction for 'No Games' message
            Label noGamesLabel = new Label("No saved games found");
            noGamesLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
            savedGamesContainer.getChildren().add(noGamesLabel);
            return;
        }

        // 2. Controller/View: Build UI based on Model data
        for (SavedGameState savedGame : savedGames) {
            savedGamesContainer.getChildren().add(createSavedGameEntry(savedGame));
        }
    }

    // UI Construction (View/Controller concern)

    private javafx.scene.layout.HBox createSavedGameEntry(SavedGameState savedGame) {
        return new com.kuroyale.view.SavedGameEntryView(savedGame,
                new com.kuroyale.view.SavedGameEntryView.SavedGameListener() {
                    @Override
                    public void onLoad() {
                        handleLoadGame(savedGame);
                    }

                    @Override
                    public void onDelete() {
                        handleDeleteGame(savedGame);
                    }
                });
    }

    // Action Handlers (Controller logic)

    private void handleLoadGame(SavedGameState savedGame) {
        SoundEffectUtil.playButtonClick();

        try {
            // Use SceneLoader utility for navigation boilerplate
            sceneLoader.load(savedGamesContainer, "/fxml/battle.fxml", "KU Royale - Battle (Resumed)", controller -> {
                // Initialize the next controller BEFORE scene switch
                if (controller instanceof BattleController battleController) {
                    battleController.setLoadedSavedGame(savedGame);
                    battleController.startGame();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load battle: " + e.getMessage());
        }
    }

    private void handleDeleteGame(SavedGameState savedGame) {
        // Show confirmation dialog using themed alert
        // For confirmation dialogs, we'll use a simple approach: just delete on click
        // since ThemedAlertController is for info/error messages
        com.kuroyale.view.ThemedAlertController.showConfirmation(
                "Delete Saved Game",
                "This will permanently delete the saved game. Are you sure?",
                () -> {
                    // MVC: Delegate deletion logic to the Model
                    boolean success = model.deleteGame(savedGame.getSaveId());

                    if (success) {
                        // Success (Controller updates the View)
                        loadSavedGames();
                    } else {
                        // Failure (Controller shows the View error)
                        showError("Failed to delete saved game");
                    }
                });
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();

        try {
            // Use SceneLoader utility for back navigation
            sceneLoader.load(savedGamesContainer, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main menu: " + e.getMessage());
        }
    }

    // Utility Methods (Controller/View concern)

    private void showError(String message) {
        com.kuroyale.view.ThemedAlertController.show("Error", message);
    }
}
