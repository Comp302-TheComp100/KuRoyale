package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.MenuModel; // Import the new Model
import com.kuroyale.model.User;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.AudioManager;
import com.kuroyale.util.SceneLoader; // Import the new utility
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/* Controller for the main menu screen
 * Implements Model-View-Controller (MVC) - Controller component*/
public class MainMenuController {

    @FXML private AnchorPane root;
    @FXML private Label titleLabel;
    @FXML private Button deckBuilderButton;
    @FXML private Button startMatchButton;
    @FXML private Button resumeGameButton;
    @FXML private Button arenaDesignButton;
    @FXML private Button settingsButton;
    @FXML private HBox goldDisplay;
    @FXML private Label goldLabel;

    private static MediaPlayer mainMenuMusicPlayer;
    private final MenuModel model = new MenuModel();
    private final SceneLoader sceneLoader = new SceneLoader();
    private final AuthenticationService authService = ServiceFactory.getInstance().getAuthenticationService();

    @FXML
    private void initialize() {
        initializeStyles();
        updateGoldDisplay();
        playMainMenuMusic();
    }

    // Initialize styles after FXML is loaded
    private void initializeStyles() {
        root.getStyleClass().add("main-menu-background");

        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }
        deckBuilderButton.getStyleClass().add("menu-button");
        startMatchButton.getStyleClass().add("menu-button");
        resumeGameButton.getStyleClass().add("menu-button");
        arenaDesignButton.getStyleClass().add("menu-button");
        settingsButton.getStyleClass().add("menu-button");

        // Add programmatic hover effects for scale transforms
        addMenuButtonHoverEffects(deckBuilderButton);
        addMenuButtonHoverEffects(startMatchButton);
        addMenuButtonHoverEffects(resumeGameButton);
        addMenuButtonHoverEffects(arenaDesignButton);
        addMenuButtonHoverEffects(settingsButton);
    }

    // Add programmatic hover effects for menu buttons (scale transforms)
    private void addMenuButtonHoverEffects(Node button) {
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
        button.setOnMousePressed(e -> {button.setTranslateY(2);});
        button.setOnMouseReleased(e -> {button.setTranslateY(0);});
    }

    @FXML
    private void handleDeckBuilder() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(deckBuilderButton, "/fxml/deck-builder.fxml", "KU Royale - Deck Builder", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Deck Builder: " + e.getMessage());
        }
    }

    @FXML
    private void handleResumeGame() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(resumeGameButton, "/fxml/saved-games.fxml", "KU Royale - Saved Games", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Saved Games: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartMatch() {
        SoundEffectUtil.playButtonClick();

        // Delegate all business logic to the Model
        List<String> validationErrors = model.validateAndPrepareMatchStart();

        if (!validationErrors.isEmpty()) {
            //Controller/View: Handle the Model's error response
            StringBuilder errorMessage = new StringBuilder("Cannot start match. Please fix the following issues:\n\n");
            validationErrors.forEach(error -> errorMessage.append("• ").append(error).append("\n"));
            showError(errorMessage.toString());
            return;
        }

        //Controller/Navigation: If valid, load the next scene.
        try {
            // Use the SceneLoader with a special initializer lambda for BattleController
            sceneLoader.load(startMatchButton, "/fxml/battle.fxml", "KU Royale - Battle", controller -> {
                if (controller instanceof BattleController battleController) {
                    battleController.startGame();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Battle: " + e.getMessage());
        }
    }

    @FXML
    private void handleArenaDesign() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(arenaDesignButton, "/fxml/arena-design.fxml", "KU Royale - Arena Design", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Arena Design: " + e.getMessage());
        }
    }

    @FXML
    private void handleSettings() {
        SoundEffectUtil.playButtonClick();
        try {
            // Note: Settings title is usually set in its own controller/FMXL,
            // but we pass a title here for consistency
            sceneLoader.load(settingsButton, "/fxml/settings.fxml", "KU Royale - Settings", null);
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

    private void updateGoldDisplay() {
        User currentUser = authService.getCurrentUser();
        if (currentUser != null && goldLabel != null) {
            goldLabel.setText(String.valueOf(currentUser.getGold()));
        } else if (goldLabel != null) {
            goldLabel.setText("0");
        }
    }

    private void playMainMenuMusic() {
        try {
            if (mainMenuMusicPlayer == null) {
                String soundPath = getClass().getResource("/musics/main_menu.mp3").toExternalForm();
                Media media = new Media(soundPath);
                mainMenuMusicPlayer = new MediaPlayer(media);
                mainMenuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                AudioManager.getInstance().registerMusicPlayer(mainMenuMusicPlayer);
                mainMenuMusicPlayer.play();
            } else {
                MediaPlayer.Status status = mainMenuMusicPlayer.getStatus();
                if (status == MediaPlayer.Status.STOPPED || status == MediaPlayer.Status.PAUSED) {
                    mainMenuMusicPlayer.play();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}