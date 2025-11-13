package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.util.StyleHelper;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the main menu screen
 * Follows Controller GRASP pattern - thin controller focused on UI concerns
 * Follows Low Coupling - uses services via dependency injection
 */
public class MainMenuController {

    @FXML
    private VBox root;

    @FXML
    private Label titleLabel;

    @FXML
    private Button deckBuilderButton;

    @FXML
    private Button startMatchButton;
    @FXML
    private void initialize() {
        initializeStyles();
    }

    /**
     * Initialize styles after FXML is loaded
     */
    private void initializeStyles() {
        // Apply main menu background
        StyleHelper.applyMainMenuBackground(root);

        // Apply title style
        if (titleLabel != null) {
            StyleHelper.applyTitleStyle(titleLabel);
        }

        // Apply button styles
        StyleHelper.applyMenuButtonStyle(deckBuilderButton);
        StyleHelper.applyMenuButtonStyle(startMatchButton);
    }

    @FXML
    private void handleDeckBuilder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/deck-builder.fxml"));
            Parent root = loader.load();

            // Styles are initialized in DeckBuilderController's initialize() method

            Stage stage = (Stage) deckBuilderButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("KU Royale - Deck Builder");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Deck Builder: " + e.getMessage());
        }
    }

    @FXML
    private void handleStartMatch() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Coming Soon");
        alert.setHeaderText("Start Match");
        alert.setContentText("This feature will be available soon!");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
