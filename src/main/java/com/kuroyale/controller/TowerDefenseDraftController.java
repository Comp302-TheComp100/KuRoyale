package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.CardCatalog;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.draft.TowerDefenseDraftView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Controller for Tower Defense Draft Phase.
 * Generates random computer attack waves and manages mega draft picking
 * for both players.
 */
public class TowerDefenseDraftController {

    private static final int MIN_ATTACK_ELIXIR = 15;
    private static final int MAX_ATTACK_ELIXIR = 35;
    // No deck size limit - players can pick as many cards as they want

    @FXML
    private AnchorPane root;
    @FXML
    private StackPane draftContainer;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final CardCatalog cardCatalog = new CardCatalog();
    private final Random random = new Random();

    // Game state
    private List<Card> attackWave;
    private int attackWaveElixir;
    private List<Card> allCards;
    private List<Card> player1Cards = new ArrayList<>(); // Use List instead of Deck for unlimited cards
    private List<Card> player2Cards = new ArrayList<>();
    private boolean isPlayer1Turn = true;
    private boolean player1Ready = false;
    private boolean player2Ready = false;
    private int currentRound = 1;
    private int player1Score = 0;
    private int player2Score = 0;

    private TowerDefenseDraftView draftView;

    @FXML
    private void initialize() {
        root.getStyleClass().add("main-menu-background");
        startNewRound();
    }

    /**
     * Starts a new round by generating attack wave and resetting drafts.
     */
    private void startNewRound() {
        // Reset card lists
        player1Cards = new ArrayList<>();
        player2Cards = new ArrayList<>();
        player1Ready = false;
        player2Ready = false;
        isPlayer1Turn = true;

        // Generate attack wave (troops only, 15-35 elixir)
        generateAttackWave();

        // Get all available cards for drafting
        allCards = new ArrayList<>(cardCatalog.getAllCards());
        Collections.shuffle(allCards);

        // Create the draft view
        draftView = new TowerDefenseDraftView(allCards, attackWave, currentRound, player1Score, player2Score);
        draftView.setOnCardSelected(this::handleCardSelection);
        draftView.setOnPlayerReady(this::handlePlayerReady);
        draftView.setOnBack(this::handleBack);
        draftView.setCurrentPlayer(1);

        draftContainer.getChildren().clear();
        draftContainer.getChildren().add(draftView);
        StackPane.setAlignment(draftView, Pos.CENTER);
    }

    /**
     * Generates random attack wave with troops only.
     */
    private void generateAttackWave() {
        attackWave = new ArrayList<>();
        int targetElixir = MIN_ATTACK_ELIXIR + random.nextInt(MAX_ATTACK_ELIXIR - MIN_ATTACK_ELIXIR + 1);
        attackWaveElixir = 0;

        // Get all troop cards
        List<Card> troops = new ArrayList<>();
        for (Card card : cardCatalog.getAllCards()) {
            if (card.getType() == CardType.TROOP) {
                troops.add(card);
            }
        }

        // Build attack wave until we reach target elixir
        while (attackWaveElixir < targetElixir && !troops.isEmpty()) {
            Card randomTroop = troops.get(random.nextInt(troops.size()));
            if (attackWaveElixir + randomTroop.getCost() <= targetElixir + 2) {
                attackWave.add(randomTroop);
                attackWaveElixir += randomTroop.getCost();
            }
            // Prevent infinite loop
            if (attackWaveElixir >= targetElixir - 1)
                break;
        }

        // Sort by elixir cost (descending) for display
        attackWave.sort((a, b) -> Integer.compare(b.getCost(), a.getCost()));

        System.out.println("[TOWER DEFENSE] Generated attack wave: " + attackWaveElixir + " elixir, "
                + attackWave.size() + " cards");
    }

    /**
     * Handles card selection during draft.
     * Alternates turns between players after each pick.
     */
    private void handleCardSelection(Card card) {
        SoundEffectUtil.playButtonClick();

        List<Card> currentCards = isPlayer1Turn ? player1Cards : player2Cards;

        // Check if this player is allowed to pick (hasn't clicked ready yet)
        if ((isPlayer1Turn && player1Ready) || (!isPlayer1Turn && player2Ready)) {
            return; // Player already confirmed deck
        }

        currentCards.add(card);
        draftView.markCardAsPicked(card, isPlayer1Turn);
        draftView.updatePlayerCards(isPlayer1Turn ? 1 : 2, currentCards);

        System.out.println("[TOWER DEFENSE] Player " + (isPlayer1Turn ? "1" : "2") + " picked: " + card.getName()
                + " (" + currentCards.size() + " cards)");

        // Switch to other player's turn (alternating picks)
        switchTurn();
    }

