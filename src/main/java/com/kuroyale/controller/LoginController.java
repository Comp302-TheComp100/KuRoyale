package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.StyleHelper;
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
        // Apply main menu background
        StyleHelper.applyMainMenuBackground(root);

        // Apply title style
        if (titleLabel != null) {
            StyleHelper.applyLoginTitleStyle(titleLabel);
        }

        // Apply login form styles
        StyleHelper.applyLoginFormStyle(usernameField);
        StyleHelper.applyLoginFormStyle(passwordField);
        StyleHelper.applyLoginButtonStyle(createAccountButton);
        StyleHelper.applyLoginButtonStyle(loginButton);
        StyleHelper.applyErrorLabelStyle(errorLabel);
    }

    @FXML
    private void handleCreateAccount() {
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
     * Navigates to the main menu (UI concern)
     */
    private void navigateToMainMenu() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            // Get controller and initialize styles
            MainMenuController controller = loader.getController();
            controller.initializeStyles();

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("KU Royale");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load main menu: " + e.getMessage());
        }
    }
}

