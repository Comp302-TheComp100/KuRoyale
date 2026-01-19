package com.kuroyale.controller;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.ArenaLayout;
import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.arena.Vector2;
import com.kuroyale.model.dto.NetworkMessage;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.state.GameState;
import com.kuroyale.service.game.BattleSessionService;
import com.kuroyale.service.network.NetworkService;
import com.kuroyale.service.network.NetworkService.ConnectionState;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ui.ElixirBarView;
import com.kuroyale.view.battle.ui.HandView;
import com.kuroyale.view.battle.ui.PauseMenuView;
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
import java.util.Locale;
import javafx.util.Duration;

/**
 * Controller for Network Battle gameplay.
 * 
 * ARCHITECTURE:
 * - HOST runs the SINGLE authoritative game loop (gameState.update())
 * - HOST broadcasts full state to CLIENT periodically
 * - CLIENT does NOT run game logic - it ONLY renders state received from HOST
 * - Card placements from CLIENT are sent to HOST, which processes them
 * 
 * SYMMETRY:
 * - Each player sees themselves at the bottom of the arena
 * - HOST's player side = CLIENT's opponent side (and vice versa)
 * - Coordinates are mirrored: X' = Arena.WIDTH - X, Y' = Arena.HEIGHT - Y
 * - Tower ownership is flipped: HOST's player towers = CLIENT's enemy towers
 */
public class NetworkBattleController implements GameEventListener {

    // FXML Components
    @FXML
    private StackPane arenaContainer;
    @FXML
    private HBox elixirContainer;
    @FXML
    private VBox handContainer;
    @FXML
    private VBox pauseMenuContainer;

    // Network status indicators
    @FXML
    private Circle connectionIndicator;
    @FXML
    private Label connectionStatusLabel;
    @FXML
    private Label pingLabel;
    @FXML
    private Label lagIndicator;

    // Timer display
    @FXML
    private Label timerLabel;

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
    private final BattleSessionService model = new BattleSessionService();

    // Game components
    private NetworkService networkService;
    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBarView elixirBar;
    private HandView handView;
    private AnimationTimer renderLoop; // Render loop (both HOST and CLIENT)
    private Timeline syncTimer; // Sync timer (HOST only broadcasts)

    // Game state flags
    private boolean isPaused = false;
    private boolean gameEnded = false;
    private boolean doubleElixirShown = false;

    // Network timing
    private long lastPingTime = 0;
    private long currentPing = 0;

    // HOST is the single source of truth
    private boolean isHost = false;

    // Sync interval (33ms = 30 updates per second for smooth updates)
    private static final int SYNC_INTERVAL_MS = 33;

    @FXML
    private void initialize() {
        // Network service will be set via setter
    }

    /**
     * Sets the network service and starts the game.
     */
    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
        this.isHost = networkService.isHost();

        System.out.println("[NetworkBattle] ====================================");
        System.out.println(
                "[NetworkBattle] Initializing as " + (isHost ? "HOST (AUTHORITATIVE)" : "CLIENT (RENDER ONLY)"));
        System.out.println("[NetworkBattle] ====================================");

        setupNetworkCallbacks();
        initializeGame();
        startRenderLoop();

        // HOST: Start sync timer to broadcast state
        if (isHost) {
            startSyncTimer();
        }

        // Subscribe to game events
        GameEventBus.getInstance().subscribe(this);

