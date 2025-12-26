package com.kuroyale.controller;

import com.kuroyale.model.*;
import com.kuroyale.view.BattleArenaView;
import com.kuroyale.view.ElixirBar;
import com.kuroyale.view.HandView;
import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/*Controller for the Battle screen.
 * Manages the game loop, user input, and UI updates.*/
public class BattleController {

    @FXML private StackPane arenaContainer;
    @FXML private HBox elixirContainer;
    @FXML private VBox handContainer;
    @FXML private VBox overlayContainer;
    @FXML private VBox pauseMenuContainer;

    private GameState gameState;
    private BattleArenaView arenaView;
    private ElixirBar elixirBar;
    private HandView handView;
    private AnimationTimer gameLoop;
    private ArenaLayout currentArenaLayout;
    private boolean isPaused = false;
    private SavedGameState loadedSavedGame = null;

    // TEAM_003: MVC pattern - use Model instead of direct service access
    private final BattleModel model = new BattleModel();
    
    //Sets a saved game to load from
    public void setLoadedSavedGame(SavedGameState savedGame) {
        this.loadedSavedGame = savedGame;
    }

    @FXML
    public void initialize() {
        // The actual initialization happens in startGame() which is called after setup
    }
    
    /*Starts the game. Must be called AFTER setLoadedSavedGame() if loading a saved game */
    public void startGame() {
        // TEAM_003: Delegate to Model
        // Initialize game state
        User currentUser = model.getCurrentUser();
        if (currentUser == null) {
            handleExit();
            return;
        }

        // Set current user in arena service to load their saved layout
        model.setCurrentUserInArenaService(currentUser);

        // Check if loading from saved game
        if (loadedSavedGame != null) {
            System.out.println("⚠️ LOADING FROM SAVED GAME ⚠️");
            // Load from saved state
            initializeFromSavedGame(loadedSavedGame);
        } else {
            System.out.println("▶️ STARTING NEW GAME");
            // Start new game and Load user data
            // Convert List<String> to Deck object
            Deck playerDeck = model.createDeckFromNames(currentUser.getDeck());
            ArenaLayout playerLayout = model.loadArenaLayout(); // Load saved layout
            currentArenaLayout = playerLayout;

            // Create Arena
            Arena arena = model.createArena(playerLayout);

            // Create Bot Deck (Random or fixed)
            Deck botDeck = model.createBotDeck(currentUser);

            // Initialize GameState
            gameState = new GameState(playerDeck, botDeck, arena);
        }

        // Initialize UI Components
        arenaView = new BattleArenaView(gameState);
        arenaContainer.getChildren().add(arenaView);

        // Handle clicks on arena for card placement
        arenaView.setOnGridClicked((tileX, tileY) -> {handleArenaClick(tileX, tileY);});

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

    private boolean doubleElixirShown = false;
    private boolean gameOverShown = false;

    private void update(double deltaTime) {
        // Skip update if paused
        if (isPaused) {
            return;
        }
        
        // Update Game Logic and UI
        gameState.update(deltaTime);
        elixirBar.update();
        handView.update();
        arenaView.update();

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
            showGameOverPopup();
        }
    }

    private void showGameOverPopup() {
        // Clear overlay and make it visible
        overlayContainer.getChildren().clear();
        overlayContainer.setVisible(true);

        VBox content = new VBox(20);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle(
                "-fx-background-color: #333; -fx-padding: 40; -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 2;");
        content.setMaxSize(400, 300);

        boolean playerWon = gameState.isPlayerWinner();
        javafx.scene.control.Label title = new javafx.scene.control.Label(playerWon ? "VICTORY" : "DEFEAT");
        String titleColor = playerWon ? "#00ff00" : "#ff0000";
        title.setStyle("-fx-font-size: 36px; -fx-text-fill: " + titleColor + "; -fx-font-weight: bold;");

        javafx.scene.control.Label score = new javafx.scene.control.Label(
                String.format("Player: %d  -  Bot: %d", gameState.getPlayerScore(), gameState.getBotScore()));
        score.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");

        javafx.scene.control.Button exitBtn = new javafx.scene.control.Button("EXIT");
        exitBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 30;");
        exitBtn.setOnAction(e -> handleExit());

        content.getChildren().addAll(title, score, exitBtn);

        overlayContainer.getChildren().add(content);
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
        // TEAM_003: Delegate to Model
        // Save the game
        User currentUser = model.getCurrentUser();
        if (currentUser != null && currentArenaLayout != null) {
            SavedGameState savedGame = model.saveGame(gameState, currentUser, currentArenaLayout);
            if (savedGame != null) {
                showSaveConfirmation();
            } else {
                System.err.println("Failed to save game");
            }
        }
        
        handleExit();
    }
    
    private void handleSaveAndResume() {
        // TEAM_003: Delegate to Model
        // Save the game
        User currentUser = model.getCurrentUser();
        if (currentUser != null && currentArenaLayout != null) {
            SavedGameState savedGame = model.saveGame(gameState, currentUser, currentArenaLayout);
            if (savedGame != null) {
                showSaveConfirmationBrief();
            } else {
                System.err.println("Failed to save game");
            }
        }
        
        handleResume();
    }
    
