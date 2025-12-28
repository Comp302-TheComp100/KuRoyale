package com.kuroyale.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Custom View component for an Achievement Card.
 */
public class AchievementCardView extends HBox {

    public interface ClaimListener {
        void onClaim();
    }

    public AchievementCardView(String name, String description, int progress, int target, int reward, boolean unlocked,
            boolean claimed, ClaimListener listener) {
        super(20);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setMaxWidth(700);

        String bgColor = claimed ? "rgba(16, 185, 129, 0.3)"
                : unlocked ? "rgba(251, 191, 36, 0.3)" : "rgba(0, 0, 0, 0.3)";
        String borderColor = claimed ? "#10b981" : unlocked ? "#fbbf24" : "#64748b";

        this.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 15;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-width: 2;");

        // Icon/Status
        Label iconLabel = new Label(claimed ? "✅" : unlocked ? "🏆" : "🔒");
        iconLabel.setStyle("-fx-font-size: 32px;");

        // Details
        VBox details = new VBox(5);
        details.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.7);");

        if (!claimed && unlocked) {
            Label progressLabel = new Label(progress + "/" + target);
            progressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #fbbf24;");
            details.getChildren().addAll(nameLabel, descLabel, progressLabel);
        } else {
            details.getChildren().addAll(nameLabel, descLabel);
        }

        // Reward
        VBox rewardPane = new VBox(5);
        rewardPane.setAlignment(Pos.CENTER);

        if (reward > 0) {
            Label rewardLabel = new Label("🪙 " + reward);
            rewardLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #fbbf24; -fx-font-weight: bold;");
            rewardPane.getChildren().add(rewardLabel);

            if (unlocked && !claimed) {
                Button claimBtn = new Button("CLAIM");
                claimBtn.setStyle(
                        "-fx-background-color: #10b981;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 12px;" +
                                "-fx-padding: 5 15;" +
                                "-fx-background-radius: 5;" +
                                "-fx-cursor: hand;");
                claimBtn.setOnAction(e -> {
                    if (listener != null)
                        listener.onClaim();
                });
                rewardPane.getChildren().add(claimBtn);
            }
        }

        if (claimed) {
            Label claimedLabel = new Label("CLAIMED");
            claimedLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            rewardPane.getChildren().add(claimedLabel);
        }

        this.getChildren().addAll(iconLabel, details, rewardPane);
    }
}
