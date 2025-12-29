package com.kuroyale.controller;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ElixirBar;
import com.kuroyale.view.battle.HandView;
import com.kuroyale.util.SceneLoader;
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
    private javafx.scene.control.Label gameOverScore;

    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
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

        // Start Game Loop
        startGameLoop();
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

        // Check for Double Elixir
        if (gameState.isDoubleElixir() && !doubleElixirShown) {
            doubleElixirShown = true;
            // Visual indicators are handled by ElixirBar and BattleArenaView
            elixirBar.setDoubleElixirActive(true);
        }

        // Check for Game Over
        if (gameState.isGameOver() && !gameOverShown) {
            gameOverShown = true;
            gameLoop.stop();

            // Handle Challenge Results
            if (currentChallenge != null) {
                handleChallengeGameOver();
            } else {
                showGameOverPopup();
            }
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

        // Track Challenge Completion (Quest)
        com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                .updateProgress(com.kuroyale.model.enums.QuestType.COMPLETE_CHALLENGES, 1);

        if (playerWon) {
            // Track Win Quests
            com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                    .updateProgress(com.kuroyale.model.enums.QuestType.WIN_MATCHES, 1);

            // Track Achievements
            com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.CHALLENGE_MASTER, 1);

            if (stars == 3) {
                com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
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

    private void showGameOverPopup() {
        // Clear overlay and make it visible
        overlayContainer.setVisible(false);
        gameOverRoot.setVisible(true);

        boolean playerWon = gameState.isPlayerWinner();
        // Track Matches Played (Veteran Player Achievement)
        com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                .updateProgress(com.kuroyale.model.enums.AchievementType.VETERAN_PLAYER, 1);

        if (playerWon) {
            // Track Win Quests
            com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                    .updateProgress(com.kuroyale.model.enums.QuestType.WIN_MATCHES, 1);
            com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                    .updateProgress(com.kuroyale.model.enums.QuestType.WIN_PVP_MATCH, 1);

            // Track Win Without Losing Tower
            if (gameState.getBotScore() == 0) {
                com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                        .updateProgress(com.kuroyale.model.enums.QuestType.WIN_WITHOUT_LOSING_TOWER, 1);
            }

            // Track Win Achievements
            com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.FIRST_BLOOD, 1);
            com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.UNDEFEATED, 1);
        }

        gameOverTitle.setText(playerWon ? "VICTORY" : "DEFEAT");

        gameOverTitle.getStyleClass().removeAll("victory-text", "defeat-text");
        gameOverTitle.getStyleClass().add(playerWon ? "victory-text" : "defeat-text");

        gameOverScore
                .setText(String.format("Player: %d  -  Bot: %d", gameState.getPlayerScore(), gameState.getBotScore()));
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
            SavedGameState savedGame = model.saveGame(gameState, currentUser, currentArenaLayout);
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

        com.kuroyale.view.battle.PauseMenuView menu = new com.kuroyale.view.battle.PauseMenuView(
                new com.kuroyale.view.battle.PauseMenuView.PauseMenuListener() {
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
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Game Saved");
        alert.setHeaderText("Success");
        alert.setContentText("Match saved successfully! You can resume it later from the main menu.");
        alert.showAndWait();
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
    }

    @FXML
    public void handleExit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        try {
            awardGoldIfEligible();
        } catch (java.io.IOException e) {
            e.printStackTrace();
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

        model.processMatchResult(playerScore, botScore);
    }
}