package com.kuroyale.controller;

import com.kuroyale.model.logic.*;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ui.ElixirBarView;
import com.kuroyale.view.battle.ui.HandView;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/*Controller for the Battle screen.
 * Manages the game loop, user input, and UI updates.*/
public class BattleController {

    @FXML
    private StackPane arenaContainer;
    @FXML
    private HBox elixirContainer;
    @FXML
    private VBox handContainer;
    @FXML
    private VBox overlayContainer;
    @FXML
    private VBox pauseMenuContainer;

    @FXML
    private VBox challengeGameOverRoot;
    @FXML
    private javafx.scene.control.Label challengeTitle;
    @FXML
    private HBox challengeStarBox;
    @FXML
    private javafx.scene.control.Label star1;
    @FXML
    private javafx.scene.control.Label star2;
    @FXML
    private javafx.scene.control.Label star3;
    @FXML
    private javafx.scene.control.Label challengeTimeLabel;
    @FXML
    private VBox challengeConditionsBox;
    @FXML
    private javafx.scene.control.Label challengeRewardLabel;

    @FXML
    private VBox gameOverRoot;
    @FXML
    private javafx.scene.control.Label gameOverTitle;
    @FXML
    private HBox gameOverPlayerCrowns;
    @FXML
    private HBox gameOverBotCrowns;
    @FXML
    private javafx.scene.text.TextFlow gameOverInfoTextFlow;

    // Right Sidebar (moved from BattleArenaView to FXML)
    @FXML
    private javafx.scene.control.Label timeLabel;
    @FXML
    private HBox playerScoreContainer;
    @FXML
    private HBox botScoreContainer;
    @FXML
    private javafx.scene.control.Label comboLabel;

    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBarView elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop;
    private ArenaLayout currentArenaLayout;
    private boolean isPaused = false;
    private SavedGameState loadedSavedGame = null;

    private final BattleModel model = new BattleModel();
    private final SceneLoader sceneLoader = new SceneLoader();

    // Challenge Mode Context
    private Challenge currentChallenge;
    private Deck challengePlayerDeck;

    private boolean doubleElixirShown = false;
    private boolean gameOverShown = false;
    private com.kuroyale.service.battle.ComboService comboService;
    private int savedComboCount = 0; // For restoring combo count from saved games

    /**
     * Returns the arena container for external navigation (used by strategies).
     */
    public StackPane getArenaContainer() {
        return arenaContainer;
    }

    // Sets a saved game to load from
    public void setLoadedSavedGame(SavedGameState savedGame) {
        this.loadedSavedGame = savedGame;
    }


    // Starts a challenge match with specific rules and deck.
    public void startChallengeGame(Challenge challenge, Deck playerDeck) {
        this.currentChallenge = challenge;
        this.challengePlayerDeck = playerDeck;
        startGame();
    }

    @FXML
    public void initialize() {
        // The actual initialization happens in startGame() which is called after setup
    }

