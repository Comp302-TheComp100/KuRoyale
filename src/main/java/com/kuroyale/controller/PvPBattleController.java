package com.kuroyale.controller;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ui.ElixirBarView;
import com.kuroyale.view.battle.ui.HandView;

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
    private javafx.scene.text.TextFlow gameOverInfoTextFlow;
    @FXML
    private VBox infoPanel;

    private PvPGameState gameState;
    private BattleArenaView arenaView;
    private ElixirBarView player1ElixirBar;
    private ElixirBarView player2ElixirBar;
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
        // Set current user in arena service to load their saved layout
        User currentUser = model.getCurrentUser();
        if (currentUser != null) {
            model.setCurrentUserInArenaService(currentUser);
        }

        // Load arena layout (loads saved layout if available)
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
        player1ElixirBar = new ElixirBarView(gameState.getPlayer1Elixir());
        player1ElixirContainer.getChildren().add(player1ElixirBar);

        player1HandView = new HandView(gameState.getPlayer1Hand(), gameState.getPlayer1Elixir());
        player1HandView.setOnCardSelected(index -> handlePlayer1CardSelected(index));
        player1HandContainer.getChildren().add(player1HandView);

        // Initialize Player 2 UI (right side)
        player2ElixirBar = new ElixirBarView(gameState.getPlayer2Elixir());
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
                if (arenaView != null) {
                    javafx.application.Platform.runLater(() -> {
                        arenaView.playTowerDeathEffect(tower);
                        PvPBattleController.this.playCrownFlyingEffect(tower);
                    });
                }
            }

            @Override
            public void onTowerDamaged(Tower tower) {
                if (arenaView != null) {
                    javafx.application.Platform.runLater(() -> arenaView.playDamageEffect(tower));
                }
            }

            @Override
            public void onElixirSpent(boolean isPlayer, int amount) {
            }

            @Override
            public void onBuildingProduction(Building building, String resource, int amount) {
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
            public void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.Vector2 center, double radius,
                    double duration, String effectType) {
            }

            @Override
            public void onComboTriggered(com.kuroyale.model.enums.ComboType combo,
                    java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
                javafx.application.Platform.runLater(() -> {
                    // 1. Show Text Feedback & Visuals via View
                    if (arenaView != null) {
                        arenaView.showComboText(combo.getDisplayName());
                        arenaView.showComboEffect(combo, affectedUnits);
                    }

                    // 2. Play Sound Effect via SoundManager
                    com.kuroyale.util.audio.SoundManager.getInstance().play("combo");
                });
            }
        });
    }

    private void setupArenaClickHandler() {
        arenaView.setOnGridClicked((tileX, tileY) -> {
            handleArenaClick(tileX, tileY);
        });
    }

    // Parametric handler for card selection
    private void handleCardSelected(TurnManager.Turn turn, int index, HandView handView) {
        // Only allow selection during own turn
        if (gameState.getTurnManager().getCurrentTurn() != turn) {
            handView.clearSelection();
            return;
        }

        if (index != -1) {
            boolean isPlayer1 = (turn == TurnManager.Turn.PLAYER_1);
            // Get hand from game state
            Hand hand = isPlayer1 ? gameState.getPlayer1Hand() : gameState.getPlayer2Hand();
            Card card = hand.getCard(index);
            boolean isSpell = (card != null && card.getType() == com.kuroyale.model.enums.CardType.SPELL);

            if (isPlayer1) {
                arenaView.highlightValidCells(true, isSpell);
            } else {
                arenaView.highlightPlayer2ValidCells(true, isSpell);
            }
        } else {
            if (turn == TurnManager.Turn.PLAYER_1) {
                arenaView.highlightValidCells(false, false);
            } else {
                arenaView.highlightPlayer2ValidCells(false, false);
            }
        }

        // Clear other player's selection
        if (turn == TurnManager.Turn.PLAYER_1) {
            player2HandView.clearSelection();
        } else {
            player1HandView.clearSelection();
        }
    }

    private void handlePlayer1CardSelected(int index) {
        handleCardSelected(TurnManager.Turn.PLAYER_1, index, player1HandView);
    }

    private void handlePlayer2CardSelected(int index) {
        handleCardSelected(TurnManager.Turn.PLAYER_2, index, player2HandView);
    }

    private void handleArenaClick(int tileX, int tileY) {
        TurnManager.Turn currentTurn = gameState.getTurnManager().getCurrentTurn();

        // Generic handling based on turn
        HandView currentHandView = (currentTurn == TurnManager.Turn.PLAYER_1) ? player1HandView : player2HandView;
        boolean isPlayer1 = (currentTurn == TurnManager.Turn.PLAYER_1);

        int selectedIndex = currentHandView.getSelectedIndex();

        if (selectedIndex != -1) {
            if (gameState.placeCard(isPlayer1, selectedIndex, tileX, tileY)) {
                currentHandView.clearSelection();
                if (isPlayer1) {
                    arenaView.highlightValidCells(false, false);
                } else {
                    arenaView.highlightPlayer2ValidCells(false, false);
                }
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

    private javafx.scene.text.Text createText(String content, javafx.scene.paint.Color color) {
        javafx.scene.text.Text text = new javafx.scene.text.Text(content);
        text.setFill(color);
        return text;
    }

    private void showGameOverPopup() {
        gameOverRoot.setVisible(true);

        TurnManager.Turn winner = gameState.getWinner();

        String titleText = "";
        String titleStyle = "";

        if (winner == TurnManager.Turn.PLAYER_1) {
            titleText = "PLAYER 1 WINS!";
            titleStyle = "-fx-text-fill: #3b82f6; -fx-font-size: 48px; -fx-font-weight: bold;";
        } else if (winner == TurnManager.Turn.PLAYER_2) {
            titleText = "PLAYER 2 WINS!";
            titleStyle = "-fx-text-fill: #ef4444; -fx-font-size: 48px; -fx-font-weight: bold;";
        } else {
            titleText = "DRAW!";
            titleStyle = "-fx-text-fill: white; -fx-font-size: 48px; -fx-font-weight: bold;";
        }

        gameOverTitle.setText(titleText);
        gameOverTitle.setStyle(titleStyle);

        renderGameOverCrowns(gameOverP1Crowns, gameState.getPlayer1Score(), false);
        renderGameOverCrowns(gameOverP2Crowns, gameState.getPlayer2Score(), true);

        gameOverInfoTextFlow.getChildren().clear();

        javafx.scene.paint.Color highlightColor = javafx.scene.paint.Color.web("#00BFFF"); // Cyan
        javafx.scene.paint.Color whiteColor = javafx.scene.paint.Color.WHITE;

        // Line 1: Score
        javafx.scene.text.Text p1Score = createText("Player 1: " + gameState.getPlayer1Score(),
                javafx.scene.paint.Color.web("#3b82f6"));
        javafx.scene.text.Text vs = createText("  -  ", whiteColor);
        javafx.scene.text.Text p2Score = createText("Player 2: " + gameState.getPlayer2Score() + "\n",
                javafx.scene.paint.Color.web("#ef4444"));

        gameOverInfoTextFlow.getChildren().addAll(
                createText("Final Score\n", highlightColor),
                p1Score, vs, p2Score);
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

        com.kuroyale.view.battle.ui.SimplePauseMenuView menu = new com.kuroyale.view.battle.ui.SimplePauseMenuView(
                new com.kuroyale.view.battle.ui.SimplePauseMenuView.SimplePauseMenuListener() {
                    @Override
                    public void onResume() {
                        handleResume();
                    }

                    @Override
                    public void onExit() {
                        handleExit();
                    }
                });

        pauseMenuContainer.getChildren().add(menu);
    }

    private void playCrownFlyingEffect(Tower tower) {
        javafx.geometry.Point2D arenaPos = arenaView.getTowerCenterPosition(tower);
        if (arenaPos == null)
            return;

        javafx.geometry.Point2D scenePos = arenaView.getGrid().localToScene(arenaPos.getX(), arenaPos.getY());
        javafx.geometry.Point2D localStart = arenaContainer.sceneToLocal(scenePos);

        double centerX = localStart.getX() - arenaContainer.getWidth() / 2;
        double centerY = localStart.getY() - arenaContainer.getHeight() / 2;

        boolean isPlayer1Tower = tower.isPlayerSide();
        boolean p1Scored = !isPlayer1Tower;
        String crownPath = p1Scored ? "/images/crown.png" : "/images/oppo_crown.png";

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

        // Determine target box
        HBox targetBox = p1Scored ? player1ScoreContainer : player2ScoreContainer;

        javafx.animation.TranslateTransition move = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(1.0), crown);

        move.setInterpolator(javafx.animation.Interpolator.EASE_IN);

        if (targetBox != null) {
            javafx.geometry.Point2D targetPoint = targetBox.localToScene(0, 0);
            javafx.geometry.Point2D localEnd = arenaContainer.sceneToLocal(
                    targetPoint.getX() + targetBox.getWidth() / 2,
                    targetPoint.getY() + targetBox.getHeight() / 2);

            double endX = localEnd.getX() - arenaContainer.getWidth() / 2;
            double endY = localEnd.getY() - arenaContainer.getHeight() / 2;
            move.setToX(endX);
            move.setToY(endY);
        } else {
            move.setByY(-200);
        }

        sequence.getChildren().addAll(fadeIn, stay, move);

        sequence.setOnFinished(e -> {
            arenaContainer.getChildren().remove(crown);
        });
        sequence.play();
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
