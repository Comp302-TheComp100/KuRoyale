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
 * Controller for PvP Mode Selection screen.
 * Allows players to choose between Classical PvP and Tower Defense modes.
 */
public class PvPModeSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private Button classicalButton;
    @FXML
    private Button towerDefenseButton;
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

        addMenuButtonHoverEffects(classicalButton);
        addMenuButtonHoverEffects(towerDefenseButton);
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
    private void handleClassicalMode() {
        SoundEffectUtil.playButtonClick();
        try {
            // Navigate to existing PvP deck selection
            sceneLoader.load(root, "/fxml/pvp-deck-selection.fxml", "KU Royale - PvP Deck Selection", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Classical PvP: " + e.getMessage());
        }
    }

    @FXML
    private void handleTowerDefenseMode() {
        SoundEffectUtil.playButtonClick();
        try {
            // Navigate to Tower Defense draft phase
            sceneLoader.load(root, "/fxml/tower-defense-draft.fxml", "KU Royale - Tower Defense", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Tower Defense: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(root, "/fxml/battle-mode-selection.fxml", "KU Royale - Battle Mode", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to battle mode selection: " + e.getMessage());
        }
    }

    private void showError(String message) {
        com.kuroyale.util.ui.ThemedAlertManager.show("Error", message);
    }
}
