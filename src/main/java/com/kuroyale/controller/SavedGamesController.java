package com.kuroyale.controller;

import com.kuroyale.model.SavedGameState;
import com.kuroyale.model.User;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.GameSaveService;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Controller for the Saved Games screen
 */
public class SavedGamesController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label infoLabel;
    @FXML
    private ScrollPane savedGamesScrollPane;
    @FXML
    private VBox savedGamesContainer;

    private final GameSaveService gameSaveService;
    private final AuthenticationService authService;

    public SavedGamesController() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.gameSaveService = factory.getGameSaveService();
        this.authService = factory.getAuthenticationService();
    }

    @FXML
    private void initialize() {
        // Apply styles
        root.getStyleClass().add("main-menu-background");
        
        // Load saved games
        loadSavedGames();
    }

    private void loadSavedGames() {
        savedGamesContainer.getChildren().clear();
        
        // Get current user
        User currentUser = authService.getCurrentUser();
        
        // Load saved games (filter by current user if logged in)
        List<SavedGameState> savedGames;
        if (currentUser != null) {
            savedGames = gameSaveService.loadSavedGamesForPlayer(currentUser.getUsername());
        } else {
            savedGames = gameSaveService.loadAllSavedGames();
        }
        
        if (savedGames.isEmpty()) {
            Label noGamesLabel = new Label("No saved games found");
            noGamesLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");
            savedGamesContainer.getChildren().add(noGamesLabel);
            return;
        }
        
        // Display each saved game
        for (SavedGameState savedGame : savedGames) {
            savedGamesContainer.getChildren().add(createSavedGameEntry(savedGame));
        }
    }

    private HBox createSavedGameEntry(SavedGameState savedGame) {
        HBox entry = new HBox(20);
        entry.setAlignment(Pos.CENTER_LEFT);
        entry.setStyle("-fx-background-color: rgba(50, 50, 50, 0.9); " +
                      "-fx-padding: 20; " +
                      "-fx-background-radius: 10; " +
                      "-fx-border-color: #888; " +
                      "-fx-border-width: 2; " +
                      "-fx-border-radius: 10;");
        entry.setPrefHeight(120);

        // Left side - Game info
        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label dateLabel = new Label("Saved: " + savedGame.getFormattedSaveTime());
        dateLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #aaaaaa;");

        Label playerLabel = new Label("Player: " + savedGame.getPlayerUsername());
        playerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label timeLabel = new Label("Time Remaining: " + savedGame.getFormattedTimeRemaining());
        timeLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50;");

        Label scoreLabel = new Label(String.format("Score: %d - %d", 
                                                   savedGame.getPlayerScore(), 
                                                   savedGame.getBotScore()));
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        Label elixirLabel = new Label(String.format("Elixir: %.1f/10", savedGame.getPlayerElixir()));
        elixirLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #bb86fc;");
        
        // Add troops/buildings info
        Label unitsLabel = new Label(String.format("Units: %d troops, %d buildings", 
                                                    savedGame.getActiveTroops().size(),
                                                    savedGame.getActiveBuildings().size()));
        unitsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffab40;");

        infoBox.getChildren().addAll(dateLabel, playerLabel, timeLabel, scoreLabel, elixirLabel, unitsLabel);

        // Right side - Action buttons
        VBox buttonBox = new VBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setMinWidth(200);

        Button loadButton = new Button("LOAD GAME");
        loadButton.setStyle("-fx-font-size: 16px; -fx-padding: 10 30; " +
                           "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        loadButton.setPrefWidth(180);
        loadButton.setOnAction(e -> handleLoadGame(savedGame));

        Button deleteButton = new Button("🗑️ DELETE");
        deleteButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 30; " +
                             "-fx-background-color: #d32f2f; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;");
        deleteButton.setPrefWidth(180);
        deleteButton.setOnAction(e -> handleDeleteGame(savedGame));
        
        // Add hover effect for delete button
        deleteButton.setOnMouseEntered(event -> {
            deleteButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 30; " +
                                 "-fx-background-color: #f44336; -fx-text-fill: white; " +
                                 "-fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;");
        });
        deleteButton.setOnMouseExited(event -> {
            deleteButton.setStyle("-fx-font-size: 14px; -fx-padding: 8 30; " +
                                 "-fx-background-color: #d32f2f; -fx-text-fill: white; " +
                                 "-fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;");
        });

        buttonBox.getChildren().addAll(loadButton, deleteButton);

        entry.getChildren().addAll(infoBox, buttonBox);

        // Add hover effect
        entry.setOnMouseEntered(e -> {
            entry.setStyle("-fx-background-color: rgba(70, 70, 70, 0.9); " +
                          "-fx-padding: 20; " +
                          "-fx-background-radius: 10; " +
                          "-fx-border-color: #4CAF50; " +
                          "-fx-border-width: 2; " +
                          "-fx-border-radius: 10;");
        });
        entry.setOnMouseExited(e -> {
            entry.setStyle("-fx-background-color: rgba(50, 50, 50, 0.9); " +
                          "-fx-padding: 20; " +
                          "-fx-background-radius: 10; " +
                          "-fx-border-color: #888; " +
                          "-fx-border-width: 2; " +
                          "-fx-border-radius: 10;");
        });

        return entry;
    }

    private void handleLoadGame(SavedGameState savedGame) {
        SoundEffectUtil.playButtonClick();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/battle.fxml"));
            Parent root = loader.load();
            
            // Get the controller and set the saved game BEFORE starting
            BattleController battleController = loader.getController();
            battleController.setLoadedSavedGame(savedGame);
            
            // NOW start the game with the saved state
            battleController.startGame();

            Stage stage = (Stage) savedGamesContainer.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Battle (Resumed)");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load battle: " + e.getMessage());
        }
    }

    private void handleDeleteGame(SavedGameState savedGame) {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Saved Game");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("This will permanently delete the saved game.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = gameSaveService.deleteSavedGame(savedGame.getSaveId());
                if (success) {
                    // Reload the list
                    loadSavedGames();
                } else {
                    showError("Failed to delete saved game");
                }
            }
        });
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) savedGamesContainer.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Main Menu");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main menu: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}

