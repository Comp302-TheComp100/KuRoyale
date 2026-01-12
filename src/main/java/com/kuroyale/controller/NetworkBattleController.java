package com.kuroyale.controller;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.service.NetworkService;
import com.kuroyale.service.NetworkService.ConnectionState;
import com.kuroyale.util.NetworkConfig;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ElixirBar;
import com.kuroyale.view.battle.HandView;
import com.kuroyale.view.battle.PauseMenuView;
import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;

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
 * Controller for Network Battle gameplay.
 * Integrates with existing game logic and synchronizes state between players.
 * 
 * GRASP Patterns:
 * - Controller: Handles UI events and game logic coordination
 * - Low Coupling: Delegates network operations to NetworkService
 * - Observer: Listens for game events and network messages
 */
public class NetworkBattleController implements GameEventListener {

    // FXML Components - matching battle.fxml structure
    @FXML private StackPane arenaContainer;
    @FXML private HBox elixirContainer;
    @FXML private VBox handContainer;
    @FXML private VBox pauseMenuContainer;
    
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
    private final NetworkConfig config = NetworkConfig.getInstance();
    private final BattleModel model = new BattleModel();
    
    // Game components
    private NetworkService networkService;
    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop;
    private Timeline syncTimer;
    private ArenaLayout currentArenaLayout;
    
    // Game state flags
    private boolean isPaused = false;
    private boolean gameEnded = false;
    private boolean doubleElixirShown = false;
    
    // Network timing
    private long lastPingTime = 0;
    private long currentPing = 0;
    
    @FXML
    private void initialize() {
        // Initial setup - network service will be set via setter
    }
    
    /**
     * Sets the network service and starts the game.
     * Called by NetworkLobbyController after loading this scene.
     */
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
        setupNetworkCallbacks();
        initializeGame();
        startGameLoop();
        
