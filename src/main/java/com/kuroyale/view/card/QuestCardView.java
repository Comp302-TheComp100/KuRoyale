package com.kuroyale.view.card;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/*Custom View component for a Quest Card.
 * Encapsulates the visual representation of a quest.*/
public class QuestCardView extends VBox {

    public interface ClaimListener {
        void onClaim();
    }

    public QuestCardView(String description, String progress, int reward, boolean completed, boolean claimed,
            ClaimListener listener) {
        super(10);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setMaxWidth(600);

        // Dynamic styling based on state
        String bgColor = completed ? "rgba(16, 185, 129, 0.3)" : "rgba(0, 0, 0, 0.4)";
        String borderColor = completed ? "#10b981" : "#fbbf24";

        this.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 20;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-width: 2;");

        // Quest description
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        // Progress section
        HBox progressBox = new HBox(15);
        progressBox.setAlignment(Pos.CENTER_LEFT);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);

        // Parse progress string (e.g., "5/10")
        try {
            String[] parts = progress.split("/");
            double progressValue = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            progressBar.setProgress(progressValue);
        } catch (Exception e) {
            progressBar.setProgress(0);
        }
        progressBar.setStyle("-fx-accent: " + (completed ? "#10b981" : "#fbbf24") + ";");

        Label progressLabel = new Label(progress);
        progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.8);");

        progressBox.getChildren().addAll(progressBar, progressLabel);

        // Reward section
        HBox rewardBox = new HBox(10);
        rewardBox.setAlignment(Pos.CENTER_LEFT);

        Label rewardLabel = new Label(reward + " Gold");
        rewardLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #fbbf24; -fx-font-weight: bold;");

        if (completed && !claimed) {
            Button claimButton = new Button("CLAIM");
            claimButton.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #10b981, #059669);" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 8 20;" +
                            "-fx-background-radius: 8;" +
                            "-fx-cursor: hand;");
            claimButton.setOnAction(e -> {
                if (listener != null)
                    listener.onClaim();
            });
            rewardBox.getChildren().addAll(rewardLabel, claimButton);
        } else if (claimed) {
            Label claimedLabel = new Label("CLAIMED");
            claimedLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            rewardBox.getChildren().addAll(rewardLabel, claimedLabel);
        } else {
            rewardBox.getChildren().add(rewardLabel);
        }

        this.getChildren().addAll(descLabel, progressBox, rewardBox);
    }
}
