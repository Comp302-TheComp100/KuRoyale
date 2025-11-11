package com.kuroyale.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the main menu screen
 */
public class MainMenuController {
    
    @FXML
    private Button deckBuilderButton;
    
    @FXML
    private Button startMatchButton;
    
    @FXML
    private void initialize() {
        // Initialization logic if needed
    }
    
    @FXML
    private void handleDeckBuilder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/deck-builder.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) deckBuilderButton.getScene().getWindow();
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
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

