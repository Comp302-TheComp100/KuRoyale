package com.kuroyale.view.card;

import com.kuroyale.model.entities.Challenge;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

// Custom View component for a Challenge Card in the selection ladder.
public class ChallengeCardView extends VBox {

    public interface ChallengeStartListener {
        void onStart(Challenge challenge);
    }

    public ChallengeCardView(Challenge challenge, int index, ChallengeStartListener listener) {
        super(10);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(20));
        this.setMaxWidth(500);
        this.setMinWidth(400);

        boolean isLocked = !challenge.isUnlocked();
        boolean isCompleted = challenge.isCompleted();

        // Apply styling based on state
        if (isLocked) {
            this.getStyleClass().addAll("challenge-card", "challenge-card-locked");
        } else if (isCompleted) {
            this.getStyleClass().addAll("challenge-card", "challenge-card-completed");
        } else {
            this.getStyleClass().addAll("challenge-card", "challenge-card-unlocked");
        }

        // Badge
        this.getChildren().add(createNumberBadge(index + 1, isLocked, isCompleted));

        // Name
        Label nameLabel = new Label(challenge.getName());
        nameLabel.getStyleClass().add("challenge-name");
        if (isLocked)
            nameLabel.setOpacity(0.5);
        this.getChildren().add(nameLabel);

        // Description
        Label descLabel = new Label(challenge.getDescription());
        descLabel.getStyleClass().add("challenge-description");
        descLabel.setWrapText(true);
        if (isLocked)
            descLabel.setOpacity(0.5);
        this.getChildren().add(descLabel);

        // Rules
        Label rulesLabel = new Label(challenge.getRules());
        rulesLabel.getStyleClass().add("challenge-rules");
        rulesLabel.setWrapText(true);
        if (isLocked)
            rulesLabel.setOpacity(0.5);
        this.getChildren().add(rulesLabel);

        // Reward
        this.getChildren().add(createRewardDisplay(challenge.getGoldReward(), isLocked));

        // Stars
        if (isCompleted) {
            this.getChildren().add(createStarRating(challenge.getStarsEarned()));
        }

        // Button
        Button startButton = new Button(isLocked ? "🔒 LOCKED" : "START CHALLENGE");
        startButton.getStyleClass().add(isLocked ? "challenge-button-locked" : "challenge-button");
        startButton.setDisable(isLocked);

        if (!isLocked) {
            startButton.setOnAction(e -> {
                if (listener != null)
                    listener.onStart(challenge);
            });
            // Hover logic added internally
            startButton.setOnMouseEntered(e -> {
                startButton.setScaleX(1.05);
                startButton.setScaleY(1.05);
            });
            startButton.setOnMouseExited(e -> {
                startButton.setScaleX(1.0);
                startButton.setScaleY(1.0);
            });
        }
        this.getChildren().add(startButton);

        // General hover for card
        if (!isLocked) {
            this.setOnMouseEntered(e -> {
                this.setScaleX(1.02);
                this.setScaleY(1.02);
            });
            this.setOnMouseExited(e -> {
                this.setScaleX(1.0);
                this.setScaleY(1.0);
            });
        }
    }

    private StackPane createNumberBadge(int number, boolean isLocked, boolean isCompleted) {
        Circle circle = new Circle(25);
        if (isLocked) {
            circle.setFill(Color.web("#64748b"));
        } else if (isCompleted) {
            circle.setFill(Color.web("#10b981"));
        } else {
            circle.setFill(Color.web("#f97316"));
        }
        circle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));

        Label numberLabel = new Label(isLocked ? "🔒" : String.valueOf(number));
        numberLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        return new StackPane(circle, numberLabel);
    }

    private HBox createRewardDisplay(int goldReward, boolean isLocked) {
        HBox rewardBox = new HBox(5);
        rewardBox.setAlignment(Pos.CENTER);
        // Coin unicode removed
        Label rewardLabel = new Label(goldReward + " Gold");
        rewardLabel.getStyleClass().add("challenge-reward");
        if (isLocked)
            rewardLabel.setOpacity(0.5);
        rewardBox.getChildren().addAll(rewardLabel);
        return rewardBox;
    }

    private HBox createStarRating(int starsEarned) {
        HBox starBox = new HBox(3);
        starBox.setAlignment(Pos.CENTER);
        for (int i = 0; i < 3; i++) {
            Label star = new Label("★");
            String color = i < starsEarned ? "#ffd700" : "#555";
            star.setStyle("-fx-font-size: 28px; -fx-text-fill: " + color
                    + "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 0);");
            starBox.getChildren().add(star);
        }
        return starBox;
    }
}
