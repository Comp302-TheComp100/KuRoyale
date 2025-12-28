package com.kuroyale.util;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

//Utility class to centralize the logic for loading new JavaFX scenes.
public class SceneLoader {

    // Functional interface to allow the calling controller to initialize the newly loaded controller
    @FunctionalInterface
    public interface ControllerInitializer {
        void initialize(Object controller);
    }

    // Loads a new FXML scene and switches the stage to display it.
    public void load(Node sourceButton, String fxmlPath, String title, ControllerInitializer initializer)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        // Execute the initializer lambda if provided
        if (initializer != null) {
            initializer.initialize(loader.getController());
        }

        Stage stage = (Stage) sourceButton.getScene().getWindow();
        Scene scene = new Scene(root, 1280, 720);

        // Load the main stylesheet for the new scene
        scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle(title);
    }
}