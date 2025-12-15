package com.kuroyale.controller;

import com.kuroyale.model.SavedGameState;
import com.kuroyale.model.SavedGamesModel; // Import the new Model
import com.kuroyale.util.SceneLoader; // Assuming SceneLoader is available
import com.kuroyale.util.SoundEffectUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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

    // Dependencies (Service classes removed from fields, now used only by the Model)

    public SavedGamesController() {
        // Dependencies are now handled within the Model, simplifying the Controller constructor.
    }

    @FXML
    private void initialize() {
        // Apply styles
        root.getStyleClass().add("main-menu-background");

        // Load saved games
        loadSavedGames();
    }

    // --- Data Loading (Delegated to Model) ---

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

    // --- UI Construction (View/Controller concern) ---

    private HBox createSavedGameEntry(SavedGameState savedGame) {
        HBox entry = new HBox(20);
        entry.setAlignment(Pos.CENTER_LEFT);

        // --- Styles (CSS should ideally handle most of this) ---
        // For dynamic elements, inline styles are often necessary, but grouped here for clarity.
        String baseStyle = "-fx-background-color: rgba(50, 50, 50, 0.9); " +
                "-fx-padding: 20; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #888; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10;";
        String hoverStyle = "-fx-background-color: rgba(70, 70, 70, 0.9); " +
                "-fx-padding: 20; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: #4CAF50; " + // Highlight color on hover
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10;";

        entry.setStyle(baseStyle);
        entry.setPrefHeight(120);

        // ... (InfoBox and ButtonBox creation remain the same) ...
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label dateLabel = new Label("Saved: " + savedGame.getFormattedSaveTime());
        dateLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #aaaaaa;");

        Label playerLabel = new Label("Player: " + savedGame.getPlayerUsername());
        playerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label timeLabel = new Label("Time Remaining: " + savedGame.getFormattedTimeRemaining());
        timeLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50;");

        Label scoreLabel = new Label(String.format("Score: %d - %d", savedGame.getPlayerScore(), savedGame.getBotScore()));
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        Label elixirLabel = new Label(String.format("Elixir: %.1f/10", savedGame.getPlayerElixir()));
        elixirLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #bb86fc;");

        Label unitsLabel = new Label(String.format("Units: %d troops, %d buildings", savedGame.getActiveTroops().size(), savedGame.getActiveBuildings().size()));
        unitsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffab40;");

        infoBox.getChildren().addAll(dateLabel, playerLabel, timeLabel, scoreLabel, elixirLabel, unitsLabel);

        // Right side - Action buttons
        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMinWidth(200);

        Button loadButton = new Button("LOAD GAME");
        loadButton.setStyle("-fx-font-size: 16px; -fx-padding: 10 30; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        loadButton.setPrefWidth(180);
        loadButton.setOnAction(e -> handleLoadGame(savedGame));

        Button deleteButton = new Button("🗑️ DELETE");
        // Delete button styles and hover effect logic remain the same
        String deleteBaseStyle = "-fx-font-size: 14px; -fx-padding: 8 30; -fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;";
        String deleteHoverStyle = "-fx-font-size: 14px; -fx-padding: 8 30; -fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;";

        deleteButton.setStyle(deleteBaseStyle);
        deleteButton.setPrefWidth(180);
        deleteButton.setOnAction(e -> handleDeleteGame(savedGame));

        deleteButton.setOnMouseEntered(event -> deleteButton.setStyle(deleteHoverStyle));
        deleteButton.setOnMouseExited(event -> deleteButton.setStyle(deleteBaseStyle));

        buttonBox.getChildren().addAll(loadButton, deleteButton);
        entry.getChildren().addAll(infoBox, buttonBox);

        // Hover effect for the entire entry
        entry.setOnMouseEntered(e -> entry.setStyle(hoverStyle));
        entry.setOnMouseExited(e -> entry.setStyle(baseStyle));

        return entry;
    }

    // --- Action Handlers (Controller logic) ---

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
        // Show confirmation dialog (UI concern)
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Saved Game");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will permanently delete the saved game.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // MVC: Delegate deletion logic to the Model
                boolean success = model.deleteGame(savedGame.getSaveId());

                if (success) {
                    // Success (Controller updates the View)
                    loadSavedGames();
                } else {
                    // Failure (Controller shows the View error)
                    showError("Failed to delete saved game");
                }
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

    // --- Utility Methods (Controller/View concern) ---

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}