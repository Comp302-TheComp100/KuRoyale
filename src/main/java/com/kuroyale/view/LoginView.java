package com.kuroyale.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// TEAM_002: View component for the login screen following MVC pattern
// Manages all UI components for login/registration interface
public class LoginView extends VBox {
    private final Label titleLabel;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button createAccountButton;
    private final Button loginButton;
    private final Label errorLabel;

    public LoginView() {
        // Set root container properties
        setAlignment(Pos.CENTER);
        setSpacing(30);
        // TEAM_001: Removed 50px padding that caused white borders
        setPadding(Insets.EMPTY);
        getStyleClass().add("main-menu-background");

        // Create title label
        titleLabel = new Label("KU ROYALE");
        titleLabel.getStyleClass().add("login-title-label");

        // Create form container
        VBox formContainer = new VBox();
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setSpacing(20);
        formContainer.setPrefWidth(400);

        // Create input fields container
        VBox inputContainer = new VBox();
        inputContainer.setSpacing(10);
        inputContainer.setAlignment(Pos.CENTER);

        // Create username field
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(250);
        usernameField.setMaxWidth(500);
        usernameField.setPrefHeight(45);
        usernameField.getStyleClass().add("login-form-field");

        // Create password field
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(250);
        passwordField.setMaxWidth(500);
        passwordField.setPrefHeight(45);
        passwordField.getStyleClass().add("login-form-field");

        inputContainer.getChildren().addAll(usernameField, passwordField);

        // Create error label
        errorLabel = new Label("");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setPrefWidth(250);
        errorLabel.setMaxWidth(500);
        errorLabel.getStyleClass().add("error-label");

        // Create buttons container
        VBox buttonContainer = new VBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(15);

        // Create account button
        createAccountButton = new Button("CREATE ACCOUNT");
        createAccountButton.setPrefWidth(250);
        createAccountButton.setMaxWidth(400);
        createAccountButton.setPrefHeight(60);
        createAccountButton.getStyleClass().add("login-button");
        addLoginButtonHoverEffects(createAccountButton);

        // Create login button
        loginButton = new Button("LOGIN");
        loginButton.setPrefWidth(250);
        loginButton.setMaxWidth(400);
        loginButton.setPrefHeight(60);
        loginButton.getStyleClass().add("login-button");
        addLoginButtonHoverEffects(loginButton);

        buttonContainer.getChildren().addAll(createAccountButton, loginButton);

        // Assemble form container
        formContainer.getChildren().addAll(inputContainer, errorLabel, buttonContainer);

        // Assemble root
        getChildren().addAll(titleLabel, formContainer);
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

        button.setOnMousePressed(e -> button.setTranslateY(2));
        button.setOnMouseReleased(e -> button.setTranslateY(0));
    }

    // Getters for controller access
    public Label getTitleLabel() {
        return titleLabel;
    }

    public TextField getUsernameField() {
        return usernameField;
    }

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public Button getCreateAccountButton() {
        return createAccountButton;
    }

    public Button getLoginButton() {
        return loginButton;
    }

    public Label getErrorLabel() {
        return errorLabel;
    }

    // Utility methods for controller
    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    public void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    public String getUsername() {
        return usernameField.getText().trim();
    }

    public String getPassword() {
        return passwordField.getText();
    }
}
