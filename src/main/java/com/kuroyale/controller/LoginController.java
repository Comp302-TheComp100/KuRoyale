package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.service.UserService;
import com.kuroyale.util.StyleHelper;

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

    @FXML
    private void initialize() {
        // Initialization logic
    }

    /**
     * Initialize styles after FXML is loaded
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

        // Clear previous error
        errorLabel.setText("");
        errorLabel.setVisible(false);

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        // Validate password requirements
        if (!isValidPassword(password)) {
            showError("Password must be at least 8 characters and contain at least one number and one letter");
            return;
        }

        try {
            // Check if username already exists
            if (UserService.findUserByUsername(username) != null) {
                showError("Username already exists. Please choose a different username.");
                return;
            }

            // Create user
            com.kuroyale.model.User user = UserService.createUser(username, password);
            if (user != null) {
                // Set as current user
                UserService.setCurrentUser(user);
                // Navigate to main menu
                navigateToMainMenu();
            } else {
                showError("Failed to create account. Username may already exist.");
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

        // Clear previous error
        errorLabel.setText("");
        errorLabel.setVisible(false);

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            return;
        }

        try {
            // Attempt login
            com.kuroyale.model.User user = UserService.login(username, password);
            if (user != null) {
                // Set as current user
                UserService.setCurrentUser(user);
                // Navigate to main menu
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
     * Validates password requirements
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasNumber = false;
        boolean hasLetter = false;

        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasNumber = true;
            }
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
        }

        return hasNumber && hasLetter;
    }

    /**
     * Shows an error message
     * @param message The error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Navigates to the main menu
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