    private void showPauseMenu() {
        pauseMenuContainer.getChildren().clear();
        pauseMenuContainer.setVisible(true);
        
        VBox content = new VBox(30);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        content.setStyle("-fx-background-color: #2a2a2a; -fx-padding: 50; -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 3;");
        content.setMaxSize(500, 400);
        
        javafx.scene.control.Label title = new javafx.scene.control.Label("PAUSED");
        title.setStyle("-fx-font-size: 42px; -fx-text-fill: white; -fx-font-weight: bold;");
        
        javafx.scene.control.Button resumeBtn = new javafx.scene.control.Button("RESUME");
        resumeBtn.setStyle("-fx-font-size: 20px; -fx-padding: 15 50; -fx-min-width: 300;");
        resumeBtn.setOnAction(e -> handleResume());
        
        javafx.scene.control.Button saveResumeBtn = new javafx.scene.control.Button("SAVE & RESUME");
        saveResumeBtn.setStyle("-fx-font-size: 20px; -fx-padding: 15 50; -fx-min-width: 300;");
        saveResumeBtn.setOnAction(e -> handleSaveAndResume());
        
        javafx.scene.control.Button saveExitBtn = new javafx.scene.control.Button("SAVE & EXIT");
        saveExitBtn.setStyle("-fx-font-size: 20px; -fx-padding: 15 50; -fx-min-width: 300;");
        saveExitBtn.setOnAction(e -> handleSaveAndExit());
        
        javafx.scene.control.Button exitBtn = new javafx.scene.control.Button("EXIT WITHOUT SAVING");
        exitBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 30; -fx-min-width: 300;");
        exitBtn.setOnAction(e -> handleExit());
        
        content.getChildren().addAll(title, resumeBtn, saveResumeBtn, saveExitBtn, exitBtn);
        pauseMenuContainer.getChildren().add(content);
    }
    
    private void showSaveConfirmation() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
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
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
        pause.setOnFinished(e -> handleResume());
        pause.play();
    }
    
    //Initializes game state from a saved game
    private void initializeFromSavedGame(SavedGameState savedGame) {
        // TEAM_003: Delegate to Model
        // Create decks from saved card names
        Deck playerDeck = model.createDeckFromNames(savedGame.getPlayerDeckCards());
        Deck botDeck = model.createDeckFromNames(savedGame.getBotDeckCards());
        
        // Load arena layout from saved game
        ArenaLayout layout = savedGame.getArenaLayout();
        currentArenaLayout = layout;
        Arena arena = model.createArena(layout);
        
        // Create game state
        gameState = new GameState(playerDeck, botDeck, arena);
        
        // Restore saved state (time, elixir, scores)
        gameState.restoreFromSaved(
            savedGame.getGameTime(),
            savedGame.isDoubleElixir(),
            savedGame.getPlayerScore(),
            savedGame.getBotScore(),
            savedGame.getPlayerElixir(),
            savedGame.getBotElixir()
        );
        
        // Restore tower health
        for (SavedGameState.SavedTower savedTower : savedGame.getTowers()) {
            gameState.restoreTowerHealth(savedTower);
        }
        
        // TEAM_003: Delegate to Model
        // Restore active troops
        for (SavedGameState.SavedTroop savedTroop : savedGame.getActiveTroops()) {
            Card card = model.getCardByName(savedTroop.getCardName());
            if (card != null) {
                GridPosition pos = GridPosition.tryCreate(savedTroop.getGridX(), savedTroop.getGridY());
                if (pos != null) {
                    Troop troop = new Troop(card, pos, savedTroop.isPlayerSide());
                    // Set health to saved value
                    double healthLoss = card.getHp() - savedTroop.getCurrentHealth();
                    if (healthLoss > 0) {
                        troop.takeDamage(healthLoss);
                    }
                    // Set state
                    try {
                        troop.setUnitState(UnitState.valueOf(savedTroop.getState()));
                    } catch (IllegalArgumentException e) {
                        troop.setUnitState(UnitState.IDLE);
                    }
                    gameState.getActiveTroops().add(troop);
                } else {}
            } else {}
        }
        
        // TEAM_003: Delegate to Model
        // Restore active buildings
        for (SavedGameState.SavedBuilding savedBuilding : savedGame.getActiveBuildings()) {
            Card card = model.getCardByName(savedBuilding.getCardName());
            if (card != null) {
                GridPosition pos = GridPosition.tryCreate(savedBuilding.getGridX(), savedBuilding.getGridY());
                if (pos != null) {
                    Building building = new Building(
                        pos, 
                        savedBuilding.getWidth(), 
                        savedBuilding.getHeight(),
                        savedBuilding.isPlayerSide(),
                        card.getHp(),
                        card.getImagePath(),
                        card.getLifetime()
                    );
                    building.configureCombatFromCard(card);
                    
                    // Set health to saved value
                    double healthLoss = card.getHp() - savedBuilding.getCurrentHealth();
                    if (healthLoss > 0) {
                        building.takeDamage(healthLoss);
                    }
                    
                    // Occupy footprint
                    for (int dx = 0; dx < building.getWidth(); dx++) {
                        for (int dy = 0; dy < building.getHeight(); dy++) {
                            int gx = pos.getX() + dx;
                            int gy = pos.getY() + dy;
                            GridPosition cellPos = GridPosition.tryCreate(gx, gy);
                            if (cellPos != null) {
                                GridCell cell = arena.getCell(cellPos);
                                if (cell != null) {
                                    try {
                                        cell.setOccupant(building);
                                    } catch (IllegalStateException e) {
                                        // Ignore if invalid
                                    }
                                }
                            }
                        }
                    }
                    gameState.getActiveBuildings().add(building);
                } else {}
            } else {}
        }
    }

    @FXML
    public void handleExit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) arenaContainer.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Main Menu");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}