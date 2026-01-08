package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.enums.BattleMode;
import com.kuroyale.model.logic.MenuModel;
import com.kuroyale.service.battle.BattleStrategy;
import com.kuroyale.service.battle.BattleStrategyFactory;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

/**
 * Controller for the Battle Mode Selection screen.
 * Implements MVC pattern (GRASP Controller) and uses Strategy pattern for
 * battle initialization.
 */
public class BattleModeSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private Button localBotButton;
    @FXML
    private Button localPvpButton;
    @FXML
    private Button networkPvpButton;
    @FXML
    private Button backButton;

    private final MenuModel model = new MenuModel();
    private final SceneLoader sceneLoader = new SceneLoader();

    @FXML
    private void initialize() {
        initializeStyles();
    }

    private void initializeStyles() {
        root.getStyleClass().add("main-menu-background");

        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }

        // Add hover effects to all buttons
        addMenuButtonHoverEffects(localBotButton);
        addMenuButtonHoverEffects(localPvpButton);
        addMenuButtonHoverEffects(networkPvpButton);
        addMenuButtonHoverEffects(backButton);
    }

    private void addMenuButtonHoverEffects(Node button) {
        if (button == null)
            return;

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
    private void handleLocalVsBot() {
        SoundEffectUtil.playButtonClick();
        startBattle(BattleMode.LOCAL_VS_BOT);
    }

    @FXML
    private void handleLocalPvP() {
        SoundEffectUtil.playButtonClick();
        startBattle(BattleMode.LOCAL_PVP);
    }

    @FXML
    private void handleNetworkPvP() {
        SoundEffectUtil.playButtonClick();
        startBattle(BattleMode.NETWORK_PVP);
    }

    /**
     * Starts a battle with the selected mode using Strategy pattern.
     * Low coupling: Controller doesn't know battle-type-specific logic.
     */
    private void startBattle(BattleMode mode) {
        // First validate that we can start a match
        List<String> validationErrors = model.validateAndPrepareMatchStart();

        if (!validationErrors.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("Cannot start match. Please fix the following issues:\n\n");
            validationErrors.forEach(error -> errorMessage.append("• ").append(error).append("\n"));
            showError(errorMessage.toString());
            return;
        }

        // Get the strategy for this battle mode
        BattleStrategy strategy = BattleStrategyFactory.createStrategy(mode);

        // Check if mode is implemented
        if (!strategy.isImplemented()) {
            showComingSoon(mode.getDisplayName(), strategy.getNotImplementedMessage());
            return;
        }

        // Handle different modes differently
        if (mode == BattleMode.LOCAL_PVP) {
            // PvP mode: Navigate directly to PvP deck selection (no BattleController)
            try {
                sceneLoader.load(root, "/fxml/pvp-deck-selection.fxml", "KU Royale - PvP Deck Selection", null);
            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to load PvP Deck Selection: " + e.getMessage());
            }
        } else {
            // Other modes: Load battle scene and initialize with strategy
            try {
                sceneLoader.load(root, "/fxml/battle.fxml", "KU Royale - Battle", controller -> {
                    if (controller instanceof BattleController battleController) {
                        strategy.initialize(battleController);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                showError("Failed to load Battle: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(backButton, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to main menu: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showComingSoon(String modeName, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText(modeName);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
