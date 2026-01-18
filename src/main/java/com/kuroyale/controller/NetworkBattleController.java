package com.kuroyale.controller;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.dto.NetworkGameStateSnapshot;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.service.NetworkService;
import com.kuroyale.service.NetworkService.ConnectionState;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ElixirBar;
import com.kuroyale.view.battle.HandView;
import com.kuroyale.view.battle.PauseMenuView;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * NETWORK BATTLE CONTROLLER
 * 
 * Works EXACTLY like the offline BattleController, with network sync added:
 * - Same AnimationTimer game loop
 * - Same gameState.update(deltaTime) call
 * - Same UI updates
 * 
 * Network additions:
 * - HOST broadcasts state to CLIENT periodically
 * - CLIENT applies state from HOST (tower health sync)
 * - Card placements are sent over network
 */
public class NetworkBattleController {

    // FXML Components
    @FXML
    private StackPane arenaContainer;
    @FXML
    private HBox elixirContainer;
    @FXML
    private VBox handContainer;
    @FXML
    private VBox pauseMenuContainer;
    @FXML
    private Label timeLabel;

    // Network status indicators
    @FXML
    private Circle connectionIndicator;
    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Label pingLabel;
    @FXML
    private Label lagIndicator;

    // Score display
    @FXML
    private Label playerNameLabel;
    @FXML
    private Label playerScoreLabel;
    @FXML
    private Label opponentNameLabel;
    @FXML
    private Label opponentScoreLabel;

    // Overlays
    @FXML
    private VBox disconnectionOverlay;
    @FXML
    private Label disconnectionLabel;
    @FXML
    private Label reconnectingLabel;
    @FXML
    private Label reconnectCountdownLabel;
    @FXML
    private VBox resultOverlay;
    @FXML
    private Label resultLabel;
    @FXML
    private Label resultDetailsLabel;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final BattleModel model = new BattleModel();

    // Core components - SAME as offline
    private NetworkService networkService;
    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop; // SAME game loop as offline

    // State tracking
    private boolean isPaused = false;
    private boolean gameEnded = false;
    private boolean doubleElixirShown = false;

    // Network sync
    private Timeline syncTimer;
    private static final int SYNC_INTERVAL_MS = 100; // Sync every 100ms

    @FXML
    private void initialize() {
        // Initial setup - network service will be set via setter
    }

