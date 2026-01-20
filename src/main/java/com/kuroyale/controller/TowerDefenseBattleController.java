package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.ArenaLayout;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.state.GameState;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.view.battle.BattleArenaView;
import com.kuroyale.view.battle.ui.ElixirBarView;
import com.kuroyale.view.battle.ui.HandView;
import com.kuroyale.view.card.CardView;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for Tower Defense Battle with REAL arena combat.
 * Uses the existing battle arena system for actual troop combat.
 */
public class TowerDefenseBattleController {

    @FXML
    private AnchorPane root;
    @FXML
    private BorderPane battleContainer;

    private final SceneLoader sceneLoader = new SceneLoader();

    // Battle state
    private List<Card> attackWave;
    private List<Card> sortedAttackWave;
    private Deck player1Deck;
    private Deck player2Deck;
    private int currentRound;
    private int player1Score;
    private int player2Score;

    private int currentDefendingPlayer = 1;
    private GameState gameState;
    private BattleArenaView arenaView;
    private HandView handView;
    private ElixirBarView elixirBar;
    private AnimationTimer gameLoop;

    // Attack wave spawning
    private int attackWaveIndex = 0;
    private double spawnTimer = 0;
    private static final double SPAWN_INTERVAL = 1.5;
    private boolean allAttackersSpawned = false;

    // Defense results
    private boolean player1DefenseSuccess = false;
    private boolean player2DefenseSuccess = false;
    private int player1ElixirSpent = 0;
    private int player2ElixirSpent = 0;
    private int currentElixirSpent = 0;

    @FXML
    private void initialize() {
        root.getStyleClass().add("main-menu-background");
    }

    /**
     * Sets up the battle with attack wave and player decks.
     */
    public void setupBattle(List<Card> attackWave, Deck player1Deck, Deck player2Deck,
            int round, int p1Score, int p2Score) {
        this.attackWave = attackWave;
        this.player1Deck = player1Deck;
        this.player2Deck = player2Deck;
        this.currentRound = round;
        this.player1Score = p1Score;
        this.player2Score = p2Score;

        // Sort by elixir descending
        this.sortedAttackWave = new ArrayList<>(attackWave);
        sortedAttackWave.sort(Comparator.comparingInt(Card::getCost).reversed());

        System.out.println("[TD BATTLE] Setup - Round " + round + ", P1: " + p1Score + ", P2: " + p2Score);
        System.out.println("[TD BATTLE] Attack wave: " + attackWave.size() + " troops");

        // Reset results
        player1DefenseSuccess = false;
        player2DefenseSuccess = false;
        player1ElixirSpent = 0;
        player2ElixirSpent = 0;

        // Start with player 1 defending
        currentDefendingPlayer = 1;
        showPreBattleScreen();
    }

    /**
     * Shows info screen before battle starts.
     */
    private void showPreBattleScreen() {
        battleContainer.getChildren().clear();

        // Reset attack wave spawning
        attackWaveIndex = 0;
        spawnTimer = 0;
        allAttackersSpawned = false;
        currentElixirSpent = 0;

        Deck currentDeck = currentDefendingPlayer == 1 ? player1Deck : player2Deck;
        String playerColor = currentDefendingPlayer == 1 ? "#22c55e" : "#3b82f6";

        VBox infoBox = new VBox(20);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setPadding(new Insets(30));
        infoBox.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);");

        Label roundLabel = new Label("ROUND " + currentRound + " | P1: " + player1Score + " - P2: " + player2Score);
        roundLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");

