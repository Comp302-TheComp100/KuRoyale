package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.Challenge;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.view.ChallengeDetailDialog;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/**
 * Controller for the Challenge Selection screen.
 * Displays challenges in a ladder format where completing one unlocks the next.
 */
public class ChallengeSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private ScrollPane challengeScrollPane;
    @FXML
    private VBox challengeLadderContainer;
    @FXML
    private Button backButton;

    private final SceneLoader sceneLoader = new SceneLoader();
    private List<Challenge> challenges;

    @FXML
    private void initialize() {
        initializeStyles();
        loadChallenges();
        buildLadderUI();
    }

    private void initializeStyles() {
        root.getStyleClass().add("main-menu-background");
        if (backButton != null) {
            addButtonHoverEffects(backButton);
        }
    }

    private void loadChallenges() {
        challenges = com.kuroyale.util.ServiceFactory.getInstance().getChallengeService().getAllChallenges();
    }

    /**
     * Builds the ladder UI with challenge cards and connecting lines.
     */
    private void buildLadderUI() {
        challengeLadderContainer.getChildren().clear();

        for (int i = 0; i < challenges.size(); i++) {
            Challenge challenge = challenges.get(i);

            // Add connecting line above (except for first challenge)
            if (i > 0) {
                VBox lineContainer = createConnectingLine();
                challengeLadderContainer.getChildren().add(lineContainer);
            }

            // Create challenge card
            VBox challengeCard = createChallengeCard(challenge, i);
            challengeLadderContainer.getChildren().add(challengeCard);
        }
    }

    /**
     * Creates a connecting line between challenge cards.
     */
    private VBox createConnectingLine() {
        VBox container = new VBox();
        container.setAlignment(Pos.CENTER);
        container.setPrefHeight(40);

        Line line = new Line(0, 0, 0, 30);
        line.setStrokeWidth(4);
        line.setStroke(Color.web("#fbbf24")); // Gold color
        line.getStyleClass().add("challenge-ladder-line");

        container.getChildren().add(line);
        return container;
    }

    /**
     * Creates a challenge card with all information and interaction.
     */
    private VBox createChallengeCard(Challenge challenge, int index) {
        boolean isLocked = !challenge.isUnlocked();
        boolean isCompleted = challenge.isCompleted();

        // Main card container
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setMaxWidth(500);
        card.setMinWidth(400);

        // Apply styling based on state
        if (isLocked) {
            card.getStyleClass().addAll("challenge-card", "challenge-card-locked");
        } else if (isCompleted) {
            card.getStyleClass().addAll("challenge-card", "challenge-card-completed");
        } else {
            card.getStyleClass().addAll("challenge-card", "challenge-card-unlocked");
        }

        // Challenge number badge
        StackPane numberBadge = createNumberBadge(index + 1, isLocked, isCompleted);

        // Challenge name
        Label nameLabel = new Label(challenge.getName());
        nameLabel.getStyleClass().add("challenge-name");
        if (isLocked) {
            nameLabel.setOpacity(0.5);
        }

        // Description
        Label descLabel = new Label(challenge.getDescription());
        descLabel.getStyleClass().add("challenge-description");
        descLabel.setWrapText(true);
        if (isLocked) {
            descLabel.setOpacity(0.5);
        }

        // Rules
        Label rulesLabel = new Label(challenge.getRules());
        rulesLabel.getStyleClass().add("challenge-rules");
        rulesLabel.setWrapText(true);
        if (isLocked) {
            rulesLabel.setOpacity(0.5);
        }

        // Reward
        HBox rewardBox = createRewardDisplay(challenge.getGoldReward(), isLocked);

        // Star rating (if completed)
        HBox starBox = null;
        if (isCompleted) {
            starBox = createStarRating(challenge.getStarsEarned());
        }

        // Start button
        Button startButton = new Button(isLocked ? "🔒 LOCKED" : "START CHALLENGE");
        startButton.getStyleClass().add(isLocked ? "challenge-button-locked" : "challenge-button");
        startButton.setDisable(isLocked);

        if (!isLocked) {
            startButton.setOnAction(e -> handleStartChallenge(challenge));
            addButtonHoverEffects(startButton);
        }

        // Assemble card
        card.getChildren().addAll(numberBadge, nameLabel, descLabel, rulesLabel, rewardBox);
        if (starBox != null) {
            card.getChildren().add(starBox);
        }
        card.getChildren().add(startButton);

        // Add hover effects for unlocked cards
        if (!isLocked) {
            card.setOnMouseEntered(e -> {
                card.setScaleX(1.02);
                card.setScaleY(1.02);
            });
            card.setOnMouseExited(e -> {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
            });
        }

        return card;
    }

    /**
     * Creates a circular badge with the challenge number.
     */
    private StackPane createNumberBadge(int number, boolean isLocked, boolean isCompleted) {
        Circle circle = new Circle(25);
        if (isLocked) {
            circle.setFill(Color.web("#64748b")); // Gray
        } else if (isCompleted) {
            circle.setFill(Color.web("#10b981")); // Green
        } else {
            circle.setFill(Color.web("#f97316")); // Orange
        }
        circle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));

        Label numberLabel = new Label(isLocked ? "🔒" : String.valueOf(number));
        numberLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        StackPane badge = new StackPane(circle, numberLabel);
        return badge;
    }

    /**
     * Creates the reward display with gold icon.
     */
    private HBox createRewardDisplay(int goldReward, boolean isLocked) {
        HBox rewardBox = new HBox(5);
        rewardBox.setAlignment(Pos.CENTER);

        Label coinLabel = new Label("🪙");
        coinLabel.setStyle("-fx-font-size: 20px;");

        Label rewardLabel = new Label(goldReward + " Gold");
        rewardLabel.getStyleClass().add("challenge-reward");
        if (isLocked) {
            rewardLabel.setOpacity(0.5);
        }

        rewardBox.getChildren().addAll(coinLabel, rewardLabel);
        return rewardBox;
    }

    /**
     * Creates star rating display.
     */
    private HBox createStarRating(int starsEarned) {
        HBox starBox = new HBox(5);
        starBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 3; i++) {
            Label star = new Label(i < starsEarned ? "⭐" : "☆");
            star.setStyle("-fx-font-size: 24px;");
            starBox.getChildren().add(star);
        }

        return starBox;
    }

    /**
     * Handles starting a challenge - shows detail dialog.
     */
    private void handleStartChallenge(Challenge challenge) {
        SoundEffectUtil.playButtonClick();

        // Show challenge detail dialog
        ChallengeDetailDialog dialog = new ChallengeDetailDialog(
                challenge,
                () -> {
                    // On close - remove dialog
                    StackPane rootStack = (StackPane) root.getScene().getRoot();
                    rootStack.getChildren().remove(rootStack.getChildren().size() - 1);
                },
                () -> {
                    // On try challenge - navigate to deck builder
                    navigateToChallengeDeckBuilder(challenge);
                });

        // Add dialog as overlay
        StackPane rootStack = (StackPane) root.getScene().getRoot();
        rootStack.getChildren().add(dialog);
    }

    /**
     * Navigates to the challenge deck builder with the selected challenge.
     */
    private void navigateToChallengeDeckBuilder(Challenge challenge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/challenge-deck-builder.fxml"));
            Parent root = loader.load();

            // Get controller and set challenge
            ChallengeDeckBuilderController controller = loader.getController();
            controller.setChallenge(challenge);

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - " + challenge.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(backButton, "/fxml/main-menu.fxml", "KU Royale", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addButtonHoverEffects(Button button) {
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
}
