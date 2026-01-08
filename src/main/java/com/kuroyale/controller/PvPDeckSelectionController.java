package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.entities.User;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.CardCatalog;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;
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
 * Both players must select their decks before the battle begins.
 * Uses MVC pattern (GRASP Controller).
 */
public class PvPDeckSelectionController {

    @FXML
    private AnchorPane root;
    @FXML
    private Label titleLabel;
    @FXML
    private VBox player1Panel;
    @FXML
    private VBox player2Panel;
    @FXML
    private ComboBox<String> player1DeckCombo;
    @FXML
    private ComboBox<String> player2DeckCombo;
    @FXML
    private GridPane player1DeckGrid;
    @FXML
    private GridPane player2DeckGrid;
    @FXML
    private Label player1ReadyLabel;
    @FXML
    private Label player2ReadyLabel;
    @FXML
    private Button backButton;
    @FXML
    private Button startBattleButton;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final CardCatalog cardCatalog = ServiceFactory.getInstance().getCardCatalog();
    private final AuthenticationService authService = ServiceFactory.getInstance().getAuthenticationService();

    private Deck player1Deck;
    private Deck player2Deck;
    private List<String> availableDeckNames;

    @FXML
    private void initialize() {
        initializeStyles();
        loadAvailableDecks();
        setupDeckSelectionListeners();
    }

    private void initializeStyles() {
        root.getStyleClass().add("main-menu-background");

        if (titleLabel != null) {
            titleLabel.getStyleClass().add("title-label");
        }

        // Initially disable start button
        startBattleButton.setDisable(true);
    }

    private void loadAvailableDecks() {
        // For simplicity, we'll use the current user's deck for selection
        // In a full implementation, you might have multiple saved decks
        availableDeckNames = List.of("Current Deck", "Random Deck");

        player1DeckCombo.setItems(FXCollections.observableArrayList(availableDeckNames));
        player2DeckCombo.setItems(FXCollections.observableArrayList(availableDeckNames));

        // Set default selection
        player1DeckCombo.getSelectionModel().selectFirst();
        player2DeckCombo.getSelectionModel().selectFirst();

        // Load initial decks
        updatePlayer1Deck();
        updatePlayer2Deck();
    }

    private void setupDeckSelectionListeners() {
        player1DeckCombo.setOnAction(e -> updatePlayer1Deck());
        player2DeckCombo.setOnAction(e -> updatePlayer2Deck());
    }

    private void updatePlayer1Deck() {
        String selected = player1DeckCombo.getValue();
        if (selected == null)
            return;

        player1Deck = loadDeckByName(selected, 1);
        displayDeckInGrid(player1Deck, player1DeckGrid);
        updateReadyState();
    }

    private void updatePlayer2Deck() {
        String selected = player2DeckCombo.getValue();
        if (selected == null)
            return;

        player2Deck = loadDeckByName(selected, 2);
        displayDeckInGrid(player2Deck, player2DeckGrid);
        updateReadyState();
    }

    private Deck loadDeckByName(String name, int playerNumber) {
        User currentUser = authService.getCurrentUser();

        if ("Current Deck".equals(name) && currentUser != null) {
            // Load the user's current deck
            List<String> cardNames = currentUser.getDeck();
            Deck deck = new Deck();
            for (String cardName : cardNames) {
                if (cardName != null && !cardName.isEmpty()) {
                    int level = currentUser.getCardLevel(cardName);
                    Card card = cardCatalog.createCardWithLevel(cardName, level);
                    if (card != null) {
                        deck.addCard(card);
                    }
                }
            }
            return deck;
        } else {
            // Generate a random deck
            return generateRandomDeck();
        }
    }

    private Deck generateRandomDeck() {
        Deck deck = new Deck();
        List<Card> allCards = new ArrayList<>(cardCatalog.getAllCards());
        Collections.shuffle(allCards);

        for (int i = 0; i < Math.min(8, allCards.size()); i++) {
            deck.addCard(allCards.get(i));
        }
        return deck;
    }

    private void displayDeckInGrid(Deck deck, GridPane grid) {
        grid.getChildren().clear();

        if (deck == null)
            return;

        List<Card> cards = deck.getCards();
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (card != null) {
                CardView cardView = new CardView(card);
                cardView.setPrefWidth(50);
                cardView.setPrefHeight(65);

                int col = i % 4;
                int row = i / 4;
                grid.add(cardView, col, row);
            }
        }
    }

    private void updateReadyState() {
        boolean p1Ready = player1Deck != null && player1Deck.getCards().size() >= 8;
        boolean p2Ready = player2Deck != null && player2Deck.getCards().size() >= 8;

        // Update ready labels
        player1ReadyLabel.setText(p1Ready ? "READY!" : "NOT READY");
        player1ReadyLabel.setStyle(p1Ready ? "-fx-text-fill: #22c55e; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        player2ReadyLabel.setText(p2Ready ? "READY!" : "NOT READY");
        player2ReadyLabel.setStyle(p2Ready ? "-fx-text-fill: #22c55e; -fx-font-weight: bold;"
                : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");

        // Enable start button only if both players are ready
        startBattleButton.setDisable(!(p1Ready && p2Ready));
    }

    @FXML
    private void handleStartBattle() {
        SoundEffectUtil.playButtonClick();

        if (player1Deck == null || player2Deck == null) {
            showError("Both players must select a deck!");
            return;
        }

        // Navigate to PvP battle with both decks
        try {
            sceneLoader.load(root, "/fxml/pvp-battle.fxml", "KU Royale - PvP Battle", controller -> {
                if (controller instanceof PvPBattleController pvpController) {
                    pvpController.initializeGame(player1Deck, player2Deck);
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