    // Starts the game. Must be called AFTER setLoadedSavedGame() if loading a saved
    // game
    public void startGame() {
        // Initialize game state
        User currentUser = model.getCurrentUser();
        // Allow starting challenge even if user logic is tricky, but we usually need
        // currentUser for other things
        if (currentUser == null && currentChallenge == null) {
            handleExit();
            return;
        }

        // Set current user in arena service to load their saved layout
        if (currentUser != null) {
            model.setCurrentUserInArenaService(currentUser);
        }

        // Check if loading from saved game
        if (loadedSavedGame != null) {
            System.out.println("⚠️ LOADING FROM SAVED GAME ⚠️");
            // Load from saved state
            initializeFromSavedGame(loadedSavedGame);
        } else {
            System.out.println("▶️ STARTING NEW GAME" + (currentChallenge != null ? " (CHALLENGE MODE)" : ""));

            // Determine Player Deck
            Deck playerDeck;
            if (currentChallenge != null) {
                playerDeck = challengePlayerDeck;
            } else {
                playerDeck = model.createDeckFromNames(currentUser.getDeck());
            }

            ArenaLayout playerLayout = model.loadArenaLayout(); // Load saved layout
            currentArenaLayout = playerLayout;

            // Create Arena
            Arena arena = model.createArena(playerLayout);

            // Create Bot Deck (Random or fixed)
            Deck botDeck = model.createBotDeck(currentUser);

            // Initialize GameState
            gameState = new GameState(playerDeck, botDeck, arena);
            gameState.setCardCatalog(name -> model.getCardByName(name));
            if (currentChallenge != null) {
                gameState.setActiveChallenge(currentChallenge.getType());
            }
        }

        // Notify quest system that a match is starting
        com.kuroyale.event.GameEventBus.getInstance().publishMatchStart();

        // Initialize UI Components
        arenaView = new BattleArenaView(gameState);
        arenaContainer.getChildren().add(arenaView);

        // Make arena view fill the container so sidebar stays on the right
        arenaView.prefWidthProperty().bind(arenaContainer.widthProperty());
        arenaView.prefHeightProperty().bind(arenaContainer.heightProperty());

        // Handle clicks on arena for card placement
        arenaView.setOnGridClicked((tileX, tileY) -> {
            handleArenaClick(tileX, tileY);
        });

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

        // Start Game Loop
        startGameLoop();

        // Subscribe to Elixir Events for visual feedback
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
                        BattleController.this.playCrownFlyingEffect(tower);
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
                // Legacy: do nothing
            }

            @Override
            public void onBuildingProduction(Building building, String resource, int amount) {
                if (building.isPlayerSide() && "ELIXIR".equals(resource)) {
                    javafx.application.Platform.runLater(() -> {
                        // Show +1 indicator on the elixir bar
                        elixirBar.showProductionIndicator(amount);
                    });
                }
            }

