package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.User;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.GameStartValidator;
import com.kuroyale.util.AudioManager;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/* Controller for the main menu screen
 *  Controller GRASP pattern - thin controller focused on UI concerns
 *  Low Coupling - uses services via dependency injection*/
public class MainMenuController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private Button deckBuilderButton;
    @FXML
    private Button startMatchButton;
    @FXML
    private Button arenaDesignButton;
    @FXML
    private Button settingsButton;

    // Static MediaPlayer for main menu music to persist across scene changes
    private static MediaPlayer mainMenuMusicPlayer;

    @FXML
    private void initialize() {
        initializeStyles();
        playMainMenuMusic();
    }

    // Initialize styles after FXML is loaded
    private void initializeStyles() {
        // Apply CSS classes
        root.getStyleClass().add("main-menu-background");

        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }
        deckBuilderButton.getStyleClass().add("menu-button");
        startMatchButton.getStyleClass().add("menu-button");
        arenaDesignButton.getStyleClass().add("menu-button");
        settingsButton.getStyleClass().add("menu-button");

        // Add programmatic hover effects for scale transforms
        addMenuButtonHoverEffects(deckBuilderButton);
        addMenuButtonHoverEffects(startMatchButton);
        addMenuButtonHoverEffects(arenaDesignButton);
        addMenuButtonHoverEffects(settingsButton);
    }

    // Add programmatic hover effects for menu buttons (scale transforms)
    private void addMenuButtonHoverEffects(Button button) {
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        button.setOnMousePressed(e -> {
            button.setTranslateY(2);
        });
        button.setOnMouseReleased(e -> {
            button.setTranslateY(0);
        });
    }

    @FXML
    private void handleDeckBuilder() {
        SoundEffectUtil.playButtonClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/deck-builder.fxml"));
            Parent root = loader.load();

            // Styles are initialized in DeckBuilderController's initialize() method
            Stage stage = (Stage) deckBuilderButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            // Load stylesheet for new scene
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Deck Builder");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Deck Builder: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartMatch() {
        SoundEffectUtil.playButtonClick();

        // Get current user and validate game start conditions
        AuthenticationService authService = ServiceFactory.getInstance().getAuthenticationService();
        User currentUser = authService.getCurrentUser();

        if (currentUser == null) {
            showError("You must be logged in to start a match.");
            return;
        }

        // Validate game start conditions
        GameStartValidator validator = new GameStartValidator();
        List<String> validationErrors = validator.validateGameStart(currentUser);

        if (!validationErrors.isEmpty()) {
            // Build error message from all validation errors
            StringBuilder errorMessage = new StringBuilder("Cannot start match. Please fix the following issues:\n\n");
            for (String error : validationErrors) {
                errorMessage.append("• ").append(error).append("\n");
            }
            showError(errorMessage.toString());
            return;
        }

        // All validations passed, proceed to battle
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/battle.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) startMatchButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Battle");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Battle: " + e.getMessage());
        }
    }

    @FXML
    private void handleArenaDesign() {
        SoundEffectUtil.playButtonClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/arena-design.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) arenaDesignButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Arena Design");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Arena Design: " + e.getMessage());
        }
    }

    @FXML
    private void handleSettings() {
        SoundEffectUtil.playButtonClick();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) settingsButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Settings: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Plays the main menu music in a loop
    private void playMainMenuMusic() {
        try {
            // Only create and start music if it's not already playing
            if (mainMenuMusicPlayer == null) {
                String soundPath = getClass().getResource("/musics/main_menu.mp3").toExternalForm();
                Media media = new Media(soundPath);
                mainMenuMusicPlayer = new MediaPlayer(media);
                mainMenuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                AudioManager.getInstance().registerMusicPlayer(mainMenuMusicPlayer);
                mainMenuMusicPlayer.play();
            } else {
                // If music player exists but is not playing, resume it
                MediaPlayer.Status status = mainMenuMusicPlayer.getStatus();
                if (status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.PAUSED) {
                    mainMenuMusicPlayer.play();
                }
            }
        } catch (Exception e) {
            // Silently fail if sound cannot be played
            e.printStackTrace();
        }
    }
}