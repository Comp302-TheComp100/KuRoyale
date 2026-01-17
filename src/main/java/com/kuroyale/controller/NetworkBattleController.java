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
 * NETWORK BATTLE CONTROLLER - HOST-AUTHORITATIVE MODEL
 * 
 * For cross-network play, we need ONE authoritative game loop:
 * 
 * HOST (Player 1):
 * - Runs gameState.update() - the ONLY simulation
 * - Broadcasts FULL state to CLIENT every frame
 * - Receives card placements from CLIENT and applies them
 * 
 * CLIENT (Player 2):
 * - Does NOT run gameState.update() - no local simulation
 * - Receives full state from HOST and applies it
 * - Sends card placements to HOST
 * - Only renders what HOST tells them
 * 
 * This ensures both players ALWAYS see the same game state.
 */
public class NetworkBattleController {

    // FXML Components
    @FXML private StackPane arenaContainer;
    @FXML private HBox elixirContainer;
    @FXML private VBox handContainer;
    @FXML private VBox pauseMenuContainer;
    @FXML private Label timeLabel;

    // Network status indicators
    @FXML private Circle connectionIndicator;
    @FXML private Label connectionStatusLabel;
    @FXML private Label pingLabel;
    @FXML private Label lagIndicator;

    // Score display
    @FXML private Label playerNameLabel;
    @FXML private Label playerScoreLabel;
    @FXML private Label opponentNameLabel;
    @FXML private Label opponentScoreLabel;

    // Overlays
    @FXML private VBox disconnectionOverlay;
    @FXML private Label disconnectionLabel;
    @FXML private Label reconnectingLabel;
    @FXML private Label reconnectCountdownLabel;
    @FXML private VBox resultOverlay;
    @FXML private Label resultLabel;
    @FXML private Label resultDetailsLabel;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final BattleModel model = new BattleModel();

    // Core components
    private NetworkService networkService;
    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop;

    // State tracking
    private boolean isPaused = false;
    private boolean gameEnded = false;
    private boolean doubleElixirShown = false;

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
        networkService.setOnMessageReceived(message -> 
            Platform.runLater(() -> handleNetworkMessage(message)));

        networkService.setOnStateChanged(state -> Platform.runLater(() -> {
            updateConnectionStatus(state);

            if (state == ConnectionState.RECONNECTING) {
                showDisconnectionOverlay();
                isPaused = true;
            } else if (state == ConnectionState.CONNECTED && disconnectionOverlay != null && disconnectionOverlay.isVisible()) {
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
        if (playerNameLabel != null) playerNameLabel.setText(myName != null ? myName : "You");
        if (opponentNameLabel != null) opponentNameLabel.setText(oppName != null ? oppName : "Opponent");

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

        // Create Arena and GameState
        Arena arena = model.createArena(layoutToUse);
        Deck opponentDeck = model.createBotDeck(currentUser);
        gameState = new GameState(playerDeck, opponentDeck, arena);
        gameState.setCardCatalog(name -> model.getCardByName(name));
        gameState.setNetworkMode(true);  // Disables bot AI

        // Initialize UI
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
     * Starts the game loop.
     * HOST: Runs simulation + broadcasts state
     * CLIENT: Only renders (no simulation)
     */
    private void startGame() {
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
                    if (networkService.isHost()) {
                        // HOST: Run simulation and broadcast
                        updateHost(deltaTime);
                    } else {
                        // CLIENT: Only render (state comes from network)
                        updateClient(deltaTime);
                    }
                }
            }
        };
        gameLoop.start();

        System.out.println("[NetworkBattle] Game started as " + (networkService.isHost() ? "HOST (authoritative)" : "CLIENT (receiver)"));
    }

    /**
     * HOST: Runs the authoritative game simulation and broadcasts state.
     */
    private void updateHost(double deltaTime) {
        // Run the simulation - HOST is the source of truth
        gameState.update(deltaTime);
        
        // Update UI
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);
        updateTimeDisplay();
        updateScoreDisplay();

        // Check for Double Elixir
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
            if (timeLabel != null) {
                timeLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 32px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
            }
        }

        // Broadcast state to CLIENT
        if (networkService.isConnected()) {
            broadcastFullState();
        }

