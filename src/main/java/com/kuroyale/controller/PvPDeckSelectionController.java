package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.state.PvPDeckBuilderSession;
import com.kuroyale.model.strategy.CustomDeckStrategy;
import com.kuroyale.model.strategy.DeckBuildingStrategy;
import com.kuroyale.model.strategy.RandomDeckStrategy;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.view.CardView;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Controller for PvP deck selection screen.
 * Both players can select deck source and build custom decks.
 * 
 * Design Patterns:
 * - Strategy Pattern: DeckBuildingStrategy for different deck sources
 * - Memento Pattern: PvPDeckBuilderSession for state across navigation
 * - GRASP Controller: Coordinates UI and model
 */
public class PvPDeckSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<String> player1DeckCombo;
    @FXML
    private ComboBox<String> player2DeckCombo;
    @FXML
    private VBox player1DeckContainer;
    @FXML
    private VBox player2DeckContainer;
    @FXML
    private GridPane player1DeckGrid;
    @FXML
    private GridPane player2DeckGrid;
    @FXML
    private Button player1BuildButton;
    @FXML
    private Button player2BuildButton;
    @FXML
    private Button player1RandomizeButton;
    @FXML
    private Button player2RandomizeButton;
    @FXML
    private Label player1ReadyLabel;
    @FXML
    private Label player2ReadyLabel;
    @FXML
    private Button backButton;
    @FXML
    private Button startBattleButton;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final PvPDeckBuilderSession session = PvPDeckBuilderSession.getInstance();

    private DeckBuildingStrategy player1Strategy;
    private DeckBuildingStrategy player2Strategy;
    private Deck player1Deck;
    private Deck player2Deck;

    @FXML
    private void initialize() {
        initializeStyles();
        loadDeckOptions();
        setupDeckSelectionListeners();

        // Start PvP session
        session.startSession();

        // Restore any previously built decks
        restoreDecksFromSession();
    }

    private void initializeStyles() {
        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }
        startBattleButton.setDisable(true);
    }

    private void loadDeckOptions() {
        var deckOptions = FXCollections.observableArrayList(
                "Build Deck",
                "Random Deck");

        player1DeckCombo.setItems(deckOptions);
        player2DeckCombo.setItems(deckOptions);

        player1DeckCombo.getSelectionModel().selectFirst();
        player2DeckCombo.getSelectionModel().selectFirst();

        updatePlayer1Selection();
        updatePlayer2Selection();
    }

    private void setupDeckSelectionListeners() {
        player1DeckCombo.setOnAction(e -> updatePlayer1Selection());
        player2DeckCombo.setOnAction(e -> updatePlayer2Selection());
    }

    private void restoreDecksFromSession() {
        // Restore P1 deck if exists
        if (session.getPlayer1Deck() != null && session.getPlayer1Deck().isValid()) {
            player1Deck = session.getPlayer1Deck();
            displayDeckInGrid(player1Deck, player1DeckGrid);
            player1DeckCombo.getSelectionModel().select("Build Deck");
        }

        // Restore P2 deck if exists
        if (session.getPlayer2Deck() != null && session.getPlayer2Deck().isValid()) {
            player2Deck = session.getPlayer2Deck();
            displayDeckInGrid(player2Deck, player2DeckGrid);
            player2DeckCombo.getSelectionModel().select("Build Deck");
        }

        updateReadyState();
    }

    private DeckBuildingStrategy createStrategy(String selected) {
        switch (selected) {
            case "Random Deck":
                return new RandomDeckStrategy();
            case "Build Deck":
            default:
                return new CustomDeckStrategy();
        }
    }

    private void updatePlayer1Selection() {
        String selected = player1DeckCombo.getValue();
        if (selected == null)
            return;

        player1Strategy = createStrategy(selected);

        if (player1Strategy.requiresUserInput()) {
            // Show BUILD button, hide randomize, hide/clear grid unless we have a saved
            // deck
            showBuildButton(1);
            hideRandomizeButton(1);
            if (session.getPlayer1Deck() != null && session.getPlayer1Deck().isValid()) {
                player1Deck = session.getPlayer1Deck();
                displayDeckInGrid(player1Deck, player1DeckGrid);
            } else {
                player1DeckGrid.getChildren().clear();
                player1Deck = null;
            }
        } else {
            // Use strategy to build deck (Random Deck)
            hideBuildButton(1);
            showRandomizeButton(1);
            player1Deck = player1Strategy.buildDeck();
            session.setPlayer1Deck(player1Deck);
            displayDeckInGrid(player1Deck, player1DeckGrid);
        }

        updateReadyState();
    }

    private void updatePlayer2Selection() {
        String selected = player2DeckCombo.getValue();
        if (selected == null)
            return;

        player2Strategy = createStrategy(selected);

        if (player2Strategy.requiresUserInput()) {
            showBuildButton(2);
            hideRandomizeButton(2);
            if (session.getPlayer2Deck() != null && session.getPlayer2Deck().isValid()) {
                player2Deck = session.getPlayer2Deck();
                displayDeckInGrid(player2Deck, player2DeckGrid);
            } else {
                player2DeckGrid.getChildren().clear();
                player2Deck = null;
            }
        } else {
            hideBuildButton(2);
            showRandomizeButton(2);
            player2Deck = player2Strategy.buildDeck();
            session.setPlayer2Deck(player2Deck);
            displayDeckInGrid(player2Deck, player2DeckGrid);
        }

        updateReadyState();
    }

    private void showBuildButton(int player) {
        if (player == 1 && player1BuildButton != null) {
            player1BuildButton.setVisible(true);
            player1BuildButton.setManaged(true);
        } else if (player == 2 && player2BuildButton != null) {
            player2BuildButton.setVisible(true);
            player2BuildButton.setManaged(true);
        }
    }

    private void hideBuildButton(int player) {
        if (player == 1 && player1BuildButton != null) {
            player1BuildButton.setVisible(false);
            player1BuildButton.setManaged(false);
        } else if (player == 2 && player2BuildButton != null) {
            player2BuildButton.setVisible(false);
            player2BuildButton.setManaged(false);
        }
    }

    private void showRandomizeButton(int player) {
        if (player == 1 && player1RandomizeButton != null) {
            player1RandomizeButton.setVisible(true);
            player1RandomizeButton.setManaged(true);
        } else if (player == 2 && player2RandomizeButton != null) {
            player2RandomizeButton.setVisible(true);
            player2RandomizeButton.setManaged(true);
        }
    }

    private void hideRandomizeButton(int player) {
        if (player == 1 && player1RandomizeButton != null) {
            player1RandomizeButton.setVisible(false);
            player1RandomizeButton.setManaged(false);
        } else if (player == 2 && player2RandomizeButton != null) {
            player2RandomizeButton.setVisible(false);
            player2RandomizeButton.setManaged(false);
        }
    }

    @FXML
    private void handlePlayer1Build() {
        SoundEffectUtil.playButtonClick();
        session.setActivePlayer(1);
        navigateToDeckBuilder();
    }

    @FXML
    private void handlePlayer2Build() {
        SoundEffectUtil.playButtonClick();
        session.setActivePlayer(2);
        navigateToDeckBuilder();
    }

    @FXML
    private void handlePlayer1Randomize() {
        SoundEffectUtil.playButtonClick();
        // Generate a new random deck for player 1
        player1Strategy = new RandomDeckStrategy();
        player1Deck = player1Strategy.buildDeck();
        session.setPlayer1Deck(player1Deck);
        displayDeckInGrid(player1Deck, player1DeckGrid);
        updateReadyState();
    }

    @FXML
    private void handlePlayer2Randomize() {
        SoundEffectUtil.playButtonClick();
        // Generate a new random deck for player 2
        player2Strategy = new RandomDeckStrategy();
        player2Deck = player2Strategy.buildDeck();
        session.setPlayer2Deck(player2Deck);
        displayDeckInGrid(player2Deck, player2DeckGrid);
        updateReadyState();
    }

    private void navigateToDeckBuilder() {
        try {
            sceneLoader.load(root, "/fxml/deck-builder.fxml", "KU Royale - Build Deck", controller -> {
                if (controller instanceof DeckBuilderController deckController) {
                    deckController.setPvPMode(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to open deck builder: " + e.getMessage());
        }
    }

    private void displayDeckInGrid(Deck deck, GridPane grid) {
        if (grid == null)
            return;
        grid.getChildren().clear();

        if (deck == null)
            return;

        var cards = deck.getCards();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card != null) {
                CardView cardView = new CardView(card);
                cardView.setPrefWidth(60);
                cardView.setPrefHeight(80);

                int col = i % 4;
                int row = i / 4;
                grid.add(cardView, col, row);
            }
        }
    }

    private void updateReadyState() {
        boolean p1Ready = player1Deck != null && player1Deck.isValid();
        boolean p2Ready = player2Deck != null && player2Deck.isValid();

        player1ReadyLabel.setText(p1Ready ? "READY!" : "NOT READY");
        player1ReadyLabel.setStyle(p1Ready ? "-fx-text-fill: #22c55e; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        player2ReadyLabel.setText(p2Ready ? "READY!" : "NOT READY");
        player2ReadyLabel.setStyle(p2Ready ? "-fx-text-fill: #22c55e; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        startBattleButton.setDisable(!(p1Ready && p2Ready));
    }

    @FXML
    private void handleStartBattle() {
        SoundEffectUtil.playButtonClick();

        if (player1Deck == null || player2Deck == null) {
            showError("Both players must have a deck!");
            return;
        }

        // End session before battle
        session.endSession();

        try {
            final Deck p1 = player1Deck;
            final Deck p2 = player2Deck;
            sceneLoader.load(root, "/fxml/pvp-battle.fxml", "KU Royale - PvP Battle", controller -> {
                if (controller instanceof PvPBattleController pvpController) {
                    pvpController.initializeGame(p1, p2);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to start battle: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        session.endSession();
        try {
            sceneLoader.load(backButton, "/fxml/battle-mode-selection.fxml", "KU Royale - Select Battle Mode", null);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
