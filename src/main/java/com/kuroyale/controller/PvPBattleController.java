package com.kuroyale.controller;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ElixirBar;
import com.kuroyale.view.battle.HandView;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for PvP Battle screen.
 * Manages two human players with turn-based gameplay.
 * Uses MVC pattern (GRASP Controller).
 */
public class PvPBattleController {

    @FXML
    private StackPane arenaContainer;
    @FXML
    private VBox player1Container;
    @FXML
    private VBox player2Container;
    @FXML
    private HBox player1ElixirContainer;
    @FXML
    private HBox player2ElixirContainer;
    @FXML
    private VBox player1HandContainer;
    @FXML
    private VBox player2HandContainer;
    @FXML
    private Button endTurnButton;
    @FXML
    private Label timeLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label turnIndicatorLabel;
    @FXML
    private Label turnArrowLabel;
    @FXML
    private VBox pauseMenuContainer;
    @FXML
    private VBox gameOverRoot;
    @FXML
    private Label gameOverTitle;
    @FXML
    private Label gameOverScore;
    @FXML
    private VBox infoPanel;

    private PvPGameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar player1ElixirBar;
    private ElixirBar player2ElixirBar;
    private HandView player1HandView;
    private HandView player2HandView;
    private AnimationTimer gameLoop;
    private boolean isPaused = false;
    private boolean gameOverShown = false;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final BattleModel model = new BattleModel();

    @FXML
    public void initialize() {
        // Actual initialization happens in initializeGame() after decks are set
    }

    /**
     * Initialize the PvP game with both player decks.
     * Called from PvPDeckSelectionController after deck selection.
     */
    public void initializeGame(Deck player1Deck, Deck player2Deck) {
        // Load arena layout (use default for PvP)
        ArenaLayout arenaLayout = model.loadArenaLayout();
        Arena arena = model.createArena(arenaLayout);

        // Create PvP game state
        gameState = new PvPGameState(player1Deck, player2Deck, arena);
        gameState.setCardCatalog(name -> model.getCardByName(name));

        // Initialize arena view
        arenaView = new BattleArenaView(gameState);
        arenaContainer.getChildren().add(arenaView);

        // Set up arena click handlers for both players
        setupArenaClickHandler();

        // Initialize Player 1 UI (left side)
        player1ElixirBar = new ElixirBar(gameState.getPlayer1Elixir());
        player1ElixirContainer.getChildren().add(player1ElixirBar);

        player1HandView = new HandView(gameState.getPlayer1Hand(), gameState.getPlayer1Elixir());
        player1HandView.setOnCardSelected(index -> handlePlayer1CardSelected(index));
        player1HandContainer.getChildren().add(player1HandView);

        // Initialize Player 2 UI (right side)
        player2ElixirBar = new ElixirBar(gameState.getPlayer2Elixir());
        player2ElixirBar.getStyleClass().add("right-side-bar"); // Fix specific to right side
        player2ElixirContainer.getChildren().add(player2ElixirBar);

        player2HandView = new HandView(gameState.getPlayer2Hand(), gameState.getPlayer2Elixir());
        player2HandView.getStyleClass().add("right-side-bar"); // Fix specific to right side
        player2HandView.setOnCardSelected(index -> handlePlayer2CardSelected(index));
        player2HandContainer.getChildren().add(player2HandView);

        // Set up turn change listener
        gameState.getTurnManager().addTurnChangeListener(this::onTurnChanged);

        // Initial UI update
        updateTurnIndicator();
        updateUI();

        // Start game loop
        startGameLoop();
    }

    private void setupArenaClickHandler() {
        arenaView.setOnGridClicked((tileX, tileY) -> {
            handleArenaClick(tileX, tileY);
        });
    }

    private void handlePlayer1CardSelected(int index) {
        // Only allow Player 1 to select cards during their turn
        if (gameState.getTurnManager().getCurrentTurn() != TurnManager.Turn.PLAYER_1) {
            player1HandView.clearSelection();
            return;
        }

        if (index != -1) {
            Card card = gameState.getPlayer1Hand().getCard(index);
            boolean isSpell = (card != null && card.getType() == com.kuroyale.model.enums.CardType.SPELL);
            arenaView.highlightValidCells(true, isSpell);
        } else {
            arenaView.highlightValidCells(false, false);
        }
        // Clear player 2 selection when player 1 selects
        player2HandView.clearSelection();
    }

    private void handlePlayer2CardSelected(int index) {
        // Only allow Player 2 to select cards during their turn
        if (gameState.getTurnManager().getCurrentTurn() != TurnManager.Turn.PLAYER_2) {
            player2HandView.clearSelection();
            return;
        }

        if (index != -1) {
            Card card = gameState.getPlayer2Hand().getCard(index);
            boolean isSpell = (card != null && card.getType() == com.kuroyale.model.enums.CardType.SPELL);
            // For player 2, highlight top half
            arenaView.highlightPlayer2ValidCells(true, isSpell);
        } else {
            arenaView.highlightPlayer2ValidCells(false, false);
        }
        // Clear player 1 selection when player 2 selects
        player1HandView.clearSelection();
    }

