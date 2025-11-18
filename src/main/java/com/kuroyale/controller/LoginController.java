package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.util.ValidationUtil;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

/**
 * Controller for the login/sign-in screen
 * Follows Controller GRASP pattern - thin controller that delegates to services
 * Follows Low Coupling - uses services via dependency injection
 * Follows High Cohesion - focused only on UI concerns
 */
public class LoginController {

    @FXML
    private VBox root;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button createAccountButton;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    @FXML
    private Label titleLabel;
    
    // Service dependencies (injected via ServiceFactory)
    private AuthenticationService authService;
    
    // MediaPlayer for start screen music
    private MediaPlayer startMusicPlayer;

    @FXML
    private void initialize() {
        // Get service from factory (dependency injection)
        this.authService = ServiceFactory.getInstance().getAuthenticationService();
    }

    /**
     * Initialize styles after FXML is loaded
     * UI concern - appropriate for controller
     */
    public void initializeStyles() {
        // Apply CSS classes
        root.getStyleClass().add("main-menu-background");
        
        if (titleLabel != null) {
            titleLabel.getStyleClass().add("login-title-label");
        }

        // Apply form field CSS classes
        usernameField.getStyleClass().add("login-form-field");
        passwordField.getStyleClass().add("login-form-field");
        
        // Apply button CSS classes
        createAccountButton.getStyleClass().add("login-button");
        loginButton.getStyleClass().add("login-button");
        
        // Apply error label CSS class
        errorLabel.getStyleClass().add("error-label");
        
        // Add programmatic hover effects for login buttons (scale transforms)
        addLoginButtonHoverEffects(createAccountButton);
        addLoginButtonHoverEffects(loginButton);
        
        // Play start screen music
        playStartMusic();
    }
    
    /**
     * Add programmatic hover effects for login buttons (scale transforms)
     */
    private void addLoginButtonHoverEffects(Button button) {
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
    private void handleCreateAccount() {
        SoundEffectUtil.playButtonClick();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Clear previous error (UI concern)
        clearError();

        // Basic UI validation - fields not empty
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        // Validate using ValidationUtil (business rule)
        if (!ValidationUtil.isValidUsername(username)) {
            showError(ValidationUtil.getUsernameRequirements());
            return;
        }
        
        if (!ValidationUtil.isValidPassword(password)) {
            showError(ValidationUtil.getPasswordRequirements());
            return;
        }

        try {
            // Delegate to service (Controller pattern)
            com.kuroyale.model.User user = authService.register(username, password);
            if (user != null) {
                // Set as current user
                authService.setCurrentUser(user);
                // Stop start screen music
                stopStartMusic();
                // Navigate to main menu (UI concern)
                navigateToMainMenu();
            } else {
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

        // Clear previous error (UI concern)
        clearError();

        // Basic UI validation - fields not empty
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        try {
            // Delegate to service (Controller pattern)
            com.kuroyale.model.User user = authService.authenticate(username, password);
            if (user != null) {
                // Set as current user
                authService.setCurrentUser(user);
                // Stop start screen music
                stopStartMusic();
                // Navigate to main menu (UI concern)
                navigateToMainMenu();
            } else {
                showError("Invalid username or password");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error during login: " + e.getMessage());
        }
    }

    /**
     * Clears error message (UI concern)
     */
    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    /**
     * Shows an error message (UI concern)
     * @param message The error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Plays the start screen music
     */
    private void playStartMusic() {
        try {
            String soundPath = getClass().getResource("/musics/start.mp3").toExternalForm();
            Media media = new Media(soundPath);
            startMusicPlayer = new MediaPlayer(media);
            startMusicPlayer.play();
        } catch (Exception e) {
            // Silently fail if sound cannot be played
            e.printStackTrace();
        }
    }
    
    /**
     * Stops the start screen music
     */
    private void stopStartMusic() {
        if (startMusicPlayer != null) {
            startMusicPlayer.stop();
            startMusicPlayer = null;
        }
    }

    /**
     * Navigates to the main menu (UI concern)
     */
    private void navigateToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            // Load stylesheet for new scene
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main menu: " + e.getMessage());
        }
    }
}
