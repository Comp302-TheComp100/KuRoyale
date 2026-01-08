package com.kuroyale.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

// Manages all UI components for login/registration interface with Clash Royale theme
public class LoginView extends AnchorPane {
    private final Label titleLabel;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final TextField newUsernameField;
    private final PasswordField newPasswordField;
    private final Button createAccountButton;
    private final Button loginButton;
    private final Label errorLabel;
    private final Label createErrorLabel;

    // Containers for different states
    private final VBox buttonContainer;
    private final VBox loginForm;
    private final VBox createAccountForm;
    private final Label tipsLabel;

    // Submit buttons for forms
    private final Button submitLoginButton;
    private final Button submitCreateButton;

    public LoginView() {
        // Set background image - stretch to fill entire window
        setStyle("-fx-background-image: url('/images/login_background.jpg'); " +
                "-fx-background-size: 100% 100%; " +
                "-fx-background-position: center;");

        // Create title label at top
        titleLabel = new Label("KU ROYALE");
        titleLabel.getStyleClass().add("title-label");
        AnchorPane.setTopAnchor(titleLabel, 60.0);
        AnchorPane.setLeftAnchor(titleLabel, 0.0);
        AnchorPane.setRightAnchor(titleLabel, 0.0);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Create button container (initial state) - centered above tips
        buttonContainer = new VBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(15);
        AnchorPane.setBottomAnchor(buttonContainer, 100.0);
        AnchorPane.setLeftAnchor(buttonContainer, 200.0);
        AnchorPane.setRightAnchor(buttonContainer, 200.0);

        loginButton = new Button("SIGN IN");
        loginButton.getStyleClass().add("login-button");
        loginButton.setPrefWidth(220);
        loginButton.setPrefHeight(55);
        addLoginButtonHoverEffects(loginButton);

        createAccountButton = new Button("SIGN UP");
        createAccountButton.getStyleClass().add("login-button");
        createAccountButton.setPrefWidth(220);
        createAccountButton.setPrefHeight(55);
        addLoginButtonHoverEffects(createAccountButton);

        buttonContainer.getChildren().addAll(loginButton, createAccountButton);

        // Create login form (hidden initially)
        loginForm = new VBox();
        loginForm.setAlignment(Pos.CENTER);
        loginForm.setSpacing(15);
        loginForm.setVisible(false);
        loginForm.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); " +
                "-fx-background-radius: 20; " +
                "-fx-padding: 30;");
        AnchorPane.setTopAnchor(loginForm, 250.0);
        AnchorPane.setLeftAnchor(loginForm, 300.0);
        AnchorPane.setRightAnchor(loginForm, 300.0);

        Label loginTitle = new Label("SIGN IN");
        loginTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Clash', Arial; -fx-text-fill: white;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setPrefWidth(280);
        usernameField.setPrefHeight(50);
        usernameField.setStyle("-fx-font-size: 18px;");
        usernameField.getStyleClass().add("login-form-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setPrefWidth(280);
        passwordField.setPrefHeight(50);
        passwordField.setStyle("-fx-font-size: 18px;");
        passwordField.getStyleClass().add("login-form-field");

        errorLabel = new Label("");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setPrefWidth(280);
        errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");

        submitLoginButton = new Button("SUBMIT");
        submitLoginButton.getStyleClass().add("login-button");
        submitLoginButton.setPrefWidth(280);
        submitLoginButton.setPrefHeight(60);
        addLoginButtonHoverEffects(submitLoginButton);

        Button backFromLoginButton = new Button("BACK");
        backFromLoginButton.getStyleClass().add("back-button");
        backFromLoginButton.setPrefWidth(280);
        backFromLoginButton.setPrefHeight(50);
        addLoginButtonHoverEffects(backFromLoginButton);

        loginForm.getChildren().addAll(loginTitle, usernameField, passwordField,
                errorLabel, submitLoginButton, backFromLoginButton);

        // Create account form (hidden initially)
        createAccountForm = new VBox();
        createAccountForm.setAlignment(Pos.CENTER);
        createAccountForm.setSpacing(15);
        createAccountForm.setVisible(false);
        createAccountForm.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7); " +
                "-fx-background-radius: 20; " +
                "-fx-padding: 30;");
        AnchorPane.setTopAnchor(createAccountForm, 250.0);
        AnchorPane.setLeftAnchor(createAccountForm, 300.0);
        AnchorPane.setRightAnchor(createAccountForm, 300.0);

        Label createTitle = new Label("SIGN UP");
        createTitle.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Clash', Arial; -fx-text-fill: white;");

        newUsernameField = new TextField();
        newUsernameField.setPromptText("Username");
        newUsernameField.setPrefWidth(280);
        newUsernameField.setPrefHeight(50);
        newUsernameField.setStyle("-fx-font-size: 18px;");
        newUsernameField.getStyleClass().add("login-form-field");

        newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Password");
        newPasswordField.setPrefWidth(280);
        newPasswordField.setPrefHeight(50);
        newPasswordField.setStyle("-fx-font-size: 18px;");
        newPasswordField.getStyleClass().add("login-form-field");

        createErrorLabel = new Label("");
        createErrorLabel.setVisible(false);
        createErrorLabel.setWrapText(true);
        createErrorLabel.setPrefWidth(280);
        createErrorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 14px;");

        submitCreateButton = new Button("CREATE");
        submitCreateButton.getStyleClass().add("login-button");
        submitCreateButton.setPrefWidth(280);
        submitCreateButton.setPrefHeight(60);
        addLoginButtonHoverEffects(submitCreateButton);

        Button backFromCreateButton = new Button("BACK");
        backFromCreateButton.getStyleClass().add("back-button");
        backFromCreateButton.setPrefWidth(280);
        backFromCreateButton.setPrefHeight(50);
        addLoginButtonHoverEffects(backFromCreateButton);

        createAccountForm.getChildren().addAll(createTitle, newUsernameField, newPasswordField,
                createErrorLabel, submitCreateButton, backFromCreateButton);

        // Create tips label at bottom
        tipsLabel = new Label("TIPS & TRICKS: NERF GERCEKASLAN");
        tipsLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; " +
                "-fx-font-family: 'Clash', Arial; -fx-text-fill: white; " +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.8), 5, 0, 0, 2);");
        tipsLabel.setAlignment(Pos.CENTER);
        tipsLabel.setMaxWidth(Double.MAX_VALUE);
        AnchorPane.setBottomAnchor(tipsLabel, 30.0);
        AnchorPane.setLeftAnchor(tipsLabel, 0.0);
        AnchorPane.setRightAnchor(tipsLabel, 0.0);

        // Setup button handlers for showing/hiding forms
        loginButton.setOnAction(e -> showLoginForm());
        createAccountButton.setOnAction(e -> showCreateAccountForm());
        backFromLoginButton.setOnAction(e -> showButtons());
        backFromCreateButton.setOnAction(e -> showButtons());
        submitLoginButton.setOnAction(e -> {
            // This will be overridden by controller
        });
        submitCreateButton.setOnAction(e -> {
            // This will be overridden by controller
        });

        // Add all to root
        getChildren().addAll(titleLabel, buttonContainer, loginForm, createAccountForm, tipsLabel);
        getStyleClass().add("main-menu-background");
    }

    private void showLoginForm() {
        buttonContainer.setVisible(false);
        createAccountForm.setVisible(false);
        loginForm.setVisible(true);
        clearError();
    }

    private void showCreateAccountForm() {
        buttonContainer.setVisible(false);
        loginForm.setVisible(false);
        createAccountForm.setVisible(true);
        clearCreateError();
    }

    private void showButtons() {
        loginForm.setVisible(false);
        createAccountForm.setVisible(false);
        buttonContainer.setVisible(true);
        clearError();
        clearCreateError();
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

    public TextField getNewUsernameField() {
        return newUsernameField;
    }

    public PasswordField getNewPasswordField() {
        return newPasswordField;
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

    public Label getCreateErrorLabel() {
        return createErrorLabel;
    }

    public Button getSubmitLoginButton() {
        return submitLoginButton;
    }

    public Button getSubmitCreateButton() {
        return submitCreateButton;
    }

    // Utility methods for controller
    public void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    public void clearCreateError() {
        createErrorLabel.setText("");
        createErrorLabel.setVisible(false);
    }

    public void showError(String message) {
        if (loginForm.isVisible()) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
        } else if (createAccountForm.isVisible()) {
            createErrorLabel.setText(message);
            createErrorLabel.setVisible(true);
        }
    }

    public String getUsername() {
        if (loginForm.isVisible()) {
            return usernameField.getText().trim();
        } else if (createAccountForm.isVisible()) {
            return newUsernameField.getText().trim();
        }
        return "";
    }

    public String getPassword() {
        if (loginForm.isVisible()) {
            return passwordField.getText();
        } else if (createAccountForm.isVisible()) {
            return newPasswordField.getText();
        }
        return "";
    }
}