    /**
     * Sets the network service and starts the game.
     */
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
        setupNetworkCallbacks();
        initializeGame();
        startGame();
    }

    private void setupNetworkCallbacks() {
        // Message handler - runs on JavaFX thread
        networkService.setOnMessageReceived(message -> Platform.runLater(() -> handleNetworkMessage(message)));

        networkService.setOnStateChanged(state -> Platform.runLater(() -> {
            updateConnectionStatus(state);

            if (state == ConnectionState.RECONNECTING) {
                showDisconnectionOverlay();
                isPaused = true;
            } else if (state == ConnectionState.CONNECTED && disconnectionOverlay != null
                    && disconnectionOverlay.isVisible()) {
                hideDisconnectionOverlay();
                isPaused = false;
            } else if (state == ConnectionState.DISCONNECTED && !gameEnded) {
                handleOpponentDisconnected();
            }
        }));
    }

    private void initializeGame() {
        // Set player names
        String myName = networkService.getPlayerName();
        String oppName = networkService.getOpponentName();
        if (playerNameLabel != null)
            playerNameLabel.setText(myName != null ? myName : "You");
        if (opponentNameLabel != null)
            opponentNameLabel.setText(oppName != null ? oppName : "Opponent");

        // Get current user
        User currentUser = model.getCurrentUser();
        if (currentUser == null) {
            handleBackToMenu();
            return;
        }

        model.setCurrentUserInArenaService(currentUser);

        // Create deck
        Deck playerDeck = model.createDeckFromNames(currentUser.getDeck());

        // Get arena layout (host's layout for consistency)
        ArenaLayout layoutToUse;
        if (!networkService.isHost() && networkService.getHostArenaLayout() != null) {
            layoutToUse = networkService.getHostArenaLayout();
            System.out.println("[NetworkBattle] CLIENT: Using HOST's arena layout");
        } else {
            layoutToUse = model.loadArenaLayout();
            System.out.println("[NetworkBattle] HOST: Using own arena layout");
        }

        // Create Arena and GameState - SAME as offline
        Arena arena = model.createArena(layoutToUse);
        Deck opponentDeck = model.createBotDeck(currentUser);
        gameState = new GameState(playerDeck, opponentDeck, arena);
        gameState.setCardCatalog(name -> model.getCardByName(name));
        gameState.setNetworkMode(true); // Disables bot AI

        // Initialize UI - SAME as offline
        arenaView = new BattleArenaView(gameState);
        arenaContainer.getChildren().add(arenaView);

        arenaView.setOnGridClicked((tileX, tileY) -> handleArenaClick(tileX, tileY));

        elixirBar = new ElixirBar(gameState.getPlayerElixir());
        elixirContainer.getChildren().add(elixirBar);

        handView = new HandView(gameState.getPlayerHand(), gameState.getPlayerElixir());
        handView.setOnCardSelected(index -> {
            if (index != -1) {
                Card card = gameState.getPlayerHand().getCard(index);
                boolean isSpell = (card != null && card.getType() == CardType.SPELL);
                arenaView.highlightValidCells(true, isSpell);
            } else {
                arenaView.highlightValidCells(false, false);
            }
        });
        handContainer.getChildren().add(handView);

        updateConnectionStatus(networkService.getState());
        updateScoreDisplay();
    }

    /**
     * Starts the game loop - EXACTLY like offline BattleController.
     */
    private void startGame() {
        // SAME game loop as offline
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

                if (!isPaused && !gameEnded) {
                    update(deltaTime);
                }
            }
        };
        gameLoop.start();

        // Network sync timer (HOST only broadcasts state)
        if (networkService.isHost()) {
            startSyncTimer();
        }

        System.out.println("[NetworkBattle] Game started as " + (networkService.isHost() ? "HOST" : "CLIENT"));
    }

    /**
     * Update method - EXACTLY like offline BattleController.
     */
    private void update(double deltaTime) {
        // Update game state - SAME as offline
        gameState.update(deltaTime);

        // Update UI - SAME as offline
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);

        // Update sidebar
        updateTimeDisplay();
        updateScoreDisplay();

        // Check for Double Elixir
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
            if (timeLabel != null) {
                timeLabel.setStyle(
                        "-fx-text-fill: #ff4444; -fx-font-size: 32px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
            }
        }

        // Check game over
        if (gameState.isGameOver() && !gameEnded) {
            endGame();
        }
    }

    private void updateTimeDisplay() {
        if (timeLabel != null) {
            int seconds = (int) gameState.getGameTime();
            int mins = seconds / 60;
            int secs = seconds % 60;
            timeLabel.setText(String.format("%02d:%02d", mins, secs));
        }
    }

    private void updateScoreDisplay() {
        if (gameState != null) {
            if (playerScoreLabel != null)
                playerScoreLabel.setText(String.valueOf(gameState.getPlayerScore()));
            if (opponentScoreLabel != null)
                opponentScoreLabel.setText(String.valueOf(gameState.getBotScore()));
        }
    }

    // ==================== Network Sync ====================

    /**
     * HOST: Broadcasts state to client periodically.
     */
    private void startSyncTimer() {
        syncTimer = new Timeline(new KeyFrame(Duration.millis(SYNC_INTERVAL_MS), e -> {
            if (!gameEnded && networkService.isConnected()) {
                broadcastState();
            }
        }));
        syncTimer.setCycleCount(Timeline.INDEFINITE);
        syncTimer.play();
    }

    private void broadcastState() {
        // Create snapshot of current state
        NetworkGameStateSnapshot snapshot = new NetworkGameStateSnapshot(gameState, 0);
        networkService.send(NetworkMessage.gameStateSync(snapshot));
    }

    // ==================== Network Message Handling ====================

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case GAME_STATE_SYNC:
                // CLIENT: Apply state from HOST
                if (!networkService.isHost()) {
                    applyHostState(message);
                }
                break;

            case CARD_PLACED:
                // Apply opponent's card placement
                handleOpponentCardPlacement(message);
                break;

            case HEARTBEAT:
                // Ignore heartbeats
                break;

            case VICTORY:
                if (message.getPlayerId() != networkService.getPlayerId()) {
                    showDefeat("Opponent won the match!");
                }
                break;

            case DEFEAT:
                if (message.getPlayerId() != networkService.getPlayerId()) {
                    handleOpponentForfeit();
                }
                break;

            case OPPONENT_DISCONNECTED:
                handleOpponentDisconnected();
                break;

            default:
                break;
        }
    }

    /**
     * CLIENT: Applies tower health from HOST's state.
     */
    private void applyHostState(NetworkMessage message) {
        NetworkGameStateSnapshot snapshot = message.parseGameStateSync();
        if (snapshot == null)
            return;

        // Only sync tower health - let local simulation handle everything else
        // This ensures towers are always in sync
        for (NetworkGameStateSnapshot.TowerSnapshot ts : snapshot.getTowers()) {
            for (Tower tower : gameState.getArena().getAllTowers()) {
                // Match by type and side (after perspective consideration)
                // HOST's player towers are CLIENT's enemy towers
                boolean isMyTower = ts.isPlayerSide(); // In HOST's perspective
                boolean matchesMySide = !isMyTower; // Flip for CLIENT's perspective

                if (tower.getType().name().equals(ts.getType()) &&
                        tower.isPlayerSide() == matchesMySide) {
                    tower.setCurrentHealth(ts.getHealth());
                }
            }
        }

        // Sync scores (flip for perspective)
        gameState.setPlayerScore(snapshot.getPlayer2Score()); // HOST's bot is CLIENT's player opponent
        gameState.setBotScore(snapshot.getPlayer1Score()); // HOST's player is CLIENT's enemy
    }

    /**
     * Handles opponent's card placement.
     */
    private void handleOpponentCardPlacement(NetworkMessage message) {
        String[] data = message.parseCardPlacement();
        if (data == null)
            return;

        String cardName = data[0];
        int x = (int) Double.parseDouble(data[1]);
        int y = (int) Double.parseDouble(data[2]);

        // Mirror coordinates for opponent (their bottom is our top)
        int mirroredY = (Arena.HEIGHT - 1) - y;

        Card card = model.getCardByName(cardName);
        if (card != null) {
            // Place as opponent (isPlayer = false)
            gameState.placeCard(false, card, x, mirroredY);
            System.out.println("[NetworkBattle] Opponent placed " + cardName + " at (" + x + ", " + mirroredY + ")");
        }
    }

    // ==================== Input Handling ====================

    /**
     * Handles arena click for card deployment - SAME as offline, plus network send.
     */
    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex == -1)
            return;

        if (tileX < 0 || tileX >= Arena.WIDTH || tileY < 0 || tileY >= Arena.HEIGHT) {
            return;
        }

        Card card = gameState.getPlayerHand().getCard(selectedIndex);
        if (card == null)
            return;

        // Check elixir - SAME as offline
        if (gameState.getPlayerElixir().getCurrentElixir() < card.getCost()) {
            return;
        }

        // Place card locally - SAME as offline
        boolean success = gameState.placeCard(true, selectedIndex, tileX, tileY);

        if (success) {
            // Send to opponent over network
            networkService.send(NetworkMessage.cardPlaced(
                    networkService.getPlayerId(),
                    card.getName(),
                    tileX,
                    tileY));
        }

        handView.clearSelection();
        arenaView.highlightValidCells(false, false);
    }

    // ==================== UI Actions ====================

    @FXML
    private void handlePause() {
        if (gameEnded)
            return;
        SoundEffectUtil.playButtonClick();
        isPaused = true;
        showPauseMenu();
    }

    private void showPauseMenu() {
        pauseMenuContainer.getChildren().clear();
        pauseMenuContainer.setVisible(true);

        PauseMenuView menu = new PauseMenuView(new PauseMenuView.PauseMenuListener() {
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
                forfeitAndExit();
            }

            @Override
            public void onExitWithoutSaving() {
                forfeitAndExit();
            }
        });

        pauseMenuContainer.getChildren().add(menu);
    }

    private void handleResume() {
        isPaused = false;
        pauseMenuContainer.setVisible(false);
        pauseMenuContainer.getChildren().clear();
    }

    private void forfeitAndExit() {
        if (!gameEnded && networkService != null && networkService.isConnected()) {
            networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));
            Timeline exitDelay = new Timeline(new KeyFrame(Duration.millis(200), e -> navigateToMenu()));
            exitDelay.play();
        } else {
            navigateToMenu();
        }
    }

    private void navigateToMenu() {
        cleanup();
        try {
            sceneLoader.load(arenaContainer, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToMenu() {
        SoundEffectUtil.playButtonClick();
        if (!gameEnded && networkService != null && networkService.isConnected()) {
            forfeitAndExit();
        } else {
            navigateToMenu();
        }
    }

    // ==================== Connection Handling ====================

    private void handleOpponentDisconnected() {
        if (gameEnded)
            return;
        System.out.println("[NetworkBattle] Opponent disconnected");
        showDisconnectionOverlay();
        isPaused = true;

        final int[] countdown = { 5 };
        Timeline countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;
            if (reconnectCountdownLabel != null) {
                reconnectCountdownLabel.setText(String.valueOf(countdown[0]));
            }
            if (countdown[0] <= 0) {
                hideDisconnectionOverlay();
                showVictory("Opponent left the game - Victory!");
            }
        }));
        countdownTimer.setCycleCount(5);
        countdownTimer.play();
    }

    private void handleOpponentForfeit() {
        if (gameEnded)
            return;
        hideDisconnectionOverlay();
        showVictory("Opponent forfeited - Victory!");
    }

    // ==================== UI Updates ====================

    private void updateConnectionStatus(ConnectionState state) {
        if (connectionIndicator == null)
            return;

        switch (state) {
            case CONNECTED:
                connectionIndicator.setFill(Color.LIME);
                if (connectionStatusLabel != null)
                    connectionStatusLabel.setText("Connected");
                break;
            case RECONNECTING:
                connectionIndicator.setFill(Color.ORANGE);
                if (connectionStatusLabel != null)
                    connectionStatusLabel.setText("Reconnecting...");
                break;
            case DISCONNECTED:
                connectionIndicator.setFill(Color.RED);
                if (connectionStatusLabel != null)
                    connectionStatusLabel.setText("Disconnected");
                break;
            default:
                break;
        }
    }

    private void showDisconnectionOverlay() {
        if (disconnectionOverlay != null) {
            disconnectionOverlay.setVisible(true);
        }
        isPaused = true;
    }

    private void hideDisconnectionOverlay() {
        if (disconnectionOverlay != null) {
            disconnectionOverlay.setVisible(false);
        }
        isPaused = false;
    }

    // ==================== Game End ====================

    private void endGame() {
        gameEnded = true;

        if (gameState.getPlayerScore() > gameState.getBotScore()) {
            showVictory("You destroyed more towers!");
            networkService.send(NetworkMessage.victory(networkService.getPlayerId()));
        } else if (gameState.getBotScore() > gameState.getPlayerScore()) {
            showDefeat("Opponent destroyed more towers!");
            networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));
        } else {
            showDraw();
        }
    }

    private void showVictory(String details) {
        gameEnded = true;
        stopLoops();

        if (resultLabel != null) {
            resultLabel.setText("VICTORY!");
            resultLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
        }
        if (resultDetailsLabel != null)
            resultDetailsLabel.setText(details);
        if (resultOverlay != null)
            resultOverlay.setVisible(true);

        ServiceFactory.getInstance().getAchievementService().updateProgress(AchievementType.FIRST_BLOOD, 1);
        ServiceFactory.getInstance().getQuestService().updateProgress(QuestType.WIN_MATCHES, 1);
        ServiceFactory.getInstance().getQuestService().updateProgress(QuestType.WIN_PVP_MATCH, 1);
    }

    private void showDefeat(String details) {
        gameEnded = true;
        stopLoops();

        if (resultLabel != null) {
            resultLabel.setText("DEFEAT");
            resultLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 48px; -fx-font-weight: bold;");
        }
        if (resultDetailsLabel != null)
            resultDetailsLabel.setText(details);
        if (resultOverlay != null)
            resultOverlay.setVisible(true);
    }

    private void showDraw() {
        gameEnded = true;
        stopLoops();

        if (resultLabel != null) {
            resultLabel.setText("DRAW");
            resultLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 48px; -fx-font-weight: bold;");
        }
        if (resultDetailsLabel != null)
            resultDetailsLabel.setText("Equal towers destroyed!");
        if (resultOverlay != null)
            resultOverlay.setVisible(true);
    }

    private void stopLoops() {
        if (gameLoop != null)
            gameLoop.stop();
        if (syncTimer != null)
            syncTimer.stop();
    }

    private void cleanup() {
        stopLoops();
        if (networkService != null) {
            networkService.disconnect();
        }
    }
}
