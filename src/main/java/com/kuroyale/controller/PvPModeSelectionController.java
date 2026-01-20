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
 * Controller for selecting between Classical PvP and Mega Draft PvP.
 */
public class PvPModeSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private Button classicPvPButton;
    @FXML
    private Button megaDraftPvPButton;
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

        addMenuButtonHoverEffects(classicPvPButton);
        addMenuButtonHoverEffects(megaDraftPvPButton);
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
    private void handleClassicPvP() {
        SoundEffectUtil.playButtonClick();
        try {
            // Navigate to existing Classical PvP Deck Selection
            sceneLoader.load(root, "/fxml/pvp-deck-selection.fxml", "KU Royale - PvP Deck Selection", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Classical PvP: " + e.getMessage());
        }
    }

    @FXML
    private void handleMegaDraftPvP() {
        SoundEffectUtil.playButtonClick();
        try {
            // Navigate to new Mega Draft PvP Phase
            sceneLoader.load(root, "/fxml/pvp-mega-draft-phase.fxml", "KU Royale - PvP Mega Draft", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Mega Draft PvP: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            // Go back to Battle Mode Selection
            sceneLoader.load(root, "/fxml/battle-mode-selection.fxml", "KU Royale - Battle Mode", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to menu: " + e.getMessage());
        }
    }

    private void showError(String message) {
        com.kuroyale.util.ui.ThemedAlertManager.show("Error", message);
    }
}
