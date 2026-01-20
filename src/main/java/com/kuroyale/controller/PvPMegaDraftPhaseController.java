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

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

/**
 * Controller for Pvp Mega Draft Phase.
 * Two players draft on the same screen (Hotseat style).
 */
public class PvPMegaDraftPhaseController {

    private static final int DECK_SIZE = 8;

    @FXML
    private AnchorPane root;
    @FXML
    private StackPane draftContainer;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final CardCatalog cardCatalog = new CardCatalog();
    private final Random random = new Random();

    private List<Card> allCards;
    private Set<Card> pickedCards;
    private Deck player1Deck;
    private Deck player2Deck;
    private MegaDraftView draftView;

    // Draft state
    private boolean isPlayer1FirstPicker;
    private boolean currentPickerIsPlayer1;
    private int picksRemainingThisTurn;
    private int totalP1Picks;
    private int totalP2Picks;

    // Pick pattern: 1, 2, 2...
    private static final int[] FIRST_PICKER_PATTERN = { 1, 2, 2, 2, 1 };
    private static final int[] SECOND_PICKER_PATTERN = { 2, 2, 2, 2 };
    private int firstPickerRound = 0;
    private int secondPickerRound = 0;

    @FXML
    private void initialize() {
        root.getStyleClass().add("main-menu-background");
        startMegaDraft();
    }

    private void startMegaDraft() {
        player1Deck = new Deck();
        player2Deck = new Deck();
        pickedCards = new HashSet<>();
        totalP1Picks = 0;
        totalP2Picks = 0;
        firstPickerRound = 0;
        secondPickerRound = 0;

        allCards = new ArrayList<>(cardCatalog.getAllCards());
        Collections.shuffle(allCards);

        // Apply levels (using logged in user for P1 for now)
        User currentUser = ServiceFactory.getInstance().getAuthenticationService().getCurrentUser();
        if (currentUser != null) {
            cardCatalog.applyUserLevels(currentUser);
        }

        // Randomize first picker
        isPlayer1FirstPicker = random.nextBoolean();
        currentPickerIsPlayer1 = isPlayer1FirstPicker;

        // Initialize view
        // Note: MegaDraftView might be designed for "Player vs Bot" text.
        // We'll rely on setTurn(isPlayer, ...) to update text.
        // Ideally MegaDraftView should support "Player 1 vs Player 2".
        // For now, isPlayer=true -> Player 1, isPlayer=false -> Player 2
        draftView = new MegaDraftView(allCards);
        draftView.setOnCardSelected(this::handleCardPick);
        draftView.setOnTimeout(this::handleTimeout);
        draftView.setupDraft(isPlayer1FirstPicker);

        draftContainer.getChildren().add(draftView);
        StackPane.setAlignment(draftView, Pos.CENTER);

        // Customize for PvP
        draftView.setTitles("PLAYER 1", "PLAYER 2");

        startNextTurn();
    }

    private void startNextTurn() {
        if (totalP1Picks >= DECK_SIZE && totalP2Picks >= DECK_SIZE) {
            completeDraft();
            return;
        }

        // Determine picks
        if (currentPickerIsPlayer1 == isPlayer1FirstPicker) {
            if (firstPickerRound < FIRST_PICKER_PATTERN.length) {
                picksRemainingThisTurn = FIRST_PICKER_PATTERN[firstPickerRound];
            } else {
                picksRemainingThisTurn = 0;
            }
        } else {
            if (secondPickerRound < SECOND_PICKER_PATTERN.length) {
                picksRemainingThisTurn = SECOND_PICKER_PATTERN[secondPickerRound];
            } else {
                picksRemainingThisTurn = 0;
            }
        }

        if (picksRemainingThisTurn <= 0) {
            if (totalP1Picks < DECK_SIZE || totalP2Picks < DECK_SIZE) {
                switchPicker();
                startNextTurn();
            } else {
                completeDraft();
            }
            return;
        }

        // Update UI
        String playerName = currentPickerIsPlayer1 ? "PLAYER 1" : "PLAYER 2";
        draftView.setHotseatTurn(playerName, currentPickerIsPlayer1, picksRemainingThisTurn);
    }

    private void handleCardPick(Card chosenCard) {
        if (pickedCards.contains(chosenCard)) {
            return;
        }

        SoundEffectUtil.playButtonClick();
        pickCard(chosenCard, currentPickerIsPlayer1);

        picksRemainingThisTurn--;
        if (currentPickerIsPlayer1)
            totalP1Picks++;
        else
            totalP2Picks++;

        if (picksRemainingThisTurn > 0) {
            String playerName = currentPickerIsPlayer1 ? "PLAYER 1" : "PLAYER 2";
            draftView.setHotseatTurn(playerName, currentPickerIsPlayer1, picksRemainingThisTurn);
        } else {
            // End of turn logic
            if (currentPickerIsPlayer1 == isPlayer1FirstPicker) {
                firstPickerRound++;
            } else {
                secondPickerRound++;
            }
            switchPicker();

            // Small delay between turns for visual clarity
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(500));
            delay.setOnFinished(e -> startNextTurn());
            delay.play();
        }
    }

    private void pickCard(Card card, boolean isP1) {
        pickedCards.add(card);
        draftView.markCardAsPicked(card, isP1);

        if (isP1) {
            player1Deck.addCard(card);
        } else {
            player2Deck.addCard(card);
        }
    }

    private void switchPicker() {
        currentPickerIsPlayer1 = !currentPickerIsPlayer1;
    }

    private void handleTimeout() {
        // Auto pick random
        List<Card> available = new ArrayList<>();
        for (Card c : allCards) {
            if (!pickedCards.contains(c))
                available.add(c);
        }
        if (!available.isEmpty()) {
            Card c = available.get(random.nextInt(available.size()));
            handleCardPick(c);
        }
    }

    private void completeDraft() {
        draftView.cleanup();
        draftView.showDraftComplete();

        // Start PvP Battle
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.5));
        delay.setOnFinished(e -> startPvPBattle());
        delay.play();
    }

    private void startPvPBattle() {
        try {
            sceneLoader.load(root, "/fxml/pvp-battle.fxml", "KU Royale - PvP Battle", controller -> {
                if (controller instanceof PvPBattleController pvbController) {
                    pvbController.initializeGame(player1Deck, player2Deck);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            com.kuroyale.util.ui.ThemedAlertManager.show("Error", "Failed to start battle: " + e.getMessage());
        }
    }
}
