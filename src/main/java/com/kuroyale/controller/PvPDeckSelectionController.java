package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Controller for PvP deck selection screen.
 * Both players must select their decks before the battle begins.
 * Supports "Current Deck", "Random Deck", and "Build Deck" options.
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
    @FXML
    private ScrollPane player1LibraryPane;
    @FXML
    private ScrollPane player2LibraryPane;
    @FXML
    private GridPane player1LibraryGrid;
    @FXML
    private GridPane player2LibraryGrid;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final CardCatalog cardCatalog = ServiceFactory.getInstance().getCardCatalog();
    private final AuthenticationService authService = ServiceFactory.getInstance().getAuthenticationService();

    private Deck player1Deck;
    private Deck player2Deck;
    private List<String> availableDeckNames;

    // Track selected cards when building decks
    private Set<String> player1SelectedCardNames = new HashSet<>();
    private Set<String> player2SelectedCardNames = new HashSet<>();

    private static final String BUILD_DECK = "Build Deck";
    private static final String CURRENT_DECK = "Current Deck";
    private static final String RANDOM_DECK = "Random Deck";

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

        // Initially hide library panes
        if (player1LibraryPane != null) {
            player1LibraryPane.setVisible(false);
            player1LibraryPane.setManaged(false);
        }
        if (player2LibraryPane != null) {
            player2LibraryPane.setVisible(false);
            player2LibraryPane.setManaged(false);
        }
    }

    private void loadAvailableDecks() {
        // Offer three options: Current Deck, Random Deck, and Build Deck
        availableDeckNames = List.of(CURRENT_DECK, RANDOM_DECK, BUILD_DECK);

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

        if (BUILD_DECK.equals(selected)) {
            // Show library and enter build mode
            player1SelectedCardNames.clear();
            player1Deck = new Deck();
            showLibrary(1, true);
            loadLibraryCards(1);
        } else {
            // Hide library and load preset deck
            showLibrary(1, false);
            player1Deck = loadDeckByName(selected, 1);
        }

        displayDeckInGrid(player1Deck, player1DeckGrid);
        updateReadyState();
    }

    private void updatePlayer2Deck() {
        String selected = player2DeckCombo.getValue();
        if (selected == null)
            return;

        if (BUILD_DECK.equals(selected)) {
            // Show library and enter build mode
            player2SelectedCardNames.clear();
            player2Deck = new Deck();
            showLibrary(2, true);
            loadLibraryCards(2);
        } else {
            // Hide library and load preset deck
            showLibrary(2, false);
            player2Deck = loadDeckByName(selected, 2);
        }

        displayDeckInGrid(player2Deck, player2DeckGrid);
        updateReadyState();
    }

    private void showLibrary(int playerNumber, boolean show) {
        ScrollPane libraryPane = playerNumber == 1 ? player1LibraryPane : player2LibraryPane;
        if (libraryPane != null) {
            libraryPane.setVisible(show);
            libraryPane.setManaged(show);
        }
    }

    private void loadLibraryCards(int playerNumber) {
        GridPane libraryGrid = playerNumber == 1 ? player1LibraryGrid : player2LibraryGrid;
        Set<String> selectedCards = playerNumber == 1 ? player1SelectedCardNames : player2SelectedCardNames;

        if (libraryGrid == null)
            return;

        libraryGrid.getChildren().clear();

        List<Card> allCards = new ArrayList<>(cardCatalog.getAllCards());

        int col = 0;
        int row = 0;
        for (Card card : allCards) {
            if (card != null) {
                CardView cardView = new CardView(card);
                cardView.setPrefWidth(50);
                cardView.setPrefHeight(65);

                // Style based on selection state
                updateCardViewStyle(cardView, selectedCards.contains(card.getName()));

                // Click handler for card selection
                final String cardName = card.getName();
                cardView.setOnMouseClicked(event -> {
                    SoundEffectUtil.playButtonClick();
                    handleLibraryCardClick(playerNumber, cardName);
                });

                libraryGrid.add(cardView, col, row);
                col++;
                if (col >= 4) {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private void handleLibraryCardClick(int playerNumber, String cardName) {
        Set<String> selectedCards = playerNumber == 1 ? player1SelectedCardNames : player2SelectedCardNames;
        Deck deck = playerNumber == 1 ? player1Deck : player2Deck;
        GridPane deckGrid = playerNumber == 1 ? player1DeckGrid : player2DeckGrid;

        if (selectedCards.contains(cardName)) {
            // Remove card from selection
            selectedCards.remove(cardName);
            // Rebuild deck from selected cards
            rebuildDeck(playerNumber);
        } else {
            // Add card if not at max
            if (selectedCards.size() < 8) {
                selectedCards.add(cardName);
                // Add card to deck
                Card card = cardCatalog.getCardByName(cardName);
                if (card != null) {
                    // Create a copy with default level
                    Card cardCopy = cardCatalog.createCardWithLevel(cardName, 1);
                    if (cardCopy != null) {
                        deck.addCard(cardCopy);
                    }
                }
            }
        }

        // Update displays
        displayDeckInGrid(deck, deckGrid);
        loadLibraryCards(playerNumber); // Refresh library to update selection visuals
        updateReadyState();
    }

    private void rebuildDeck(int playerNumber) {
        Set<String> selectedCards = playerNumber == 1 ? player1SelectedCardNames : player2SelectedCardNames;
        Deck deck = new Deck();

        for (String cardName : selectedCards) {
            Card card = cardCatalog.createCardWithLevel(cardName, 1);
            if (card != null) {
                deck.addCard(card);
            }
        }

        if (playerNumber == 1) {
            player1Deck = deck;
        } else {
            player2Deck = deck;
        }
    }

    private void updateCardViewStyle(CardView cardView, boolean isSelected) {
        if (isSelected) {
            cardView.setStyle("-fx-opacity: 0.5; -fx-border-color: #22c55e; -fx-border-width: 2;");
        } else {
            cardView.setStyle("-fx-opacity: 1.0;");
        }
    }

    private Deck loadDeckByName(String name, int playerNumber) {
        User currentUser = authService.getCurrentUser();

        if (CURRENT_DECK.equals(name) && currentUser != null) {
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
