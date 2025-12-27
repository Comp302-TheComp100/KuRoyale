package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.Achievement;
import com.kuroyale.model.Quest;
import com.kuroyale.service.AchievementService;
import com.kuroyale.service.QuestService;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the Quests & Achievements screen.
 * Manages daily quests and permanent achievements display.
 */
public class QuestAchievementController {

    @FXML
    private AnchorPane root;
    @FXML
    private Button backButton;
    @FXML
    private Label titleLabel;
    @FXML
    private TabPane tabPane;
    @FXML
    private VBox questsContainer;
    @FXML
    private VBox questCardsContainer;
    @FXML
    private Label resetTimerLabel;
    @FXML
    private VBox achievementsContainer;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final QuestService questService = ServiceFactory.getInstance().getQuestService();
    private final AchievementService achievementService = ServiceFactory.getInstance().getAchievementService();
    private final com.kuroyale.service.AuthenticationService authService = ServiceFactory.getInstance()
            .getAuthenticationService();
    private Timeline timerTimeline;

    @FXML
    private void initialize() {
        initializeStyles();
        loadDailyQuests();
        loadAchievements();
        startResetTimer();
    }

    private void initializeStyles() {
        root.getStyleClass().add("main-menu-background");
        if (backButton != null) {
            addButtonHoverEffects(backButton);
        }
    }

    /**
     * Loads the 3 daily quests and displays them.
     */
    private void loadDailyQuests() {
        questCardsContainer.getChildren().clear();

        List<Quest> quests = questService.getDailyQuests();
        for (Quest quest : quests) {
            addQuestCard(
                    quest.getDescription(),
                    quest.getProgressText(),
                    quest.getGoldReward(),
                    quest.isCompleted(),
                    quest.isClaimed(),
                    quest.getId());
        }
    }

    /**
     * Adds a quest card to the display.
     */
    private void addQuestCard(String description, String progress, int reward, boolean completed, boolean claimed,
            String questId) {
        VBox card = new VBox(10);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setMaxWidth(600);
        card.setStyle(
                "-fx-background-color: " + (completed ? "rgba(16, 185, 129, 0.3)" : "rgba(0, 0, 0, 0.4)") + ";" +
                        "-fx-background-radius: 15;" +
                        "-fx-padding: 20;" +
                        "-fx-border-color: " + (completed ? "#10b981" : "#fbbf24") + ";" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-width: 2;");

        // Quest description
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

        // Progress section
        javafx.scene.layout.HBox progressBox = new javafx.scene.layout.HBox(15);
        progressBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(20);

        // Parse progress
        String[] parts = progress.split("/");
        double progressValue = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        progressBar.setProgress(progressValue);
        progressBar.setStyle("-fx-accent: " + (completed ? "#10b981" : "#fbbf24") + ";");

        Label progressLabel = new Label(progress);
        progressLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.8);");

        progressBox.getChildren().addAll(progressBar, progressLabel);

        // Reward section
        javafx.scene.layout.HBox rewardBox = new javafx.scene.layout.HBox(10);
        rewardBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label rewardLabel = new Label("🪙 " + reward + " Gold");
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
            claimButton.setOnAction(e -> handleClaimQuest(questId));
            addButtonHoverEffects(claimButton);
            rewardBox.getChildren().addAll(rewardLabel, claimButton);
        } else if (claimed) {
            Label claimedLabel = new Label("CLAIMED");
            claimedLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            rewardBox.getChildren().addAll(rewardLabel, claimedLabel);
        } else {
            rewardBox.getChildren().add(rewardLabel);
        }

