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
    private final java.util.Map<Long, javafx.scene.Node> cellIndex = new java.util.HashMap<>();

    // Track hovered tile for highlighting
    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

    private static final int TILE_SIZE = com.kuroyale.util.GameConstants.TILE_SIZE;

    /**
     * Constructor for PvP mode.
     */
    public BattleArenaView(com.kuroyale.model.logic.PvPGameState pvpGameState) {
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
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

        StackPane centerContainer = new StackPane(arenaPane);
        centerContainer.setAlignment(javafx.geometry.Pos.CENTER);
        centerContainer.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
        this.setCenter(centerContainer);

        // Initialize renderers - create a minimal proxy for rendering
        this.troopRenderer = new TroopRenderer(unitLayer);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode);
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);

        com.kuroyale.event.GameEventBus.getInstance().subscribe(this);

        renderArenaPvP(arena);
    }

    public BattleArenaView(GameState gameState) {
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.gameState = gameState;
        this.pvpGameState = null;

        // Sidebar is now defined in battle.fxml - no longer created here

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
        this.troopRenderer = new TroopRenderer(unitLayer);
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
                // Use ViewUtils for consistent coloring
                rect.setFill(com.kuroyale.view.battle.ViewUtils.getTileColor(type, x, y));

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
                // Use ViewUtils for consistent coloring
                rect.setFill(com.kuroyale.view.battle.ViewUtils.getTileColor(type, x, y));

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

    public void update(double deltaTime) {
        // === OBSERVER PATTERN: Only update UI when values change ===

        // === OBSERVER PATTERN: Only update UI when values change ===

        // Delegate to Renderers

        towerRenderer.cleanupDestroyedTowers(gameState.getArena());
        towerRenderer.updateHealthBars(gameState.getArena());
        troopRenderer.render(gameState);
        buildingRenderer.render(gameState);

        // Render projectiles from state
        projectileRenderer.render(gameState.getProjectiles(), deltaTime);

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

        // Render projectiles from state
        projectileRenderer.render(pvpGameState.getProjectiles(), deltaTime);

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
        gc.setFill(com.kuroyale.util.GameColors.HIGHLIGHT_VALID);

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
        gc.setFill(com.kuroyale.util.GameColors.HIGHLIGHT_VALID_P2);

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
        overlay.setFill(com.kuroyale.util.GameColors.HOVER_FILL);
        overlay.setStroke(com.kuroyale.util.GameColors.HOVER_STROKE);
        overlay.setStrokeWidth(2.0);
        overlay.setStrokeType(javafx.scene.shape.StrokeType.INSIDE); // Stroke inside to avoid gaps

        // Position overlay
        overlay.setLayoutX(cellX + TILE_SIZE / 2.0 - TILE_SIZE / 2.0);
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
            aoe.setFill(isPlayerSource ? com.kuroyale.util.GameColors.AOE_PLAYER
                    : com.kuroyale.util.GameColors.AOE_ENEMY);
            aoe.setStroke(com.kuroyale.util.GameColors.AOE_STROKE);
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

        // Visual duration
        double duration = 2.0;

        switch (combo) {
            case TANK_SUPPORT:
                // Gold glow around ranged units
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addVisualEffect(unit, duration, node -> {
                            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                            glow.setColor(Color.GOLD);
                            glow.setRadius(20);
                            glow.setSpread(0.5);
                            node.setEffect(glow);
                        });
                    }
                }
                break;

            case SPELL_SYNERGY:
                // Sparkle effect at center/King Tower (since no units)
                javafx.application.Platform.runLater(() -> {
                    double cx = com.kuroyale.model.entities.Arena.WIDTH * TILE_SIZE / 2.0;
                    double cy = com.kuroyale.model.entities.Arena.HEIGHT * TILE_SIZE / 2.0; // Center of arena

                    for (int i = 0; i < 10; i++) {
                        javafx.scene.shape.Circle sparkle = new javafx.scene.shape.Circle(cx, cy, 5, Color.CYAN);
                        sparkle.setEffect(new javafx.scene.effect.Glow(1.0));
                        unitLayer.getChildren().add(sparkle);

                        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                                javafx.util.Duration.seconds(1.0), sparkle);
                        tt.setByX((Math.random() - 0.5) * 100);
                        tt.setByY((Math.random() - 0.5) * 100);

                        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                                javafx.util.Duration.seconds(1.0), sparkle);
                        ft.setToValue(0);

                        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
                        pt.setOnFinished(e -> unitLayer.getChildren().remove(sparkle));
                        pt.play();
                    }
                });
                break;

            case SWARM_ATTACK:
                // Speed lines on units (Running effect)
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, y + 15, x - 25, y + 15); // Slightly
                                                                                                                   // lower
                                                                                                                   // and
                                                                                                                   // behind
                            line.setStroke(Color.WHITE);
                            line.setStrokeWidth(1.5);
                            line.getStrokeDashArray().addAll(4d, 6d);
                            return line;
                        });
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, y - 15, x - 25, y - 15); // Slightly
                                                                                                                   // higher
                                                                                                                   // and
                                                                                                                   // behind
                            line.setStroke(Color.WHITE);
                            line.setStrokeWidth(1.5);
                            line.getStrokeDashArray().addAll(4d, 6d);
                            return line;
                        });
                    }
                }
                break;

            case BUILDING_DEFENSE:
                // Shield icon on buildings
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.SVGPath shield = new javafx.scene.shape.SVGPath();
                            shield.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
                            shield.setFill(Color.LIGHTBLUE); // Use a visible color
                            shield.setStroke(Color.BLUE);
                            shield.setScaleX(1.5);
                            shield.setScaleY(1.5);
                            shield.setLayoutX(x - 12); // Center somewhat (path is ~24x24)
                            shield.setLayoutY(y - 12);
                            return shield;
                        });
                    }
                }
                break;

            case AIR_ASSAULT:
                // Lightning effect around flying units
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Polyline lightning = new javafx.scene.shape.Polyline();
                            lightning.getPoints().addAll(
                                    x, y - 20.0,
                                    x + 5.0, y - 10.0,
                                    x - 5.0, y,
                                    x + 5.0, y + 10.0,
                                    x, y + 20.0);
                            lightning.setStroke(Color.YELLOW);
                            lightning.setStrokeWidth(2);
                            lightning.setEffect(new javafx.scene.effect.Glow(0.8));
                            return lightning;
                        });
                    }
                }
                break;

            case ROYAL_COMBO:
                // Crown icon above Knight health bar
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                                    new javafx.scene.image.Image(getClass().getResourceAsStream("/images/crown.png")));
                            crown.setFitWidth(24);
                            crown.setFitHeight(24);
                            crown.setLayoutX(x - 12);
                            crown.setLayoutY(y - 30); // Above unit
                            return crown;
                        });
                    }
                }
                break;

            case SIEGE_MODE:
                // Range indicator circle expands
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Circle rangeCircle = new javafx.scene.shape.Circle(x, y, TILE_SIZE * 2);
                            rangeCircle.setFill(null);
                            rangeCircle.setStroke(Color.ORANGE);
                            rangeCircle.setStrokeWidth(2);
                            rangeCircle.getStrokeDashArray().addAll(10d, 10d);

                            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                                    javafx.util.Duration.seconds(1.0), rangeCircle);
                            st.setFromX(1.0);
                            st.setFromY(1.0);
                            st.setToX(1.5); // Expand to show increased range
                            st.setToY(1.5);
                            st.setCycleCount(2);
                            st.setAutoReverse(true);
                            st.play();

                            return rangeCircle;
                        });
                    }
                }
                break;

            case RUSH_ATTACK:
                // Dust trail behind Hog Rider
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        // Create a recurring dust effect
                        final javafx.animation.Timeline dust = new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> {
                                    com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
                                    if (pos == null)
                                        return;
                                    double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                                    double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                                    javafx.scene.shape.Circle dustParticle = new javafx.scene.shape.Circle(cx, cy, 5,
                                            Color.GRAY);
                                    dustParticle.setOpacity(0.6);
                                    unitLayer.getChildren().add(dustParticle);

                                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                                            javafx.util.Duration.seconds(0.5), dustParticle);
                                    ft.setToValue(0);
                                    ft.setOnFinished(ev -> unitLayer.getChildren().remove(dustParticle));
                                    ft.play();
                                }));
                        dust.setCycleCount(10); // Run for 2 seconds
                        dust.play();
                    }
                }
                break;

            default:
                // Default generic effect
                if (affectedUnits != null && !affectedUnits.isEmpty()) {
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
                break;
        }
    }

    // Helper to apply effects that need to follow units or just stay for duration
    private void addVisualEffect(com.kuroyale.model.entities.ICombatant unit, double duration,
            java.util.function.Consumer<javafx.scene.Node> effectApplier) {
        // This is a bit tricky since we don't have direct access to unit nodes easily
        // here without querying the renderer.
        // But for simple effects like glow, we can try to find the node.
        if (unit instanceof com.kuroyale.model.entities.Troop) {
            // We can't easily get the node from here without exposing it from Renderer.
            // Alternative: generic overlay.
            // For now, let's use the layout coordinates to spawn a visual that stays for a
            // bit.
            // But for "Glow", we need the actual node.
            // Let's rely on a temporary overlay or visual marker for now as true node
            // access is complex.

            // Simpler approach: Spawn a "Highlight" circle that follows?
            // Or just static at spawn point for MVP visuals.
            // Let's try to get coordinates and spawn a static visual at that location.

            com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
            if (pos != null) {
                double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                javafx.scene.shape.Circle highlight = new javafx.scene.shape.Circle(cx, cy, TILE_SIZE / 2);
                highlight.setFill(null);
                highlight.setStroke(Color.GOLD);
                highlight.setStrokeWidth(3);

                // Apply the custom effect to this highlight
                effectApplier.accept(highlight);

                unitLayer.getChildren().add(highlight);
                activeSpellVisuals.add(new ActiveSpellVisual(highlight, duration));
            }
        }
    }

    private void addAttachedVisual(com.kuroyale.model.entities.ICombatant unit, double duration,
            AttachedVisualCreator creator) {
        com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            javafx.scene.Node visual = creator.create(unit, cx, cy);
            unitLayer.getChildren().add(visual);
            activeSpellVisuals.add(new ActiveSpellVisual(visual, duration));
        }
    }

    @FunctionalInterface
    private interface AttachedVisualCreator {
        javafx.scene.Node create(com.kuroyale.model.entities.ICombatant unit, double x, double y);
    }

    public javafx.scene.Node getTowerNode(com.kuroyale.model.entities.Tower tower) {
        if (tower == null || tower.getPosition() == null || towerRenderer == null) {
            return null;
        }
        return towerRenderer.getTowerVisual(tower.getPosition());
    }
}
