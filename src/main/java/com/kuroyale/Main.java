package com.kuroyale;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for KU Royale application
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load main menu
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            // Create scene with CSS styling
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            primaryStage.setTitle("KU Royale - Clash Royale Clone");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load application: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
