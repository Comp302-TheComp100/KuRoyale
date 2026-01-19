package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.CardCatalog;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.entities.User;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.draft.DraftCardChoiceView;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

/**
 * Controller for the Draft Phase screen.
 * Manages the card drafting process before battle starts.
 */
public class DraftPhaseController {

    private static final int DRAFT_ROUNDS = 4; // Each player picks 4 cards

    @FXML
    private AnchorPane root;
    @FXML
    private StackPane draftOverlay;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final CardCatalog cardCatalog = new CardCatalog();
    private final Random random = new Random();

    private List<Card> availableCards;
    private Deck playerDeck;
    private Deck botDeck;
    private int currentRound;
    private DraftCardChoiceView choiceView;

    @FXML
    private void initialize() {
        root.getStyleClass().add("main-menu-background");
        startDraft();
    }

    /**
     * Initializes the draft phase.
     */
    private void startDraft() {
        // Initialize decks
        playerDeck = new Deck();
        botDeck = new Deck();
        currentRound = 0;

        // Get all available cards and shuffle
        availableCards = new ArrayList<>(cardCatalog.getAllCards());
        Collections.shuffle(availableCards);

        // Apply user card levels
        User currentUser = ServiceFactory.getInstance().getAuthenticationService().getCurrentUser();
        if (currentUser != null) {
            cardCatalog.applyUserLevels(currentUser);
        }

        // Create draft choice UI
        choiceView = new DraftCardChoiceView();
        choiceView.setOnCardSelected(this::onPlayerChoice);
        choiceView.setOnTimeout(this::onTimeout);

        draftOverlay.getChildren().add(choiceView);
        StackPane.setAlignment(choiceView, Pos.CENTER);

        // Start first round
        nextRound();
    }

    /**
     * Proceeds to the next draft round.
     */
    private void nextRound() {
        currentRound++;

        if (currentRound > DRAFT_ROUNDS) {
            // Draft complete - fill remaining slots with random cards
            fillRemainingSlots();
            completeDraft();
            return;
        }

        // Get two random cards for player
        if (availableCards.size() < 4) {
            // Not enough cards left - complete early
            fillRemainingSlots();
            completeDraft();
            return;
        }

        Card playerCard1 = availableCards.remove(0);
        Card playerCard2 = availableCards.remove(0);

        // Bot simultaneously picks from different pair
        Card botCard1 = availableCards.remove(0);
        Card botCard2 = availableCards.remove(0);

        // Bot picks randomly
        Card botChoice = random.nextBoolean() ? botCard1 : botCard2;
        Card botReject = (botChoice == botCard1) ? botCard2 : botCard1;

        // Add bot's choice to bot deck, rejected card goes to player deck
        botDeck.addCard(botChoice);
        playerDeck.addCard(botReject);

        System.out.println("[DRAFT] Round " + currentRound + ": Bot chose " + botChoice.getName() +
                ", Player gets " + botReject.getName() + " from bot's pair");

        // Present player's choice
        choiceView.presentChoice(playerCard1, playerCard2, currentRound, DRAFT_ROUNDS);
    }

    /**
     * Called when player selects a card.
     */
    private void onPlayerChoice(Card chosenCard) {
        SoundEffectUtil.playButtonClick();

        // Determine which card was rejected
        Card rejectedCard = (chosenCard == choiceView.getLeftCard()) ? choiceView.getRightCard()
                : choiceView.getLeftCard();

        // Player's choice goes to player, rejected goes to bot
        playerDeck.addCard(chosenCard);
        botDeck.addCard(rejectedCard);

        System.out.println("[DRAFT] Round " + currentRound + ": Player chose " + chosenCard.getName() +
                ", Bot gets " + rejectedCard.getName());

        // Delay before next round for animation to complete
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(600));
        delay.setOnFinished(e -> nextRound());
        delay.play();
    }

    /**
     * Called when player runs out of time.
     */
    private void onTimeout() {
        System.out.println("[DRAFT] Timeout! Auto-picking...");

        // Auto-pick left card
        Card autoChoice = choiceView.getLeftCard();
        onPlayerChoice(autoChoice);
    }

    /**
     * Fills remaining deck slots with random cards.
     */
    private void fillRemainingSlots() {
        // Each deck should have 8 cards, fill remaining slots
        while (playerDeck.size() < 8 && !availableCards.isEmpty()) {
            playerDeck.addCard(availableCards.remove(0));
        }
        while (botDeck.size() < 8 && !availableCards.isEmpty()) {
            botDeck.addCard(availableCards.remove(0));
        }

        System.out.println("[DRAFT] Filled remaining slots. Player: " + playerDeck.size() +
                " cards, Bot: " + botDeck.size() + " cards");
    }

    /**
     * Completes the draft and starts the battle.
     */
    private void completeDraft() {
        choiceView.cleanup();

        // Show completion message briefly
        draftOverlay.getChildren().clear();
        Label completeLabel = new Label("DRAFT COMPLETE!");
        completeLabel.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: #22c55e; " +
                "-fx-effect: dropshadow(gaussian, black, 10, 0.5, 0, 2);");
        draftOverlay.getChildren().add(completeLabel);

        System.out.println("[DRAFT] Complete! Starting battle...");
        System.out.println("[DRAFT] Player deck: " + playerDeck.getCardNames());
        System.out.println("[DRAFT] Bot deck: " + botDeck.getCardNames());

        // Delay before starting battle
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.5));
        delay.setOnFinished(e -> startBattle());
        delay.play();
    }

    /**
     * Starts the battle with drafted decks.
     */
    private void startBattle() {
        try {
            sceneLoader.load(root, "/fxml/battle.fxml", "KU Royale - Battle", controller -> {
                if (controller instanceof BattleController battleController) {
                    battleController.setDraftedDecks(playerDeck, botDeck);
                    battleController.startGame();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            com.kuroyale.util.ui.ThemedAlertManager.show("Error", "Failed to start battle: " + e.getMessage());
        }
    }
}
