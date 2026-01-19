package com.kuroyale.view.battle;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.GridCell;
import com.kuroyale.model.enums.TileType;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

/**
 * Handles user input interactions for the battle arena.
 * Responsibilities:
 * - Mouse click detection and coordinate conversion
 * - Tile hover highlighting
 * - Valid placement area highlighting
 */
public class ArenaInputHandler {
    private static final int TILE_SIZE = com.kuroyale.util.config.GameConstants.TILE_SIZE;

    private final Pane arenaPane;
    private final GridPane grid;
    private final Pane unitLayer;
    private final Canvas highlightLayer;
    private final java.util.Map<Long, javafx.scene.Node> cellIndex;

    // Hover state tracking
    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

    private java.util.function.BiConsumer<Integer, Integer> onGridClick;

    public ArenaInputHandler(Pane arenaPane, GridPane grid, Pane unitLayer, Canvas highlightLayer,
            java.util.Map<Long, javafx.scene.Node> cellIndex) {
        this.arenaPane = arenaPane;
        this.grid = grid;
        this.unitLayer = unitLayer;
        this.highlightLayer = highlightLayer;
        this.cellIndex = cellIndex;

        setupInteractions();
    }

    public void setOnGridClicked(java.util.function.BiConsumer<Integer, Integer> handler) {
        this.onGridClick = handler;
    }

    private void setupInteractions() {
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
    }

    /**
     * Converts mouse coordinates to tile coordinates.
     * 
     * @return int array [x, y] or null if out of bounds
     */
    private int[] calculateTileCoordinates(double mouseX, double mouseY) {
        double gridOffsetX = grid.getLayoutX();
        double gridOffsetY = grid.getLayoutY();
        double gridX = mouseX - gridOffsetX;
        double gridY = mouseY - gridOffsetY;
        int tileX = (int) Math.floor(gridX / TILE_SIZE);
        int tileY = (int) Math.floor(gridY / TILE_SIZE);
        if (tileX >= 0 && tileX < Arena.WIDTH && tileY >= 0 && tileY < Arena.HEIGHT) {
            return new int[] { tileX, tileY };
        }
        return null;
    }

    /**
     * Highlights valid placement cells for the player.
     */
    public void highlightValidCells(boolean show, boolean isSpell, Arena arena) {
        GraphicsContext gc = highlightLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());

        if (!show)
            return;

        gc.setFill(com.kuroyale.util.config.GameColors.HIGHLIGHT_VALID);

        if (isSpell) {
            gc.fillRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());
        } else {
            if (arena == null)
                return;
            for (int x = 0; x < Arena.WIDTH; x++) {
                for (int y = Arena.HEIGHT / 2; y < Arena.HEIGHT; y++) {
                    GridCell cell = arena.getCell(x, y);
                    if (cell.getTileType() == TileType.GRASS) {
                        gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    /**
     * Highlights valid placement cells for player 2 (PvP mode).
     */
    public void highlightPlayer2ValidCells(boolean show, boolean isSpell, Arena arena) {
        GraphicsContext gc = highlightLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());

        if (!show)
            return;

        gc.setFill(com.kuroyale.util.config.GameColors.HIGHLIGHT_VALID_P2);

        if (isSpell) {
            gc.fillRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());
        } else {
            if (arena == null)
                return;
            for (int x = 0; x < Arena.WIDTH; x++) {
                for (int y = 0; y < Arena.HEIGHT / 2; y++) {
                    GridCell cell = arena.getCell(x, y);
                    if (cell.getTileType() == TileType.GRASS) {
                        gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    private void highlightHoveredTile(int tileX, int tileY) {
        if (currentHoveredTileX == tileX && currentHoveredTileY == tileY) {
            return;
        }

        clearHoverHighlight();

        javafx.scene.Node node = getGridCell(tileX, tileY);
        if (node == null) {
            return;
        }

        if (node instanceof javafx.scene.layout.StackPane) {
            return;
        }

        javafx.geometry.Point2D topLeft = node.localToParent(0, 0);

        double cellX = topLeft.getX();
        double cellY = topLeft.getY();

        Rectangle overlay = new Rectangle(TILE_SIZE, TILE_SIZE);

        overlay.setFill(com.kuroyale.util.config.GameColors.HOVER_FILL);
        overlay.setStroke(com.kuroyale.util.config.GameColors.HOVER_STROKE);
        overlay.setStrokeWidth(2.0);
        overlay.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);

        overlay.setLayoutX(cellX + TILE_SIZE / 2.0 - TILE_SIZE / 2.0);
        overlay.setLayoutY(cellY);

        overlay.setMouseTransparent(true);

        unitLayer.getChildren().add(overlay);

        currentHoveredTileX = tileX;
        currentHoveredTileY = tileY;
        currentHoveredOverlay = overlay;
    }

    private void clearHoverHighlight() {
        if (currentHoveredOverlay != null) {
            unitLayer.getChildren().remove(currentHoveredOverlay);
            currentHoveredOverlay = null;
        }

        currentHoveredTileX = -1;
        currentHoveredTileY = -1;
    }

    private javafx.scene.Node getGridCell(int x, int y) {
        return cellIndex.getOrDefault(key(x, y), null);
    }

    private long key(int x, int y) {
        return (((long) x) << 32) | (y & 0xFFFFFFFFL);
    }
}
