package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.ui.SceneLoader;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

/**
 * Controller for Solo Mode Selection screen.
 * Allows players to choose between Normal and 7x Elixir game modes.
 */
public class SoloModeSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private Button normalButton;
    @FXML
    private Button sevenXElixirButton;
    @FXML
    private Button backButton;

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

        addMenuButtonHoverEffects(normalButton);
        addMenuButtonHoverEffects(sevenXElixirButton);
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
    private void handleNormalMode() {
        SoundEffectUtil.playButtonClick();
        startBattleWithMultiplier(1.0);
    }

    @FXML
    private void handleSevenXElixirMode() {
        SoundEffectUtil.playButtonClick();
        startBattleWithMultiplier(7.0);
    }

    /**
     * Starts a battle with the specified elixir multiplier.
     */
    private void startBattleWithMultiplier(double multiplier) {
        try {
            sceneLoader.load(root, "/fxml/battle.fxml", "KU Royale - Battle", controller -> {
                if (controller instanceof BattleController battleController) {
                    battleController.setElixirMultiplier(multiplier);
                    battleController.startGame();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Battle: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(backButton, "/fxml/battle-mode-selection.fxml", "KU Royale - Select Battle Mode", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to battle mode selection: " + e.getMessage());
        }
    }

    private void showError(String message) {
        com.kuroyale.util.ui.ThemedAlertManager.show("Error", message);
    }
}
