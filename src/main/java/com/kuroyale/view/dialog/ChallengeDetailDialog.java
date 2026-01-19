package com.kuroyale.view.dialog;

import com.kuroyale.model.entities.Challenge;
import com.kuroyale.util.SoundEffectUtil;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/* Dialog showing challenge details, stats, and action buttons.
 * Opens when a player clicks on an unlocked challenge.*/
public class ChallengeDetailDialog extends StackPane {

    private final Challenge challenge;
    private final Runnable onClose;
    private final Runnable onTryChallenge;

    public ChallengeDetailDialog(Challenge challenge, Runnable onClose, Runnable onTryChallenge) {
        this.challenge = challenge;
        this.onClose = onClose;
        this.onTryChallenge = onTryChallenge;

        // Full screen overlay
        getStyleClass().add("overlay-background");

        // Main container
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(450);
        mainContainer.setMaxHeight(550);
        mainContainer.setPadding(new Insets(30));
        mainContainer.getStyleClass().add("challenge-detail-container");
        mainContainer.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #3d3d5c 0%, #2d2d44 100%);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: #8b5cf6;" +
                        "-fx-border-width: 3;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 20, 0, 0, 5);");

        // Challenge number badge
        StackPane badge = createNumberBadge();

        // Challenge name
        Label nameLabel = new Label(challenge.getName());
        nameLabel.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 5, 0, 0, 2);");

        // Description
        Label descLabel = new Label(challenge.getDescription());
        descLabel.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: rgba(255, 255, 255, 0.9);" +
                        "-fx-text-alignment: center;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(380);

        // Rules box
        VBox rulesBox = createRulesBox();

        // Stats box
        VBox statsBox = createStatsBox();

        // Reward display
        HBox rewardBox = createRewardDisplay();

        // Star Conditions
        VBox conditionsBox = createStarConditionsBox();

        // Star rating (if completed)
        HBox starBox = createStarRating();

        // Content Container for ScrollPane (everything except title/badge/buttons)
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(descLabel, rulesBox, statsBox, rewardBox, conditionsBox, starBox);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("scroll-pane-transparent"); // Ensure transparency

        // Allow ScrollPane to grow
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Action buttons
        HBox buttonsBox = createActionButtons();

        mainContainer.getChildren().addAll(badge, nameLabel, scrollPane, buttonsBox);

        getChildren().add(mainContainer);

        // Click outside to close
        setOnMouseClicked(event -> {
            if (event.getTarget() == this) {
                SoundEffectUtil.playButtonClick();
                onClose.run();
            }
        });
    }

    private StackPane createNumberBadge() {
        Circle circle = new Circle(30);
        if (challenge.isCompleted()) {
            circle.setFill(Color.web("#10b981")); // Green for completed
        } else {
            circle.setFill(Color.web("#f97316")); // Orange for unlocked
        }
        circle.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.3)));

        Label numberLabel = new Label(String.valueOf(challenge.getId()));
        numberLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        StackPane badge = new StackPane(circle, numberLabel);
        return badge;
    }

    private VBox createRulesBox() {
        VBox rulesBox = new VBox(5);
        rulesBox.setAlignment(Pos.CENTER);
        rulesBox.setPadding(new Insets(15));
        rulesBox.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.3);" +
                        "-fx-background-radius: 10;");
        rulesBox.setMaxWidth(380);

        Label rulesTitle = new Label("📜 RULES");
        rulesTitle.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: #fbbf24;");

        Label rulesLabel = new Label(challenge.getRules());
        rulesLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: rgba(255, 255, 255, 0.8);");
        rulesLabel.setWrapText(true);
        rulesLabel.setMaxWidth(350);

        rulesBox.getChildren().addAll(rulesTitle, rulesLabel);
        return rulesBox;
    }

    private VBox createStatsBox() {
        VBox statsBox = new VBox(8);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.2);" +
                        "-fx-background-radius: 10;");
        statsBox.setMaxWidth(300);

        Label statsTitle = new Label("📊 YOUR STATS");
        statsTitle.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: #3b82f6;");

        HBox statsRow = new HBox(30);
        statsRow.setAlignment(Pos.CENTER);

        VBox attemptsBox = createStatItem("Attempts", String.valueOf(challenge.getAttempts()));
        VBox completionsBox = createStatItem("Wins", String.valueOf(challenge.getCompletions()));
        VBox starsBox = createStatItem("Best", challenge.getStarsEarned() + "⭐");

        statsRow.getChildren().addAll(attemptsBox, completionsBox, starsBox);
        statsBox.getChildren().addAll(statsTitle, statsRow);

        return statsBox;
    }

    private VBox createStatItem(String label, String value) {
        VBox item = new VBox(2);
        item.setAlignment(Pos.CENTER);

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
                "-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: white;");

        Label labelLabel = new Label(label);
        labelLabel.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: rgba(255, 255, 255, 0.6);");

        item.getChildren().addAll(valueLabel, labelLabel);
        return item;
    }

    private HBox createRewardDisplay() {
        HBox rewardBox = new HBox(8);
        rewardBox.setAlignment(Pos.CENTER);

        // Grid/Coin glitched character removed

        Label rewardLabel = new Label(challenge.getGoldReward() + " Gold");
        rewardLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-text-fill: #fbbf24;");

        rewardBox.getChildren().addAll(rewardLabel);
        return rewardBox;
    }

    private VBox createStarConditionsBox() {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10;");
        box.setMaxWidth(300);

        box.getChildren().add(createConditionRow("1 ⭐", "Win the Battle"));

        String twoStarTime = String.format("%d:%02d", challenge.getTwoStarTimeSeconds() / 60,
                challenge.getTwoStarTimeSeconds() % 60);
        box.getChildren().add(createConditionRow("2 ⭐", "Win under " + twoStarTime));

        String threeStarTime = String.format("%d:%02d", challenge.getThreeStarTimeSeconds() / 60,
                challenge.getThreeStarTimeSeconds() % 60);
        box.getChildren().add(createConditionRow("3 ⭐", "Win under " + threeStarTime + " OR No Damage"));

        return box;
    }

    private HBox createConditionRow(String stars, String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label starLabel = new Label(stars);
        starLabel.setStyle(
                "-fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-font-family: 'Clash', Arial; -fx-min-width: 40;");

        Label textLabel = new Label(text);
        textLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.8); -fx-font-family: 'Clash', Arial; -fx-font-size: 12px;");

        row.getChildren().addAll(starLabel, textLabel);
        row.setAlignment(Pos.CENTER); // Center the whole row content visually
        return row;
    }

    private HBox createStarRating() {
        HBox starBox = new HBox(5);
        starBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < 3; i++) {
            Label star = new Label("★"); // Always filled star
            star.setStyle("-fx-font-size: 36px; -fx-text-fill: " +
                    (i < challenge.getStarsEarned() ? "#ffd700" : "#555555") + // Yellow for earned, Dark Gray for empty
                    "; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 5, 0, 0, 0);");
            starBox.getChildren().add(star);
        }

        return starBox;
    }

    private HBox createActionButtons() {
        HBox buttonsBox = new HBox(20);
        buttonsBox.setAlignment(Pos.CENTER);

        // Close button
        Button closeButton = new Button("CLOSE");
        closeButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #6b7280 0%, #4b5563 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: #374151;" +
                        "-fx-border-width: 2;" +
                        "-fx-padding: 10 25 10 25;" +
                        "-fx-cursor: hand;");
        closeButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            onClose.run();
        });
        addButtonHoverEffects(closeButton);

        // Try Challenge button
        Button tryButton = new Button("TRY CHALLENGE");
        tryButton.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #10b981 0%, #059669 100%);" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: 'Clash', Arial;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-border-color: #047857;" +
                        "-fx-border-width: 2;" +
                        "-fx-padding: 10 25 10 25;" +
                        "-fx-cursor: hand;");
        tryButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            onTryChallenge.run();
        });
        addButtonHoverEffects(tryButton);

        buttonsBox.getChildren().addAll(closeButton, tryButton);
        return buttonsBox;
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