        // Add window close handler for graceful disconnection
        setupWindowCloseHandler();
    }

    /**
     * Sets up handler to detect when window is closed, ensuring graceful
     * disconnection.
     */
    private void setupWindowCloseHandler() {
        // Use Platform.runLater to ensure the scene is available
        Platform.runLater(() -> {
            if (arenaContainer != null && arenaContainer.getScene() != null) {
                javafx.stage.Stage stage = (javafx.stage.Stage) arenaContainer.getScene().getWindow();
                if (stage != null) {
                    stage.setOnCloseRequest(event -> {
                        System.out.println("[NetworkBattle] Window close detected - sending disconnect notification");

                        // Send disconnect/forfeit message to opponent
                        if (networkService != null && networkService.isConnected()) {
                            if (!gameEnded) {
                                networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));
                            }
                            // Small delay to ensure message is sent
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }

                        // Cleanup
                        cleanup();
                    });
                    System.out.println("[NetworkBattle] Window close handler registered");
                }
            }
        });
    }

    private void setupNetworkCallbacks() {
        networkService.setOnMessageReceived(message -> Platform.runLater(() -> handleNetworkMessage(message)));

        networkService.setOnStateChanged(state -> Platform.runLater(() -> {
            updateConnectionStatus(state);

            if (state == ConnectionState.RECONNECTING) {
                showDisconnectionOverlay();
            } else if (state == ConnectionState.CONNECTED && disconnectionOverlay != null
                    && disconnectionOverlay.isVisible()) {
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

        // Create player's deck
        Deck playerDeck = model.createDeckFromNames(currentUser.getDeck());

        // Determine arena layout (HOST's layout is used by both)
        // CLIENT uses HOST's layout for complete synchronization
        ArenaLayout layoutToUse;
        if (!isHost && networkService.getHostArenaLayout() != null) {
            ArenaLayout hostLayout = networkService.getHostArenaLayout();
            // CLIENT uses HOST's layout with mirrored bridges AND tower positions
            // This ensures projectiles and effects appear at correct positions
            layoutToUse = mirrorLayoutForClient(hostLayout);
            System.out.println(
                    "[NetworkBattle] CLIENT using HOST's arena layout (fully mirrored): " + layoutToUse.getName());
        } else if (!isHost) {
            // CLIENT but no host layout received - wait briefly and check again
            System.out.println("[NetworkBattle] CLIENT: Waiting for HOST's arena layout...");
            try {
                Thread.sleep(500); // Brief wait for layout to arrive
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (networkService.getHostArenaLayout() != null) {
                layoutToUse = mirrorLayoutForClient(networkService.getHostArenaLayout());
                System.out
                        .println("[NetworkBattle] CLIENT: Received HOST's layout after wait: " + layoutToUse.getName());
            } else {
                // Fallback to own layout if HOST's not received (shouldn't happen normally)
                layoutToUse = model.loadArenaLayout();
                System.out.println("[NetworkBattle] CLIENT: WARNING - Using own layout as fallback!");
            }
        } else {
            layoutToUse = model.loadArenaLayout();
            System.out.println("[NetworkBattle] HOST using own arena layout: " + layoutToUse.getName());
        }

        // Create Arena
        Arena arena = model.createArena(layoutToUse);

        // Create opponent deck
        Deck opponentDeck = model.createBotDeck(currentUser);

        // Initialize GameState
        gameState = new GameState(playerDeck, opponentDeck, arena);
        gameState.setCardCatalog(name -> model.getCardByName(name));
        gameState.setNetworkMode(true); // Disable bot AI

        // Initialize UI Components
        arenaView = new BattleArenaView(gameState);
        arenaContainer.getChildren().add(arenaView);

        arenaView.setOnGridClicked((tileX, tileY) -> handleArenaClick(tileX, tileY));

        elixirBar = new ElixirBarView(gameState.getPlayerElixir());
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
     * Starts the render loop. Both HOST and CLIENT run this.
     * - HOST: Runs full game logic
     * - CLIENT: Updates elixir locally (for responsive UI) but receives
     * authoritative state from HOST
     */
    private void startRenderLoop() {
        renderLoop = new AnimationTimer() {
            private long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                if (isPaused || gameEnded)
                    return;

                double deltaTime = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                try {
                    if (isHost) {
                        // HOST: Run full game logic - THE SINGLE AUTHORITATIVE GAME LOOP
                        gameState.update(deltaTime);

                        // Check game over
                        if (gameState.isGameOver() && !gameEnded) {
                            endGame();
                        }
                    } else {
                        // CLIENT: Update elixir and timer locally for responsive UI
                        // (Will be corrected by FULL_STATE_SYNC from HOST)
                        gameState.getPlayerElixir().update(deltaTime);

                        // Update local timer (will be synced from HOST)
                        if (gameState.getGameTime() > 0) {
                            gameState.setGameTime(gameState.getGameTime() - deltaTime);
                        }
                    }

                    // Update UI (both HOST and CLIENT)
                    updateUI(deltaTime);
                } catch (Exception e) {
                    System.err.println("[NetworkBattle] Error in render loop: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        renderLoop.start();
    }

    /**
     * HOST: Starts the sync timer to broadcast state to client.
     */
    private void startSyncTimer() {
        syncTimer = new Timeline(new KeyFrame(Duration.millis(SYNC_INTERVAL_MS), e -> {
            if (!gameEnded && networkService.isConnected()) {
                broadcastFullState();
            }
        }));
        syncTimer.setCycleCount(Timeline.INDEFINITE);
        syncTimer.play();
        System.out.println("[NetworkBattle] HOST sync timer started (interval: " + SYNC_INTERVAL_MS + "ms)");
    }

    /**
     * HOST: Broadcasts complete game state to client.
     */
    private void broadcastFullState() {
        if (!isHost)
            return;

        // 1. Send full entity state (troops, buildings, projectiles)
        String troopData = serializeTroopsForNetwork();
        String buildingData = serializeBuildingsForNetwork();
        String projectileData = serializeProjectilesForNetwork();

        // Log sync details
        int troopCount = troopData.isEmpty() ? 0 : troopData.split("\\|").length;
        int buildingCount = buildingData.isEmpty() ? 0 : buildingData.split("\\|").length;
        System.out.println(
                "[NetworkBattle] HOST: Broadcasting state - Troops: " + troopCount + ", Buildings: " + buildingCount);

        networkService.send(NetworkMessage.fullStateSync(
                troopData,
                buildingData,
                gameState.getGameTime(),
                gameState.getPlayerElixir().getCurrentElixir(),
                gameState.getBotElixir().getCurrentElixir(),
                gameState.getPlayerScore(),
                gameState.getBotScore(),
                gameState.isDoubleElixir(),
                gameState.isGameOver()));

        // 2. Send tower health sync
        String towerData = serializeTowersForNetwork();
        if (towerData != null && !towerData.isEmpty()) {
            networkService.send(NetworkMessage.towerSync(towerData));
        }

        // 3. Send projectile sync for visual effects
        if (projectileData != null && !projectileData.isEmpty()) {
            networkService.send(NetworkMessage.troopSync(projectileData)); // Reuse troopSync message type
        }
    }

    /**
     * Serializes troops for network transmission.
     * Format: cardName,worldX,worldY,health,isPlayerSide,state|...
     */
    private String serializeTroopsForNetwork() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Troop troop : gameState.getActiveTroops()) {
            if (!troop.isAlive())
                continue;

            if (!first)
                sb.append("|");
            first = false;

            Card card = troop.getBaseCard();
            Vector2 pos = troop.getWorldPosition();

            sb.append(card != null ? card.getName() : "Unknown")
                    .append(",").append(String.format(Locale.US, "%.2f", pos.getX()))
                    .append(",").append(String.format(Locale.US, "%.2f", pos.getY()))
                    .append(",").append(troop.getCurrentHealth())
                    .append(",").append(troop.isPlayerSide())
                    .append(",").append(troop.getUnitState().name());
        }

        return sb.toString();
    }

    /**
     * Serializes buildings for network transmission.
     * Format:
     * cardName,gridX,gridY,health,isPlayerSide,lifetime,width,height,maxHealth,imagePath|...
     */
    private String serializeBuildingsForNetwork() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Building building : gameState.getActiveBuildings()) {
            if (!building.isAlive())
                continue;

            if (!first)
                sb.append("|");
            first = false;

            GridPosition pos = building.getPosition();

            sb.append(building.getCardName() != null ? building.getCardName() : "Building")
                    .append(",").append(pos.getX())
                    .append(",").append(pos.getY())
                    .append(",").append(building.getCurrentHealth())
                    .append(",").append(building.isPlayerSide())
                    .append(",").append(String.format(Locale.US, "%.1f", building.getRemainingLifetime()))
                    .append(",").append(building.getWidth())
                    .append(",").append(building.getHeight())
                    .append(",").append(building.getMaxHealth())
                    .append(",").append(building.getImagePath() != null ? building.getImagePath() : "");
        }

        return sb.toString();
    }

    /**
     * Serializes projectiles for network transmission (for visual effects).
     * Format: PROJ#x,y,targetX,targetY,isPlayerSide|...
     */
    private String serializeProjectilesForNetwork() {
        StringBuilder sb = new StringBuilder();
        sb.append("PROJ#");
        boolean first = true;

        for (Projectile proj : gameState.getProjectiles()) {
            if (!proj.isActive())
                continue;

            if (!first)
                sb.append("|");
            first = false;

            Vector2 pos = proj.getPosition();
            ICombatant target = proj.getTarget();
            double targetX = pos.getX();
            double targetY = pos.getY();
            if (target != null && target.getCenterPosition() != null) {
                targetX = target.getCenterPosition().getX();
                targetY = target.getCenterPosition().getY();
            }

            sb.append(String.format(Locale.US, "%.2f", pos.getX()))
                    .append(",").append(String.format(Locale.US, "%.2f", pos.getY()))
                    .append(",").append(String.format(Locale.US, "%.2f", targetX))
                    .append(",").append(String.format(Locale.US, "%.2f", targetY))
                    .append(",").append(proj.isPlayerSide());
        }

        return sb.toString();
    }

    /**
     * Serializes towers for network transmission.
     * Format:
     * towerType,isPlayerSide,currentHealth,maxHealth,gridX,gridY,isAlive;...
     */
    private String serializeTowersForNetwork() {
        if (gameState == null || gameState.getArena() == null)
            return null;

        StringBuilder sb = new StringBuilder();
        java.util.Set<Tower> towers = gameState.getArena().getAllTowers();

        boolean first = true;
        for (Tower tower : towers) {
            if (!first)
                sb.append(";");
            first = false;

            GridPosition pos = tower.getPosition();
            sb.append(tower.getType().name())
                    .append(",").append(tower.isPlayerSide())
                    .append(",").append(tower.getCurrentHealth())
                    .append(",").append(tower.getMaxHealth())
                    .append(",").append(pos != null ? pos.getX() : 0)
                    .append(",").append(pos != null ? pos.getY() : 0)
                    .append(",").append(tower.isAlive());
        }

        return sb.toString();
    }

    /**
     * Updates UI elements (called by both HOST and CLIENT).
     */
    private void updateUI(double deltaTime) {
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);

        updateTimerDisplay();
        updateScoreDisplay();

        // Check for Double Elixir
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            elixirBar.setDoubleElixirActive(true);
        }
    }

    // ==================== Network Message Handling ====================

    private void handleNetworkMessage(NetworkMessage message) {
        switch (message.getType()) {
            case CARD_PLACED:
                handleOpponentCardPlaced(message);
                break;

            case FULL_STATE_SYNC:
                if (!isHost) {
                    handleFullStateSync(message);
                }
                break;

            case TOWER_SYNC:
                if (!isHost) {
                    handleTowerSync(message);
                }
                break;

            case TROOP_SYNC:
                // Used for projectile sync (reusing message type)
                if (!isHost) {
                    handleProjectileSync(message);
                }
                break;

            case GAME_OVER:
                if (!isHost) {
                    handleGameOver(message);
                }
                break;

            case HEARTBEAT:
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
                    handleOpponentForfeit();
                }
                break;

            case OPPONENT_DISCONNECTED:
                handleOpponentDisconnected();
                break;

            case AREA_EFFECT:
                if (!isHost) {
                    handleAreaEffectSync(message);
                }
                break;

            case SPELL_CAST:
                if (!isHost) {
                    handleSpellCastSync(message);
                }
                break;

            default:
                break;
        }
    }

    /**
     * CLIENT: Handles area effect sync from HOST for visual effects.
     * Mirrors the position for client's perspective.
     */
    private void handleAreaEffectSync(NetworkMessage message) {
        String data = message.getData();
        if (data == null || data.isEmpty())
            return;

        try {
            String[] parts = data.split(";");
            if (parts.length < 6)
                return;

            boolean hostIsPlayerSource = Boolean.parseBoolean(parts[0]);
            double centerX = Double.parseDouble(parts[1]);
            double centerY = Double.parseDouble(parts[2]);
            double radius = Double.parseDouble(parts[3]);
            double duration = Double.parseDouble(parts[4]);
            String effectType = parts[5];

            // Mirror for client perspective (180° rotation)
            boolean clientIsPlayerSource = !hostIsPlayerSource;
            double clientCenterX = Arena.WIDTH - centerX;
            double clientCenterY = Arena.HEIGHT - centerY;

            // Trigger the visual effect on the client's arena view
            Vector2 clientCenter = new Vector2(clientCenterX, clientCenterY);
            arenaView.onAreaEffect(clientIsPlayerSource, clientCenter, radius, duration, effectType);

        } catch (Exception e) {
            System.err.println("[NetworkBattle] Error parsing area effect: " + e.getMessage());
        }
    }

    /**
     * CLIENT: Handles spell cast sync from HOST for spell animations.
     * Mirrors the position for client's perspective.
     */
    private void handleSpellCastSync(NetworkMessage message) {
        String data = message.getData();
        if (data == null || data.isEmpty())
            return;

        try {
            String[] parts = data.split(";");
            if (parts.length < 5)
                return;

            boolean hostIsPlayer = Boolean.parseBoolean(parts[0]);
            String cardName = parts[1];
            int centerX = Integer.parseInt(parts[2]);
            int centerY = Integer.parseInt(parts[3]);

            // Mirror for client perspective (180° rotation)
            boolean clientIsPlayer = !hostIsPlayer;
            int clientCenterX = Arena.WIDTH - 1 - centerX;
            int clientCenterY = Arena.HEIGHT - 1 - centerY;

            // Get the spell card for visual effect
            Card spell = model.getCardByName(cardName);
            if (spell != null) {
                GridPosition clientCenter = GridPosition.tryCreate(clientCenterX, clientCenterY);
                if (clientCenter != null) {
                    arenaView.onSpellCast(clientIsPlayer, spell, clientCenter);
                }
            }

        } catch (Exception e) {
            System.err.println("[NetworkBattle] Error parsing spell cast: " + e.getMessage());
        }
    }

    /**
     * CLIENT: Handles projectile sync from HOST for visual effects.
     */
    private void handleProjectileSync(NetworkMessage message) {
        String data = message.getData();
        if (data == null || !data.startsWith("PROJ#"))
            return;

        String projData = data.substring(5);
        if (projData.isEmpty())
            return;

        // Clear existing projectiles
        gameState.getProjectiles().clear();

        String[] projectiles = projData.split("\\|");
        for (String projStr : projectiles) {
            if (projStr.isEmpty())
                continue;

            String[] parts = projStr.split(",");
            if (parts.length < 5)
                continue;

            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                double targetX = Double.parseDouble(parts[2]);
                double targetY = Double.parseDouble(parts[3]);
                boolean hostIsPlayerSide = Boolean.parseBoolean(parts[4]);

                // Mirror for client perspective: (W-x, H-y)
                boolean clientIsPlayerSide = !hostIsPlayerSide;
                double clientX = Arena.WIDTH - x;
                double clientY = Arena.HEIGHT - y;
                double clientTargetX = Arena.WIDTH - targetX;
                double clientTargetY = Arena.HEIGHT - targetY;

                // Clamp to valid arena bounds
                clientX = Math.max(0, Math.min(Arena.WIDTH - 1, clientX));
                clientY = Math.max(0, Math.min(Arena.HEIGHT - 1, clientY));
                clientTargetX = Math.max(0, Math.min(Arena.WIDTH - 1, clientTargetX));
                clientTargetY = Math.max(0, Math.min(Arena.HEIGHT - 1, clientTargetY));

                // Create a visual-only projectile for rendering
                Projectile proj = new Projectile(
                        new Vector2(clientX, clientY),
                        new Vector2(clientTargetX, clientTargetY),
                        clientIsPlayerSide);
                gameState.addProjectile(proj);

            } catch (Exception e) {
                System.err.println("[NetworkBattle] Error parsing projectile: " + projStr);
            }
        }
    }

    /**
     * Handles opponent's card placement.
     * 
     * ARCHITECTURE:
     * - HOST processes CARD_PLACED from CLIENT (spawns troops for the client)
     * - CLIENT does NOT process CARD_PLACED from HOST (troops come via
     * FULL_STATE_SYNC)
     * 
     * SYMMETRY: Both X and Y are mirrored (180° rotation) because players
     * sit at opposite ends of the arena.
     */
    private void handleOpponentCardPlaced(NetworkMessage message) {
        // CLIENT: Do NOT spawn troops locally!
        // CLIENT receives all entity positions via FULL_STATE_SYNC from HOST
        if (!isHost) {
            System.out.println("[NetworkBattle] CLIENT: CARD_PLACED received (entities will arrive via sync)");
            return;
        }

        // HOST: Process the client's card placement
        String[] data = message.parseCardPlacement();
        if (data == null)
            return;

        String cardName = data[0];
        int x = (int) Double.parseDouble(data[1]);
        int y = (int) Double.parseDouble(data[2]);

        // Mirror BOTH X and Y (180° rotation) for proper symmetry
        // CLIENT→HOST uses (W-1-x, H-1-y) which is inverse of HOST→CLIENT display
        int mirroredX = (Arena.WIDTH - 1) - x;
        int mirroredY = (Arena.HEIGHT - 1) - y;

        // Clamp to valid arena bounds (half of arena for opponent side)
        mirroredX = Math.max(0, Math.min(Arena.WIDTH - 1, mirroredX));
        mirroredY = Math.max(0, Math.min(Arena.HEIGHT / 2 - 1, mirroredY)); // Opponent spawns in top half

        Card card = model.getCardByName(cardName);
        if (card == null) {
            System.err.println("[NetworkBattle] Unknown card: " + cardName);
            return;
        }

        System.out.println("[NetworkBattle] HOST: Spawning client's " + cardName +
                " at (" + mirroredX + ", " + mirroredY + ") [original: (" + x + ", " + y + ")]");

        // Deduct elixir from the "bot" (client's) elixir pool for consistency
        int cost = card.getCost();
        if (gameState.getBotElixir().getCurrentElixir() >= cost) {
            gameState.getBotElixir().spend(cost);
        }

        // Spawn as opponent (isPlayer=false from HOST's perspective = client's troop)
        gameState.placeCard(false, card, mirroredX, mirroredY);
    }

    /**
     * CLIENT: Handles full state sync from HOST.
     * Recreates all entities based on HOST's authoritative state.
     */
    private void handleFullStateSync(NetworkMessage message) {
        if (gameState == null)
            return;

        String data = message.getData();
        if (data == null || data.isEmpty())
            return;

        try {
            // Split by section delimiter (@@) - NOT entity delimiter (|)
            String[] sections = data.split(NetworkMessage.SECTION_DELIMITER);
            String troopData = "";
            String buildingData = "";
            String gameData = "";

            for (String section : sections) {
                if (section.startsWith("TROOPS#")) {
                    troopData = section.substring(7);
                } else if (section.startsWith("BUILDINGS#")) {
                    buildingData = section.substring(10);
                } else if (section.startsWith("GAME#")) {
                    gameData = section.substring(5);
                }
            }

            // Sync entities from HOST state without full clear
            // Parse expected entities from HOST
            java.util.Set<String> expectedTroops = new java.util.HashSet<>();
            java.util.Set<String> expectedBuildings = new java.util.HashSet<>();

            // Track what HOST expects to exist
            if (!troopData.isEmpty()) {
                String[] troops = troopData.split("\\|");
                for (String troop : troops) {
                    if (!troop.isEmpty()) {
                        expectedTroops.add(troop);
                    }
                }
            }

            if (!buildingData.isEmpty()) {
                String[] buildings = buildingData.split("\\|");
                for (String building : buildings) {
                    if (!building.isEmpty()) {
                        expectedBuildings.add(building);
                    }
                }
            }

            // Apply troop state with smart updates (updates existing troops instead of
            // recreating)
            try {
                applyTroopState(troopData);
            } catch (Exception e) {
                System.err.println("[NetworkBattle] Error applying troop state: " + e.getMessage());
                e.printStackTrace();
            }

            // Apply building state with smart updates (updates existing buildings instead
            // of recreating)
            try {
                applyBuildingState(buildingData);
            } catch (Exception e) {
                System.err.println("[NetworkBattle] Error applying building state: " + e.getMessage());
                e.printStackTrace();
            }

            // Apply game state
            if (!gameData.isEmpty()) {
                try {
                    applyGameData(gameData);
                } catch (Exception e) {
                    System.err.println("[NetworkBattle] Error applying game data: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("[NetworkBattle] Error parsing full state sync: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * CLIENT: Applies troop state from HOST.
     * Uses position-based matching to update existing troops instead of recreating.
     * Mirrors positions (180° rotation) and ownership for client's perspective.
     */
    private void applyTroopState(String troopData) {
        String[] troops = troopData.split("\\|");
        java.util.List<Troop> existingTroops = new java.util.ArrayList<>(gameState.getActiveTroops());
        java.util.Set<Troop> matchedTroops = new java.util.HashSet<>();

        for (String troopStr : troops) {
            if (troopStr.isEmpty())
                continue;

            String[] parts = troopStr.split(",");
            if (parts.length < 6)
                continue;

            try {
                String cardName = parts[0];
                double worldX = Double.parseDouble(parts[1]);
                double worldY = Double.parseDouble(parts[2]);
                int health = Integer.parseInt(parts[3]);
                boolean hostIsPlayerSide = Boolean.parseBoolean(parts[4]);
                String state = parts[5];

                // MIRROR for client perspective:
                // - HOST's player troops = CLIENT's opponent troops
                // - HOST's opponent troops = CLIENT's player troops
                boolean clientIsPlayerSide = !hostIsPlayerSide;

                // Mirror BOTH X and Y (180° rotation)
                // Mirror for client perspective: (W-x, H-y)
                double clientWorldX = Arena.WIDTH - worldX;
                double clientWorldY = Arena.HEIGHT - worldY;

                // Clamp to valid arena bounds
                clientWorldX = Math.max(0, Math.min(Arena.WIDTH, clientWorldX));
                clientWorldY = Math.max(0, Math.min(Arena.HEIGHT, clientWorldY));

                // Try to find an existing troop to update (match by card name, side, and
                // proximity)
                Troop matchedTroop = findMatchingTroop(existingTroops, matchedTroops, cardName, clientIsPlayerSide,
                        clientWorldX, clientWorldY);

                if (matchedTroop != null) {
                    // Update existing troop position, health, and state
                    matchedTroop.setWorldPosition(new Vector2(clientWorldX, clientWorldY));
                    matchedTroop.setCurrentHealth(health);
                    if (state != null && !state.isEmpty()) {
                        try {
                            matchedTroop.setUnitState(com.kuroyale.model.enums.UnitState.valueOf(state));
                        } catch (IllegalArgumentException e) {
                            matchedTroop.setUnitState(com.kuroyale.model.enums.UnitState.MOVING);
                        }
                    }
                    matchedTroops.add(matchedTroop);
                } else {
                    // Spawn new troop
                    gameState.spawnTroopAtPosition(cardName, clientWorldX, clientWorldY, health, clientIsPlayerSide,
                            state);
                }

            } catch (Exception e) {
                System.err.println("[NetworkBattle] Error parsing troop: " + troopStr);
            }
        }

        // Remove troops that weren't in the host's state (they died on host)
        for (Troop troop : existingTroops) {
            if (!matchedTroops.contains(troop)) {
                gameState.removeTroop(troop);
            }
        }
    }

    /**
     * Finds an existing troop that matches the given parameters.
     * Uses card name, side, and proximity for matching.
     * For swarm troops (barbarians, skeletons), uses a larger matching distance.
     */
    private Troop findMatchingTroop(java.util.List<Troop> existingTroops, java.util.Set<Troop> alreadyMatched,
            String cardName, boolean isPlayerSide, double targetX, double targetY) {
        Troop bestMatch = null;
        // Larger matching distance for swarm troops to handle multiple units of same
        // type
        double maxDistance = isSwarmTroop(cardName) ? 5.0 : 3.0;
        double bestDistance = maxDistance;

        for (Troop troop : existingTroops) {
            if (alreadyMatched.contains(troop))
                continue;
            if (troop.isPlayerSide() != isPlayerSide)
                continue;
            if (troop.getBaseCard() == null)
                continue;
            if (!troop.getBaseCard().getName().equals(cardName))
                continue;

            Vector2 pos = troop.getWorldPosition();
            if (pos == null)
                continue;

            double dx = pos.getX() - targetX;
            double dy = pos.getY() - targetY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = troop;
            }
        }

        return bestMatch;
    }

    /**
     * Checks if a card represents a swarm troop (multiple units from one card).
     */
    private boolean isSwarmTroop(String cardName) {
        if (cardName == null)
            return false;
        String lower = cardName.toLowerCase();
        return lower.contains("skeleton") || lower.contains("barbarian") ||
                lower.contains("minion") || lower.contains("goblin") ||
                lower.contains("bat") || lower.contains("spear");
    }

    /**
     * CLIENT: Applies building state from HOST.
     * Uses smart updates to avoid flickering - updates existing buildings instead
     * of recreating.
     * Mirrors positions (180° rotation) for client's perspective.
     */
    private void applyBuildingState(String buildingData) {
        String[] buildings = buildingData.split("\\|");
        java.util.List<Building> existingBuildings = new java.util.ArrayList<>(gameState.getActiveBuildings());
        java.util.Set<Building> matchedBuildings = new java.util.HashSet<>();

        for (String buildingStr : buildings) {
            if (buildingStr.isEmpty())
                continue;

            String[] parts = buildingStr.split(",");
            if (parts.length < 6) {
                continue;
            }

            try {
                String cardName = parts[0];
                int gridX = Integer.parseInt(parts[1]);
                int gridY = Integer.parseInt(parts[2]);
                int health = Integer.parseInt(parts[3]);
                boolean hostIsPlayerSide = Boolean.parseBoolean(parts[4]);
                double lifetime = Double.parseDouble(parts[5]);

                // Get width and height if provided, default to 3
                int width = parts.length > 6 ? Integer.parseInt(parts[6]) : 3;
                int height = parts.length > 7 ? Integer.parseInt(parts[7]) : 3;
                int maxHealth = parts.length > 8 ? Integer.parseInt(parts[8]) : health;
                String imagePath = parts.length > 9 ? parts[9] : "";

                // Mirror for client perspective
                boolean clientIsPlayerSide = !hostIsPlayerSide;

                // Mirror for client perspective (180° rotation)
                // For a building at (X, Y) with width W and height H:
                // New position = (WIDTH - W - X, HEIGHT - H - Y)
                int clientGridX = Arena.WIDTH - width - gridX;
                int clientGridY = Arena.HEIGHT - height - gridY;

                // Clamp to valid arena bounds
                clientGridX = Math.max(0, Math.min(Arena.WIDTH - width, clientGridX));
                clientGridY = Math.max(0, Math.min(Arena.HEIGHT - height, clientGridY));

                // Try to find an existing building to update
                Building matchedBuilding = findMatchingBuilding(existingBuildings, matchedBuildings,
                        cardName, clientIsPlayerSide, clientGridX, clientGridY);

                if (matchedBuilding != null) {
                    // Update existing building
                    matchedBuilding.setCurrentHealth(health);
                    matchedBuilding.setRemainingLifetime(lifetime);
                    matchedBuildings.add(matchedBuilding);
                } else {
                    // Spawn new building
                    boolean spawned = gameState.spawnBuildingDirect(cardName, clientGridX, clientGridY,
                            health, maxHealth, clientIsPlayerSide, lifetime, width, height, imagePath);

                    if (spawned) {
                        // Find and add the newly spawned building to matched set
                        java.util.List<Building> newBuildings = gameState.getActiveBuildings();
                        for (Building b : newBuildings) {
                            if (!existingBuildings.contains(b) && !matchedBuildings.contains(b)) {
                                matchedBuildings.add(b);
                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[NetworkBattle] Error parsing building: " + buildingStr + " - " + e.getMessage());
            }
        }

        // Remove buildings that weren't in the host's state (they were destroyed on
        // host)
        for (Building building : existingBuildings) {
            if (!matchedBuildings.contains(building)) {
                gameState.removeBuilding(building);
            }
        }
    }

    /**
     * Finds an existing building that matches the given parameters.
     */
    private Building findMatchingBuilding(java.util.List<Building> existingBuildings,
            java.util.Set<Building> alreadyMatched,
            String cardName, boolean isPlayerSide, int targetX, int targetY) {
        for (Building building : existingBuildings) {
            if (alreadyMatched.contains(building))
                continue;
            if (building.isPlayerSide() != isPlayerSide)
                continue;

            // Match by card name if available
            String buildingCardName = building.getCardName();
            if (buildingCardName != null && !buildingCardName.equals(cardName))
                continue;

            // Match by position (buildings don't move, so exact match is fine)
            GridPosition pos = building.getPosition();
            if (pos != null && pos.getX() == targetX && pos.getY() == targetY) {
                return building;
            }
        }
        return null;
    }

    /**
     * CLIENT: Applies game state data from HOST.
     */
    private void applyGameData(String gameData) {
        String[] parts = gameData.split(",");
        if (parts.length < 7)
            return;

        try {
            double gameTime = Double.parseDouble(parts[0]);
            double hostPlayerElixir = Double.parseDouble(parts[1]);
            double hostBotElixir = Double.parseDouble(parts[2]);
            int hostPlayerScore = Integer.parseInt(parts[3]);
            int hostBotScore = Integer.parseInt(parts[4]);
            boolean doubleElixir = Boolean.parseBoolean(parts[5]);

            // Apply game state (inverted for client perspective)
            // Client's score = Host's botScore (towers client destroyed)
            // Client's opponent score = Host's playerScore
            gameState.setGameTime(gameTime);
            gameState.setScores(hostBotScore, hostPlayerScore);

            // Sync elixir (client's elixir = host's bot elixir, since client is the "bot"
            // from host's view)
            gameState.getPlayerElixir().setCurrentElixir(hostBotElixir);
            gameState.getBotElixir().setCurrentElixir(hostPlayerElixir);

            if (doubleElixir && !doubleElixirShown) {
                doubleElixirShown = true;
                gameState.setDoubleElixir(true);
                elixirBar.setDoubleElixirActive(true);
            }

        } catch (Exception e) {
            System.err.println("[NetworkBattle] Error parsing game data: " + e.getMessage());
        }
    }

    /**
     * CLIENT: Handles tower health sync from HOST.
     * 
     * CRITICAL: Tower matching must account for mirrored perspective.
     * HOST's player towers = CLIENT's enemy towers (at top)
     * HOST's enemy towers = CLIENT's player towers (at bottom)
     * 
     * For PRINCESS towers, we must use position to distinguish left from right.
     * 
     * IMPORTANT: Also detects towers that are MISSING from HOST's sync -
     * these were destroyed and removed from HOST's arena, so CLIENT must remove
     * them too.
     */
    private void handleTowerSync(NetworkMessage message) {
        String towerData = message.getTowerSyncData();
        if (gameState == null)
            return;

        Arena arena = gameState.getArena();

        // Track which towers we received from HOST (to detect missing/destroyed towers)
        java.util.Set<Tower> towersInSync = new java.util.HashSet<>();

        if (towerData != null && !towerData.isEmpty()) {
            String[] towers = towerData.split(";");

            for (String towerStr : towers) {
                String[] parts = towerStr.split(",");
                if (parts.length < 7)
                    continue;

                try {
                    Tower.TowerType type = Tower.TowerType.valueOf(parts[0]);
                    boolean hostIsPlayerSide = Boolean.parseBoolean(parts[1]);
                    int currentHealth = Integer.parseInt(parts[2]);
                    // maxHealth and hostGridY are unused but kept for protocol compatibility
                    int hostGridX = Integer.parseInt(parts[4]);
                    boolean isAlive = Boolean.parseBoolean(parts[6]);

                    // MIRROR for client perspective:
                    // HOST's player towers = CLIENT's enemy towers
                    boolean clientIsPlayerSide = !hostIsPlayerSide;

                    // Mirror the X position accounting for tower size
                    // PRINCESS = 3x3, KING = 4x4
                    int towerSize = (type == Tower.TowerType.KING) ? 4 : 3;
                    int clientGridX = Arena.WIDTH - towerSize - hostGridX;

                    // Find the tower by type, side, AND position (important for princess towers)
                    Tower targetTower = findTowerByTypeAndPosition(arena, type, clientIsPlayerSide, clientGridX);

                    if (targetTower != null) {
                        towersInSync.add(targetTower);

                        // IMPORTANT: Check if tower was alive BEFORE updating health
                        boolean wasAlive = targetTower.isAlive();

                        targetTower.setCurrentHealth(currentHealth);

                        if (!isAlive && wasAlive) {
                            System.out.println("[NetworkBattle] Tower destroyed (via sync): " + type +
                                    " (client side: " + clientIsPlayerSide + ") at X=" + clientGridX);

                            // Remove the dead tower from the arena so visuals update correctly
                            arena.removeTower(targetTower);
                            towersInSync.remove(targetTower);

                            // Check win/lose conditions
                            if (type == Tower.TowerType.KING) {
                                if (clientIsPlayerSide) {
                                    showDefeat("Your King Tower was destroyed!");
                                } else {
                                    showVictory("You destroyed the enemy King Tower!");
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    System.err.println("[NetworkBattle] Failed to parse tower sync: " + towerStr);
                }
            }
        }

        // CRITICAL: Remove any CLIENT towers that weren't in HOST's sync
        // This handles the case where HOST already removed the tower before syncing
        java.util.Set<Tower> clientTowers = new java.util.HashSet<>(arena.getAllTowers());
        for (Tower clientTower : clientTowers) {
            if (!towersInSync.contains(clientTower)) {
                System.out.println("[NetworkBattle] Tower missing from HOST sync, removing: " +
                        clientTower.getType() + " (player side: " + clientTower.isPlayerSide() + ")");

                // Check win/lose conditions before removing
                if (clientTower.getType() == Tower.TowerType.KING) {
                    if (clientTower.isPlayerSide()) {
                        showDefeat("Your King Tower was destroyed!");
                    } else {
                        showVictory("You destroyed the enemy King Tower!");
                    }
                }

                arena.removeTower(clientTower);
            }
        }
    }

    /**
     * Finds a tower by type, side, and X position.
     * For PRINCESS towers, uses X position to distinguish left from right.
     * LEFT princess is on left side of arena (low X), RIGHT is on right side (high
     * X).
     */
    private Tower findTowerByTypeAndPosition(Arena arena, Tower.TowerType type, boolean isPlayerSide, int clientGridX) {
        java.util.List<Tower> matching = arena.getTowersByType(type, isPlayerSide);

        if (matching.isEmpty()) {
            return null;
        }

        // For KING tower, there's only one per side
        if (type == Tower.TowerType.KING) {
            return matching.get(0);
        }

        // For PRINCESS towers, use X position to find the correct one
        // Arena center is around WIDTH/2 = 9
        boolean isLeftSide = clientGridX < Arena.WIDTH / 2;

        for (Tower tower : matching) {
            GridPosition pos = tower.getPosition();
            if (pos != null) {
                boolean towerIsLeft = pos.getX() < Arena.WIDTH / 2;
                if (towerIsLeft == isLeftSide) {
                    return tower;
                }
            }
        }

        // Fallback to first match if position matching fails
        return matching.get(0);
    }

    /**
     * CLIENT: Handles game over message from HOST.
     */
    private void handleGameOver(NetworkMessage message) {
        if (gameEnded)
            return;

        String[] data = message.parseGameOver();
        if (data == null || data.length < 2)
            return;

        boolean hostWon = Boolean.parseBoolean(data[0]);
        String reason = data[1];

        // From client's perspective: if host won, client lost
        if (hostWon) {
            showDefeat(reason);
        } else {
            showVictory(reason);
        }
    }

    // ==================== Input Handling ====================

    /**
     * Handles arena click for card placement.
     * HOST places cards locally (authoritative).
     * CLIENT sends placement request to HOST - does NOT place locally to avoid sync
     * issues.
     */
    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex == -1)
            return;

        if (tileX < 0 || tileX >= Arena.WIDTH || tileY < 0 || tileY >= Arena.HEIGHT)
            return;

        Card card = gameState.getPlayerHand().getCard(selectedIndex);
        if (card == null)
            return;

        if (isHost) {
            // HOST: Place card locally (authoritative)
            if (gameState.placeCard(true, selectedIndex, tileX, tileY)) {
                System.out.println(
                        "[NetworkBattle] HOST: Placed " + card.getName() + " at (" + tileX + ", " + tileY + ")");
                handView.clearSelection();
                arenaView.highlightValidCells(false, false);
            }
        } else {
            // CLIENT: Do NOT place locally - send to HOST and wait for sync
            // This prevents the "appear then disappear" issue
            int cost = card.getCost();
            if (gameState.getPlayerElixir().getCurrentElixir() >= cost) {
                // Check if placement is valid (player's side)
                if (tileY >= Arena.HEIGHT / 2 || card.getType() == CardType.SPELL) {
                    System.out.println("[NetworkBattle] CLIENT: Sending " + card.getName() + " placement to HOST at ("
                            + tileX + ", " + tileY + ")");

                    // Send to HOST - HOST will spawn and sync back
                    networkService.sendCardPlaced(card.getName(), tileX, tileY);

                    // Deduct elixir and remove card from hand locally
                    gameState.getPlayerElixir().spend(cost);
                    gameState.getPlayerHand().playCard(selectedIndex);

                    handView.clearSelection();
                    arenaView.highlightValidCells(false, false);
                }
            }
        }
    }

    // ==================== Game Event Listener ====================

    @Override
    public void onCardPlayed(boolean isPlayer, Card card, java.util.List<ICombatant> spawnedUnits) {
        // Handled by arena click
    }

    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        if (isHost) {
            String towerName = (isPlayerTower ? "PLAYER_" : "OPPONENT_") + tower.getType().name();
            networkService.send(NetworkMessage.towerDestroyed(networkService.getPlayerId(), towerName));
        }
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
    public void onAreaEffect(boolean isPlayerSource, Vector2 center, double radius, double duration,
            String effectType) {
        // HOST: Send area effect to CLIENT for visual sync
        if (isHost && networkService != null && networkService.isConnected()) {
            networkService.send(NetworkMessage.areaEffect(isPlayerSource, center.getX(), center.getY(),
                    radius, duration, effectType));
        }
    }

    @Override
    public void onSpellCast(boolean isPlayer, Card spell, GridPosition center) {
        // HOST: Send spell cast to CLIENT for visual sync
        if (isHost && networkService != null && networkService.isConnected() && spell != null && center != null) {
            networkService.send(NetworkMessage.spellCast(isPlayer, spell.getName(),
                    center.getX(), center.getY(), spell.getRange()));
        }
    }

    /**
     * Mirrors the entire layout for CLIENT's 180° rotated perspective.
     * This ensures towers, bridges, and all elements appear at correct positions
     * when HOST sends projectile/effect data that gets mirrored.
     * 
     * For 180° rotation:
     * - Bridge X: mirroredX = WIDTH - 1 - x
     * - Tower X: mirroredX = WIDTH - towerSize - x (so the mirrored top-left is
     * correct)
     */
    private ArenaLayout mirrorLayoutForClient(ArenaLayout hostLayout) {
        ArenaLayout clientLayout = new ArenaLayout(hostLayout.getName());

        // Mirror bridge X positions
        for (GridPosition bridgePos : hostLayout.getBridgePositions()) {
            int mirroredX = Arena.WIDTH - 1 - bridgePos.getX();
            clientLayout.addBridgePosition(mirroredX, bridgePos.getY());
        }

        // Mirror princess tower X positions (3x3 towers)
        // For a tower at X with width 3, after 180° rotation: newX = WIDTH - 3 - X
        for (GridPosition princessPos : hostLayout.getPrincessTowerPositions()) {
            int mirroredX = Arena.WIDTH - 3 - princessPos.getX();
            clientLayout.addPrincessTowerPosition(mirroredX, princessPos.getY());
        }

        // Mirror king tower X position (4x4 tower)
        GridPosition kingPos = hostLayout.getKingTowerPosition();
        if (kingPos != null) {
            int mirroredX = Arena.WIDTH - 4 - kingPos.getX();
            clientLayout.setKingTowerPosition(mirroredX, kingPos.getY());
        }

        System.out.println("[NetworkBattle] Mirrored layout for CLIENT - Bridges: " +
                clientLayout.getBridgePositions().size() + ", Princess towers: " +
                clientLayout.getPrincessTowerPositions().size());

        return clientLayout;
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
            // Graceful shutdown: notify opponent before closing connection
            System.out.println("[NetworkBattle] Sending forfeit notification to opponent...");
            networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));

            // Small delay to ensure message is sent before disconnecting
            Timeline exitDelay = new Timeline(new KeyFrame(Duration.millis(300), e -> {
                System.out.println("[NetworkBattle] Gracefully closing connection...");
                navigateToMenu();
            }));
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

    private Timeline reconnectionTimer;

    private void handleOpponentDisconnected() {
        if (gameEnded)
            return;

        // Show disconnection overlay with proper messages
        showDisconnectionOverlay();
        if (disconnectionLabel != null) {
            disconnectionLabel.setText("Opponent Disconnected");
        }
        if (reconnectingLabel != null) {
            reconnectingLabel.setText("Attempting to reconnect...");
        }
        if (reconnectCountdownLabel != null) {
            reconnectCountdownLabel.setText("5");
        }

        System.out.println("[NetworkBattle] Opponent disconnected - waiting 5 seconds for reconnection");

        // Cancel any existing timer
        if (reconnectionTimer != null) {
            reconnectionTimer.stop();
        }

        final int[] countdown = { 5 };
        reconnectionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;

            // Update countdown display
            if (reconnectCountdownLabel != null) {
                reconnectCountdownLabel.setText(String.valueOf(countdown[0]));
            }

            // Check if reconnected
            if (networkService != null && networkService.getState() == ConnectionState.CONNECTED) {
                System.out.println("[NetworkBattle] Opponent reconnected!");
                hideDisconnectionOverlay();
                if (reconnectionTimer != null) {
                    reconnectionTimer.stop();
                }
                return;
            }

            // Countdown finished - award victory
            if (countdown[0] <= 0) {
                System.out.println("[NetworkBattle] Reconnection timeout - awarding victory");
                hideDisconnectionOverlay();
                showVictory("Opponent disconnected - Victory!");
            }
        }));
        reconnectionTimer.setCycleCount(5);
        reconnectionTimer.play();
    }

    private void handleOpponentForfeit() {
        if (gameEnded)
            return;

        // Show disconnection overlay with forfeit message
        showDisconnectionOverlay();
        if (disconnectionLabel != null) {
            disconnectionLabel.setText("Opponent Forfeited");
        }
        if (reconnectingLabel != null) {
            reconnectingLabel.setText("Victory in...");
        }
        if (reconnectCountdownLabel != null) {
            reconnectCountdownLabel.setText("5");
        }

        System.out.println("[NetworkBattle] Opponent forfeited - showing 5 second countdown");

        // Cancel any existing timer
        if (reconnectionTimer != null) {
            reconnectionTimer.stop();
        }

        final int[] countdown = { 5 };
        reconnectionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            countdown[0]--;

            if (reconnectCountdownLabel != null) {
                reconnectCountdownLabel.setText(String.valueOf(countdown[0]));
            }

            if (countdown[0] <= 0) {
                hideDisconnectionOverlay();
                showVictory("Opponent forfeited - Victory!");
            }
        }));
        reconnectionTimer.setCycleCount(5);
        reconnectionTimer.play();
    }

    // ==================== UI Updates ====================

    private void updateTimerDisplay() {
        if (gameState == null || timerLabel == null)
            return;

        double time = gameState.getGameTime();
        int minutes = (int) (time / 60);
        int seconds = (int) (time % 60);

        timerLabel.setText(String.format("%d:%02d", minutes, seconds));

        if (time <= 60) {
            timerLabel.setStyle("-fx-text-fill: #ff6600; -fx-font-size: 32px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 32px; -fx-font-weight: bold;");
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

    private void updatePingDisplay() {
        if (pingLabel != null)
            pingLabel.setText("Ping: " + currentPing + "ms");

        if (lagIndicator != null) {
            if (currentPing > 100) {
                lagIndicator.setText("⚠ High Latency");
                lagIndicator.setVisible(true);
            } else {
                lagIndicator.setVisible(false);
            }
        }
    }

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
        if (disconnectionOverlay != null)
            disconnectionOverlay.setVisible(true);
        isPaused = true;
    }

    private void hideDisconnectionOverlay() {
        if (disconnectionOverlay != null)
            disconnectionOverlay.setVisible(false);
        isPaused = false;
    }

    // ==================== Game End ====================

    private void endGame() {
        if (gameEnded)
            return;
        gameEnded = true;

        int playerScore = gameState.getPlayerScore();
        int opponentScore = gameState.getBotScore();

        if (playerScore > opponentScore) {
            showVictory("You destroyed more towers!");
            networkService.send(NetworkMessage.gameOver(true, "Opponent destroyed more towers!"));
            networkService.send(NetworkMessage.victory(networkService.getPlayerId()));
        } else if (opponentScore > playerScore) {
            showDefeat("Opponent destroyed more towers!");
            networkService.send(NetworkMessage.gameOver(false, "You destroyed more towers!"));
            networkService.send(NetworkMessage.defeat(networkService.getPlayerId()));
        } else {
            // Tiebreaker
            double playerLowestHP = getLowestTowerHealth(true);
            double opponentLowestHP = getLowestTowerHealth(false);

            if (playerLowestHP < opponentLowestHP) {
                showDefeat("Your lowest tower had less HP!");
                networkService.send(NetworkMessage.gameOver(false, "Opponent's lowest tower had less HP!"));
            } else if (opponentLowestHP < playerLowestHP) {
                showVictory("Opponent's lowest tower had less HP!");
                networkService.send(NetworkMessage.gameOver(true, "Your lowest tower had less HP!"));
            } else {
                showDraw();
                networkService.send(NetworkMessage.gameOver(false, "Perfect draw!"));
            }
        }
    }

    private double getLowestTowerHealth(boolean isPlayer) {
        if (gameState == null || gameState.getArena() == null)
            return Double.MAX_VALUE;

        return gameState.getArena().getAllTowers().stream()
                .filter(t -> t.isPlayerSide() == isPlayer && t.isAlive())
                .mapToDouble(Tower::getCurrentHealth)
                .min()
                .orElse(Double.MAX_VALUE);
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

        // Track network-specific quests and achievements
        ServiceFactory.getInstance().getQuestService().updateProgress(QuestType.WIN_NETWORK_MATCH, 1);
        ServiceFactory.getInstance().getAchievementService().updateProgress(AchievementType.NETWORK_WARRIOR, 1);

        // Notify for win streak tracking
        com.kuroyale.event.GameEventBus.getInstance().publishMatchEnd(true);
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

        // Notify for win streak reset
        com.kuroyale.event.GameEventBus.getInstance().publishMatchEnd(false);
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
        if (renderLoop != null)
            renderLoop.stop();
        if (syncTimer != null)
            syncTimer.stop();
    }

    private void cleanup() {
        GameEventBus.getInstance().unsubscribe(this);
        stopLoops();
        if (networkService != null) {
            networkService.disconnect();
        }
    }
}
