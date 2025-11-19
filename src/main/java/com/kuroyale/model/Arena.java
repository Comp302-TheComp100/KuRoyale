package com.kuroyale.model;

import java.awt.Point;

/**
 * Represents the active game board during a match.
 * Information Expert: Knows the state of the board (tiles, valid positions).
 * High Cohesion: Focuses solely on map geometry and state.
 */
public class Arena {
    public static final int WIDTH = 18;
    public static final int HEIGHT = 32;

    private final Tile[][] grid;
    private final ArenaLayout layout;

    public Arena(ArenaLayout layout) {
        this.layout = layout;
        this.grid = new Tile[WIDTH][HEIGHT];
        initializeGrid();
    }

    private void initializeGrid() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                // Default to GRASS
                grid[x][y] = new Tile(x, y, TileType.GRASS);

                // Simple river logic (middle of the map)
                if (y == HEIGHT / 2 || y == (HEIGHT / 2) - 1) {
                    grid[x][y].setType(TileType.WATER);
                }
            }
        }

        // Apply bridges from layout
        if (layout != null && layout.getBridgePositions() != null) {
            for (Point p : layout.getBridgePositions()) {
                if (isValidPosition(p.x, p.y)) {
                    grid[p.x][p.y].setType(TileType.BRIDGE);
                    // Bridges usually span the river, so we might need to set adjacent tiles too
                    // For now, we assume the point represents the center or start of the bridge
                }
            }
        }
    }

    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public Tile getTile(int x, int y) {
        if (isValidPosition(x, y)) {
            return grid[x][y];
        }
        return null;
    }

    public ArenaLayout getLayout() {
        return layout;
    }
}
