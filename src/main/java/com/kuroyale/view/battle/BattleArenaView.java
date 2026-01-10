package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.logic.GameState;
import com.kuroyale.model.entities.GridCell;
import com.kuroyale.model.enums.TileType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import javafx.scene.shape.Rectangle;

// Renders the battle arena and placed units.
public class BattleArenaView extends javafx.scene.layout.BorderPane implements com.kuroyale.event.GameEventListener {
    private final GridPane grid;
    private final Pane unitLayer;
    private final Canvas highlightLayer;
    private final Pane arenaPane;
    private final GameState gameState;
    private final com.kuroyale.model.logic.PvPGameState pvpGameState; // For PvP mode
    private final javafx.scene.control.Label timerLabel;
    private final javafx.scene.layout.HBox playerScoreContainer;
    private final javafx.scene.layout.HBox botScoreContainer;
    private final javafx.scene.control.Label comboLabel;
    private final java.util.Map<Long, javafx.scene.Node> cellIndex = new java.util.HashMap<>();

    // Track hovered tile for highlighting
    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

    private static final int TILE_SIZE = 18; // Matches ArenaDesignController

    /**
     * Constructor for PvP mode.
     */
    public BattleArenaView(com.kuroyale.model.logic.PvPGameState pvpGameState) {
        this.gameState = null;
        this.pvpGameState = pvpGameState;

        Arena arena = pvpGameState.getArena();

        // Initialize with common setup
        this.grid = new GridPane();
        this.grid.setHgap(0);
        this.grid.setVgap(0);
        this.unitLayer = new Pane();
        unitLayer.setMouseTransparent(true);
        this.highlightLayer = new Canvas(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        this.highlightLayer.setMouseTransparent(true);
        this.arenaPane = new Pane(grid, highlightLayer, unitLayer);
        arenaPane.setPrefSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        unitLayer.layoutXProperty().bind(grid.layoutXProperty());
        unitLayer.layoutYProperty().bind(grid.layoutYProperty());
        highlightLayer.layoutXProperty().bind(grid.layoutXProperty().add(TILE_SIZE / 2.0));
        highlightLayer.layoutYProperty().bind(grid.layoutYProperty().add(TILE_SIZE / 2.0));

        arenaPane.setOnMouseClicked(e -> {
            if (onGridClick != null) {
                int[] coords = calculateTileCoordinates(e.getX(), e.getY());
                if (coords != null) {
                    onGridClick.accept(coords[0], coords[1]);
                }
            }
        });

        arenaPane.setOnMouseMoved(e -> {
            int[] coords = calculateTileCoordinates(e.getX(), e.getY());
            if (coords != null) {
                highlightHoveredTile(coords[0], coords[1]);
            } else {
                clearHoverHighlight();
            }
        });

        arenaPane.setOnMouseExited(e -> clearHoverHighlight());

        // PvP mode: No sidebar (timer/score handled in controller)
        this.timerLabel = null;
        this.playerScoreContainer = null;
        this.botScoreContainer = null;
        this.comboLabel = null;

        StackPane centerContainer = new StackPane(arenaPane);
        centerContainer.setAlignment(javafx.geometry.Pos.CENTER);
        centerContainer.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
        this.setCenter(centerContainer);

        // Initialize renderers - create a minimal proxy for rendering
        this.troopRenderer = new TroopRenderer(unitLayer, this::getGridCell);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode);
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);

        com.kuroyale.event.GameEventBus.getInstance().subscribe(this);

