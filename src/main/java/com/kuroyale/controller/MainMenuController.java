package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.util.AudioManager;
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

/**
 * Controller for the main menu screen
 * Follows Controller GRASP pattern - thin controller focused on UI concerns
 * Follows Low Coupling - uses services via dependency injection
 */
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

    /**
     * Initialize styles after FXML is loaded
     */
    private void initializeStyles() {
        // Apply CSS classes
        root.getStyleClass().add("main-menu-background");

        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }

        // Apply button CSS classes
        deckBuilderButton.getStyleClass().add("menu-button");
        startMatchButton.getStyleClass().add("menu-button");
        arenaDesignButton.getStyleClass().add("menu-button");
        settingsButton.getStyleClass().add("menu-button");

        // Add programmatic hover effects for scale transforms (CSS can't handle this
        // easily)
        addMenuButtonHoverEffects(deckBuilderButton);
        addMenuButtonHoverEffects(startMatchButton);
        addMenuButtonHoverEffects(arenaDesignButton);
        addMenuButtonHoverEffects(settingsButton);
    }

    /**
     * Add programmatic hover effects for menu buttons (scale transforms)
     */
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Start Match");
        alert.setContentText("This feature will be available soon!");
        alert.showAndWait();
    }

    @FXML
    private void handleArenaDesign() {
        SoundEffectUtil.playButtonClick();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Arena Design");
        alert.setContentText("This feature will be available soon!");
        alert.showAndWait();
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

    /**
     * Plays the main menu music in a loop
     * Uses static player to persist across scene changes
     */
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
