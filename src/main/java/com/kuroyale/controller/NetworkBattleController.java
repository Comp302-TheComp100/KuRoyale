package com.kuroyale.controller;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.enums.NetworkMessageType;
import com.kuroyale.service.NetworkService;
import com.kuroyale.service.NetworkService.ConnectionState;
import com.kuroyale.util.NetworkConfig;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Controller for Network Battle gameplay.
 * Handles synchronized game state between two players over the network.
 * 
 * GRASP Patterns:
 * - Controller: Handles UI events and game logic coordination
 * - Low Coupling: Delegates network operations to NetworkService
 * - Observer: Listens for network messages
 */
public class NetworkBattleController {

    @FXML private AnchorPane root;
    
    // Top bar
    @FXML private Label playerNameLabel;
    @FXML private Label playerScoreLabel;
    @FXML private Label timerLabel;
    @FXML private Circle connectionIndicator;
    @FXML private Label connectionStatusLabel;
    @FXML private Label opponentNameLabel;
    @FXML private Label opponentScoreLabel;
    
    // Arena
    @FXML private Pane arenaPane;
    
    // Left panel
    @FXML private ProgressBar elixirBar;
    @FXML private Label elixirLabel;
    @FXML private Button pauseButton;
    
    // Right panel
    @FXML private Label pingLabel;
    @FXML private Label lagIndicator;
    
    // Card hand
    @FXML private HBox cardHandPane;
    
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
    
    private NetworkService networkService;
    private AnimationTimer gameLoop;
    private Timeline syncTimer;
    
    // Game state
    private double gameTime = 180.0; // 3 minutes
    private double playerElixir = 5.0;
    private double opponentElixir = 5.0;
    private int playerScore = 0;
    private int opponentScore = 0;
    private boolean isDoubleElixir = false;
    private boolean gameEnded = false;
    private boolean isPaused = false;
    
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
        
        // Initialize UI
        updateTimerDisplay();
        updateElixirDisplay();
        updateScoreDisplay();
        updateConnectionStatus(networkService.getState());
        
