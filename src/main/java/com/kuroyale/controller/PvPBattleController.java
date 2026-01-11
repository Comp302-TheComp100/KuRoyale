package com.kuroyale.controller;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ElixirBar;
import com.kuroyale.view.battle.HandView;
import com.kuroyale.util.SceneLoader;

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
    private HBox player1ScoreContainer;
    @FXML
    private HBox player2ScoreContainer;
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
    private HBox gameOverP1Crowns;
    @FXML
    private HBox gameOverP2Crowns;
    @FXML
    private Label gameOverInfoLabel;
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

        // Center the arena view within the container
        StackPane.setAlignment(arenaView, javafx.geometry.Pos.CENTER);
        arenaView.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        arenaView.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

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
        player2ElixirContainer.getChildren().add(player2ElixirBar);

        player2HandView = new HandView(gameState.getPlayer2Hand(), gameState.getPlayer2Elixir());
        player2HandView.setOnCardSelected(index -> handlePlayer2CardSelected(index));
        player2HandContainer.getChildren().add(player2HandView);

        // Set up turn change listener
        gameState.getTurnManager().addTurnChangeListener(this::onTurnChanged);

        // Initial UI update (0 deltaTime since no time has passed yet)
        updateTurnIndicator();
        updateUI(0);

        // Start game loop
        startGameLoop();

        // Subscribe to Game Events for effects
        com.kuroyale.event.GameEventBus.getInstance().subscribe(new com.kuroyale.event.GameEventListener() {
            @Override
            public void onCardPlayed(boolean isPlayer, Card card,
                    java.util.List<com.kuroyale.model.entities.ICombatant> spawnedUnits) {
            }

            @Override
            public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
                playTowerDeathEffect(tower);
            }

            @Override
            public void onTowerDamaged(Tower tower) {
                playDamageEffect(tower);
            }

            @Override
            public void onElixirSpent(boolean isPlayer, int amount) {
            }

            @Override
            public void onBuildingProduction(Building building, String resource, int amount) {
                // Handle production visuals if needed (similar to SP)
                if ("ELIXIR".equals(resource)) {
                    javafx.application.Platform.runLater(() -> {
                        if (building.isPlayerSide()) {
                            player1ElixirBar.showProductionIndicator(amount);
                        } else {
                            player2ElixirBar.showProductionIndicator(amount);
                        }
                    });
                }
            }

            @Override
            public void onAreaEffect(boolean isPlayerSource, GridPosition center, double radius, double duration) {
            }

            @Override
            public void onComboTriggered(com.kuroyale.model.enums.ComboType combo,
                    java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
                javafx.application.Platform.runLater(() -> {
                    // 1. Show Text Feedback
                    try {
                        showComboText(combo.getDisplayName());
                    } catch (Exception e) {
                        // Ignore UI errors if controller is dying
                    }

                    // 2. Show Visual Effects
                    if (arenaView != null) {
                        arenaView.showComboEffect(combo, affectedUnits);
                    }

                    // 3. Play Sound Effect
                    try {
                        java.net.URL soundUrl = getClass().getResource("/musics/combo.mp3");
                        if (soundUrl != null) {
                            javafx.scene.media.Media sound = new javafx.scene.media.Media(soundUrl.toExternalForm());
                            javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(sound);
                            mediaPlayer.setVolume(0.5);
                            mediaPlayer.play();
                        }
                    } catch (Exception e) {
                        // Silently ignore sound errors
                    }
                });
            }
        });
    }

    private void showComboText(String text) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(text + "!");
        label.setStyle(
                "-fx-font-size: 32px; -fx-text-fill: gold; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);");
        label.setTranslateY(-100);

        StackPane container = new StackPane(label);
        container.setPickOnBounds(false);
        if (arenaContainer != null) {
            arenaContainer.getChildren().add(container);

            javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2.0),
                    label);
            ft.setFromValue(1.0);
            ft.setToValue(0.0);
            ft.setOnFinished(e -> {
                if (arenaContainer != null)
                    arenaContainer.getChildren().remove(container);
            });

            javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.seconds(2.0), label);
            tt.setByY(-50);

            javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
            pt.play();
        }
    }

    private void playDamageEffect(Tower tower) {
        javafx.scene.Node towerNode = arenaView.getTowerNode(tower);
        if (towerNode != null && towerNode instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane stack = (javafx.scene.layout.StackPane) towerNode;

            // 1. Red Overlay Flash
            javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(stack.getWidth(),
                    stack.getHeight());
            overlay.setFill(javafx.scene.paint.Color.RED);
            overlay.setOpacity(0.0);
            overlay.setMouseTransparent(true);

            stack.getChildren().add(overlay);

            javafx.animation.FadeTransition flash = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(100), overlay);
            flash.setFromValue(0.0);
            flash.setToValue(0.3);
            flash.setCycleCount(2);
            flash.setAutoReverse(true);
            flash.setOnFinished(e -> stack.getChildren().remove(overlay));
            flash.play();

            // 2. Shake
            javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(50), towerNode);
            shake.setByX(2);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }

    private void playTowerDeathEffect(Tower tower) {
        javafx.scene.Node towerNode = arenaView.getTowerNode(tower);
        if (towerNode == null)
            return;

        javafx.geometry.Bounds bounds = towerNode.localToScene(towerNode.getBoundsInLocal());
        double startX = bounds.getCenterX();
        double startY = bounds.getCenterY();

        javafx.geometry.Point2D localStart = arenaContainer.sceneToLocal(startX, startY);
        double centerX = localStart.getX() - arenaContainer.getWidth() / 2;
        double centerY = localStart.getY() - arenaContainer.getHeight() / 2;

        // 1. Procedural Explosion
        javafx.scene.shape.Circle explosionCore = new javafx.scene.shape.Circle(10, javafx.scene.paint.Color.ORANGE);
        explosionCore.setStroke(javafx.scene.paint.Color.RED);
        explosionCore.setStrokeWidth(2);
        explosionCore.setTranslateX(centerX);
        explosionCore.setTranslateY(centerY);

        javafx.scene.shape.Circle explosionRing = new javafx.scene.shape.Circle(10,
                javafx.scene.paint.Color.TRANSPARENT);
        explosionRing.setStroke(javafx.scene.paint.Color.YELLOW);
        explosionRing.setStrokeWidth(4);
        explosionRing.setTranslateX(centerX);
        explosionRing.setTranslateY(centerY);

        arenaContainer.getChildren().addAll(explosionCore, explosionRing);

        javafx.animation.Timeline explodeAnim = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(explosionCore.radiusProperty(), 10),
                        new javafx.animation.KeyValue(explosionCore.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400),
                        new javafx.animation.KeyValue(explosionCore.radiusProperty(), 60),
                        new javafx.animation.KeyValue(explosionCore.opacityProperty(), 0.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(explosionRing.radiusProperty(), 10),
                        new javafx.animation.KeyValue(explosionRing.opacityProperty(), 1.0),
                        new javafx.animation.KeyValue(explosionRing.strokeWidthProperty(), 4)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600),
                        new javafx.animation.KeyValue(explosionRing.radiusProperty(), 80),
                        new javafx.animation.KeyValue(explosionRing.opacityProperty(), 0.0),
                        new javafx.animation.KeyValue(explosionRing.strokeWidthProperty(), 0)));

        explodeAnim.setOnFinished(e -> arenaContainer.getChildren().removeAll(explosionCore, explosionRing));
        explodeAnim.play();

        // 2. Crown Animation
        // Player 1 (Bottom) uses "crown.png" isPlayerSide=true
        // Player 2 (Top) uses "oppo_crown.png" isPlayerSide=false
        // If P1 tower dies, P2 gets a point. We want P2 Crown (oppo_crown/RED) to fly
        // to P2 Score (Right).
        // If P2 tower dies, P1 gets a point. We want P1 Crown (crown/BLUE) to fly to P1
        // Score (Left).
        boolean isPlayer1Tower = tower.isPlayerSide();
        String crownPath = isPlayer1Tower ? "/images/oppo_crown.png" : "/images/crown.png";

        javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResourceAsStream(crownPath)));
        crown.setFitWidth(40);
        crown.setFitHeight(40);

        crown.setTranslateX(centerX);
        crown.setTranslateY(centerY);
        crown.setOpacity(0.0);

        arenaContainer.getChildren().add(crown);

        javafx.animation.SequentialTransition sequence = new javafx.animation.SequentialTransition();

        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200),
                crown);
        fadeIn.setToValue(1.0);

        javafx.animation.PauseTransition stay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.0));

        javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(1.0), crown);

        // Determine target box
        HBox targetBox = isPlayer1Tower ? player2ScoreContainer : player1ScoreContainer;

        javafx.geometry.Point2D targetPoint = targetBox.localToScene(0, 0);
        javafx.geometry.Point2D localEnd = arenaContainer.sceneToLocal(targetPoint.getX() + targetBox.getWidth() / 2,
                targetPoint.getY() + targetBox.getHeight() / 2);

        double endX = localEnd.getX() - arenaContainer.getWidth() / 2;
        double endY = localEnd.getY() - arenaContainer.getHeight() / 2;

        move.setToX(endX);
        move.setToY(endY);
        move.setInterpolator(javafx.animation.Interpolator.EASE_IN);

        sequence.getChildren().addAll(fadeIn, stay, move);

        sequence.setOnFinished(e -> {
            arenaContainer.getChildren().remove(crown);
            // Score UI update handled by update() loop reading gameState, but animation
            // adds
            // flavor
        });
        sequence.play();
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
                // Auto-switch turn after successful card deployment
                gameState.getTurnManager().endTurn();
            }
            return;
        }

        // Check if Player 2 has a selected card AND it's their turn
        int p2SelectedIndex = player2HandView.getSelectedIndex();
        if (p2SelectedIndex != -1 && currentTurn == TurnManager.Turn.PLAYER_2) {
            if (gameState.placeCard(false, p2SelectedIndex, tileX, tileY)) {
                player2HandView.clearSelection();
                arenaView.highlightPlayer2ValidCells(false, false);
                // Auto-switch turn after successful card deployment
                gameState.getTurnManager().endTurn();
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
        updateUI(deltaTime);

        // Check for game over
        if (gameState.isGameOver() && !gameOverShown) {
            gameOverShown = true;
            gameLoop.stop();

            // Wait 1 second before showing Game Over screen
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.0));
            delay.setOnFinished(e -> showGameOverPopup());
            delay.play();
        }
    }

    private void updateUI(double deltaTime) {
        // Update elixir bars
        player1ElixirBar.update();
        player2ElixirBar.update();

        // Update hands
        player1HandView.update();
        player2HandView.update();

        // Update arena (use PvP update method with actual deltaTime for spell effect
        // cleanup)
        arenaView.updatePvP(deltaTime);

        // Update time display
        int seconds = (int) gameState.getGameTime();
        int mins = seconds / 60;
        int secs = seconds % 60;
        timeLabel.setText(String.format("%02d:%02d", mins, secs));

        // Update score display with crowns
        updateScoreCrowns(gameState.getPlayer1Score(), gameState.getPlayer2Score());

        // Update double elixir indicators
        if (gameState.isDoubleElixir()) {
            player1ElixirBar.setDoubleElixirActive(true);
            player2ElixirBar.setDoubleElixirActive(true);
        }
    }

    private void updateScoreCrowns(int p1Score, int p2Score) {
        // Only update if child count differs (simple check) to avoid clearing/re-adding
        // every frame
        if (player1ScoreContainer.getChildren().size() != p1Score) {
            player1ScoreContainer.getChildren().clear();
            for (int i = 0; i < p1Score; i++) {
                addCrown(player1ScoreContainer, false);
            }
        }

        if (player2ScoreContainer.getChildren().size() != p2Score) {
            player2ScoreContainer.getChildren().clear();
            for (int i = 0; i < p2Score; i++) {
                addCrown(player2ScoreContainer, true);
            }
        }
    }

    private void addCrown(HBox container, boolean isOpponent) {
        String imagePath = isOpponent ? "/images/oppo_crown.png" : "/images/crown.png";
        javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath)));
        crown.setFitWidth(32);
        crown.setFitHeight(32);
        container.getChildren().add(crown);
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

        renderGameOverCrowns(gameOverP1Crowns, gameState.getPlayer1Score(), false);
        renderGameOverCrowns(gameOverP2Crowns, gameState.getPlayer2Score(), true);

        if (gameOverInfoLabel != null) {
            gameOverInfoLabel.setText(String.format("Final Score\nPlayer 1: %d  -  Player 2: %d",
                    gameState.getPlayer1Score(), gameState.getPlayer2Score()));
        }
    }

    private void renderGameOverCrowns(HBox container, int count, boolean isOpponent) {
        container.getChildren().clear();
        String imagePath = isOpponent ? "/images/oppo_crown.png" : "/images/crown.png";

        for (int i = 0; i < count; i++) {
            javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath)));
            crown.setFitWidth(64);
            crown.setFitHeight(64);
            container.getChildren().add(crown);
        }
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
        if (gameState != null) {
            gameState.cleanup();
        }

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