        Label playerLabel = new Label("PLAYER " + currentDefendingPlayer + " - PREPARE TO DEFEND!");
        playerLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + playerColor + ";");

        // Attack wave preview
        int totalElixir = sortedAttackWave.stream().mapToInt(Card::getCost).sum();
        Label attackInfo = new Label(
                "⚔️ Incoming Attack: " + sortedAttackWave.size() + " troops, " + totalElixir + " elixir");
        attackInfo.setStyle("-fx-font-size: 18px; -fx-text-fill: #ef4444;");

        HBox attackPreview = new HBox(8);
        attackPreview.setAlignment(Pos.CENTER);
        for (Card card : sortedAttackWave) {
            CardView cv = new CardView(card);
            cv.setHoverScalingEnabled(false);
            cv.setScaleX(0.5);
            cv.setScaleY(0.5);
            StackPane wrapper = new StackPane(cv);
            wrapper.setMinSize(45, 60);
            wrapper.setMaxSize(45, 60);
            attackPreview.getChildren().add(wrapper);
        }

        // Defense deck preview
        Label defenseInfo = new Label("🛡️ Your Defense: " + currentDeck.size() + " cards");
        defenseInfo.setStyle("-fx-font-size: 18px; -fx-text-fill: #22c55e;");

        HBox defensePreview = new HBox(8);
        defensePreview.setAlignment(Pos.CENTER);
        for (Card card : currentDeck.getCards()) {
            CardView cv = new CardView(card);
            cv.setHoverScalingEnabled(false);
            cv.setScaleX(0.5);
            cv.setScaleY(0.5);
            StackPane wrapper = new StackPane(cv);
            wrapper.setMinSize(45, 60);
            wrapper.setMaxSize(45, 60);
            defensePreview.getChildren().add(wrapper);
        }

        Button startButton = new Button("⚔️ START DEFENSE");
        startButton.setStyle("-fx-font-size: 20px; -fx-background-color: #ef4444; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 15 40; -fx-cursor: hand;");
        startButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            startDefensePhase();
        });

        infoBox.getChildren().addAll(roundLabel, playerLabel, attackInfo, attackPreview,
                defenseInfo, defensePreview, startButton);
        battleContainer.setCenter(infoBox);
    }

    /**
     * Starts the real arena defense phase.
     */
    private void startDefensePhase() {
        battleContainer.getChildren().clear();

        Deck currentDeck = currentDefendingPlayer == 1 ? player1Deck : player2Deck;

        // Create empty bot deck for enemy (we'll spawn troops manually)
        Deck emptyBotDeck = new Deck();

        // Create arena with default layout
        ArenaLayout layout = new ArenaLayout("Default");
        Arena arena = new Arena(layout);

        // Create game state - player defends, no bot AI
        gameState = new GameState(currentDeck, emptyBotDeck, arena);
        gameState.setNetworkMode(true); // Disable bot AI

        // Give player faster elixir regeneration
        gameState.getPlayerElixir().setElixirMultiplier(3.0);
        gameState.getPlayerElixir().setCurrentElixir(10); // Start with full elixir

        // Create arena view
        arenaView = new BattleArenaView(gameState);

        // Handle clicks for card placement
        arenaView.setOnGridClicked((tileX, tileY) -> {
            handleArenaClick(tileX, tileY);
        });

        // Create hand view
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

        // Create elixir bar
        elixirBar = new ElixirBarView(gameState.getPlayerElixir());

        // Layout
        VBox topBar = new VBox(5);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: rgba(20, 20, 30, 0.9);");

        String playerColor = currentDefendingPlayer == 1 ? "#22c55e" : "#3b82f6";
        Label statusLabel = new Label("PLAYER " + currentDefendingPlayer + " DEFENDING!");
        statusLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + playerColor + ";");

        Label waveLabel = new Label("Wave: " + sortedAttackWave.size() + " troops incoming...");
        waveLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #fbbf24;");
        waveLabel.setId("waveLabel");

        topBar.getChildren().addAll(statusLabel, waveLabel);

        VBox bottomPanel = new VBox(10);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(10));
        bottomPanel.setStyle("-fx-background-color: rgba(30, 30, 40, 0.95);");
        bottomPanel.getChildren().addAll(elixirBar, handView);

        battleContainer.setTop(topBar);
        battleContainer.setCenter(arenaView);
        battleContainer.setBottom(bottomPanel);

        // Start game loop
        startGameLoop();
    }

    private void handleArenaClick(int tileX, int tileY) {
        int selectedIndex = handView.getSelectedIndex();
        if (selectedIndex == -1)
            return;

        Card card = gameState.getPlayerHand().getCard(selectedIndex);
        if (card == null)
            return;

        int cost = card.getCost();
        if (gameState.getPlayerElixir().getCurrentElixir() < cost)
            return;

        // Try to play the card
        if (gameState.placeCard(true, selectedIndex, tileX, tileY)) {
            SoundEffectUtil.playButtonClick();
            currentElixirSpent += cost;
            handView.clearSelection();
            arenaView.highlightValidCells(false, false);
        }
    }

    private void startGameLoop() {
        final long[] lastTime = { System.nanoTime() };

        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double deltaTime = (now - lastTime[0]) / 1_000_000_000.0;
                lastTime[0] = now;

                // Cap delta time
                deltaTime = Math.min(deltaTime, 0.05);

                // Spawn attack wave troops
                spawnAttackWave(deltaTime);

                // Update game state (use updateClientOnly to bypass automatic win conditions)
                gameState.updateClientOnly(deltaTime);

                // Update visuals
                arenaView.update(deltaTime);
                elixirBar.update();
                handView.update();

                // Update wave label
                Label waveLabel = (Label) battleContainer.lookup("#waveLabel");
                if (waveLabel != null) {
                    int remaining = sortedAttackWave.size() - attackWaveIndex;
                    long enemiesAlive = gameState.getTroops().stream()
                            .filter(t -> !t.isPlayerSide() && t.getCurrentHealth() > 0).count();
                    waveLabel.setText("Spawning: " + remaining + " | Enemies alive: " + enemiesAlive);
                }

                // Check if defense is over
                if (checkDefenseComplete()) {
                    stop();
                    handleDefenseComplete();
                }
            }
        };
        gameLoop.start();
    }

    private void spawnAttackWave(double deltaTime) {
        if (allAttackersSpawned)
            return;

        spawnTimer += deltaTime;
        if (spawnTimer >= SPAWN_INTERVAL && attackWaveIndex < sortedAttackWave.size()) {
            Card troopCard = sortedAttackWave.get(attackWaveIndex);

            // Spawn from top bridge area
            int spawnX = (attackWaveIndex % 2 == 0) ? 4 : 13;
            int spawnY = 2; // Top of arena (enemy side)

            // Spawn as enemy troop
            gameState.spawnTroopDirectly(false, troopCard, spawnX, spawnY, troopCard.getCount());

            System.out.println("[TD BATTLE] Spawned: " + troopCard.getName() +
                    " (" + troopCard.getCost() + " elixir) at (" + spawnX + "," + spawnY + ")");

            attackWaveIndex++;
            spawnTimer = 0;

            if (attackWaveIndex >= sortedAttackWave.size()) {
                allAttackersSpawned = true;
                System.out.println("[TD BATTLE] All attackers spawned!");
            }
        }
    }

    private boolean checkDefenseComplete() {
        // Check if player's tower is destroyed (manually)
        if (!gameState.getArena().isPlayerKingAlive()) {
            return true;
        }

        // Check if all attackers killed
        if (allAttackersSpawned) {
            long enemiesAlive = gameState.getTroops().stream()
                    .filter(t -> !t.isPlayerSide() && t.getCurrentHealth() > 0).count();
            if (enemiesAlive == 0) {
                return true;
            }
        }

        return false;
    }

    private void handleDefenseComplete() {
        // Determine success: tower still standing = success
        boolean success = gameState.getArena().isPlayerKingAlive();

        if (currentDefendingPlayer == 1) {
            player1DefenseSuccess = success;
            player1ElixirSpent = currentElixirSpent;
        } else {
            player2DefenseSuccess = success;
            player2ElixirSpent = currentElixirSpent;
        }

        System.out.println("[TD BATTLE] Player " + currentDefendingPlayer + " defense: " +
                (success ? "SUCCESS" : "FAILED") + " | Elixir spent: " + currentElixirSpent);

        showDefenseResult(success);
    }

    private void showDefenseResult(boolean success) {
        battleContainer.getChildren().clear();

        String resultText = success ? "✅ DEFENSE SUCCESSFUL!" : "❌ TOWER DESTROYED!";
        String resultColor = success ? "#22c55e" : "#ef4444";

        VBox resultBox = new VBox(15);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPadding(new Insets(30));
        resultBox.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);");

        Label resultLabel = new Label(resultText);
        resultLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + resultColor + ";");

        Label statsLabel = new Label("Player " + currentDefendingPlayer + " - Elixir Spent: " + currentElixirSpent);
        statsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        resultBox.getChildren().addAll(resultLabel, statsLabel);
        battleContainer.setCenter(resultBox);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> {
            if (currentDefendingPlayer == 1) {
                currentDefendingPlayer = 2;
                showPreBattleScreen();
            } else {
                calculateRoundResult();
            }
        });
        delay.play();
    }

    private void calculateRoundResult() {
        String resultMessage;
        String winnerColor = "#fbbf24";

        if (player1DefenseSuccess && !player2DefenseSuccess) {
            player1Score++;
            resultMessage = "🏆 Player 1 wins! (Tower survived)";
            winnerColor = "#22c55e";
        } else if (!player1DefenseSuccess && player2DefenseSuccess) {
            player2Score++;
            resultMessage = "🏆 Player 2 wins! (Tower survived)";
            winnerColor = "#3b82f6";
        } else if (player1DefenseSuccess && player2DefenseSuccess) {
            if (player1ElixirSpent < player2ElixirSpent) {
                player1Score++;
                resultMessage = "Both defended! P1 wins (" + player1ElixirSpent + " vs " + player2ElixirSpent
                        + " elixir)";
                winnerColor = "#22c55e";
            } else if (player2ElixirSpent < player1ElixirSpent) {
                player2Score++;
                resultMessage = "Both defended! P2 wins (" + player2ElixirSpent + " vs " + player1ElixirSpent
                        + " elixir)";
                winnerColor = "#3b82f6";
            } else {
                resultMessage = "Both defended with same elixir! Tie";
            }
        } else {
            resultMessage = "Both towers destroyed! No points";
        }

        System.out.println("[TD BATTLE] " + resultMessage);

        if (player1Score >= 2) {
            showFinalResult("PLAYER 1 WINS!", "#22c55e");
        } else if (player2Score >= 2) {
            showFinalResult("PLAYER 2 WINS!", "#3b82f6");
        } else {
            showRoundResult(resultMessage, winnerColor);
        }
    }

    private void showRoundResult(String message, String color) {
        battleContainer.getChildren().clear();

        VBox resultBox = new VBox(20);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPadding(new Insets(40));
        resultBox.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);");

        Label roundLabel = new Label("Round " + currentRound + " Complete!");
        roundLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");

        Label resultLabel = new Label(message);
        resultLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label scoreLabel = new Label("Score: P1 " + player1Score + " - P2 " + player2Score);
        scoreLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        resultBox.getChildren().addAll(roundLabel, resultLabel, scoreLabel);
        battleContainer.setCenter(resultBox);

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> startNextRound());
        delay.play();
    }

    private void startNextRound() {
        currentRound++;
        try {
            sceneLoader.load(root, "/fxml/tower-defense-draft.fxml", "KU Royale - Tower Defense", controller -> {
                if (controller instanceof TowerDefenseDraftController dc) {
                    dc.setScores(currentRound, player1Score, player2Score);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showFinalResult(String winner, String color) {
        battleContainer.getChildren().clear();

        VBox resultBox = new VBox(25);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPadding(new Insets(50));
        resultBox.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);");

        Label trophyLabel = new Label("🏆");
        trophyLabel.setStyle("-fx-font-size: 80px;");

        Label winnerLabel = new Label(winner);
        winnerLabel.setStyle("-fx-font-size: 48px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label scoreLabel = new Label("Final: P1 " + player1Score + " - P2 " + player2Score);
        scoreLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        Button menuButton = new Button("🏠 BACK TO MENU");
        menuButton.setStyle("-fx-font-size: 18px; -fx-background-color: #22c55e; -fx-text-fill: white; " +
                "-fx-background-radius: 10; -fx-padding: 15 40; -fx-cursor: hand;");
        menuButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            try {
                sceneLoader.load(root, "/fxml/pvp-mode-selection.fxml", "KU Royale - PvP Mode", null);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        resultBox.getChildren().addAll(trophyLabel, winnerLabel, scoreLabel, menuButton);
        battleContainer.setCenter(resultBox);
    }
}