        renderArenaPvP(arena);
    }

    public BattleArenaView(GameState gameState) {
        this.gameState = gameState;
        this.pvpGameState = null;

        // Sidebar is now defined in battle.fxml - no longer created here
        this.timerLabel = null;
        this.playerScoreContainer = null;
        this.botScoreContainer = null;
        this.comboLabel = null;

        // Center Arena
        this.grid = new GridPane();
        this.grid.setHgap(0);
        this.grid.setVgap(0);
        this.unitLayer = new Pane();

        // Make unit layer transparent to mouse events so clicks go to grid for
        // placement
        unitLayer.setMouseTransparent(true);

        // Highlight Layer
        this.highlightLayer = new Canvas(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        this.highlightLayer.setMouseTransparent(true);

        this.arenaPane = new Pane(grid, highlightLayer, unitLayer);
        // Set preferred size to match grid size
        arenaPane.setPrefSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        // Bind unitLayer position to grid position to ensure perfect alignment
        unitLayer.layoutXProperty().bind(grid.layoutXProperty());
        unitLayer.layoutYProperty().bind(grid.layoutYProperty());
        highlightLayer.layoutXProperty().bind(grid.layoutXProperty().add(TILE_SIZE / 2.0));
        highlightLayer.layoutYProperty().bind(grid.layoutYProperty().add(TILE_SIZE / 2.0));

        // Handle clicks directly on the arena pane to get correct local coordinates
        arenaPane.setOnMouseClicked(e -> {
            if (onGridClick != null) {
                int[] coords = calculateTileCoordinates(e.getX(), e.getY());
                if (coords != null) {
                    onGridClick.accept(coords[0], coords[1]);
                }
            }
        });

        // Handle mouse movement for hover highlighting
        arenaPane.setOnMouseMoved(e -> {
            int[] coords = calculateTileCoordinates(e.getX(), e.getY());
            if (coords != null) {
                highlightHoveredTile(coords[0], coords[1]);
            } else {
                clearHoverHighlight();
            }
        });

        // Clear hover highlight when mouse leaves arena
        arenaPane.setOnMouseExited(e -> {
            clearHoverHighlight();
        });

        StackPane centerContainer = new StackPane(arenaPane);
        // Explicitly center the arenaPane within the StackPane
        StackPane.setAlignment(arenaPane, javafx.geometry.Pos.CENTER);
        // Constrain arenaPane to its preferred size so it doesn't stretch
        arenaPane.setMaxSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        arenaPane.setMinSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        // Shift arena 20px to the left
        arenaPane.setTranslateX(-10);

        this.setCenter(centerContainer);

        // Initialize renderers
        this.troopRenderer = new TroopRenderer(unitLayer, this::getGridCell);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode); // requires CellIndexer or
                                                                                      // compatible lambda
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);

        com.kuroyale.event.GameEventBus.getInstance().subscribe(this);

        renderArena();
    }

    private java.util.function.BiConsumer<Integer, Integer> onGridClick;

    public void setOnGridClicked(java.util.function.BiConsumer<Integer, Integer> handler) {
        this.onGridClick = handler;
    }

    private void renderArena() {
        grid.getChildren().clear();
        cellIndex.clear();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = gameState.getArena().getCell(x, y);
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                // Style based on type
                TileType type = cell.getTileType();

                // Treat towers as grass for the base tile so they look right when destroyed
                if (type == TileType.PRINCESS_TOWER_USER || type == TileType.PRINCESS_TOWER_COMPUTER
                        || type == TileType.KING_TOWER_USER || type == TileType.KING_TOWER_COMPUTER) {
                    type = TileType.GRASS;
                }

                switch (type) {
                    case GRASS:
                        // Checkered pattern
                        if ((x + y) % 2 == 0) {
                            rect.setFill(Color.rgb(124, 252, 0)); // LawnGreen
                        } else {
                            rect.setFill(Color.rgb(50, 205, 50)); // LimeGreen
                        }
                        break;
                    case WATER:
                        rect.setFill(Color.LIGHTBLUE);
                        break;
                    case BRIDGE:
                        rect.setFill(Color.SADDLEBROWN);
                        break;
                    case ROAD:
                        rect.setFill(Color.SANDYBROWN);
                        break;
                    default:
                        rect.setFill(Color.GRAY);
                        break;
                }
                // Remove stroke for seamless look
                rect.setStroke(Color.TRANSPARENT);
                rect.setStrokeWidth(0.0);

                grid.add(rect, x, y);
                indexCellNode(x, y, rect);
            }
        }

        // Render Towers on top
        towerRenderer.renderTowers(gameState.getArena());
    }

    /**
     * Render arena for PvP mode (takes Arena directly instead of from gameState).
     */
    private void renderArenaPvP(Arena arena) {
        grid.getChildren().clear();
        cellIndex.clear();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = arena.getCell(x, y);
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                TileType type = cell.getTileType();

                // Treat towers as grass for base tile
                if (type == TileType.PRINCESS_TOWER_USER || type == TileType.PRINCESS_TOWER_COMPUTER
                        || type == TileType.KING_TOWER_USER || type == TileType.KING_TOWER_COMPUTER) {
                    type = TileType.GRASS;
                }

                switch (type) {
                    case GRASS:
                        if ((x + y) % 2 == 0) {
                            rect.setFill(Color.rgb(124, 252, 0));
                        } else {
                            rect.setFill(Color.rgb(50, 205, 50));
                        }
                        break;
                    case WATER:
                        rect.setFill(Color.LIGHTBLUE);
                        break;
                    case BRIDGE:
                        rect.setFill(Color.SADDLEBROWN);
                        break;
                    case ROAD:
                        rect.setFill(Color.SANDYBROWN);
                        break;
                    default:
                        rect.setFill(Color.GRAY);
                        break;
                }
                rect.setStroke(Color.TRANSPARENT);
                rect.setStrokeWidth(0.0);
                grid.add(rect, x, y);
                indexCellNode(x, y, rect);
            }
        }

        towerRenderer.renderTowers(arena);
    }

    private final java.util.List<ActiveSpellVisual> activeSpellVisuals = new java.util.ArrayList<>();

    private static class ActiveSpellVisual {
        final javafx.scene.Node node;
        double timeRemaining;

        ActiveSpellVisual(javafx.scene.Node node, double timeRemaining) {
            this.node = node;
            this.timeRemaining = timeRemaining;
        }
    }

    // Extracted renderers
    private TroopRenderer troopRenderer;
    private ProjectileRenderer projectileRenderer;
    private TowerRenderer towerRenderer;
    private BuildingRenderer buildingRenderer;

    // Last-value tracking for observer pattern (only update UI when changed)
    private int lastDisplayedSeconds = -1;
    private int lastPlayerScore = -1;
    private int lastBotScore = -1;
    private boolean lastDoubleElixir = false;

    public void update(double deltaTime) {
        // === OBSERVER PATTERN: Only update UI when values change ===

        // Update Timer (only when second changes)
        int totalSeconds = (int) Math.ceil(gameState.getGameTime());
        if (totalSeconds != lastDisplayedSeconds) {
            lastDisplayedSeconds = totalSeconds;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }

        // Change timer color during double elixir
        boolean isDoubleElixir = gameState.isDoubleElixir();
        if (isDoubleElixir != lastDoubleElixir) {
            lastDoubleElixir = isDoubleElixir;
            if (isDoubleElixir) {
                timerLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 24px; -fx-font-weight: bold;");
            } else {
                timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
            }
        }

        // Update Score
        int playerScore = gameState.getPlayerScore();
        int botScore = gameState.getBotScore();

        // Update crowns if score changed
        if (playerScore != lastPlayerScore) {
            lastPlayerScore = playerScore;
            updateScoreContainer(playerScoreContainer, playerScore, false);
        }
        if (botScore != lastBotScore) {
            lastBotScore = botScore;
            updateScoreContainer(botScoreContainer, botScore, true);
        }

        // Delegate to Renderers
        towerRenderer.cleanupDestroyedTowers(gameState.getArena());
        towerRenderer.updateHealthBars(gameState.getArena());
        troopRenderer.render(gameState);
        buildingRenderer.render(gameState);

        // Render projectiles for all combatants
        java.util.List<com.kuroyale.model.entities.ICombatant> combatants = new java.util.ArrayList<>();
        combatants.addAll(gameState.getArena().getAllTowers());
        combatants.addAll(gameState.getActiveBuildings());
        projectileRenderer.render(combatants);

        // Spell Effects
        updateSpellEffects(deltaTime);
    }

    /**
     * Update method for PvP mode.
     * Timer and score are handled by PvPBattleController, not in BattleArenaView.
     */
    public void updatePvP(double deltaTime) {
        if (pvpGameState == null) {
            return;
        }

        Arena arena = pvpGameState.getArena();

        // Delegate to Renderers
        towerRenderer.cleanupDestroyedTowers(arena);
        towerRenderer.updateHealthBars(arena);

        // Render troops using PvP renderers
        troopRenderer.renderPvP(pvpGameState);
        buildingRenderer.renderPvP(pvpGameState);

        // Render projectiles for all combatants
        java.util.List<com.kuroyale.model.entities.ICombatant> combatants = new java.util.ArrayList<>();
        combatants.addAll(arena.getAllTowers());
        combatants.addAll(pvpGameState.getActiveBuildings());
        projectileRenderer.render(combatants);

        // Spell Effects
        updateSpellEffects(deltaTime);
    }

    /*
     * Helper method to calculate tile coordinates from mouse position.
     * Finds which cell actually contains the mouse point to avoid offset issues.
     */
    private int[] calculateTileCoordinates(double mouseX, double mouseY) {
        // Use grid's layout position (more reliable than boundsInParent which can
        // include strokes)
        double gridOffsetX = grid.getLayoutX();
        double gridOffsetY = grid.getLayoutY();

        // Calculate relative position within grid
        double gridX = mouseX - gridOffsetX;
        double gridY = mouseY - gridOffsetY;

        // Calculate tile coordinates using arithmetic
        int tileX = (int) Math.floor(gridX / TILE_SIZE);
        int tileY = (int) Math.floor(gridY / TILE_SIZE);

        // Ensure coordinates are within valid bounds
        if (tileX >= 0 && tileX < Arena.WIDTH && tileY >= 0 && tileY < Arena.HEIGHT) {
            return new int[] { tileX, tileY };
        }
        return null;
    }

    // Helper method to get the grid cell node at the specified grid coordinates.
    private javafx.scene.Node getGridCell(int x, int y) {
        return cellIndex.getOrDefault(key(x, y), null);
    }

    private void indexCellNode(int x, int y, javafx.scene.Node node) {
        cellIndex.put(key(x, y), node);
    }

    private long key(int x, int y) {
        return (((long) x) << 32) | (y & 0xFFFFFFFFL);
    }

    /**
     * Helper to get the arena from either GameState or PvPGameState.
     */
    private Arena getArena() {
        if (gameState != null) {
            return gameState.getArena();
        } else if (pvpGameState != null) {
            return pvpGameState.getArena();
        }
        return null;
    }

    public void highlightValidCells(boolean show, boolean isSpell) {
        GraphicsContext gc = highlightLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());

        if (!show)
            return;

        // Use semi-transparent yellow/gold for simple highlight
        gc.setFill(Color.rgb(255, 215, 0, 0.3));

        if (isSpell) {
            // Spells can be placed anywhere - highlight full arena
            gc.fillRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());
        } else {
            // Standard units: Player side (bottom half) AND walkable (Grass)
            Arena arena = getArena();
            if (arena == null)
                return;
            for (int x = 0; x < Arena.WIDTH; x++) {
                // Iterating only bottom half (Player Side)
                for (int y = Arena.HEIGHT / 2; y < Arena.HEIGHT; y++) {
                    GridCell cell = arena.getCell(x, y);
                    // Check logic matches original: GRASS check
                    if (cell.getTileType() == TileType.GRASS) {
                        gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    /**
     * Highlights valid placement cells for Player 2 (top half of arena).
     * Used in PvP mode.
     */
    public void highlightPlayer2ValidCells(boolean show, boolean isSpell) {
        GraphicsContext gc = highlightLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());

        if (!show)
            return;

        // Use semi-transparent red for Player 2
        gc.setFill(Color.rgb(255, 100, 100, 0.3));

        if (isSpell) {
            // Spells can be placed anywhere - highlight full arena
            gc.fillRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());
        } else {
            // Standard units: Player 2 side (top half) AND walkable (Grass)
            Arena arena = getArena();
            if (arena == null)
                return;
            for (int x = 0; x < Arena.WIDTH; x++) {
                // Iterating only top half (Player 2 Side)
                for (int y = 0; y < Arena.HEIGHT / 2; y++) {
                    GridCell cell = arena.getCell(x, y);
                    if (cell.getTileType() == TileType.GRASS) {
                        gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    // Highlights the tile at the specified coordinates to show where the mouse is
    // hovering.
    private void highlightHoveredTile(int tileX, int tileY) {
        // If hovering over the same tile, no need to update
        if (currentHoveredTileX == tileX && currentHoveredTileY == tileY) {
            return;
        }

        // Clear previous hover highlight
        clearHoverHighlight();

        // Get the cell node for the hovered tile to get its bounds
        javafx.scene.Node node = getGridCell(tileX, tileY);
        if (node == null) {
            return;
        }

        // Skip towers (StackPanes) for hover highlight
        if (node instanceof StackPane) {
            return;
        }

        // Convert local bounds to parent coordinates
        javafx.geometry.Point2D topLeft = node.localToParent(0, 0);

        // Calculate position
        double cellX = topLeft.getX();
        double cellY = topLeft.getY();

        // Use TILE_SIZE for overlay dimensions
        Rectangle overlay = new Rectangle(TILE_SIZE, TILE_SIZE);

        // Clash Royale style: semi-transparent cyan fill with bright border
        overlay.setFill(Color.color(0.0, 0.8, 1.0, 0.25)); // Cyan with 25% opacity
        overlay.setStroke(Color.CYAN);
        overlay.setStrokeWidth(2.0);
        overlay.setStrokeType(javafx.scene.shape.StrokeType.INSIDE); // Stroke inside to avoid gaps

        // Position overlay
        overlay.setLayoutX(cellX);
        overlay.setLayoutY(cellY);

        // Make overlay transparent to mouse events so clicks pass through
        overlay.setMouseTransparent(true);

        // Add overlay to unitLayer
        unitLayer.getChildren().add(overlay);

        // Track current hovered tile
        currentHoveredTileX = tileX;
        currentHoveredTileY = tileY;
        currentHoveredOverlay = overlay;
    }

    // Clears the hover highlight from the currently hovered tile.
    private void clearHoverHighlight() {
        if (currentHoveredOverlay != null) {
            unitLayer.getChildren().remove(currentHoveredOverlay);
            currentHoveredOverlay = null;
        }

        currentHoveredTileX = -1;
        currentHoveredTileY = -1;
    }

    public GridPane getGrid() {
        return grid;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.GridPosition center, double radius,
            double duration) {
        javafx.application.Platform.runLater(() -> {
            double cx = center.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = center.getY() * TILE_SIZE + (TILE_SIZE / 2.0);
            double rPixels = radius * TILE_SIZE;
            javafx.scene.shape.Circle aoe = new javafx.scene.shape.Circle(cx, cy, rPixels);
            aoe.setFill(isPlayerSource ? javafx.scene.paint.Color.color(0.2, 0.6, 1.0, 0.18)
                    : javafx.scene.paint.Color.color(1.0, 0.3, 0.2, 0.18));
            aoe.setStroke(javafx.scene.paint.Color.color(1, 1, 1, 0.6));
            aoe.setStrokeWidth(1.2);
            unitLayer.getChildren().add(aoe);
            activeSpellVisuals.add(new ActiveSpellVisual(aoe, duration));
        });
    }

    private void updateSpellEffects(double deltaTime) {
        java.util.Iterator<ActiveSpellVisual> it = activeSpellVisuals.iterator();
        while (it.hasNext()) {
            ActiveSpellVisual visual = it.next();
            visual.timeRemaining -= deltaTime;
            if (visual.timeRemaining <= 0) {
                unitLayer.getChildren().remove(visual.node);
                it.remove();
            }
        }
    }

    public void showComboEffect(com.kuroyale.model.enums.ComboType combo,
            java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
        if (affectedUnits == null || affectedUnits.isEmpty())
            return;

        // Visual duration
        double duration = 2.0;

        for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
            com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
            if (pos == null)
                continue;

            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            // Draw a gold star or ring
            javafx.scene.shape.Circle ring = new javafx.scene.shape.Circle(cx, cy, TILE_SIZE * 0.8);
            ring.setFill(null);
            ring.setStroke(Color.GOLD);
            ring.setStrokeWidth(3.0);
            ring.setEffect(new javafx.scene.effect.Glow(0.8));

            unitLayer.getChildren().add(ring);
            activeSpellVisuals.add(new ActiveSpellVisual(ring, duration));

            // Add a scaling animation for pop
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                    javafx.util.Duration.seconds(0.5), ring);
            st.setFromX(0.5);
            st.setFromY(0.5);
            st.setToX(1.2);
            st.setToY(1.2);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        }
    }

    public void updateComboCount(int count) {
        if (comboLabel != null) {
            javafx.application.Platform.runLater(() -> {
                comboLabel.setText(String.valueOf(count));
            });
        }
    }

    private void updateScoreContainer(javafx.scene.layout.HBox container, int score, boolean isOpponent) {
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
}