    /**
     * Switches turn to the other player.
     * Handles cases where one player's deck is full.
     */
    private void switchTurn() {
        // If both decks have cards, don't switch
        if (player1Ready && player2Ready) {
            return;
        }

        // Toggle turn
        isPlayer1Turn = !isPlayer1Turn;

        // If new player already confirmed, switch back
        if (isPlayer1Turn && player1Ready) {
            isPlayer1Turn = false;
        } else if (!isPlayer1Turn && player2Ready) {
            isPlayer1Turn = true;
        }

        // Update UI
        draftView.setCurrentPlayer(isPlayer1Turn ? 1 : 2);
        System.out.println("[TOWER DEFENSE] Now Player " + (isPlayer1Turn ? "1" : "2") + "'s turn");
    }

    /**
     * Handles player ready button click.
     */
    private void handlePlayerReady(int player) {
        SoundEffectUtil.playButtonClick();

        if (player == 1) {
            player1Ready = true;
            isPlayer1Turn = false;
            draftView.setCurrentPlayer(2);
            System.out.println("[TOWER DEFENSE] Player 1 is ready!");
        } else {
            player2Ready = true;
            System.out.println("[TOWER DEFENSE] Player 2 is ready!");
        }

        // If both players are ready, start the battle
        if (player1Ready && player2Ready) {
            startBattle();
        }
    }

    /**
     * Starts the tower defense battle.
     */
    private void startBattle() {
        System.out.println("[TOWER DEFENSE] Both players ready! Starting battle...");
        System.out.println("[TOWER DEFENSE] Player 1 cards: " + player1Cards.size());
        System.out.println("[TOWER DEFENSE] Player 2 cards: " + player2Cards.size());

        // Show transition message
        draftView.showBattleStarting();

        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
        delay.setOnFinished(e -> loadBattleScreen());
        delay.play();
    }

    /**
     * Loads the tower defense battle screen.
     */
    private void loadBattleScreen() {
        // Convert Lists to Decks for battle (Deck has max 8 limit, but battle
        // controller will use all cards)
        Deck p1Deck = new Deck();
        Deck p2Deck = new Deck();
        // Add up to 8 cards (Deck limit) - the rest will be handled differently if
        // needed
        for (int i = 0; i < Math.min(8, player1Cards.size()); i++) {
            p1Deck.addCard(player1Cards.get(i));
        }
        for (int i = 0; i < Math.min(8, player2Cards.size()); i++) {
            p2Deck.addCard(player2Cards.get(i));
        }

        try {
            sceneLoader.load(root, "/fxml/tower-defense-battle.fxml", "KU Royale - Tower Defense", controller -> {
                if (controller instanceof TowerDefenseBattleController battleController) {
                    battleController.setupBattle(
                            attackWave,
                            p1Deck,
                            p2Deck,
                            currentRound,
                            player1Score,
                            player2Score);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            com.kuroyale.util.ui.ThemedAlertManager.show("Error", "Failed to start battle: " + e.getMessage());
        }
    }

    /**
     * Handles back button click.
     */
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(root, "/fxml/pvp-mode-selection.fxml", "KU Royale - PvP Mode", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Card> getAttackWave() {
        return attackWave;
    }

    public int getAttackWaveElixir() {
        return attackWaveElixir;
    }

    /**
     * Sets scores for continuing from a previous round.
     */
    public void setScores(int round, int p1Score, int p2Score) {
        this.currentRound = round;
        this.player1Score = p1Score;
        this.player2Score = p2Score;

        // Regenerate the view with updated scores
        draftView = new TowerDefenseDraftView(allCards, attackWave, currentRound, player1Score, player2Score);
        draftView.setOnCardSelected(this::handleCardSelection);
        draftView.setOnPlayerReady(this::handlePlayerReady);
        draftView.setOnBack(this::handleBack);
        draftView.setCurrentPlayer(1);

        draftContainer.getChildren().clear();
        draftContainer.getChildren().add(draftView);
        StackPane.setAlignment(draftView, Pos.CENTER);
    }
}
