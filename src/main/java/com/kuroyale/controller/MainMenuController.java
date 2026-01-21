package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.logic.MenuModel; // Import the new Model
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.util.audio.AudioManager;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.ui.SceneLoader;

import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/* Controller for the main menu screen
 * Implements Model-View-Controller (MVC) - Controller component*/
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
    private Button resumeGameButton;
    @FXML
    private Button arenaDesignButton;
    @FXML
    private Button challengesButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button questsButton;
    @FXML
    private Button profileButton;

    private static MediaPlayer mainMenuMusicPlayer;
    private final MenuModel model = new MenuModel();
    private final SceneLoader sceneLoader = new SceneLoader();

    @FXML
    private void initialize() {
        initializeStyles();
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
        challengesButton.getStyleClass().add("menu-button");
        if (questsButton != null)
            questsButton.getStyleClass().add("menu-button");
        settingsButton.getStyleClass().add("menu-button");
        profileButton.getStyleClass().add("menu-button");

        // Add programmatic hover effects for scale transforms
        addMenuButtonHoverEffects(deckBuilderButton);
        addMenuButtonHoverEffects(startMatchButton);
        addMenuButtonHoverEffects(resumeGameButton);
        addMenuButtonHoverEffects(arenaDesignButton);
        addMenuButtonHoverEffects(challengesButton);
        addMenuButtonHoverEffects(questsButton);
        addMenuButtonHoverEffects(settingsButton);
        addMenuButtonHoverEffects(profileButton);
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
            // Controller/View: Handle the Model's error response
            StringBuilder errorMessage = new StringBuilder("Cannot start match. Please fix the following issues:\n\n");
            validationErrors.forEach(error -> errorMessage.append("• ").append(error).append("\n"));
            showError(errorMessage.toString());
            return;
        }

        // Navigate to battle mode selection screen
        try {
            sceneLoader.load(startMatchButton, "/fxml/battle-mode-selection.fxml", "KU Royale - Select Battle Mode",
                    null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Battle Mode Selection: " + e.getMessage());
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
    private void handleChallenges() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(challengesButton, "/fxml/challenge-selection.fxml", "KU Royale - Challenges", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Challenges: " + e.getMessage());
        }
    }

    @FXML
    private void handleQuests() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(questsButton, "/fxml/quest-achievements.fxml", "KU Royale - Quests", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Quests: " + e.getMessage());
        }
    }

    @FXML
    private void handleSettings() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(settingsButton, "/fxml/settings.fxml", "KU Royale - Settings", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Settings: " + e.getMessage());
        }
    }

    @FXML
    private void handleProfile() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(profileButton, "/fxml/player-profile.fxml", "KU Royale - Player Profile", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Profile: " + e.getMessage());
        }
    }

    private void showError(String message) {
        com.kuroyale.util.ui.ThemedAlertManager.show(root.getScene().getWindow(), "Error", message, null);
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
