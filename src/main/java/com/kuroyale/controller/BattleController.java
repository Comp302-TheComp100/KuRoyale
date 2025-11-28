package com.kuroyale.controller;

import com.kuroyale.model.*;
import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.DeckManagementService;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.view.BattleArenaView;
import com.kuroyale.view.ElixirBar;
import com.kuroyale.view.HandView;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Battle screen.
 * Manages the game loop, user input, and UI updates.
 */
public class BattleController {

    @FXML
    private StackPane arenaContainer;
    @FXML
    private HBox elixirContainer;
    @FXML
    private VBox handContainer;
    @FXML
    private VBox overlayContainer;

    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop;

    private final AuthenticationService authService;
    private final ArenaService arenaService;
    private final DeckManagementService deckService;
    private final com.kuroyale.service.CardCatalog cardCatalog;

    public BattleController() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.arenaService = factory.getArenaService();
        this.deckService = factory.getDeckManagementService();
        this.cardCatalog = factory.getCardCatalog();
    }

    @FXML
    public void initialize() {
        // Initialize game state
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            // Should not happen if flow is correct, but handle gracefully
            handleExit();
            return;
        }

        // Set current user in arena service to load their saved layout
        arenaService.setCurrentUser(currentUser);

        // Load user data
        // Convert List<String> to Deck object
        Deck playerDeck = createDeckFromNames(currentUser.getDeck());
        ArenaLayout playerLayout = arenaService.loadArenaLayout(); // Load saved layout

        // Create Arena
        Arena arena = arenaService.createArena(playerLayout);

        // Create Bot Deck (Random or fixed)
        Deck botDeck = createBotDeck();

        // Initialize GameState
        gameState = new GameState(playerDeck, botDeck, arena);

        // Initialize UI Components
        arenaView = new BattleArenaView(gameState);
        arenaContainer.getChildren().add(arenaView);

        // Handle clicks on arena for card placement
        arenaView.setOnGridClicked((tileX, tileY) -> {
            handleArenaClick(tileX, tileY);
        });

        elixirBar = new ElixirBar(gameState.getPlayerElixir());
        elixirContainer.getChildren().add(elixirBar);

        handView = new HandView(gameState.getPlayerHand(), gameState.getPlayerElixir());
        handView.setOnCardSelected(index -> {
            if (index != -1) {
                arenaView.highlightValidCells(true);
            } else {
                arenaView.highlightValidCells(false);
            }
        });
        handContainer.getChildren().add(handView);

        // Start Game Loop
        startGameLoop();
    }

    private Deck createDeckFromNames(java.util.List<String> cardNames) {
        Deck deck = new Deck();
        if (cardNames != null) {
            for (String name : cardNames) {
                Card card = cardCatalog.getCardByName(name);
                if (card != null) {
                    deck.addCard(card);
                }
            }
        }
        return deck;
    }

    private Deck createBotDeck() {
        // Create a simple deck for bot
        // For MVP, just use the player's deck (mirror match)
        return createDeckFromNames(authService.getCurrentUser().getDeck());
    }

    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                update(deltaTime);
            }
        };
        gameLoop.start();
    }

    private boolean doubleElixirShown = false;
    private boolean gameOverShown = false;

    private void update(double deltaTime) {
        // Update Game Logic
        gameState.update(deltaTime);

        // Update UI
        elixirBar.update();
        handView.update();
        arenaView.update();

        // Check for Double Elixir (track state but don't show popup)
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            // Visual indicators are handled by ElixirBar and BattleArenaView
            elixirBar.setDoubleElixirActive(true);
        }

        // Check for Game Over
        if (gameState.isGameOver() && !gameOverShown) {
            gameOverShown = true;
            gameLoop.stop();
            System.out.println("Game Over! Showing popup...");
            showGameOverPopup();
        }
    }

    private void showGameOverPopup() {
        // Clear overlay and make it visible
        overlayContainer.getChildren().clear();
        overlayContainer.setVisible(true);

        VBox content = new VBox(20);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle(
                "-fx-background-color: #333; -fx-padding: 40; -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 2;");
        content.setMaxSize(400, 300);

        javafx.scene.control.Label title = new javafx.scene.control.Label("MATCH ENDED");
        title.setStyle("-fx-font-size: 36px; -fx-text-fill: white; -fx-font-weight: bold;");

        javafx.scene.control.Label score = new javafx.scene.control.Label(
                String.format("Player: %d  -  Bot: %d", gameState.getPlayerScore(), gameState.getBotScore()));
        score.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        javafx.scene.control.Button exitBtn = new javafx.scene.control.Button("EXIT");
        exitBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 30;");
        exitBtn.setOnAction(e -> handleExit());

        content.getChildren().addAll(title, score, exitBtn);

        overlayContainer.getChildren().add(content);
    }

    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex != -1) {
            // Validate bounds
            if (tileX >= 0 && tileX < Arena.WIDTH && tileY >= 0 && tileY < Arena.HEIGHT) {
                // Try to place card
                if (gameState.placeCard(true, selectedIndex, tileX, tileY)) {
                    // Success
                    handView.clearSelection();
                    arenaView.highlightValidCells(false);
                } else {
                    // Failed (not enough elixir, invalid position, etc.)
                    // Feedback?
                }
            }
        }
    }

    @FXML
    public void handleExit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) arenaContainer.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Main Menu");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
