package com.kuroyale.view.battle;

import com.kuroyale.model.Arena;
import com.kuroyale.model.GameState;
import com.kuroyale.model.GridCell;
import com.kuroyale.model.TileType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import javafx.scene.shape.Rectangle;

// Renders the battle arena and placed units.
public class BattleArenaView extends javafx.scene.layout.BorderPane {
    private final GridPane grid;
    private final Pane unitLayer;
    private final Pane arenaPane;
    private final GameState gameState;
    private final javafx.scene.control.Label timerLabel;
    private final javafx.scene.control.Label scoreLabel;
    private final java.util.Map<Long, javafx.scene.Node> cellIndex = new java.util.HashMap<>();

    // Track hovered tile for highlighting
    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

    private static final int TILE_SIZE = 18; // Matches ArenaDesignController

    public BattleArenaView(GameState gameState) {
        this.gameState = gameState;

        // Right Sidebar (Timer and Score)
        javafx.scene.layout.VBox sidebar = new javafx.scene.layout.VBox(20);
        sidebar.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        sidebar.setPadding(new javafx.geometry.Insets(20));
        sidebar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        sidebar.setPrefWidth(200);

        javafx.scene.control.Label timerTitle = new javafx.scene.control.Label("TIME");
        timerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        timerLabel = new javafx.scene.control.Label("03:00");
        timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        javafx.scene.control.Label scoreTitle = new javafx.scene.control.Label("SCORE");
        scoreTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        scoreLabel = new javafx.scene.control.Label("0 - 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        javafx.scene.control.Label opponentLabel = new javafx.scene.control.Label("OPPONENT");
        opponentLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 18px; -fx-font-weight: bold;");

        sidebar.getChildren().addAll(timerTitle, timerLabel, new javafx.scene.control.Separator(), scoreTitle,
                scoreLabel, new javafx.scene.control.Separator(), opponentLabel);
        this.setRight(sidebar);

        // Center Arena
        this.grid = new GridPane();
        this.grid.setHgap(0);
        this.grid.setVgap(0);
        this.unitLayer = new Pane();

        // Make unit layer transparent to mouse events so clicks go to grid for
        // placement
        unitLayer.setMouseTransparent(true);

        this.arenaPane = new Pane(grid, unitLayer);
        // Set preferred size to match grid size
        arenaPane.setPrefSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        // Bind unitLayer position to grid position to ensure perfect alignment
        unitLayer.layoutXProperty().bind(grid.layoutXProperty());
        unitLayer.layoutYProperty().bind(grid.layoutYProperty());

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
        centerContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER); // Align to top as requested
        centerContainer.setPadding(new javafx.geometry.Insets(20, 0, 0, 0)); // Add some top padding

        this.setCenter(centerContainer);

        // Initialize renderers
        this.troopRenderer = new TroopRenderer(unitLayer, this::getGridCell);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, this::getGridCell, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode); // requires CellIndexer or
                                                                                      // compatible lambda
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);

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

    private final java.util.Map<com.kuroyale.model.GameState.SpellEffect, javafx.scene.Node> activeSpellVisuals = new java.util.HashMap<>();

    // Extracted renderers
    private TroopRenderer troopRenderer;
    private ProjectileRenderer projectileRenderer;
    private TowerRenderer towerRenderer;
    private BuildingRenderer buildingRenderer;

    // === PERFORMANCE OPTIMIZATION FIELDS ===

    // Last-value tracking for observer pattern (only update UI when changed)
    private int lastDisplayedSeconds = -1;
    private int lastPlayerScore = -1;
    private int lastBotScore = -1;
    private boolean lastDoubleElixir = false;

    public void update() {
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
        if (playerScore != lastPlayerScore || botScore != lastBotScore) {
            lastPlayerScore = playerScore;
            lastBotScore = botScore;
            scoreLabel.setText(String.format("%d - %d", playerScore, botScore));
        }

        // Delegate to Renderers
        towerRenderer.cleanupDestroyedTowers(gameState.getArena());
        towerRenderer.updateHealthBars(gameState.getArena());
        troopRenderer.render(gameState);
        buildingRenderer.render(gameState);

        // Render projectiles for all combatants
        java.util.List<com.kuroyale.model.ICombatant> combatants = new java.util.ArrayList<>();
        combatants.addAll(gameState.getArena().getAllTowers());
        combatants.addAll(gameState.getActiveBuildings());
        projectileRenderer.render(combatants);

        // Spell Effects
        renderSpellEffects();
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

        // Debug output (disabled)
        // System.out.printf("Mouse(%.1f,%.1f) GridOffset(%.1f,%.1f) GridPos(%.1f,%.1f)
        // Tile(%d,%d)%n",
        // mouseX, mouseY, gridOffsetX, gridOffsetY, gridX, gridY, tileX, tileY);

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

    public void highlightValidCells(boolean show, boolean isSpell) {
        Arena arena = gameState.getArena();
        for (javafx.scene.Node node : grid.getChildren()) {
            Rectangle rect = null;

            if (node instanceof Rectangle) {
                rect = (Rectangle) node;
            } else if (node instanceof StackPane) {
                // Towers are StackPanes - skip them
                continue;
            }

            if (rect != null) {
                Integer x = GridPane.getColumnIndex(node);
                Integer y = GridPane.getRowIndex(node);

                // Handle null indices
                if (x == null || y == null)
                    continue;

                if (show) {
                    boolean shouldHighlight = false;

                    if (isSpell) {
                        // Spells can be placed anywhere
                        shouldHighlight = true;
                    } else {
                        // Standard units: Player side (bottom half) AND walkable (Grass)
                        boolean isPlayerSide = y >= Arena.HEIGHT / 2;
                        GridCell cell = arena.getCell(x, y);
                        boolean isWalkable = cell.getTileType() == TileType.GRASS;

                        if (isPlayerSide && isWalkable) {
                            shouldHighlight = true;
                        }
                    }

                    if (shouldHighlight) {
                        // Use glow effect for visibility on colored tiles
                        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                        glow.setColor(Color.YELLOW);
                        glow.setRadius(8);
                        glow.setSpread(0.6);
                        rect.setEffect(glow);
                    } else {
                        rect.setEffect(null);
                    }
                } else {
                    // Reset - remove effects
                    rect.setEffect(null);
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

    private void renderSpellEffects() {
        java.util.List<com.kuroyale.model.GameState.SpellEffect> effects = gameState.getActiveSpellEffects();
        java.util.Set<com.kuroyale.model.GameState.SpellEffect> currentEffects = new java.util.HashSet<>();
        if (effects != null) {
            currentEffects.addAll(effects);
        }

        // Cleanup expired spell visuals
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.GameState.SpellEffect, javafx.scene.Node>> spellIt = activeSpellVisuals
                .entrySet().iterator();
        while (spellIt.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.GameState.SpellEffect, javafx.scene.Node> entry = spellIt.next();
            if (!currentEffects.contains(entry.getKey())) {
                // Spell effect expired, remove visual
                unitLayer.getChildren().remove(entry.getValue());
                spellIt.remove();
            }
        }

        // Create visuals for new spell effects
        if (effects == null || effects.isEmpty())
            return;
        for (com.kuroyale.model.GameState.SpellEffect se : effects) {
            // Skip if we already have a visual for this effect
            if (activeSpellVisuals.containsKey(se))
                continue;

            com.kuroyale.model.GridPosition c = se.center;
            if (c == null)
                continue;
            javafx.scene.Node centerCell = getGridCell(c.getX(), c.getY());
            if (centerCell == null)
                continue;
            javafx.geometry.Bounds cb = centerCell.getBoundsInParent();
            double cx = cb.getMinX() + cb.getWidth() / 2.0;
            double cy = cb.getMinY() + cb.getHeight() / 2.0;
            double rPixels = se.radiusTiles * TILE_SIZE;
            javafx.scene.shape.Circle aoe = new javafx.scene.shape.Circle(cx, cy, rPixels);
            aoe.setFill(se.isPlayerSide ? javafx.scene.paint.Color.color(0.2, 0.6, 1.0, 0.18)
                    : javafx.scene.paint.Color.color(1.0, 0.3, 0.2, 0.18));
            aoe.setStroke(javafx.scene.paint.Color.color(1, 1, 1, 0.6));
            aoe.setStrokeWidth(1.2);
            unitLayer.getChildren().add(aoe);
            activeSpellVisuals.put(se, aoe);
        }
    }
}