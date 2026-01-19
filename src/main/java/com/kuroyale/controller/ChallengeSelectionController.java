package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.entities.Challenge;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.view.dialog.ChallengeDetailDialog;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

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
            com.kuroyale.view.card.ChallengeCardView challengeCard = new com.kuroyale.view.card.ChallengeCardView(
                    challenge, i,
                    this::handleStartChallenge);
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

    // Handles starting a challenge - shows detail dialog.
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

    private void navigateToChallengeDeckBuilder(Challenge challenge) {
        try {
            sceneLoader.load(backButton, "/fxml/challenge-deck-builder.fxml", "KU Royale - " + challenge.getName(),
                    controller -> {
                        if (controller instanceof ChallengeDeckBuilderController deckController) {
                            deckController.setChallenge(challenge);
                        }
                    });
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