            @Override
            public void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.Vector2 center,
                    double radius,
                    double duration,
                    String effectType) {
            }

            @Override
            public void onComboTriggered(com.kuroyale.model.enums.ComboType combo,
                    java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
                javafx.application.Platform.runLater(() -> {
                    // 1. Show Text Feedback via View
                    if (arenaView != null) {
                        arenaView.showComboText(combo.getDisplayName());
                        arenaView.showComboEffect(combo, affectedUnits);
                    }

                    // 2. Update Sidebar (Controller responsibility)
                    if (comboService != null && comboLabel != null) {
                        int count = comboService.getUniqueComboCount();
                        comboLabel.setText(String.valueOf(count));
                    }

                    // 3. Play Sound Effect via SoundManager
                    com.kuroyale.util.audio.SoundManager.getInstance().play("combo");
                });
            }
        });

        // Initialize Combo Service
        if (comboService != null)
            comboService.cleanup();
        comboService = new com.kuroyale.service.battle.ComboService();
        comboService.setGameState(gameState);

        // Restore combo count from saved game if applicable
        if (savedComboCount > 0) {
            comboService.restoreComboCount(savedComboCount);
            // Update the label immediately to show restored combo count
            if (comboLabel != null) {
                comboLabel.setText(String.valueOf(savedComboCount));
            }
            savedComboCount = 0; // Reset after restoring
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
        // Skip update if paused
        if (isPaused) {
            return;
        }

        // Update Game Logic and UI
        gameState.update(deltaTime);
        elixirBar.update();
        handView.update();
        arenaView.update(deltaTime);

        // Update Sidebar (moved from BattleArenaView)
        updateSidebar();

        // Check for Double Elixir
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            // Visual indicators are handled by ElixirBar and BattleArenaView
            elixirBar.setDoubleElixirActive(true);
            // Update timer color for double elixir
            timeLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 36px; -fx-font-weight: bold;");
        }

        // Check for Game Over
        checkGameOver();
    }

    // Sidebar tracking variables
    private int lastDisplayedSeconds = -1;
    private int lastPlayerScore = -1;
    private int lastBotScore = -1;

    private void updateSidebar() {
        // Update Timer (only when second changes)
        int totalSeconds = (int) Math.ceil(gameState.getGameTime());
        if (totalSeconds != lastDisplayedSeconds) {
            lastDisplayedSeconds = totalSeconds;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            timeLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }

        // Update Score crowns
        int playerScore = gameState.getPlayerScore();
        int botScore = gameState.getBotScore();

        if (playerScore != lastPlayerScore) {
            lastPlayerScore = playerScore;
            updateScoreContainerUI(playerScoreContainer, playerScore, false);
        }
        if (botScore != lastBotScore) {
            lastBotScore = botScore;
            updateScoreContainerUI(botScoreContainer, botScore, true);
        }
    }

    private void updateScoreContainerUI(HBox container, int score, boolean isOpponent) {
        if (container == null)
            return;
        container.getChildren().clear();
        String imagePath = isOpponent ? "/images/oppo_crown.png" : "/images/crown.png";

        for (int i = 0; i < score; i++) {
            javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath)));
            crown.setFitWidth(32);
            crown.setFitHeight(32);
            container.getChildren().add(crown);
        }
    }

    // Check for Game Over - called from update() method
    private void checkGameOver() {
        if (gameState.isGameOver() && !gameOverShown) {
            gameOverShown = true;
            gameLoop.stop();

            // Wait 1 second before showing Game Over screen
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.0));
            delay.setOnFinished(e -> {
                // Handle Challenge Results
                if (currentChallenge != null) {
                    handleChallengeGameOver();
                } else {
                    showGameOverPopup();
                }
            });
            delay.play();
        }
    }

    private void handleChallengeGameOver() {
        boolean playerWon = gameState.isPlayerWinner();
        int timeSeconds = (int) (180.0 - gameState.getGameTime()); // Approximate
        int damageTaken = gameState.getPlayerDamageTaken();

        // Calculate stars locally for display
        int stars = 0;
        if (playerWon) {
            stars = currentChallenge.calculateStars(timeSeconds, damageTaken);
        }

        // Record attempt
        model.recordChallengeAttempt(currentChallenge.getId(), playerWon, timeSeconds, damageTaken);

        // Notify quest system about match end
        com.kuroyale.event.GameEventBus.getInstance().publishMatchEnd(playerWon);

        // Track Challenge Completion (Quest)
        com.kuroyale.util.common.ServiceFactory.getInstance().getQuestService()
                .updateProgress(com.kuroyale.model.enums.QuestType.COMPLETE_CHALLENGES, 1);

        if (playerWon) {
            // Track Win Quests
            com.kuroyale.util.common.ServiceFactory.getInstance().getQuestService()
                    .updateProgress(com.kuroyale.model.enums.QuestType.WIN_MATCHES, 1);

            // Track Achievements
            com.kuroyale.util.common.ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.CHALLENGE_MASTER, 1);

            if (stars == 3) {
                com.kuroyale.util.common.ServiceFactory.getInstance().getAchievementService()
                        .updateProgress(com.kuroyale.model.enums.AchievementType.THREE_STAR_HERO, 1);
            }
        }

        // Show Popup via FXML
        overlayContainer.setVisible(false); // Ensure generic overlay is hidden
        challengeGameOverRoot.setVisible(true);

        // Title
        challengeTitle.setText(playerWon ? "CHALLENGE COMPLETE!" : "CHALLENGE FAILED");

        // The original code set color manually.
        challengeTitle.getStyleClass().removeAll("challenge-victory-text", "challenge-defeat-text");
        challengeTitle.getStyleClass().add(playerWon ? "challenge-victory-text" : "challenge-defeat-text");

        // Stars Display
        // Reset stars
        star1.getStyleClass().removeAll("star-filled", "star-empty");
        star2.getStyleClass().removeAll("star-filled", "star-empty");
        star3.getStyleClass().removeAll("star-filled", "star-empty");

        if (playerWon) {
            challengeStarBox.setVisible(true);
            star1.getStyleClass().add(stars >= 1 ? "star-filled" : "star-empty");
            star2.getStyleClass().add(stars >= 2 ? "star-filled" : "star-empty");
            star3.getStyleClass().add(stars >= 3 ? "star-filled" : "star-empty");
        } else {
            challengeStarBox.setVisible(false);
        }

        // Stats Display
        String timeStr = String.format("%d:%02d", timeSeconds / 60, timeSeconds % 60);
        challengeTimeLabel.setText("Time: " + timeStr + "  Damage: " + damageTaken);

        // Star Conditions Feedback
        challengeConditionsBox.getChildren().clear();
        challengeConditionsBox.getChildren().add(createConditionLabel("Win", true, playerWon));
        if (playerWon) {
            // 2 stars: time <= 2 star time
            boolean metTime2 = timeSeconds <= currentChallenge.getTwoStarTimeSeconds();
            challengeConditionsBox.getChildren()
                    .add(createConditionLabel(
                            "Under " + String.format("%d:%02d", currentChallenge.getTwoStarTimeSeconds() / 60,
                                    currentChallenge.getTwoStarTimeSeconds() % 60),
                            metTime2, true));

            // 3 stars: no damage OR time <= 3 star time
            boolean metDamage = damageTaken == 0;
            boolean metTime3 = timeSeconds <= currentChallenge.getThreeStarTimeSeconds();
            String threeStarText = "Under " + String.format("%d:%02d", currentChallenge.getThreeStarTimeSeconds() / 60,
                    currentChallenge.getThreeStarTimeSeconds() % 60) + " OR No Damage";
            challengeConditionsBox.getChildren().add(createConditionLabel(threeStarText, metDamage || metTime3, true));
        }

        challengeRewardLabel.setText(playerWon
                ? "Reward: " + currentChallenge.getGoldReward() + " Gold\nNext challenge unlocked!"
                : "Don't give up! Try adjusting your deck.");
    }

    private javafx.scene.control.Label createConditionLabel(String text, boolean met, boolean showCheck) {
        String icon = showCheck ? (met ? "✅" : "❌") : "⚪";
        javafx.scene.control.Label label = new javafx.scene.control.Label(icon + " " + text);
        label.getStyleClass().add("condition-label");
        label.getStyleClass().add(met ? "condition-met" : "condition-unmet");
        return label;
    }

    @FXML
    private void handleExitToChallenges() {
        try {
            sceneLoader.load(arenaContainer, "/fxml/challenge-selection.fxml", "KU Royale - Challenges", null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private javafx.scene.text.Text createText(String content, javafx.scene.paint.Color color) {
        javafx.scene.text.Text text = new javafx.scene.text.Text(content);
        text.setFill(color);
        return text;
    }

    private void showGameOverPopup() {
        // Clear overlay and make it visible
        overlayContainer.setVisible(false);
        gameOverRoot.setVisible(true);

        boolean playerWon = gameState.isPlayerWinner();
        boolean isDraw = false;
        if (gameState.getPlayerScore() == gameState.getBotScore()) {
            isDraw = true;
        }

        // Notify quest system about match end
        com.kuroyale.event.GameEventBus.getInstance().publishMatchEnd(playerWon);

        // Track Matches Played (Veteran Player Achievement)
        com.kuroyale.util.common.ServiceFactory.getInstance().getAchievementService()
                .updateProgress(com.kuroyale.model.enums.AchievementType.VETERAN_PLAYER, 1);

        int baseGold = 0;
        String titleText = "";
        String titleStyle = "";

        if (playerWon) {
            baseGold = 150;
            titleText = "VICTORY";
            titleStyle = "victory-text";

            // Track Win Quests
            com.kuroyale.util.common.ServiceFactory.getInstance().getQuestService()
                    .updateProgress(com.kuroyale.model.enums.QuestType.WIN_MATCHES, 1);
            com.kuroyale.util.common.ServiceFactory.getInstance().getQuestService()
                    .updateProgress(com.kuroyale.model.enums.QuestType.WIN_PVP_MATCH, 1);

            // Track Win Without Losing Tower
            if (gameState.getBotScore() == 0) {
                com.kuroyale.util.common.ServiceFactory.getInstance().getQuestService()
                        .updateProgress(com.kuroyale.model.enums.QuestType.WIN_WITHOUT_LOSING_TOWER, 1);
            }

            // Track Win Achievements
            com.kuroyale.util.common.ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.FIRST_BLOOD, 1);
            com.kuroyale.util.common.ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.UNDEFEATED, 1);
        } else if (isDraw) {
            baseGold = 75;
            titleText = "DRAW";
            titleStyle = "victory-text"; // Or neutral style if available
        } else {
            baseGold = 50;
            titleText = "DEFEAT";
            titleStyle = "defeat-text";
        }

        gameOverTitle.setText(titleText);
        gameOverTitle.getStyleClass().removeAll("victory-text", "defeat-text");
        gameOverTitle.getStyleClass().add(titleStyle);

        int playerScore = gameState.getPlayerScore();
        int botScore = gameState.getBotScore();

        int comboCount = 0;
        if (comboService != null) {
            comboCount = comboService.getUniqueComboCount();
        }

        int comboGold = comboCount * 10;
        int totalGold = baseGold + comboGold;

        // Render crowns (using larger size for Game Over)
        renderGameOverCrowns(gameOverPlayerCrowns, playerScore, false);
        renderGameOverCrowns(gameOverBotCrowns, botScore, true);

        gameOverInfoTextFlow.getChildren().clear();

        javafx.scene.paint.Color highlightColor = javafx.scene.paint.Color.web("#00BFFF"); // Deep Sky Blue for emphasis
        javafx.scene.paint.Color goldColor = javafx.scene.paint.Color.GOLD;
        javafx.scene.paint.Color whiteColor = javafx.scene.paint.Color.WHITE;

        // Line 1: Score
        gameOverInfoTextFlow.getChildren().addAll(
                createText("Score: ", highlightColor),
                createText(playerScore + " - " + botScore + "\n", whiteColor));

        // Line 2: Combos
        gameOverInfoTextFlow.getChildren().addAll(
                createText("Combos Triggered: ", highlightColor),
                createText(String.valueOf(comboCount), whiteColor),
                createText(" (+" + comboGold + " ", whiteColor),
                createText("gold", goldColor),
                createText(")\n", whiteColor));

        // Line 3: Result Gold
        gameOverInfoTextFlow.getChildren().addAll(
                createText(titleText + " Gold: ", highlightColor),
                createText(String.valueOf(baseGold) + " ", whiteColor),
                createText("gold", goldColor),
                createText("\n", whiteColor));

        // Line 4: Total
        gameOverInfoTextFlow.getChildren().addAll(
                createText("Total: ", highlightColor),
                createText(String.valueOf(totalGold) + " ", whiteColor),
                createText("gold", goldColor));
        // User will click EXIT button to return to menu
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
                    arenaView.highlightValidCells(false, false);
                } else {
                    // Failed (not enough elixir, invalid position, etc.)
                }
            }
        }
    }

    private void playCrownFlyingEffect(Tower tower) {
        javafx.geometry.Point2D arenaPos = arenaView.getTowerCenterPosition(tower);
        if (arenaPos == null)
            return;

        javafx.geometry.Point2D scenePos = arenaView.getGrid().localToScene(arenaPos.getX(), arenaPos.getY());
        javafx.geometry.Point2D localStart = arenaContainer.sceneToLocal(scenePos);

        double centerX = localStart.getX() - arenaContainer.getWidth() / 2;
        double centerY = localStart.getY() - arenaContainer.getHeight() / 2;

        boolean isPlayerTower = tower.isPlayerSide();
        // Asset selection logic
        String crownPath = isPlayerTower ? "/images/oppo_crown.png" : "/images/crown.png";

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
        HBox targetBox = isPlayerTower ? botScoreContainer : playerScoreContainer;

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
            if (!isPlayerTower) {
                move.setByY(300);
                move.setByX(-300);
            } else {
                move.setByY(-300);
                move.setByX(300);
            }
        }

        sequence.getChildren().addAll(fadeIn, stay, move);

        sequence.setOnFinished(e -> {
            arenaContainer.getChildren().remove(crown);
        });
        sequence.play();
    }

    @FXML
    public void handlePause() {
        if (gameOverShown) {
            return; // Don't allow pause after game over
        }
        isPaused = true;
        showPauseMenu();
    }

    private void handleResume() {
        isPaused = false;
        pauseMenuContainer.setVisible(false);
        pauseMenuContainer.getChildren().clear();
    }

    private void handleSaveAndExit() {
        if (saveGame()) {
            showSaveConfirmation();
        }
        handleExit();
    }

    private void handleSaveAndResume() {
        if (saveGame()) {
            showSaveConfirmationBrief();
        }
        handleResume();
    }

    private boolean saveGame() {
        User currentUser = model.getCurrentUser();
        if (currentUser != null && currentArenaLayout != null) {
            int comboCount = 0;
            if (comboService != null) {
                comboCount = comboService.getUniqueComboCount();
            }
            SavedGameState savedGame = model.saveGame(gameState, currentUser, currentArenaLayout, comboCount);
            if (savedGame != null) {
                return true;
            } else {
                System.err.println("Failed to save game");
            }
        }
        return false;
    }

    private void showPauseMenu() {
        pauseMenuContainer.getChildren().clear();
        pauseMenuContainer.setVisible(true);

        com.kuroyale.view.battle.ui.PauseMenuView menu = new com.kuroyale.view.battle.ui.PauseMenuView(
                new com.kuroyale.view.battle.ui.PauseMenuView.PauseMenuListener() {
                    @Override
                    public void onResume() {
                        handleResume();
                    }

                    @Override
                    public void onSaveAndResume() {
                        handleSaveAndResume();
                    }

                    @Override
                    public void onSaveAndExit() {
                        handleSaveAndExit();
                    }

                    @Override
                    public void onExitWithoutSaving() {
                        handleExit();
                    }
                });

        pauseMenuContainer.getChildren().add(menu);
    }

    private void showSaveConfirmation() {
        com.kuroyale.util.ui.ThemedAlertManager.show(
                "Game Saved",
                "Match saved successfully! You can resume it later from the main menu.");
    }

    private void showSaveConfirmationBrief() {
        // Create a temporary label overlay
        javafx.scene.control.Label saveLabel = new javafx.scene.control.Label("Match Saved!");
        saveLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #00ff00; -fx-font-weight: bold; " +
                "-fx-background-color: rgba(0,0,0,0.8); -fx-padding: 20; -fx-background-radius: 10;");

        pauseMenuContainer.getChildren().clear();
        pauseMenuContainer.getChildren().add(saveLabel);

        // Hide after 2 seconds
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(1.5));
        pause.setOnFinished(e -> handleResume());
        pause.play();
    }

    // Initializes game state from a saved game
    private void initializeFromSavedGame(SavedGameState savedGame) {
        gameState = model.loadGame(savedGame);
        currentArenaLayout = savedGame.getArenaLayout();
        savedComboCount = savedGame.getComboCount(); // Store for later restoration
    }

    @FXML
    public void handleExit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        // Award gold BEFORE cleaning up combo service so combo count is available
        try {
            awardGoldIfEligible();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        if (comboService != null) {
            comboService.cleanup();
            comboService = null;
        }

        try {
            sceneLoader.load(arenaContainer, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void awardGoldIfEligible() throws java.io.IOException {
        if (gameState == null) {
            return;
        }
        // Early exits (game not finished) grant no gold
        if (!gameState.isGameOver()) {
            return;
        }

        User currentUser = model.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        int playerScore = gameState.getPlayerScore();
        int botScore = gameState.getBotScore();
        boolean playerWon = gameState.isPlayerWinner();
        boolean isDraw = gameState.isDraw();

        int comboBonus = 0;
        if (comboService != null) {
            comboBonus = comboService.getUniqueComboCount();
        }

        model.processMatchResult(playerScore, botScore, comboBonus, playerWon, isDraw);
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
}