        card.getChildren().addAll(descLabel, progressBox, rewardBox);
        questCardsContainer.getChildren().add(card);
    }

    /**
     * Handles claiming a quest reward.
     */
    private void handleClaimQuest(String questId) {
        SoundEffectUtil.playButtonClick();
        int reward = questService.claimReward(questId);
        if (reward > 0) {
            com.kuroyale.model.User currentUser = authService.getCurrentUser();
            if (currentUser != null) {
                currentUser.setGold(currentUser.getGold() + reward);
                try {
                    authService.saveCurrentUser();
                    System.out.println("Claimed quest for " + reward + " gold. New balance: " + currentUser.getGold());
                } catch (IOException e) {
                    System.err.println("Failed to save user data after claiming quest: " + e.getMessage());
                }
            }
        }
        loadDailyQuests(); // Refresh
    }

    /**
     * Loads all achievements and displays them.
     */
    private void loadAchievements() {
        achievementsContainer.getChildren().clear();

        List<Achievement> achievements = achievementService.getAllAchievements();
        for (Achievement achievement : achievements) {
            String name = achievement.isUnlocked() ? achievement.getName() : "???";
            String desc = achievement.getDescription();
            addAchievementCard(
                    name,
                    desc,
                    achievement.getProgress(),
                    achievement.getTarget(),
                    achievement.getGoldReward(),
                    achievement.isUnlocked(),
                    achievement.isClaimed(),
                    achievement.getType());
        }
    }

    /**
     * Adds an achievement card to the display.
     */
    private void addAchievementCard(String name, String description, int progress, int target, int reward,
            boolean unlocked, boolean claimed, com.kuroyale.model.AchievementType type) {
        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(20);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setMaxWidth(700);

        String bgColor = claimed ? "rgba(16, 185, 129, 0.3)"
                : unlocked ? "rgba(251, 191, 36, 0.3)" : "rgba(0, 0, 0, 0.3)";
        String borderColor = claimed ? "#10b981" : unlocked ? "#fbbf24" : "#64748b";

        card.setStyle(
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
        details.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.layout.HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);

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
        VBox rewardBox = new VBox(5);
        rewardBox.setAlignment(javafx.geometry.Pos.CENTER);

        if (reward > 0) {
            Label rewardLabel = new Label("🪙 " + reward);
            rewardLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #fbbf24; -fx-font-weight: bold;");
            rewardBox.getChildren().add(rewardLabel);

            if (unlocked && !claimed) {
                Button claimBtn = new Button("CLAIM");
                claimBtn.setStyle(
                        "-fx-background-color: #10b981;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 12px;" +
                                "-fx-padding: 5 15;" +
                                "-fx-background-radius: 5;" +
                                "-fx-cursor: hand;");
                claimBtn.setOnAction(e -> handleClaimAchievement(type));
                addButtonHoverEffects(claimBtn);
                rewardBox.getChildren().add(claimBtn);
            }
        }

        if (claimed) {
            Label claimedLabel = new Label("CLAIMED");
            claimedLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            rewardBox.getChildren().add(claimedLabel);
        }

        card.getChildren().addAll(iconLabel, details, rewardBox);
        achievementsContainer.getChildren().add(card);
    }

    /**
     * Handles claiming an achievement reward.
     */
    private void handleClaimAchievement(com.kuroyale.model.AchievementType type) {
        SoundEffectUtil.playButtonClick();
        int reward = achievementService.claimReward(type);
        if (reward > 0) {
            com.kuroyale.model.User currentUser = authService.getCurrentUser();
            if (currentUser != null) {
                currentUser.setGold(currentUser.getGold() + reward);
                try {
                    authService.saveCurrentUser();
                    System.out.println(
                            "Claimed achievement for " + reward + " gold. New balance: " + currentUser.getGold());
                } catch (IOException e) {
                    System.err.println("Failed to save user data after claiming achievement: " + e.getMessage());
                }
            }
        }
        loadAchievements(); // Refresh
    }

    /**
     * Starts the countdown timer for daily reset.
     */
    private void startResetTimer() {
        // Update timer immediately
        resetTimerLabel.setText(questService.getTimeUntilReset());

        // Update every second
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            resetTimerLabel.setText(questService.getTimeUntilReset());
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
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
