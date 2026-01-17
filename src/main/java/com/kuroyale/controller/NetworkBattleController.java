package com.kuroyale.controller;

import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.dto.NetworkGameStateSnapshot;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;
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

import java.util.*;

/**
 * NETWORK BATTLE CONTROLLER - SINGLE AUTHORITATIVE GAME LOOP
 * 
 * Architecture:
 * - HOST runs the ONLY game simulation (gameState.update())
 * - HOST broadcasts EVERYTHING to CLIENT: state, effects, projectiles
 * - CLIENT does NO simulation - only receives and renders
 * - Both players see EXACTLY the same game (180° mirrored perspective)
 */
public class NetworkBattleController implements GameEventListener {

    @FXML private StackPane arenaContainer;
    @FXML private HBox elixirContainer;
    @FXML private VBox handContainer;
    @FXML private VBox pauseMenuContainer;
    @FXML private Label timeLabel;
    @FXML private Circle connectionIndicator;
    @FXML private Label connectionStatusLabel;
    @FXML private Label pingLabel;
    @FXML private Label lagIndicator;
    @FXML private Label playerNameLabel;
    @FXML private Label playerScoreLabel;
    @FXML private Label opponentNameLabel;
    @FXML private Label opponentScoreLabel;
    @FXML private VBox disconnectionOverlay;
    @FXML private Label disconnectionLabel;
    @FXML private Label reconnectingLabel;
    @FXML private Label reconnectCountdownLabel;
    @FXML private VBox resultOverlay;
    @FXML private Label resultLabel;
    @FXML private Label resultDetailsLabel;

    private final SceneLoader sceneLoader = new SceneLoader();
    private final BattleModel model = new BattleModel();

    private NetworkService networkService;
    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop;

    private boolean isPaused = false;
    private boolean gameEnded = false;
    private boolean doubleElixirShown = false;
    
    // Track entities by ID for proper sync on CLIENT
    private Map<Integer, Troop> troopMap = new HashMap<>();
    private Map<Integer, Building> buildingMap = new HashMap<>();

    @FXML
    private void initialize() {}

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
        try {
            System.out.println("[NetworkBattle] Initializing as " + (networkService.isHost() ? "HOST" : "CLIENT"));
            
            // Reset ID counters at the start of a new match (HOST only generates IDs)
            if (networkService.isHost()) {
                Troop.resetIdCounter();
                Building.resetIdCounter();
            }
            
            setupNetworkCallbacks();
            initializeGame();
            
            // HOST subscribes to game events to forward effects to CLIENT
            if (networkService.isHost()) {
                GameEventBus.getInstance().subscribe(this);
            }
            
            startGame();
            System.out.println("[NetworkBattle] Game started successfully!");
        } catch (Exception e) {
            System.err.println("[NetworkBattle] ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== GameEventListener - HOST forwards effects to CLIENT ====================
    
    @Override
    public void onAreaEffect(boolean isPlayerSource, GridPosition center, double radius, double duration) {
        if (networkService.isHost() && networkService.isConnected()) {
            // Send effect to CLIENT
            networkService.send(NetworkMessage.effectArea(
                networkService.getPlayerId(), isPlayerSource, center.getX(), center.getY(), radius, duration));
        }
    }
    
    @Override
    public void onCardPlayed(boolean isPlayer, Card card, List<ICombatant> spawnedUnits) {
        // Effects handled elsewhere
    }
    
    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        // State sync handles this
    }
    
    @Override
    public void onTowerDamaged(Tower tower) {
        // State sync handles this
    }

    // ==================== Network Setup ====================

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
        String myName = networkService.getPlayerName();
        String oppName = networkService.getOpponentName();
        if (playerNameLabel != null) playerNameLabel.setText(myName != null ? myName : "You");
        if (opponentNameLabel != null) opponentNameLabel.setText(oppName != null ? oppName : "Opponent");

        User currentUser = model.getCurrentUser();
        if (currentUser == null) {
            System.err.println("[NetworkBattle] No current user!");
            handleBackToMenu();
            return;
        }

        model.setCurrentUserInArenaService(currentUser);
        Deck playerDeck = model.createDeckFromNames(currentUser.getDeck());

        ArenaLayout layoutToUse;
        if (!networkService.isHost() && networkService.getHostArenaLayout() != null) {
            layoutToUse = networkService.getHostArenaLayout();
        } else {
            layoutToUse = model.loadArenaLayout();
        }

        Arena arena = model.createArena(layoutToUse);
        Deck opponentDeck = model.createBotDeck(currentUser);
        gameState = new GameState(playerDeck, opponentDeck, arena);
        gameState.setCardCatalog(name -> model.getCardByName(name));
        gameState.setNetworkMode(true);

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
    }