        // Subscribe to game events for network sync
        GameEventBus.getInstance().subscribe(this);
    }
    
    private void setupNetworkCallbacks() {
        networkService.setOnMessageReceived(message -> Platform.runLater(() -> handleNetworkMessage(message)));
        
        networkService.setOnStateChanged(state -> Platform.runLater(() -> {
            updateConnectionStatus(state);
            
            if (state == ConnectionState.RECONNECTING) {
                showDisconnectionOverlay();
            } else if (state == ConnectionState.CONNECTED && disconnectionOverlay.isVisible()) {
                hideDisconnectionOverlay();
            } else if (state == ConnectionState.DISCONNECTED && !gameEnded) {
                handleOpponentDisconnected();
            }
        }));
    }
    
    private void initializeGame() {
        // Set player names
        String myName = networkService.getPlayerName();
        String oppName = networkService.getOpponentName();
        
        playerNameLabel.setText(myName != null ? myName : "You");
        opponentNameLabel.setText(oppName != null ? oppName : "Opponent");
        
        // Get current user
        User currentUser = model.getCurrentUser();
        if (currentUser == null) {
            handleBackToMenu();
            return;
        }
        
        // Set current user in arena service
        model.setCurrentUserInArenaService(currentUser);
        
        // Create deck
        Deck playerDeck = model.createDeckFromNames(currentUser.getDeck());
        
        // Determine which arena layout to use:
        // - If we're the client and received the host's layout, use that
        // - Otherwise (we're the host or no layout received), use our own layout
        ArenaLayout layoutToUse;
        if (!networkService.isHost() && networkService.getHostArenaLayout() != null) {
            // Client: use the host's arena layout for consistency
            layoutToUse = networkService.getHostArenaLayout();
            System.out.println("[NetworkBattle] Using HOST's arena layout: " + layoutToUse.getName());
        } else {
            // Host: use own layout
            layoutToUse = model.loadArenaLayout();
            System.out.println("[NetworkBattle] Using own arena layout: " + layoutToUse.getName());
        }
        currentArenaLayout = layoutToUse;
        
        // Create Arena
        Arena arena = model.createArena(layoutToUse);
        
        // Create opponent deck (mirrored for network play)
        Deck opponentDeck = model.createBotDeck(currentUser);
        
        // Initialize GameState
        gameState = new GameState(playerDeck, opponentDeck, arena);
        gameState.setCardCatalog(name -> model.getCardByName(name));
        
        // IMPORTANT: Enable network mode to disable bot AI
        // In network mode, the opponent is a real player, not AI
        gameState.setNetworkMode(true);
        
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
    
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            private long lastTime = 0;
            
            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }
                
                if (isPaused || gameEnded) return;
                
                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;
                
                update(deltaTime);
            }
        };
        gameLoop.start();
        
        // Sync timer - periodically sync state with opponent
        syncTimer = new Timeline(new KeyFrame(Duration.millis(config.getSyncInterval()), e -> syncGameState()));
        syncTimer.setCycleCount(Timeline.INDEFINITE);
        syncTimer.play();
    }
    
    private void update(double deltaTime) {
        // Update Game Logic and UI
        gameState.update(deltaTime);
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);
        
        // Update score display
        updateScoreDisplay();
        
        // Check for Double Elixir
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
        }
        
        // Check for Game Over
        if (gameState.isGameOver() && !gameEnded) {
            endGame();
        }
    }
    
    private void syncGameState() {
        if (gameEnded || !networkService.isConnected()) return;
        
        // Host syncs timer
        if (networkService.isHost()) {
            networkService.send(NetworkMessage.timerSync(gameState.getGameTime()));
        }
        
        // Both sync elixir
        networkService.sendElixirUpdate(gameState.getPlayerElixir().getCurrentElixir());
        
        // Measure ping
        lastPingTime = System.currentTimeMillis();
        networkService.send(NetworkMessage.heartbeat(networkService.getPlayerId()));
    }
    
    private void handleNetworkMessage(NetworkMessage message) {
        System.out.println("[NetworkBattle] Received message type: " + message.getType());
        
        switch (message.getType()) {
            case CARD_PLACED:
                System.out.println("[NetworkBattle] Processing CARD_PLACED message");
                handleOpponentCardPlaced(message);
                break;
                
            case TOWER_DAMAGED:
                // Tower damage is handled by game state, but we can validate
                break;
                
            case TOWER_DESTROYED:
                handleTowerDestroyed(message);
                break;
                
            case TIMER_SYNC:
                // Client syncs timer with host (host is authoritative)
                if (!networkService.isHost()) {
                    try {
                        double hostTime = Double.parseDouble(message.getData());
                        // Sync game timer with host
                        if (gameState != null) {
                            gameState.setGameTime(hostTime);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid timer data
                    }
                }
                break;
                
            case HEARTBEAT:
                // Calculate ping
                if (message.getPlayerId() != networkService.getPlayerId()) {
                    currentPing = System.currentTimeMillis() - lastPingTime;
                    updatePingDisplay();
                }
                break;
                
            case VICTORY:
                if (message.getPlayerId() != networkService.getPlayerId()) {
                    showDefeat("Opponent won the match!");
                }
                break;
                
            case DEFEAT:
                if (message.getPlayerId() != networkService.getPlayerId()) {
                    showVictory("Opponent forfeited!");
                }
                break;
                
            case OPPONENT_DISCONNECTED:
                handleOpponentDisconnected();
                break;
                
            default:
                break;
        }
    }
    
    private void handleOpponentCardPlaced(NetworkMessage message) {
        System.out.println("[NetworkBattle] handleOpponentCardPlaced called with: " + message.getData());
        
        String[] data = message.parseCardPlacement();
        if (data == null) {
            System.err.println("[NetworkBattle] Failed to parse card placement data");
            return;
        }
        
        String cardName = data[0];
        int x = (int) Double.parseDouble(data[1]);
        int y = (int) Double.parseDouble(data[2]);
        
        System.out.println("[NetworkBattle] Parsed: card=" + cardName + ", x=" + x + ", y=" + y);
        
        // Get the card from catalog
        Card card = model.getCardByName(cardName);
        if (card == null) {
            System.err.println("[NetworkBattle] Unknown card: " + cardName);
            return;
        }
        
        // Mirror the Y position because:
        // - Opponent placed at (x, y) on THEIR bottom half (y >= 16)
        // - From OUR perspective, that's on the TOP half (enemy side)
        // - So we mirror: newY = (Arena.HEIGHT - 1) - y
        // Example: They place at y=20 -> We see at y=11 (top half, enemy territory)
        int mirroredY = (Arena.HEIGHT - 1) - y;
        
        System.out.println("[NetworkBattle] Spawning opponent's " + cardName + " at (" + x + ", " + mirroredY + ") [original y=" + y + "]");
        
        // Spawn the card for the opponent (isPlayer=false means enemy from our perspective)
        // Use placeCard with the card directly to bypass hand/elixir checks
        gameState.placeCard(false, card, x, mirroredY);
        
        System.out.println("[NetworkBattle] ✓ Opponent card spawned successfully");
    }
    
    private void handleTowerDestroyed(NetworkMessage message) {
        // Score is tracked by game state
        updateScoreDisplay();
        
        String towerName = message.getData();
        if (towerName != null && towerName.contains("KING")) {
            if (message.getPlayerId() == networkService.getPlayerId()) {
                // Our tower was destroyed
                showDefeat("Your King Tower was destroyed!");
            } else {
                showVictory("You destroyed the enemy King Tower!");
            }
        }
    }
    
    private void handleOpponentDisconnected() {
        if (gameEnded) return;
        
        showDisconnectionOverlay();
        
        // Start countdown
        final int[] countdown = {5};
        Timeline countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;
            reconnectCountdownLabel.setText(String.valueOf(countdown[0]));
            
            if (countdown[0] <= 0 && !networkService.isConnected()) {
                showVictory("Opponent disconnected - Victory awarded!");
            }
        }));
        countdownTimer.setCycleCount(5);
        countdownTimer.play();
    }
    
    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex != -1) {
            // Validate bounds
            if (tileX >= 0 && tileX < Arena.WIDTH && tileY >= 0 && tileY < Arena.HEIGHT) {
                Card card = gameState.getPlayerHand().getCard(selectedIndex);
                
                // Try to place card
                if (gameState.placeCard(true, selectedIndex, tileX, tileY)) {
                    // Success - send to opponent
                    if (card != null) {
                        System.out.println("[NetworkBattle] YOU placed " + card.getName() + " at (" + tileX + ", " + tileY + ") - sending to opponent");
                        networkService.sendCardPlaced(card.getName(), tileX, tileY);
                    }
                    
                    handView.clearSelection();
                    arenaView.highlightValidCells(false, false);
                } else {
                    System.out.println("[NetworkBattle] Failed to place card at (" + tileX + ", " + tileY + ")");
                }
            }
        }
    }
    
    // ==================== Game Event Listener ====================
    
    @Override
    public void onCardPlayed(boolean isPlayer, Card card, java.util.List<com.kuroyale.model.entities.ICombatant> spawnedUnits) {
        // Card played events are handled by arena click
    }
    
    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        // Send tower destroyed message
        String towerName = (isPlayerTower ? "PLAYER_" : "OPPONENT_") + tower.getType().name();
        networkService.send(NetworkMessage.towerDestroyed(networkService.getPlayerId(), towerName));
        updateScoreDisplay();
    }
    
    @Override
    public void onElixirSpent(boolean isPlayer, int amount) {
        // Handled by sync timer
    }
    
    @Override
    public void onBuildingProduction(Building building, String resource, int amount) {
        if (building.isPlayerSide() && "ELIXIR".equals(resource)) {
            Platform.runLater(() -> elixirBar.showProductionIndicator(amount));
        }
    }
    
    @Override
    public void onAreaEffect(boolean isPlayerSource, GridPosition center, double radius, double duration) {
        // Visual effects handled by arena view
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
                // Network games can't be saved mid-match
                handleResume();
            }
            
            @Override
            public void onSaveAndExit() {
                // Network games can't be saved
                handleBackToMenu();
            }
            
            @Override
            public void onExitWithoutSaving() {
                // Send defeat message before exiting
                networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));
                handleBackToMenu();
            }
        });
        
        pauseMenuContainer.getChildren().add(menu);
    }
    
    private void handleResume() {
        isPaused = false;
        pauseMenuContainer.setVisible(false);
        pauseMenuContainer.getChildren().clear();
    }
    
    @FXML
    private void handleBackToMenu() {
        SoundEffectUtil.playButtonClick();
        cleanup();
        
        try {
            sceneLoader.load(arenaContainer, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // ==================== UI Updates ====================
    
    private void updateScoreDisplay() {
        if (gameState != null) {
            playerScoreLabel.setText(String.valueOf(gameState.getPlayerScore()));
            opponentScoreLabel.setText(String.valueOf(gameState.getBotScore()));
        }
    }
    
    private void updatePingDisplay() {
        pingLabel.setText("Ping: " + currentPing + "ms");
        
        // Show lag indicator if ping is high
        if (currentPing > 100) {
            lagIndicator.setText("⚠ High Latency");
            lagIndicator.setVisible(true);
        } else {
            lagIndicator.setVisible(false);
        }
    }
    
    private void updateConnectionStatus(ConnectionState state) {
        switch (state) {
            case CONNECTED:
                connectionIndicator.setFill(Color.LIME);
                connectionStatusLabel.setText("Connected");
                break;
            case RECONNECTING:
                connectionIndicator.setFill(Color.ORANGE);
                connectionStatusLabel.setText("Reconnecting...");
                break;
            case DISCONNECTED:
                connectionIndicator.setFill(Color.RED);
                connectionStatusLabel.setText("Disconnected");
                break;
            default:
                break;
        }
    }
    
    private void showDisconnectionOverlay() {
        disconnectionOverlay.setVisible(true);
        isPaused = true;
    }
    
    private void hideDisconnectionOverlay() {
        disconnectionOverlay.setVisible(false);
        isPaused = false;
    }
    
    private void endGame() {
        gameEnded = true;
        
        // Determine winner
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
        if (syncTimer != null) syncTimer.stop();
        
        resultLabel.setText("VICTORY!");
        resultLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;");
        resultDetailsLabel.setText(details);
        resultOverlay.setVisible(true);
        
        // Track achievements
        ServiceFactory.getInstance().getAchievementService()
                .updateProgress(AchievementType.FIRST_BLOOD, 1);
        ServiceFactory.getInstance().getQuestService()
                .updateProgress(QuestType.WIN_MATCHES, 1);
        ServiceFactory.getInstance().getQuestService()
                .updateProgress(QuestType.WIN_PVP_MATCH, 1);
    }
    
    private void showDefeat(String details) {
        gameEnded = true;
        if (gameLoop != null) gameLoop.stop();
        if (syncTimer != null) syncTimer.stop();
        
        resultLabel.setText("DEFEAT");
        resultLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 48px; -fx-font-weight: bold;");
        resultDetailsLabel.setText(details);
        resultOverlay.setVisible(true);
    }
    
    private void showDraw() {
        gameEnded = true;
        if (gameLoop != null) gameLoop.stop();
        if (syncTimer != null) syncTimer.stop();
        
        resultLabel.setText("DRAW");
        resultLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 48px; -fx-font-weight: bold;");
        resultDetailsLabel.setText("Equal towers destroyed!");
        resultOverlay.setVisible(true);
    }
    
    private void cleanup() {
        // Unsubscribe from events
        GameEventBus.getInstance().unsubscribe(this);
        
        if (gameLoop != null) {
            gameLoop.stop();
        }
        if (syncTimer != null) {
            syncTimer.stop();
        }
        if (networkService != null) {
            networkService.disconnect();
        }
    }
}
