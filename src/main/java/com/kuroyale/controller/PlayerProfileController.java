package com.kuroyale.controller;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.PlayerStats;
import com.kuroyale.model.entities.User;
import com.kuroyale.model.enums.Rarity;
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.service.game.PlayerStatsService;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.card.CardView;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/* Controller for the Player Profile screen.
 * Displays player stats, gold, and card progression.*/
public class PlayerProfileController {

    @FXML
    private AnchorPane root;
    @FXML
    private Button backButton;
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label goldLabel;
    @FXML
    private Label totalMatchesLabel;
    @FXML
    private Label winsLabel;
    @FXML
    private Label lossesLabel;
    @FXML
    private Label winRateLabel;
    @FXML
    private Label bestStreakLabel;
    @FXML
    private Label towersDestroyedLabel;
    @FXML
    private Label totalGoldSpentLabel;
    @FXML
    private Label collectionProgressLabel;
    @FXML
    private GridPane cardsGrid;
    @FXML
    private ScrollPane cardsScrollPane;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final AuthenticationService authService = ServiceFactory.getInstance().getAuthenticationService();
    private final PlayerStatsService statsService = ServiceFactory.getInstance().getPlayerStatsService();

    private static final int CARDS_PER_ROW = 5;

    @FXML
    private void initialize() {
        initializeStyles();
        loadPlayerData();
    }

    private void initializeStyles() {
        if (backButton != null) {
            // Add hover effects similar to main menu
            backButton.getStyleClass().add("back-button");
            addButtonHoverEffects(backButton);
        }
    }

    private void loadPlayerData() {
        User user = authService.getCurrentUser();
        if (user == null)
            return;

        // Load stats
        statsService.loadForUser(user.getUsername());
        PlayerStats stats = statsService.getStats();

        // Populate User Info
        playerNameLabel.setText(user.getUsername());
        goldLabel.setText(String.format("%,d", user.getGold()));

        // Populate Stats
        totalMatchesLabel.setText(String.valueOf(stats.getTotalMatches()));
        winsLabel.setText(String.valueOf(stats.getTotalWins()));
        int losses = stats.getTotalMatches() - stats.getTotalWins(); // Simple calc, ignoring draws if any
        lossesLabel.setText(String.valueOf(Math.max(0, losses)));

        if (stats.getTotalMatches() > 0) {
            double winRate = (double) stats.getTotalWins() / stats.getTotalMatches() * 100.0;
            winRateLabel.setText(String.format(Locale.ENGLISH, "%.1f%%", winRate));
        } else {
            winRateLabel.setText("0%");
        }

        bestStreakLabel.setText(String.valueOf(stats.getBestWinStreak()));
        towersDestroyedLabel.setText(String.valueOf(stats.getTowersDestroyed()));

        // Calculate Total Gold Spent and populate Cards
        loadCardsAndCalculateSpent(user);
    }

    private void loadCardsAndCalculateSpent(User user) {
        List<Card> allCards = ServiceFactory.getInstance().getCardCatalog().getAllCards();
        long totalSpent = 0;

        int col = 0;
        int row = 0;

        for (Card baseCard : allCards) {
            // Create a copy or use factory to get card with user's level
            // CardCatalog has a helper for this usually, or we just set level on a copy
            int level = user.getCardLevel(baseCard.getName());

            // Calculate spent logic: sum upgrades from 1 to current level
            totalSpent += calculateCumulativeCost(baseCard.getRarity(), level);

            // Create CardView
            // We need to display the card with its level. created card copy.
            Card displayCard = ServiceFactory.getInstance().getCardCatalog().createCardWithLevel(baseCard.getName(),
                    level);
            if (displayCard == null)
                displayCard = baseCard; // Fallback

            CardView cardView = new CardView(displayCard);
            // Scale it down slightly if needed
            double baseScale = 0.85;
            cardView.setScaleX(baseScale);
            cardView.setScaleY(baseScale);

            // Disable default CardView hover scaling (which resets to 1.0)
            cardView.setHoverScalingEnabled(false);

            // Add custom hover scaling that respects the base scale
            cardView.setOnMouseEntered(e -> {
                cardView.setScaleX(baseScale * 1.1);
                cardView.setScaleY(baseScale * 1.1);
            });
            cardView.setOnMouseExited(e -> {
                cardView.setScaleX(baseScale);
                cardView.setScaleY(baseScale);
            });

            // Wrap in VBox to handle scaling spacing
            VBox wrapper = new VBox(cardView);
            wrapper.setAlignment(Pos.CENTER);
            wrapper.setPrefSize(100, 130);

            cardsGrid.add(wrapper, col, row);
            col++;
            if (col >= CARDS_PER_ROW) {
                col = 0;
                row++;
            }
        }

        totalGoldSpentLabel.setText(String.format("%,d", totalSpent));
        collectionProgressLabel.setText(String.format("Found: %d/%d", allCards.size(), allCards.size())); // Assuming
                                                                                                          // all 28
                                                                                                          // available
    }

    // Calculates cost to reach a certain level from level 1
    private int calculateCumulativeCost(Rarity rarity, int targetLevel) {
        if (targetLevel <= 1)
            return 0;

        int total = 0;
        // Sum costs for each step: 1->2, 2->3, ... (targetLevel-1)->targetLevel
        for (int i = 1; i < targetLevel; i++) {
            total += getUpgradeCost(rarity, i);
        }
        return total;
    }

    private int getUpgradeCost(Rarity rarity, int currentLevel) {
        // Logic duplicated from Card.calculateUpgradeCost or similar
        // Based on provided Card logic earlier:
        // MAX_LEVEL = 3
        if (currentLevel >= 3)
            return 0;

        switch (rarity) {
            case COMMON:
                return currentLevel == 1 ? 200 : 500;
            case RARE:
                return currentLevel == 1 ? 400 : 1000;
            case EPIC:
                return currentLevel == 1 ? 800 : 2000;
            case LEGENDARY:
                return currentLevel == 1 ? 1500 : 4000;
            default:
                return 0;
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
