package com.kuroyale.view.dialog;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.User;
import com.kuroyale.util.StyleHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class UpgradeDialog extends StackPane {
    private final Card card;
    private final User user;
    private final Runnable onUpgradeSuccess;
    @SuppressWarnings("unused")
    private final Runnable onCancel;

    public UpgradeDialog(Card card, User user, Runnable onUpgradeSuccess, Runnable onCancel) {
        this.card = card;
        this.user = user;
        this.onUpgradeSuccess = onUpgradeSuccess;
        this.onCancel = onCancel;

        // Full screen overlay
        getStyleClass().add("overlay-background");

        // Main container
        VBox mainContainer = new VBox(15);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(400);
        mainContainer.setMaxHeight(600);
        mainContainer.getStyleClass().add("overlay-card-container");
        mainContainer.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label("Upgrade " + card.getName());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                "-fx-font-family: '" + StyleHelper.FONT_FAMILY + "', Arial; " +
                "-fx-text-fill: " + StyleHelper.COLOR_DARK + ";");
        titleLabel.setWrapText(true);
        titleLabel.setTextAlignment(TextAlignment.CENTER);
        titleLabel.setMaxWidth(350);

        // Current level display
        int currentLevel = card.getLevel();
        Label currentLevelLabel = new Label("Current Level: " + currentLevel);
        currentLevelLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + StyleHelper.COLOR_DARK + ";");

        // Current stats box
        VBox currentStatsBox = createStatsBox("Current Stats", currentLevel);

        // Arrow indicator
        Label arrowLabel = new Label("↓");
        arrowLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + StyleHelper.COLOR_YELLOW_DARK + ";");

        // Next level display
        int nextLevel = currentLevel + 1;
        Label nextLevelLabel = new Label("Next Level: " + nextLevel);
        nextLevelLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + StyleHelper.COLOR_YELLOW_DARK + ";");

        // Next level stats box
        VBox nextStatsBox = createStatsBox("Next Level Stats", nextLevel);

        // Upgrade cost and gold balance
        int upgradeCost = card.calculateUpgradeCost();
        int currentGold = user.getGold();

        HBox costBox = new HBox(10);
        costBox.setAlignment(Pos.CENTER);
        Label costLabel = new Label("Upgrade Cost: " + upgradeCost + " gold");
        costLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + (currentGold >= upgradeCost ? StyleHelper.COLOR_YELLOW_DARK : "#ef4444") + ";");
        costBox.getChildren().add(costLabel);

        HBox goldBox = new HBox(10);
        goldBox.setAlignment(Pos.CENTER);
        Label goldLabel = new Label("Your Gold: " + currentGold);
        goldLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + StyleHelper.COLOR_DARK + ";");
        goldBox.getChildren().add(goldLabel);

        // Buttons
        HBox buttonsContainer = new HBox(10);
        buttonsContainer.setAlignment(Pos.CENTER);

        Button confirmButton = new Button("CONFIRM");
        confirmButton.getStyleClass().add("overlay-button");
        confirmButton.setPrefWidth(150);
        confirmButton.setPrefHeight(40);
        String confirmNormalStyle = "-fx-background-color: " + StyleHelper.COLOR_YELLOW_DARK + "; " +
                "-fx-text-fill: white; -fx-font-size: 14px;";
        String confirmHoverStyle = "-fx-background-color: " + StyleHelper.COLOR_YELLOW + "; " +
                "-fx-text-fill: white; -fx-font-size: 14px;";

        confirmButton.setStyle(confirmNormalStyle);
        confirmButton.setOnMouseEntered(e -> {
            if (!confirmButton.isDisabled())
                confirmButton.setStyle(confirmHoverStyle);
        });
        confirmButton.setOnMouseExited(e -> {
            if (!confirmButton.isDisabled())
                confirmButton.setStyle(confirmNormalStyle);
        });

        confirmButton.setOnAction(e -> handleConfirm());

        // Disable confirm if insufficient gold
        if (currentGold < upgradeCost) {
            confirmButton.setDisable(true);
        }

        Button cancelButton = new Button("CANCEL");
        cancelButton.getStyleClass().add("overlay-button");
        cancelButton.setPrefWidth(150);
        cancelButton.setPrefHeight(40);
        cancelButton.setOnAction(e -> onCancel.run());

        buttonsContainer.getChildren().addAll(confirmButton, cancelButton);

        mainContainer.getChildren().addAll(
                titleLabel,
                currentLevelLabel,
                currentStatsBox,
                arrowLabel,
                nextLevelLabel,
                nextStatsBox,
                costBox,
                goldBox,
                buttonsContainer);

        getChildren().add(mainContainer);

        // Click outside to close
        setOnMouseClicked(event -> {
            if (event.getTarget() == this) {
                onCancel.run();
            }
        });
    }

    private VBox createStatsBox(String title, int level) {
        VBox statsBox = new VBox(5);
        statsBox.setAlignment(Pos.TOP_LEFT);
        statsBox.setPadding(new Insets(10));
        statsBox.setStyle("-fx-background-color: #f8fafc; " +
                "-fx-background-radius: 8; -fx-border-color: " + StyleHelper.COLOR_GRAY + "; " +
                "-fx-border-radius: 8; -fx-border-width: 1;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-text-fill: " + StyleHelper.COLOR_DARK + ";");

        // Calculate stats for this level
        int hp = Card.calculateStatForLevel(card.getBaseHp(), level);
        int damage = Card.calculateStatForLevel(card.getBaseDamage(), level);

        if (hp > 0) {
            addStatRow(statsBox, "HP", String.valueOf(hp));
        }
        if (damage > 0) {
            addStatRow(statsBox, "DMG", String.valueOf(damage));
            if (card.getHitSpeed() > 0) {
                double dps = damage / card.getHitSpeed();
                addStatRow(statsBox, "DPS", String.format("%.1f", dps));
            }
        }

        statsBox.getChildren().add(0, titleLabel);
        return statsBox;
    }

    private void addStatRow(VBox container, String label, String value) {
        HBox statRow = new HBox(5);
        statRow.setAlignment(Pos.CENTER_LEFT);

        Label labelText = new Label(label + ":");
        labelText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        labelText.setMinWidth(60);

        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");

        statRow.getChildren().addAll(labelText, valueText);
        container.getChildren().add(statRow);
    }

    private void handleConfirm() {
        com.kuroyale.service.CardService service = new com.kuroyale.service.CardService();
        try {
            service.upgradeCard(card, user);

            // Call success callback
            if (onUpgradeSuccess != null) {
                onUpgradeSuccess.run();
            }
        } catch (Exception e) {
            System.err.println("Upgrade failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
