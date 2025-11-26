package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the active game board during a match.
 * Information Expert: Knows the state of the board (cells, valid positions,
 * occupancy).
 * Creator: Creates and manages GridCell instances.
 * High Cohesion: Focuses solely on map geometry and state.
 */
public class Arena {
    public static final int WIDTH = 18;
    public static final int HEIGHT = 32;

    private final GridCell[][] grid;
    private final ArenaLayout layout;

    public Arena(ArenaLayout layout) {
        this.layout = layout;
        this.grid = new GridCell[WIDTH][HEIGHT];
        initializeGrid();
    }

    /**
     * Initializes the grid with GridCell objects.
     * Sets up terrain (grass, water, bridges) based on layout.
     * Places towers and mirrors user's layout for computer side.
     */
    private void initializeGrid() {
        // Initialize all cells as grass by default
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = new GridCell(x, y, TileType.GRASS);

                // Simple river logic (middle of the map)
                if (y == HEIGHT / 2 || y == (HEIGHT / 2) - 1) {
                    grid[x][y].setTileType(TileType.WATER);
                }
            }
        }

        // Apply bridges from layout
        if (layout != null && layout.getBridgePositions() != null) {
            for (GridPosition p : layout.getBridgePositions()) {
                if (isValidPosition(p.getX(), p.getY())) {
                    grid[p.getX()][p.getY()].setTileType(TileType.BRIDGE);
                }
            }
        }

        // Apply user's Princess towers
        if (layout != null && layout.getPrincessTowerPositions() != null) {
            for (GridPosition p : layout.getPrincessTowerPositions()) {
                // Place 3x3 tower for user
                for (int dx = 0; dx < 3; dx++) {
                    for (int dy = 0; dy < 3; dy++) {
                        if (isValidPosition(p.getX() + dx, p.getY() + dy)) {
                            grid[p.getX() + dx][p.getY() + dy].setTileType(TileType.PRINCESS_TOWER_USER);
                        }
                    }
                }

                // Mirror for computer side (3x3)
                // Mirroring logic: The top-left of the mirrored tower should be:
                // x' = x
                // y' = HEIGHT - 3 - y
                int mirroredY = HEIGHT - 3 - p.getY();
                for (int dx = 0; dx < 3; dx++) {
                    for (int dy = 0; dy < 3; dy++) {
                        if (isValidPosition(p.getX() + dx, mirroredY + dy)) {
                            grid[p.getX() + dx][mirroredY + dy].setTileType(TileType.PRINCESS_TOWER_COMPUTER);
                        }
                    }
                }
            }
        }

        // Apply user's King tower
        if (layout != null && layout.getKingTowerPosition() != null) {
            GridPosition p = layout.getKingTowerPosition();
            // Place 4x4 tower for user
            for (int dx = 0; dx < 4; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    if (isValidPosition(p.getX() + dx, p.getY() + dy)) {
                        grid[p.getX() + dx][p.getY() + dy].setTileType(TileType.KING_TOWER_USER);
                    }
                }
            }

            // Mirror for computer side (4x4)
            int mirroredY = HEIGHT - 4 - p.getY();
            for (int dx = 0; dx < 4; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    if (isValidPosition(p.getX() + dx, mirroredY + dy)) {
                        grid[p.getX() + dx][mirroredY + dy].setTileType(TileType.KING_TOWER_COMPUTER);
                    }
                }
            }
        }
    }

    /**
     * Checks if the given coordinates are within valid arena bounds.
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    /**
     * Checks if the given position is within valid arena bounds.
     */
    public boolean isValidPosition(GridPosition position) {
        if (position == null) {
            return false;
        }
        return isValidPosition(position.getX(), position.getY());
    }

    /**
     * Gets the GridCell at the specified position.
     * 
     * @param position The grid position
     * @return The GridCell, or null if position is invalid
     */
    public GridCell getCell(GridPosition position) {
        if (!isValidPosition(position)) {
            return null;
        }
        return grid[position.getX()][position.getY()];
    }

    /**
     * Gets the GridCell at the specified coordinates (convenience method).
     * 
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @return The GridCell, or null if coordinates are invalid
     */
    public GridCell getCell(int x, int y) {
        if (!isValidPosition(x, y)) {
            return null;
        }
        return grid[x][y];
    }

    /**
     * Checks if a unit can be placed at the specified position.
     * 
     * @param position The position to check
     * @return true if placement is valid
     */
    public boolean canPlaceUnit(GridPosition position) {
        GridCell cell = getCell(position);
        return cell != null && cell.canPlaceUnit();
    }

    /**
     * Places a unit at the specified position.
     * 
     * @param position The position to place the unit
     * @param unit     The unit to place
     * @throws IllegalArgumentException if position is invalid
     * @throws IllegalStateException    if placement is not allowed
     */
    public void placeUnit(GridPosition position, Object unit) {
        GridCell cell = getCell(position);
        if (cell == null) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
        cell.setOccupant(unit);
    }

    /**
     * Removes the unit at the specified position.
     * 
     * @param position The position to clear
     */
    public void removeUnit(GridPosition position) {
        GridCell cell = getCell(position);
        if (cell != null) {
            cell.clearOccupant();
        }
    }

    /**
     * Gets all adjacent positions to the specified position (4-directional: up,
     * down, left, right).
     * 
     * @param position The center position
     * @return List of valid adjacent positions
     */
    public List<GridPosition> getAdjacentPositions(GridPosition position) {
        List<GridPosition> adjacent = new ArrayList<>();
        if (position == null) {
            return adjacent;
        }

        int x = position.getX();
        int y = position.getY();

        // Up, Down, Left, Right (4-directional)
        int[][] directions = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

        for (int[] dir : directions) {
            GridPosition neighbor = GridPosition.tryCreate(x + dir[0], y + dir[1]);
            if (neighbor != null) {
                adjacent.add(neighbor);
            }
        }

        return adjacent;
    }

    /**
     * Gets all adjacent positions including diagonals (8-directional).
     * 
     * @param position The center position
     * @return List of valid adjacent positions (including diagonals)
     */
    public List<GridPosition> getAdjacentPositionsWithDiagonals(GridPosition position) {
        List<GridPosition> adjacent = new ArrayList<>();
        if (position == null) {
            return adjacent;
        }

        int x = position.getX();
        int y = position.getY();

        // All 8 directions
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0)
                    continue; // Skip center
                GridPosition neighbor = GridPosition.tryCreate(x + dx, y + dy);
                if (neighbor != null) {
                    adjacent.add(neighbor);
                }
            }
        }

        return adjacent;
    }

    /**
     * Gets all cells in the grid as a flat list.
     * 
     * @return List of all GridCells
     */
    public List<GridCell> getAllCells() {
        List<GridCell> cells = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                cells.add(grid[x][y]);
            }
        }
        return cells;
    }

    /**
     * Gets all cells that are currently occupied by units.
     * 
     * @return List of occupied GridCells
     */
    public List<GridCell> getOccupiedCells() {
        return getAllCells().stream()
                .filter(GridCell::isOccupied)
                .collect(Collectors.toList());
    }

    /**
     * Gets all cells within a rectangular region.
     * 
     * @param topLeft     Top-left corner of the region
     * @param bottomRight Bottom-right corner of the region
     * @return List of GridCells in the region
     */
    public List<GridCell> getCellsInRegion(GridPosition topLeft, GridPosition bottomRight) {
        List<GridCell> cells = new ArrayList<>();
        if (topLeft == null || bottomRight == null) {
            return cells;
        }

        int minX = Math.max(0, Math.min(topLeft.getX(), bottomRight.getX()));
        int maxX = Math.min(WIDTH - 1, Math.max(topLeft.getX(), bottomRight.getX()));
        int minY = Math.max(0, Math.min(topLeft.getY(), bottomRight.getY()));
        int maxY = Math.min(HEIGHT - 1, Math.max(topLeft.getY(), bottomRight.getY()));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                cells.add(grid[x][y]);
            }
        }

        return cells;
    }

    /**
     * Gets all cells within a certain distance from a center position.
     * 
     * @param center The center position
     * @param radius The maximum distance (Manhattan distance)
     * @return List of GridCells within the radius
     */
    public List<GridCell> getCellsInRadius(GridPosition center, int radius) {
        List<GridCell> cells = new ArrayList<>();
        if (center == null || radius < 0) {
            return cells;
        }

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                GridPosition pos = new GridPosition(x, y);
                if (center.getDistanceTo(pos) <= radius) {
                    cells.add(grid[x][y]);
                }
            }
        }

        return cells;
    }

    /**
     * Gets the arena layout.
     */
    public ArenaLayout getLayout() {
        return layout;
    }

    // ========== Backward Compatibility Methods ==========

    /**
     * Gets the tile at the specified position (backward compatibility).
     * 
     * @deprecated Use getCell() instead. This method is kept for backward
     *             compatibility.
     */
    @Deprecated
    public Tile getTile(int x, int y) {
        GridCell cell = getCell(x, y);
        if (cell == null) {
            return null;
        }
        return new Tile(x, y, cell.getTileType());
    }
}
