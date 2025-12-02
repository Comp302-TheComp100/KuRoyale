package com.kuroyale;

import com.kuroyale.controller.LoginController;
import com.kuroyale.util.ServiceFactory;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

//Main entry point for KU Royale application - Initializes the ServiceFactory
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize ServiceFactory
            // Low Coupling - centralized service management
            ServiceFactory.initialize();
            
            // Load custom fonts
            Font.loadFont(getClass().getResourceAsStream("/fonts/Clash_Regular.otf"), 12);
            Font.loadFont(getClass().getResourceAsStream("/fonts/Clash_Bold.otf"), 12);

            // Load login page first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            // Get controller and initialize styles
            LoginController controller = loader.getController();
            controller.initializeStyles();

            // Create scene (1280x720 HD resolution)
            Scene scene = new Scene(root, 1280, 720);
            
            // Load application stylesheet
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());

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
