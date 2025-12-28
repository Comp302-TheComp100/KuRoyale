package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.entities.Achievement;
import com.kuroyale.model.entities.Quest;
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

    private void addQuestCard(String description, String progress, int reward, boolean completed, boolean claimed,
            String questId) {
        com.kuroyale.view.QuestCardView card = new com.kuroyale.view.QuestCardView(
                description, progress, reward, completed, claimed, () -> handleClaimQuest(questId));
        questCardsContainer.getChildren().add(card);
    }

    /**
     * Handles claiming a quest reward.
     */
    private void handleClaimQuest(String questId) {
        SoundEffectUtil.playButtonClick();
        int reward = questService.claimReward(questId);
        if (reward > 0) {
            com.kuroyale.model.entities.User currentUser = authService.getCurrentUser();
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

    private void addAchievementCard(String name, String description, int progress, int target, int reward,
            boolean unlocked, boolean claimed, com.kuroyale.model.enums.AchievementType type) {
        com.kuroyale.view.AchievementCardView card = new com.kuroyale.view.AchievementCardView(
                name, description, progress, target, reward, unlocked, claimed, () -> handleClaimAchievement(type));
        achievementsContainer.getChildren().add(card);
    }

    /**
     * Handles claiming an achievement reward.
     */
    private void handleClaimAchievement(com.kuroyale.model.enums.AchievementType type) {
        SoundEffectUtil.playButtonClick();
        int reward = achievementService.claimReward(type);
        if (reward > 0) {
            com.kuroyale.model.entities.User currentUser = authService.getCurrentUser();
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
