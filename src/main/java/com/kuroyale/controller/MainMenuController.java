package com.kuroyale.controller;

import java.io.IOException;

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
        // Apply CSS classes
        root.getStyleClass().add("main-menu-background");
        
        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }

        // Apply button CSS classes
        deckBuilderButton.getStyleClass().add("menu-button");
        startMatchButton.getStyleClass().add("menu-button");
        
        // Add programmatic hover effects for scale transforms (CSS can't handle this easily)
        addMenuButtonHoverEffects(deckBuilderButton);
        addMenuButtonHoverEffects(startMatchButton);
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
