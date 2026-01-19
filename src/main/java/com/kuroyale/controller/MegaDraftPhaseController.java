package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.CardCatalog;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.entities.User;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.draft.MegaDraftView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller for the Mega Draft Phase screen.
 * Manages the mega draft card selection process where all cards are shown
 * and players alternate picking cards with the pattern: 1-2-2-2-2-2-2-2-1.
 */
public class MegaDraftPhaseController {

    private static final int DECK_SIZE = 8; // Each player drafts 8 cards

    @FXML
    private AnchorPane root;
    @FXML
    private StackPane draftContainer;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final CardCatalog cardCatalog = new CardCatalog();
    private final Random random = new Random();

    private List<Card> allCards;
    private Set<Card> pickedCards;
    private Deck playerDeck;
    private Deck botDeck;
    private MegaDraftView draftView;

    // Draft state
    private boolean isPlayerFirstPicker;
    private boolean currentPickerIsPlayer;
    private int picksRemainingThisTurn;
    private int totalPlayerPicks;
    private int totalBotPicks;

    // Pick pattern: First picker gets 1, then alternating 2s, last picker gets 1
    // Pattern for first picker: 1, 2, 2, 2, 1 = 8 cards
    // Pattern for second picker: 2, 2, 2, 2 = 8 cards
    private static final int[] FIRST_PICKER_PATTERN = { 1, 2, 2, 2, 1 };
    private static final int[] SECOND_PICKER_PATTERN = { 2, 2, 2, 2 };
    private int firstPickerRound = 0;
    private int secondPickerRound = 0;

    @FXML
    private void initialize() {
        root.getStyleClass().add("main-menu-background");
        startMegaDraft();
    }

    /**
     * Initializes the mega draft phase.
     */
    private void startMegaDraft() {
        // Initialize decks
        playerDeck = new Deck();
        botDeck = new Deck();
        pickedCards = new HashSet<>();
        totalPlayerPicks = 0;
        totalBotPicks = 0;
        firstPickerRound = 0;
        secondPickerRound = 0;

        // Get all available cards
        allCards = new ArrayList<>(cardCatalog.getAllCards());
        Collections.shuffle(allCards);

        // Apply user card levels
        User currentUser = ServiceFactory.getInstance().getAuthenticationService().getCurrentUser();
        if (currentUser != null) {
            cardCatalog.applyUserLevels(currentUser);
        }

        // Randomly determine who picks first
        isPlayerFirstPicker = random.nextBoolean();
        currentPickerIsPlayer = isPlayerFirstPicker;

        System.out.println("[MEGA DRAFT] Starting draft with " + allCards.size() + " cards");
        System.out.println("[MEGA DRAFT] " + (isPlayerFirstPicker ? "PLAYER" : "BOT") + " picks first!");

        // Create the mega draft view
        draftView = new MegaDraftView(allCards);
        draftView.setOnCardSelected(this::handlePlayerPick);
        draftView.setOnTimeout(this::handleTimeout);
        draftView.setupDraft(isPlayerFirstPicker);

        draftContainer.getChildren().add(draftView);
        StackPane.setAlignment(draftView, Pos.CENTER);

        // Start first turn
        startNextTurn();
    }

    /**
     * Determines picks remaining for current picker and starts their turn.
     */
    private void startNextTurn() {
        // Check if draft is complete
        if (totalPlayerPicks >= DECK_SIZE && totalBotPicks >= DECK_SIZE) {
            completeDraft();
            return;
        }

        // Determine picks for this turn based on pattern
        if (currentPickerIsPlayer == isPlayerFirstPicker) {
            // Current picker is the first picker
            if (firstPickerRound < FIRST_PICKER_PATTERN.length) {
                picksRemainingThisTurn = FIRST_PICKER_PATTERN[firstPickerRound];
            } else {
                picksRemainingThisTurn = 0;
            }
        } else {
            // Current picker is the second picker
            if (secondPickerRound < SECOND_PICKER_PATTERN.length) {
                picksRemainingThisTurn = SECOND_PICKER_PATTERN[secondPickerRound];
            } else {
                picksRemainingThisTurn = 0;
            }
        }

        // If no picks remaining for current player, switch or complete
        if (picksRemainingThisTurn <= 0) {
            if (totalPlayerPicks < DECK_SIZE || totalBotPicks < DECK_SIZE) {
                switchPicker();
                startNextTurn();
            } else {
                completeDraft();
            }
            return;
        }

        System.out.println("[MEGA DRAFT] " + (currentPickerIsPlayer ? "PLAYER" : "BOT") +
                "'s turn - " + picksRemainingThisTurn + " pick(s)");

        // Update UI
        draftView.setTurn(currentPickerIsPlayer, picksRemainingThisTurn);

        // If bot's turn, handle bot pick with delay
        if (!currentPickerIsPlayer) {
            handleBotTurn();
        }
    }

