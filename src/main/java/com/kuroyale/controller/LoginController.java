package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.model.Login;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/* Controller for the login/sign-in screen
 * Implements Model-View-Controller (MVC) - Controller component
 * Logic is delegated to the LoginModel. Navigation is centralized in SceneLoader. */
public class LoginController {
    @FXML private VBox root;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button createAccountButton;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;
    @FXML private Label titleLabel;

    // Instantiate the Model
    private final Login model = new Login();
    //Instantiate the SceneLoader
    private final SceneLoader sceneLoader = new SceneLoader();

    // MediaPlayer for start screen music
    private MediaPlayer startMusicPlayer;

    @FXML
    private void initialize() {
        // Initialization for the controller itself
    }

    // Initialize styles after FXML is loaded
    public void initializeStyles() {
        root.getStyleClass().add("main-menu-background");

        if (titleLabel != null) {
            titleLabel.getStyleClass().add("login-title-label");
        }
        usernameField.getStyleClass().add("login-form-field");
        passwordField.getStyleClass().add("login-form-field");
        createAccountButton.getStyleClass().add("login-button");
        loginButton.getStyleClass().add("login-button");
        errorLabel.getStyleClass().add("error-label");

        addLoginButtonHoverEffects(createAccountButton);
        addLoginButtonHoverEffects(loginButton);
        playStartMusic();
    }

    // Add programmatic hover effects for login buttons (scale transforms)
    private void addLoginButtonHoverEffects(Node button) {
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });

        button.setOnMousePressed(e -> {button.setTranslateY(2);});
        button.setOnMouseReleased(e -> {button.setTranslateY(0);});
    }

    @FXML
    private void handleCreateAccount() {
        SoundEffectUtil.playButtonClick();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        clearError();

        //Delegate all validation logic to the Model
        String validationError = model.validateCredentials(username, password);
        if (!validationError.isEmpty()) {
            showError(validationError);
            return;
        }

        try {
            // Delegate registration logic to the Model
            com.kuroyale.model.User user = model.registerUser(username, password);

            if (user != null) {
                // Success
                stopStartMusic();
                navigateToMainMenu();
            } else {
                // Failure (e.g., username exists)
                showError("Username already exists. Please choose a different username.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error creating account: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        SoundEffectUtil.playButtonClick();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        clearError();

        // Delegate all validation logic to the Model
        String validationError = model.validateCredentials(username, password);
        if (!validationError.isEmpty()) {
            showError(validationError);
            return;
        }

        try {
            //Delegate authentication logic to the Model
            com.kuroyale.model.User user = model.authenticateUser(username, password);

            if (user != null) {
                // Success
                stopStartMusic();
                navigateToMainMenu();
            } else {
                // Failure (invalid credentials)
                showError("Invalid username or password");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error during login: " + e.getMessage());
        }
    }

    // Clears error message
    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    // Shows an error message
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    // Plays the start screen music
    private void playStartMusic() {
        try {
            String soundPath = getClass().getResource("/musics/start.mp3").toExternalForm();
            Media media = new Media(soundPath);
            startMusicPlayer = new MediaPlayer(media);
            startMusicPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Stops the start screen music
    private void stopStartMusic() {
        if (startMusicPlayer != null) {
            startMusicPlayer.stop();
            startMusicPlayer = null;
        }
    }

    // Navigates to the main menu
    private void navigateToMainMenu() {
        try {
            // Use the centralized SceneLoader utility
            sceneLoader.load(loginButton, "/fxml/main-menu.fxml", "KU Royale", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main menu: " + e.getMessage());
        }
    }
}