    private void handleArenaClick(int tileX, int tileY) {
        TurnManager.Turn currentTurn = gameState.getTurnManager().getCurrentTurn();

        // Check if Player 1 has a selected card AND it's their turn
        int p1SelectedIndex = player1HandView.getSelectedIndex();
        if (p1SelectedIndex != -1 && currentTurn == TurnManager.Turn.PLAYER_1) {
            if (gameState.placeCard(true, p1SelectedIndex, tileX, tileY)) {
                player1HandView.clearSelection();
                arenaView.highlightValidCells(false, false);
            }
            return;
        }

        // Check if Player 2 has a selected card AND it's their turn
        int p2SelectedIndex = player2HandView.getSelectedIndex();
        if (p2SelectedIndex != -1 && currentTurn == TurnManager.Turn.PLAYER_2) {
            if (gameState.placeCard(false, p2SelectedIndex, tileX, tileY)) {
                player2HandView.clearSelection();
                arenaView.highlightPlayer2ValidCells(false, false);
            }
        }
    }

    @FXML
    private void handleEndTurn() {
        gameState.getTurnManager().endTurn();
    }

    private void onTurnChanged(TurnManager.Turn newTurn) {
        updateTurnIndicator();
    }

    private void updateTurnIndicator() {
        TurnManager.Turn currentTurn = gameState.getTurnManager().getCurrentTurn();

        if (currentTurn == TurnManager.Turn.PLAYER_1) {
            turnIndicatorLabel.setText("P1 TURN");
            turnIndicatorLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 20px; -fx-font-weight: bold;");
            turnArrowLabel.setText("◀");
            turnArrowLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 24px;");

            // Highlight P1 panel with border, keep blue background
            player1Container.setStyle(
                    "-fx-padding: 20; -fx-background-color: rgba(30, 60, 120, 0.5); -fx-border-color: #3b82f6; -fx-border-width: 3;");
            player2Container.setStyle("-fx-padding: 20; -fx-background-color: rgba(120, 30, 30, 0.3);");
        } else {
            turnIndicatorLabel.setText("P2 TURN");
            turnIndicatorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 20px; -fx-font-weight: bold;");
            turnArrowLabel.setText("▶");
            turnArrowLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 24px;");

            // Highlight P2 panel with border, keep red background
            player1Container.setStyle("-fx-padding: 20; -fx-background-color: rgba(30, 60, 120, 0.3);");
            player2Container.setStyle(
                    "-fx-padding: 20; -fx-background-color: rgba(120, 30, 30, 0.5); -fx-border-color: #ef4444; -fx-border-width: 3;");
        }
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

    private void update(double deltaTime) {
        if (isPaused)
            return;

        // Update game logic
        gameState.update(deltaTime);

        // Update UI
        updateUI();

        // Check for game over
        if (gameState.isGameOver() && !gameOverShown) {
            gameOverShown = true;
            gameLoop.stop();
            showGameOverPopup();
        }
    }

    private void updateUI() {
        // Update elixir bars
        player1ElixirBar.update();
        player2ElixirBar.update();

        // Update hands
        player1HandView.update();
        player2HandView.update();

        // Update arena (use PvP update method)
        arenaView.updatePvP(0);

        // Update time display
        int seconds = (int) gameState.getGameTime();
        int mins = seconds / 60;
        int secs = seconds % 60;
        timeLabel.setText(String.format("%02d:%02d", mins, secs));

        // Update score display
        scoreLabel.setText(String.format("%d - %d", gameState.getPlayer1Score(), gameState.getPlayer2Score()));

        // Update double elixir indicators
        if (gameState.isDoubleElixir()) {
            player1ElixirBar.setDoubleElixirActive(true);
            player2ElixirBar.setDoubleElixirActive(true);
        }
    }

    private void showGameOverPopup() {
        gameOverRoot.setVisible(true);

        TurnManager.Turn winner = gameState.getWinner();
        if (winner == TurnManager.Turn.PLAYER_1) {
            gameOverTitle.setText("PLAYER 1 WINS!");
            gameOverTitle.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 48px; -fx-font-weight: bold;");
        } else if (winner == TurnManager.Turn.PLAYER_2) {
            gameOverTitle.setText("PLAYER 2 WINS!");
            gameOverTitle.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 48px; -fx-font-weight: bold;");
        } else {
            gameOverTitle.setText("DRAW!");
            gameOverTitle.setStyle("-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: bold;");
        }

        gameOverScore.setText(String.format("Player 1: %d - Player 2: %d",
                gameState.getPlayer1Score(), gameState.getPlayer2Score()));
    }

    @FXML
    public void handlePause() {
        if (gameOverShown)
            return;
        isPaused = true;
        showPauseMenu();
    }

    private void handleResume() {
        isPaused = false;
        pauseMenuContainer.setVisible(false);
        pauseMenuContainer.getChildren().clear();
    }

    private void showPauseMenu() {
        pauseMenuContainer.getChildren().clear();
        pauseMenuContainer.setVisible(true);

        com.kuroyale.view.battle.PauseMenuView menu = new com.kuroyale.view.battle.PauseMenuView(
                new com.kuroyale.view.battle.PauseMenuView.PauseMenuListener() {
                    @Override
                    public void onResume() {
                        handleResume();
                    }

                    @Override
                    public void onSaveAndResume() {
                        handleResume();
                    }

                    @Override
                    public void onSaveAndExit() {
                        handleExit();
                    }

                    @Override
                    public void onExitWithoutSaving() {
                        handleExit();
                    }
                });

        pauseMenuContainer.getChildren().add(menu);
    }

    @FXML
    public void handleExit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        try {
            sceneLoader.load(arenaContainer, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