        // TODO: Load cards into hand, setup arena, etc.
        // This would integrate with existing BattleController logic
    }
    
    private void startGameLoop() {
        // Game loop for continuous updates
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                if (isPaused || gameEnded) return;
                
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                
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
        // Update game time
        gameTime -= deltaTime;
        if (gameTime <= 0) {
            gameTime = 0;
            endGame();
            return;
        }
        
        // Check for double elixir
        if (!isDoubleElixir && gameTime <= 60) {
            isDoubleElixir = true;
        }
        
        // Regenerate elixir
        double elixirRate = isDoubleElixir ? 2.0 : 1.0;
        playerElixir = Math.min(10.0, playerElixir + elixirRate * deltaTime / 2.8);
        
        // Update displays
        updateTimerDisplay();
        updateElixirDisplay();
    }
    
    private void syncGameState() {
        if (gameEnded || !networkService.isConnected()) return;
        
        // Host syncs timer
        if (networkService.isHost()) {
            networkService.send(NetworkMessage.timerSync(gameTime));
        }
        
        // Both sync elixir
        networkService.sendElixirUpdate(playerElixir);
        
        // Measure ping
        lastPingTime = System.currentTimeMillis();
        networkService.send(NetworkMessage.heartbeat(networkService.getPlayerId()));
    }
    
    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case CARD_PLACED:
                handleOpponentCardPlaced(message);
                break;
                
            case TOWER_DAMAGED:
                handleTowerDamaged(message);
                break;
                
            case TOWER_DESTROYED:
                handleTowerDestroyed(message);
                break;
                
            case ELIXIR_UPDATE:
                // Update opponent elixir (for display if needed)
                try {
                    opponentElixir = Double.parseDouble(message.getData());
                } catch (NumberFormatException e) {
                    // Ignore
                }
                break;
                
            case TIMER_SYNC:
                // Client syncs timer with host
                if (!networkService.isHost()) {
                    try {
                        gameTime = Double.parseDouble(message.getData());
                    } catch (NumberFormatException e) {
                        // Ignore
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
        String[] data = message.parseCardPlacement();
        if (data == null) return;
        
        String cardName = data[0];
        double x = Double.parseDouble(data[1]);
        double y = Double.parseDouble(data[2]);
        
        // TODO: Spawn opponent's card on arena
        // This would integrate with existing card spawning logic
        System.out.println("[NetworkBattle] Opponent placed " + cardName + " at (" + x + ", " + y + ")");
    }
    
    private void handleTowerDamaged(NetworkMessage message) {
        // TODO: Apply damage to tower
        String[] parts = message.getData().split("\\|");
        if (parts.length >= 2) {
            String towerName = parts[0];
            int damage = Integer.parseInt(parts[1]);
            System.out.println("[NetworkBattle] Tower " + towerName + " took " + damage + " damage");
        }
    }
    
    private void handleTowerDestroyed(NetworkMessage message) {
        String towerName = message.getData();
        
        // Update score
        if (message.getPlayerId() == networkService.getPlayerId()) {
            // Our tower was destroyed, opponent scores
            opponentScore++;
        } else {
            // Opponent's tower destroyed, we score
            playerScore++;
        }
        
        updateScoreDisplay();
        
        // Check for king tower (instant win)
        if (towerName.contains("KING")) {
            if (message.getPlayerId() == networkService.getPlayerId()) {
                showDefeat("Your King Tower was destroyed!");
            } else {
                showVictory("You destroyed the enemy King Tower!");
            }
        }
    }
    
    private void handleOpponentDisconnected() {
        if (gameEnded) return;
        
        // Show overlay
        showDisconnectionOverlay();
        
        // Start countdown
        final int[] countdown = {5};
        Timeline countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;
            reconnectCountdownLabel.setText(String.valueOf(countdown[0]));
            
            if (countdown[0] <= 0) {
                // Reconnection failed, award victory
                showVictory("Opponent disconnected - Victory awarded!");
            }
        }));
        countdownTimer.setCycleCount(5);
        countdownTimer.play();
    }
    
    // ==================== UI Actions ====================
    
    @FXML
    private void handlePause() {
        SoundEffectUtil.playButtonClick();
        isPaused = !isPaused;
        pauseButton.setText(isPaused ? "RESUME" : "PAUSE");
        
        // In network mode, we might want to pause for both players
        // For now, just local pause
    }
    
    @FXML
    private void handleBackToMenu() {
        SoundEffectUtil.playButtonClick();
        cleanup();
        
        try {
            sceneLoader.load(root, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called when player places a card.
     * Sends card placement to opponent.
     */
    public void onCardPlaced(String cardName, double x, double y) {
        networkService.sendCardPlaced(cardName, x, y);
    }
    
    /**
     * Called when a tower takes damage.
     */
    public void onTowerDamaged(String towerName, int damage) {
        networkService.sendTowerDamaged(towerName, damage);
    }
    
    // ==================== UI Updates ====================
    
    private void updateTimerDisplay() {
        int minutes = (int) gameTime / 60;
        int seconds = (int) gameTime % 60;
        timerLabel.setText(String.format("%d:%02d", minutes, seconds));
    }
    
    private void updateElixirDisplay() {
        elixirBar.setProgress(playerElixir / 10.0);
        elixirLabel.setText(String.format("%.0f/10", playerElixir));
    }
    
    private void updateScoreDisplay() {
        playerScoreLabel.setText(String.valueOf(playerScore));
        opponentScoreLabel.setText(String.valueOf(opponentScore));
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
        if (playerScore > opponentScore) {
            showVictory("You destroyed more towers!");
            networkService.send(NetworkMessage.victory(networkService.getPlayerId()));
        } else if (opponentScore > playerScore) {
            showDefeat("Opponent destroyed more towers!");
            networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));
        } else {
            showDraw();
        }
    }
    
    private void showVictory(String details) {
        gameEnded = true;
        resultLabel.setText("VICTORY!");
        resultLabel.setStyle("-fx-text-fill: gold;");
        resultDetailsLabel.setText(details);
        resultOverlay.setVisible(true);
    }
    
    private void showDefeat(String details) {
        gameEnded = true;
        resultLabel.setText("DEFEAT");
        resultLabel.setStyle("-fx-text-fill: #ff4444;");
        resultDetailsLabel.setText(details);
        resultOverlay.setVisible(true);
    }
    
    private void showDraw() {
        gameEnded = true;
        resultLabel.setText("DRAW");
        resultLabel.setStyle("-fx-text-fill: #aaaaaa;");
        resultDetailsLabel.setText("Equal towers destroyed!");
        resultOverlay.setVisible(true);
    }
    
    private void cleanup() {
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