    // ==================== Game Loop ====================

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
                        updateHost(deltaTime);
                    } else {
                        updateClient(deltaTime);
                    }
                }
            }
        };
        gameLoop.start();
    }

    /**
     * HOST: Run the ONLY simulation and broadcast everything.
     */
    private void updateHost(double deltaTime) {
        // Run simulation - this is the SINGLE game loop
        gameState.update(deltaTime);
        
        // Update UI
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);
        updateTimeDisplay();
        updateScoreDisplay();

        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
        }

        // Broadcast FULL state to CLIENT
        if (networkService.isConnected()) {
            NetworkGameStateSnapshot snapshot = new NetworkGameStateSnapshot(gameState, 0);
            networkService.send(NetworkMessage.gameStateSync(snapshot));
        }

        if (gameState.isGameOver() && !gameEnded) {
            endGame();
        }
    }

    /**
     * CLIENT: NO simulation - only render state received from HOST.
     */
    private void updateClient(double deltaTime) {
        // DO NOT call gameState.update() - we only render!
        
        // Update UI based on synced state
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);
        updateTimeDisplay();
        updateScoreDisplay();

        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
        }

        if (gameState.isGameOver() && !gameEnded) {
            endGame();
        }
    }

    // ==================== Network Message Handling ====================

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case GAME_STATE_SYNC:
                if (!networkService.isHost()) {
                    applyStateFromHost(message);
                }
                break;
            case CARD_PLACED:
                if (networkService.isHost()) {
                    handleClientCardPlacement(message);
                }
                break;
            case EFFECT_AREA:
                if (!networkService.isHost()) {
                    handleAreaEffect(message);
                }
                break;
            case EFFECT_PROJECTILE_HIT:
                if (!networkService.isHost()) {
                    handleProjectileHitEffect(message);
                }
                break;
            case VICTORY:
                if (message.getPlayerId() != networkService.getPlayerId()) {
                    showDefeat("Opponent won!");
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

    /**
     * CLIENT: Display area effect received from HOST.
     */
    private void handleAreaEffect(NetworkMessage message) {
        double[] data = message.parseEffectArea();
        if (data == null) return;
        
        boolean isPlayerSource = data[0] > 0.5;
        double centerX = data[1];
        double centerY = data[2];
        double radius = data[3];
        double duration = data[4];
        
        // Mirror coordinates for CLIENT perspective (180° rotation)
        final double maxX = Arena.WIDTH - 1;
        final double maxY = Arena.HEIGHT - 1;
        double mirroredX = maxX - centerX;
        double mirroredY = maxY - centerY;
        boolean mirroredPlayerSource = !isPlayerSource; // Flip ownership
        
        // Trigger the effect via GameEventBus so BattleArenaView displays it
        GridPosition mirroredCenter = new GridPosition((int) Math.round(mirroredX), (int) Math.round(mirroredY));
        GameEventBus.getInstance().publishAreaEffect(mirroredPlayerSource, mirroredCenter, radius, duration);
    }
    
    /**
     * CLIENT: Display projectile hit effect.
     */
    private void handleProjectileHitEffect(NetworkMessage message) {
        double[] data = message.parseEffectProjectileHit();
        if (data == null) return;
        
        double x = data[0];
        double y = data[1];
        
        // Mirror for CLIENT
        final double maxX = Arena.WIDTH - 1;
        final double maxY = Arena.HEIGHT - 1;
        double mirroredX = maxX - x;
        double mirroredY = maxY - y;
        
        // Simple hit effect - publish as small area effect
        GridPosition mirroredPos = new GridPosition((int) Math.round(mirroredX), (int) Math.round(mirroredY));
        GameEventBus.getInstance().publishAreaEffect(data[2] < 0.5, mirroredPos, 0.5, 0.2);
    }

    /**
     * CLIENT: Apply the FULL state from HOST.
     * This is the single source of truth - CLIENT just renders it.
     */
    private void applyStateFromHost(NetworkMessage message) {
        NetworkGameStateSnapshot snapshot = message.parseGameStateSync();
        if (snapshot == null) return;
        
        // Sync game time
        gameState.setGameTime(snapshot.getGameTime());
        
        // Sync scores (MIRROR: HOST's player score = CLIENT's opponent score)
        gameState.setPlayerScore(snapshot.getPlayer2Score());
        gameState.setBotScore(snapshot.getPlayer1Score());
        
        // Sync elixir (MIRROR)
        gameState.getPlayerElixir().setCurrentElixir(snapshot.getPlayer2Elixir());
        gameState.getBotElixir().setCurrentElixir(snapshot.getPlayer1Elixir());
        
        if (snapshot.isDoubleElixir()) {
            gameState.setDoubleElixir(true);
        }
        
        // Sync towers (MIRROR)
        for (NetworkGameStateSnapshot.TowerSnapshot ts : snapshot.getTowers()) {
            for (Tower tower : gameState.getArena().getAllTowers()) {
                boolean isMyTower = !ts.isPlayerSide(); // Mirror ownership
                if (tower.getType().name().equals(ts.getType()) && tower.isPlayerSide() == isMyTower) {
                    tower.setCurrentHealth(ts.getHealth());
                }
            }
        }
        
        // Sync troops (MIRROR positions - 180° rotation)
        syncTroopsFromHost(snapshot.getTroops());
        
        // Sync buildings (MIRROR)
        syncBuildingsFromHost(snapshot.getBuildings());
        
        // Sync projectiles (MIRROR)
        syncProjectilesFromHost(snapshot.getProjectiles());
        
        // Sync game over
        if (snapshot.isGameOver() && !gameEnded) {
            int winner = snapshot.getWinner();
            // Mirror winner
            if (winner == 1) winner = 2;
            else if (winner == 2) winner = 1;
            gameState.setGameOver(true, winner == 1, winner == 3);
        }
    }

    /**
     * Sync troops from HOST with 180° coordinate mirroring.
     * For a grid of size W x H (0-indexed from 0 to W-1, 0 to H-1):
     * - Mirrored X = (W - 1) - X
     * - Mirrored Y = (H - 1) - Y
     */
    private void syncTroopsFromHost(List<NetworkGameStateSnapshot.TroopSnapshot> hostTroops) {
        List<Troop> currentTroops = gameState.getTroops();
        Set<Integer> hostTroopIds = new HashSet<>();
        
        // Arena dimensions for mirroring (0-indexed, so max index is DIM - 1)
        final double maxX = Arena.WIDTH - 1;  // 17 for width of 18
        final double maxY = Arena.HEIGHT - 1; // 31 for height of 32
        
        for (NetworkGameStateSnapshot.TroopSnapshot ts : hostTroops) {
            hostTroopIds.add(ts.getId());
            
            // Mirror BOTH axes (180° rotation around center)
            double mirroredX = maxX - ts.getX();
            double mirroredY = maxY - ts.getY();
            double mirroredTargetX = maxX - ts.getTargetX();
            double mirroredTargetY = maxY - ts.getTargetY();
            boolean isMyTroop = !ts.isPlayerSide(); // Mirror ownership
            
            Troop existingTroop = troopMap.get(ts.getId());
            
            if (existingTroop != null && currentTroops.contains(existingTroop)) {
                // Update existing troop position and state
                existingTroop.setWorldPosition(mirroredX, mirroredY);
                existingTroop.setTargetWorldPosition(mirroredTargetX, mirroredTargetY);
                existingTroop.setCurrentHealth(ts.getHealth());
            } else {
                // Create new troop with the HOST's ID
                Card card = model.getCardByName(ts.getCardName());
                if (card != null) {
                    GridPosition pos = new GridPosition((int) mirroredX, (int) mirroredY);
                    Troop newTroop = new Troop(card, pos, isMyTroop);
                    newTroop.setId(ts.getId());  // Use HOST's ID for consistent tracking
                    newTroop.setWorldPosition(mirroredX, mirroredY);
                    newTroop.setTargetWorldPosition(mirroredTargetX, mirroredTargetY);
                    newTroop.setCurrentHealth(ts.getHealth());
                    currentTroops.add(newTroop);
                    troopMap.put(ts.getId(), newTroop);
                }
            }
        }
        
        // Remove troops that no longer exist on HOST
        Iterator<Map.Entry<Integer, Troop>> it = troopMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Troop> entry = it.next();
            if (!hostTroopIds.contains(entry.getKey())) {
                currentTroops.remove(entry.getValue());
                it.remove();
            }
        }
    }

    /**
     * Sync buildings from HOST with 180° coordinate mirroring.
     */
    private void syncBuildingsFromHost(List<NetworkGameStateSnapshot.BuildingSnapshot> hostBuildings) {
        List<Building> currentBuildings = gameState.getBuildings();
        Set<Integer> hostBuildingIds = new HashSet<>();
        
        final double maxX = Arena.WIDTH - 1;
        final double maxY = Arena.HEIGHT - 1;
        
        for (NetworkGameStateSnapshot.BuildingSnapshot bs : hostBuildings) {
            hostBuildingIds.add(bs.getId());
            
            double mirroredX = maxX - bs.getX();
            double mirroredY = maxY - bs.getY();
            boolean isMyBuilding = !bs.isPlayerSide();
            
            Building existingBuilding = buildingMap.get(bs.getId());
            
            if (existingBuilding != null && currentBuildings.contains(existingBuilding)) {
                existingBuilding.setCurrentHealth(bs.getHealth());
            } else {
                Card card = model.getCardByName(bs.getCardName());
                if (card != null) {
                    GridPosition pos = new GridPosition((int) mirroredX, (int) mirroredY);
                    int bw = Math.max(1, card.getFootprintWidthTiles());
                    int bh = Math.max(1, card.getFootprintHeightTiles());
                    Building newBuilding = new Building(pos, bw, bh, isMyBuilding,
                            card.getHp(), card.getImagePath(), card.getLifetime());
                    newBuilding.setId(bs.getId());  // Use HOST's ID for consistent tracking
                    newBuilding.configureCombatFromCard(card);
                    newBuilding.setCurrentHealth(bs.getHealth());
                    currentBuildings.add(newBuilding);
                    buildingMap.put(bs.getId(), newBuilding);
                }
            }
        }
        
        Iterator<Map.Entry<Integer, Building>> it = buildingMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Building> entry = it.next();
            if (!hostBuildingIds.contains(entry.getKey())) {
                currentBuildings.remove(entry.getValue());
                it.remove();
            }
        }
    }
    
    /**
     * Sync projectiles from HOST with 180° coordinate mirroring.
     */
    private void syncProjectilesFromHost(List<NetworkGameStateSnapshot.ProjectileSnapshot> hostProjectiles) {
        List<Projectile> currentProjectiles = gameState.getProjectiles();
        
        final double maxX = Arena.WIDTH - 1;
        final double maxY = Arena.HEIGHT - 1;
        
        // Clear and rebuild projectiles each frame (they're short-lived)
        currentProjectiles.clear();
        
        for (NetworkGameStateSnapshot.ProjectileSnapshot ps : hostProjectiles) {
            double mirroredX = maxX - ps.getX();
            double mirroredY = maxY - ps.getY();
            double mirroredTargetX = maxX - ps.getTargetX();
            double mirroredTargetY = maxY - ps.getTargetY();
            boolean isMyProjectile = !ps.isPlayerSide();
            
            // Create a simple projectile for rendering
            Projectile proj = new Projectile(
                new Vector2(mirroredX, mirroredY),
                new Vector2(mirroredTargetX, mirroredTargetY),
                isMyProjectile
            );
            currentProjectiles.add(proj);
        }
    }

    /**
     * HOST: Handle card placement from CLIENT.
     */
    private void handleClientCardPlacement(NetworkMessage message) {
        String[] data = message.parseCardPlacement();
        if (data == null) return;
        
        String cardName = data[0];
        int x = (int) Double.parseDouble(data[1]);
        int y = (int) Double.parseDouble(data[2]);
        
        // Mirror BOTH axes (CLIENT sends in their perspective)
        int mirroredX = (Arena.WIDTH - 1) - x;  // 17 - x for width 18
        int mirroredY = (Arena.HEIGHT - 1) - y; // 31 - y for height 32
        
        Card card = model.getCardByName(cardName);
        if (card != null) {
            double oppElixir = gameState.getBotElixir().getCurrentElixir();
            if (oppElixir >= card.getCost()) {
                gameState.getBotElixir().spend(card.getCost());
                gameState.placeCard(false, card, mirroredX, mirroredY);
                System.out.println("[HOST] Client placed " + cardName + " at (" + mirroredX + "," + mirroredY + ")");
            }
        }
    }

    // ==================== UI Interactions ====================

    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex == -1) return;
        if (tileX < 0 || tileX >= Arena.WIDTH || tileY < 0 || tileY >= Arena.HEIGHT) return;
        
        Card card = gameState.getPlayerHand().getCard(selectedIndex);
        if (card == null) return;
        if (gameState.getPlayerElixir().getCurrentElixir() < card.getCost()) return;

        if (networkService.isHost()) {
            // HOST: Place directly in the simulation
            gameState.placeCard(true, selectedIndex, tileX, tileY);
            System.out.println("[HOST] Placed " + card.getName() + " at (" + tileX + "," + tileY + ")");
        } else {
            // CLIENT: Send placement request to HOST
            networkService.send(NetworkMessage.cardPlaced(
                networkService.getPlayerId(), card.getName(), tileX, tileY));
            // Optimistic update for responsive UI
            gameState.getPlayerElixir().spend(card.getCost());
            gameState.getPlayerHand().playCard(selectedIndex);
            System.out.println("[CLIENT] Sent " + card.getName() + " at (" + tileX + "," + tileY + ")");
        }

        handView.clearSelection();
        arenaView.highlightValidCells(false, false);
    }

    private void updateTimeDisplay() {
        if (timeLabel != null) {
            int seconds = (int) gameState.getGameTime();
            timeLabel.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
        }
    }

    private void updateScoreDisplay() {
        if (playerScoreLabel != null) playerScoreLabel.setText(String.valueOf(gameState.getPlayerScore()));
        if (opponentScoreLabel != null) opponentScoreLabel.setText(String.valueOf(gameState.getBotScore()));
    }

    // ==================== Pause/Menu ====================

    @FXML private void handlePause() {
        if (gameEnded) return;
        SoundEffectUtil.playButtonClick();
        isPaused = true;
        showPauseMenu();
    }

    private void showPauseMenu() {
        pauseMenuContainer.getChildren().clear();
        pauseMenuContainer.setVisible(true);
        PauseMenuView menu = new PauseMenuView(new PauseMenuView.PauseMenuListener() {
            @Override public void onResume() { handleResume(); }
            @Override public void onSaveAndResume() { handleResume(); }
            @Override public void onSaveAndExit() { forfeitAndExit(); }
            @Override public void onExitWithoutSaving() { forfeitAndExit(); }
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
            new Timeline(new KeyFrame(Duration.millis(200), e -> navigateToMenu())).play();
        } else {
            navigateToMenu();
        }
    }

    private void navigateToMenu() {
        cleanup();
        try {
            sceneLoader.load(arenaContainer, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleBackToMenu() {
        SoundEffectUtil.playButtonClick();
        if (!gameEnded && networkService != null && networkService.isConnected()) forfeitAndExit();
        else navigateToMenu();
    }

    // ==================== Connection Handling ====================

    private void handleOpponentDisconnected() {
        if (gameEnded) return;
        showDisconnectionOverlay();
        isPaused = true;
        final int[] countdown = { 5 };
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;
            if (reconnectCountdownLabel != null) reconnectCountdownLabel.setText(String.valueOf(countdown[0]));
            if (countdown[0] <= 0) { hideDisconnectionOverlay(); showVictory("Opponent left!"); }
        }));
        timer.setCycleCount(5);
        timer.play();
    }

    private void updateConnectionStatus(ConnectionState state) {
        if (connectionIndicator == null) return;
        switch (state) {
            case CONNECTED -> { connectionIndicator.setFill(Color.LIME); if (connectionStatusLabel != null) connectionStatusLabel.setText("Connected"); }
            case RECONNECTING -> { connectionIndicator.setFill(Color.ORANGE); if (connectionStatusLabel != null) connectionStatusLabel.setText("Reconnecting..."); }
            case DISCONNECTED -> { connectionIndicator.setFill(Color.RED); if (connectionStatusLabel != null) connectionStatusLabel.setText("Disconnected"); }
            default -> {}
        }
    }

    private void showDisconnectionOverlay() { if (disconnectionOverlay != null) disconnectionOverlay.setVisible(true); isPaused = true; }
    private void hideDisconnectionOverlay() { if (disconnectionOverlay != null) disconnectionOverlay.setVisible(false); isPaused = false; }

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
        gameEnded = true; cleanup();
        if (resultLabel != null) { resultLabel.setText("VICTORY!"); resultLabel.setStyle("-fx-text-fill: gold; -fx-font-size: 48px; -fx-font-weight: bold;"); }
        if (resultDetailsLabel != null) resultDetailsLabel.setText(details);
        if (resultOverlay != null) resultOverlay.setVisible(true);
        ServiceFactory.getInstance().getAchievementService().updateProgress(AchievementType.FIRST_BLOOD, 1);
        ServiceFactory.getInstance().getQuestService().updateProgress(QuestType.WIN_MATCHES, 1);
    }

    private void showDefeat(String details) {
        gameEnded = true; cleanup();
        if (resultLabel != null) { resultLabel.setText("DEFEAT"); resultLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 48px; -fx-font-weight: bold;"); }
        if (resultDetailsLabel != null) resultDetailsLabel.setText(details);
        if (resultOverlay != null) resultOverlay.setVisible(true);
    }

    private void showDraw() {
        gameEnded = true; cleanup();
        if (resultLabel != null) { resultLabel.setText("DRAW"); resultLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 48px; -fx-font-weight: bold;"); }
        if (resultDetailsLabel != null) resultDetailsLabel.setText("Equal towers destroyed!");
        if (resultOverlay != null) resultOverlay.setVisible(true);
    }

    private void cleanup() {
        if (gameLoop != null) gameLoop.stop();
        if (networkService.isHost()) {
            GameEventBus.getInstance().unsubscribe(this);
        }
    }
}
