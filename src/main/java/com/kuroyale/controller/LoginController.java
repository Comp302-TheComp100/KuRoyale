package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.model.Login;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.view.LoginView;

import javafx.fxml.FXML;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/* Controller for the login/sign-in screen
 * Implements Model-View-Controller (MVC) - Controller component
 * Logic is delegated to the LoginModel. Navigation is centralized in SceneLoader. */
public class LoginController {
    private final LoginView loginView;

    // Instantiate the Model
    private final Login model = new Login();
    // Instantiate the SceneLoader
    private final SceneLoader sceneLoader = new SceneLoader();

    // MediaPlayer for start screen music
    private MediaPlayer startMusicPlayer;

    public LoginController(LoginView view) {
        this.loginView = view;
        setupHandlers();
        playStartMusic();
    }

    private void setupHandlers() {
        loginView.getCreateAccountButton().setOnAction(e -> handleCreateAccount());
        loginView.getLoginButton().setOnAction(e -> handleLogin());
    }

    public void initialize() {
    }

    // Initialize styles after FXML is loaded (kept for compatibility with
    // Main.java)
    public void initializeStyles() {
        // This method is kept for backward compatibility but does nothing
        // LoginView applies all styles during construction
    }

    @FXML
    private void handleCreateAccount() {
        SoundEffectUtil.playButtonClick();
        String username = loginView.getUsername();
        String password = loginView.getPassword();

        loginView.clearError();

        // Delegate all validation logic to the Model
        String validationError = model.validateCredentials(username, password);
        if (!validationError.isEmpty()) {
            loginView.showError(validationError);
            return;
        }

        try {
            // Delegate registration logic to the Model
            com.kuroyale.model.User user = model.registerUser(username, password);

            if (user != null) {
                // Success
                stopStartMusic();

                // Load user-specific data into services
                com.kuroyale.util.ServiceFactory.getInstance().getQuestService().loadForUser(user.getUsername());
                com.kuroyale.util.ServiceFactory.getInstance().getAchievementService().loadForUser(user.getUsername());
                com.kuroyale.util.ServiceFactory.getInstance().getChallengeService().loadForUser(user.getUsername());

                navigateToMainMenu();
            } else {
                // Failure (e.g., username exists)
                loginView.showError("Username already exists. Please choose a different username.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            loginView.showError("Error creating account: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        SoundEffectUtil.playButtonClick();
        String username = loginView.getUsername();
        String password = loginView.getPassword();

        loginView.clearError();

        // Delegate all validation logic to the Model
        String validationError = model.validateCredentials(username, password);
        if (!validationError.isEmpty()) {
            loginView.showError(validationError);
            return;
        }

        try {
            // Delegate authentication logic to the Model
            com.kuroyale.model.User user = model.authenticateUser(username, password);

            if (user != null) {
                // Success
                stopStartMusic();

                // Load user-specific data into services
                com.kuroyale.util.ServiceFactory.getInstance().getQuestService().loadForUser(user.getUsername());
                com.kuroyale.util.ServiceFactory.getInstance().getAchievementService().loadForUser(user.getUsername());
                com.kuroyale.util.ServiceFactory.getInstance().getChallengeService().loadForUser(user.getUsername());

                navigateToMainMenu();
            } else {
                // Failure (invalid credentials)
                loginView.showError("Invalid username or password");
            }
        } catch (IOException e) {
            e.printStackTrace();
            loginView.showError("Error during login: " + e.getMessage());
        }
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
            sceneLoader.load(loginView.getLoginButton(), "/fxml/main-menu.fxml", "KU Royale", null);
        } catch (IOException e) {
            e.printStackTrace();
            loginView.showError("Failed to load main menu: " + e.getMessage());
        }
    }
}