        // Check game over
        if (gameState.isGameOver() && !gameEnded) {
            endGame();
        }
    }

    /**
     * CLIENT: Only renders, does not simulate.
     * State is received from HOST via handleNetworkMessage.
     */
    private void updateClient(double deltaTime) {
        // NO gameState.update() here - CLIENT does not simulate!
        
        // Only update UI rendering
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);  // Visual updates only
        updateTimeDisplay();
        updateScoreDisplay();

        // Check for Double Elixir (based on synced state)
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
            if (timeLabel != null) {
                timeLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 32px; -fx-font-weight: bold; -fx-font-family: 'Courier New';");
            }
        }

        // Check game over (based on synced state)
        if (gameState.isGameOver() && !gameEnded) {
            endGame();
        }
    }

    /**
     * HOST: Broadcasts the full game state to CLIENT.
     */
    private void broadcastFullState() {
        NetworkGameStateSnapshot snapshot = new NetworkGameStateSnapshot(gameState, 0);
        networkService.send(NetworkMessage.gameStateSync(snapshot));
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
            if (playerScoreLabel != null) playerScoreLabel.setText(String.valueOf(gameState.getPlayerScore()));
            if (opponentScoreLabel != null) opponentScoreLabel.setText(String.valueOf(gameState.getBotScore()));
        }
    }

    // ==================== Network Message Handling ====================

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case GAME_STATE_SYNC:
                // CLIENT: Apply full state from HOST
                if (!networkService.isHost()) {
                    applyFullStateFromHost(message);
                }
                break;
                
            case CARD_PLACED:
                // HOST: Apply opponent's card placement
                if (networkService.isHost()) {
                    handleOpponentCardPlacement(message);
                }
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
     * CLIENT: Applies the full game state received from HOST.
     * This is the key to keeping both players in sync.
     */
    private void applyFullStateFromHost(NetworkMessage message) {
        NetworkGameStateSnapshot snapshot = message.parseGameStateSync();
        if (snapshot == null) return;
        
        // Apply game time
        gameState.setGameTime(snapshot.getGameTime());
        
        // Apply scores (swap perspective: HOST's player is CLIENT's enemy)
        gameState.setPlayerScore(snapshot.getPlayer2Score());
        gameState.setBotScore(snapshot.getPlayer1Score());
        
        // Apply double elixir flag
        if (snapshot.isDoubleElixir()) {
            gameState.setDoubleElixir(true);
        }
        
        // Apply game over state
        if (snapshot.isGameOver()) {
            // Swap winner perspective
            int winner = snapshot.getWinner();
            if (winner == 1) winner = 2;
            else if (winner == 2) winner = 1;
            
            boolean playerWon = (winner == 1);
            boolean isDraw = (winner == 3);
            gameState.setGameOver(true, playerWon, isDraw);
        }
        
        // Apply elixir (swap perspective)
        gameState.getPlayerElixir().setCurrentElixir(snapshot.getPlayer2Elixir());
        gameState.getBotElixir().setCurrentElixir(snapshot.getPlayer1Elixir());
        
        // Apply tower health (swap perspective)
        for (NetworkGameStateSnapshot.TowerSnapshot ts : snapshot.getTowers()) {
            for (Tower tower : gameState.getArena().getAllTowers()) {
                // HOST's player towers = CLIENT's enemy towers
                boolean isMyTower = !ts.isPlayerSide();  // Swap perspective
                
                if (tower.getType().name().equals(ts.getType()) && 
                    tower.isPlayerSide() == isMyTower) {
                    tower.setCurrentHealth(ts.getHealth());
                }
            }
        }
        
        // Apply troop state (health and positions)
        applyTroopsFromHost(snapshot);
        
        // Apply building state
        applyBuildingsFromHost(snapshot);
    }

    /**
     * Syncs troops from HOST state.
     */
    private void applyTroopsFromHost(NetworkGameStateSnapshot snapshot) {
        // Clear existing troops and rebuild from snapshot
        // This is simpler and ensures perfect sync
        gameState.getTroops().clear();
        
        for (NetworkGameStateSnapshot.TroopSnapshot ts : snapshot.getTroops()) {
            Card card = model.getCardByName(ts.getCardName());
            if (card != null) {
                // Swap perspective: HOST's player = CLIENT's enemy
                boolean isMyTroop = !ts.isPlayerSide();
                
                // Mirror Y coordinate for perspective
                double mirroredY = (Arena.HEIGHT - 1) - ts.getY();
                
                GridPosition pos = new GridPosition((int) ts.getX(), (int) mirroredY);
                Troop troop = new Troop(card, pos, isMyTroop);
                troop.setCurrentHealth(ts.getHealth());
                troop.setWorldPosition(ts.getX(), mirroredY);
                gameState.getTroops().add(troop);
            }
        }
    }

    /**
     * Syncs buildings from HOST state.
     */
    private void applyBuildingsFromHost(NetworkGameStateSnapshot snapshot) {
        // Clear existing buildings and rebuild from snapshot
        gameState.getBuildings().clear();
        
        for (NetworkGameStateSnapshot.BuildingSnapshot bs : snapshot.getBuildings()) {
            Card card = model.getCardByName(bs.getCardName());
            if (card != null) {
                // Swap perspective
                boolean isMyBuilding = !bs.isPlayerSide();
                
                // Mirror Y coordinate
                double mirroredY = (Arena.HEIGHT - 1) - bs.getY();
                
                GridPosition pos = new GridPosition((int) bs.getX(), (int) mirroredY);
                int bw = Math.max(1, card.getFootprintWidthTiles());
                int bh = Math.max(1, card.getFootprintHeightTiles());
                
                Building building = new Building(pos, bw, bh, isMyBuilding,
                        card.getHp(), card.getImagePath(), card.getLifetime());
                building.configureCombatFromCard(card);
                building.setCurrentHealth(bs.getHealth());
                gameState.getBuildings().add(building);
            }
        }
    }

    /**
     * HOST: Handles card placement from CLIENT.
     */
    private void handleOpponentCardPlacement(NetworkMessage message) {
        String[] data = message.parseCardPlacement();
        if (data == null) return;
        
        String cardName = data[0];
        int x = (int) Double.parseDouble(data[1]);
        int y = (int) Double.parseDouble(data[2]);
        
        // Mirror Y coordinate (CLIENT's view is flipped)
        int mirroredY = (Arena.HEIGHT - 1) - y;
        
        Card card = model.getCardByName(cardName);
        if (card != null) {
            // Spend opponent's elixir
            if (gameState.getBotElixir().getCurrentElixir() >= card.getCost()) {
                gameState.getBotElixir().spend(card.getCost());
                // Place as opponent (isPlayer = false)
                gameState.placeCard(false, card, x, mirroredY);
                System.out.println("[NetworkBattle] HOST: Opponent placed " + cardName + " at (" + x + ", " + mirroredY + ")");
            }
        }
    }

    // ==================== Input Handling ====================

    /**
     * Handles arena click for card deployment.
     */
    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex == -1) return;
        
        if (tileX < 0 || tileX >= Arena.WIDTH || tileY < 0 || tileY >= Arena.HEIGHT) {
            return;
        }
        
        Card card = gameState.getPlayerHand().getCard(selectedIndex);
        if (card == null) return;
        
        // Check elixir
        if (gameState.getPlayerElixir().getCurrentElixir() < card.getCost()) {
            return;
        }

        if (networkService.isHost()) {
            // HOST: Place card locally (simulation will handle it)
            boolean success = gameState.placeCard(true, selectedIndex, tileX, tileY);
            if (success) {
                System.out.println("[NetworkBattle] HOST: Placed " + card.getName() + " at (" + tileX + ", " + tileY + ")");
            }
        } else {
            // CLIENT: Send to HOST, also place locally for immediate feedback
            networkService.send(NetworkMessage.cardPlaced(
                networkService.getPlayerId(), 
                card.getName(), 
                tileX, 
                tileY
            ));
            
            // Optimistic local update
            gameState.getPlayerElixir().spend(card.getCost());
            gameState.getPlayerHand().playCard(selectedIndex);
            
            System.out.println("[NetworkBattle] CLIENT: Sent card placement " + card.getName());
        }

        handView.clearSelection();
        arenaView.highlightValidCells(false, false);
    }

    // ==================== UI Actions ====================

    @FXML
    private void handlePause() {
        if (gameEnded) return;
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
        if (gameEnded) return;
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
        if (gameEnded) return;
        hideDisconnectionOverlay();
        showVictory("Opponent forfeited - Victory!");
    }

    // ==================== UI Updates ====================

    private void updateConnectionStatus(ConnectionState state) {
        if (connectionIndicator == null) return;
        
        switch (state) {
            case CONNECTED:
                connectionIndicator.setFill(Color.LIME);
                if (connectionStatusLabel != null) connectionStatusLabel.setText("Connected");
                break;
            case RECONNECTING:
                connectionIndicator.setFill(Color.ORANGE);
                if (connectionStatusLabel != null) connectionStatusLabel.setText("Reconnecting...");
                break;
            case DISCONNECTED:
                connectionIndicator.setFill(Color.RED);
                if (connectionStatusLabel != null) connectionStatusLabel.setText("Disconnected");
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
        if (gameLoop != null) gameLoop.stop();

        if (resultLabel != null) {
            resultLabel.setText("VICTORY!");
            resultLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
        }
        if (resultDetailsLabel != null) resultDetailsLabel.setText(details);
        if (resultOverlay != null) resultOverlay.setVisible(true);

        ServiceFactory.getInstance().getAchievementService().updateProgress(AchievementType.FIRST_BLOOD, 1);
        ServiceFactory.getInstance().getQuestService().updateProgress(QuestType.WIN_MATCHES, 1);
        ServiceFactory.getInstance().getQuestService().updateProgress(QuestType.WIN_PVP_MATCH, 1);
    }

    private void showDefeat(String details) {
        gameEnded = true;
        if (gameLoop != null) gameLoop.stop();

        if (resultLabel != null) {
            resultLabel.setText("DEFEAT");
            resultLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 48px; -fx-font-weight: bold;");
        }
        if (resultDetailsLabel != null) resultDetailsLabel.setText(details);
        if (resultOverlay != null) resultOverlay.setVisible(true);
    }

    private void showDraw() {
        gameEnded = true;
        if (gameLoop != null) gameLoop.stop();

        if (resultLabel != null) {
            resultLabel.setText("DRAW");
            resultLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 48px; -fx-font-weight: bold;");
        }
        if (resultDetailsLabel != null) resultDetailsLabel.setText("Equal towers destroyed!");
        if (resultOverlay != null) resultOverlay.setVisible(true);
    }

    private void cleanup() {
        if (gameLoop != null) gameLoop.stop();
        if (networkService != null) {
            networkService.disconnect();
        }
    }
}