    /**
     * Handles a player's card pick.
     */
    private void handlePlayerPick(Card chosenCard) {
        if (!currentPickerIsPlayer || pickedCards.contains(chosenCard)) {
            return;
        }

        SoundEffectUtil.playButtonClick();
        pickCard(chosenCard, true);

        // Check if player has more picks this turn
        picksRemainingThisTurn--;
        totalPlayerPicks++;

        if (picksRemainingThisTurn > 0) {
            // Player picks again
            draftView.setTurn(true, picksRemainingThisTurn);
        } else {
            // Advance round counter for first picker
            if (isPlayerFirstPicker) {
                firstPickerRound++;
            } else {
                secondPickerRound++;
            }

            // Switch to bot
            switchPicker();

            // Small delay before bot picks
            PauseTransition delay = new PauseTransition(Duration.millis(500));
            delay.setOnFinished(e -> startNextTurn());
            delay.play();
        }
    }

    /**
     * Handles bot's turn - bot picks random available cards.
     */
    private void handleBotTurn() {
        // Add delay for each bot pick
        PauseTransition pickDelay = new PauseTransition(Duration.millis(800));
        pickDelay.setOnFinished(e -> {
            List<Card> availableCards = getAvailableCards();
            if (availableCards.isEmpty()) {
                completeDraft();
                return;
            }

            // Bot picks random card
            Card botChoice = availableCards.get(random.nextInt(availableCards.size()));
            pickCard(botChoice, false);
            picksRemainingThisTurn--;
            totalBotPicks++;

            System.out.println("[MEGA DRAFT] Bot picked: " + botChoice.getName());

            if (picksRemainingThisTurn > 0) {
                // Bot picks again
                handleBotTurn();
            } else {
                // Advance round counter for second picker
                if (!isPlayerFirstPicker) {
                    firstPickerRound++;
                } else {
                    secondPickerRound++;
                }

                // Switch to player
                switchPicker();
                startNextTurn();
            }
        });
        pickDelay.play();
    }

    /**
     * Marks a card as picked and adds to appropriate deck.
     */
    private void pickCard(Card card, boolean isPlayerPick) {
        pickedCards.add(card);
        draftView.markCardAsPicked(card, isPlayerPick);

        if (isPlayerPick) {
            playerDeck.addCard(card);
        } else {
            botDeck.addCard(card);
        }
    }

    /**
     * Returns list of cards not yet picked.
     */
    private List<Card> getAvailableCards() {
        List<Card> available = new ArrayList<>();
        for (Card card : allCards) {
            if (!pickedCards.contains(card)) {
                available.add(card);
            }
        }
        return available;
    }

    /**
     * Switches the current picker.
     */
    private void switchPicker() {
        currentPickerIsPlayer = !currentPickerIsPlayer;
    }

    /**
     * Called when player runs out of time - auto-pick random card.
     */
    private void handleTimeout() {
        System.out.println("[MEGA DRAFT] Timeout! Auto-picking...");

        List<Card> availableCards = getAvailableCards();
        if (!availableCards.isEmpty()) {
            Card randomCard = availableCards.get(random.nextInt(availableCards.size()));
            handlePlayerPick(randomCard);
        }
    }

    /**
     * Completes the draft and starts the battle.
     */
    private void completeDraft() {
        draftView.cleanup();
        draftView.showDraftComplete();

        System.out.println("[MEGA DRAFT] Complete!");
        System.out.println("[MEGA DRAFT] Player deck (" + playerDeck.size() + "): " + playerDeck.getCardNames());
        System.out.println("[MEGA DRAFT] Bot deck (" + botDeck.size() + "): " + botDeck.getCardNames());

        // Delay before starting battle
        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
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
