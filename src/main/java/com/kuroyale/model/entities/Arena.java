package com.kuroyale.model.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.kuroyale.model.enums.*;

/*Represents the active game board during a match.
 * Information Expert: Knows the state of the board (cells, valid positions, occupancy).
 * Creator: Creates and manages GridCell instances.
 * High Cohesion: Focuses solely on map geometry and state. */
public class Arena {
    public static final int WIDTH = 18;
    public static final int HEIGHT = 32;

    private final GridCell[][] grid;
    private final ArenaLayout layout;
    private final java.util.Map<GridPosition, Tower> towerMap;
    private final com.kuroyale.model.logic.SpatialGrid spatialGrid;

    public Arena(ArenaLayout layout) {
        this.layout = layout;
        this.grid = new GridCell[WIDTH][HEIGHT];
        this.towerMap = new java.util.HashMap<>();
        this.spatialGrid = new com.kuroyale.model.logic.SpatialGrid(WIDTH, HEIGHT);
        initializeGrid();
    }

    /*
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

        // Apply bridges from layout with Y-axis symmetry
        // Both players should see symmetric bridges from their perspective
        if (layout != null && layout.getBridgePositions() != null) {
            for (GridPosition p : layout.getBridgePositions()) {
                // Place bridge at original position
                if (isValidPosition(p.getX(), p.getY())) {
                    grid[p.getX()][p.getY()].setTileType(TileType.BRIDGE);
                }
                // Mirror bridge on Y-axis: y' = HEIGHT - 1 - y
                int mirroredY = HEIGHT - 1 - p.getY();
                if (isValidPosition(p.getX(), mirroredY)) {
                    grid[p.getX()][mirroredY].setTileType(TileType.BRIDGE);
                }
            }
        }

        // Apply user's Princess towers
        if (layout != null && layout.getPrincessTowerPositions() != null) {
            for (GridPosition p : layout.getPrincessTowerPositions()) {
                // Place 3x3 tower for user
                Tower tower = new Tower(Tower.TowerType.PRINCESS, true);
                tower.setPosition(p); // Set position for ICombatant
                for (int dx = 0; dx < 3; dx++) {
                    for (int dy = 0; dy < 3; dy++) {
                        if (isValidPosition(p.getX() + dx, p.getY() + dy)) {
                            GridPosition pos = new GridPosition(p.getX() + dx, p.getY() + dy);
                            grid[pos.getX()][pos.getY()].setTileType(TileType.PRINCESS_TOWER_USER);
                            towerMap.put(pos, tower);
                        }
                    }
                }
                spatialGrid.add(tower);
                // Mirroring logic for computer side: The top-left of the mirrored tower should
                // be
                // x' = x and y' = HEIGHT - 3 - y
                int mirroredY = HEIGHT - 3 - p.getY();
                Tower computerTower = new Tower(Tower.TowerType.PRINCESS, false);
                GridPosition computerPos = new GridPosition(p.getX(), mirroredY);
                computerTower.setPosition(computerPos); // Set position for ICombatant

                for (int dx = 0; dx < 3; dx++) {
                    for (int dy = 0; dy < 3; dy++) {
                        if (isValidPosition(p.getX() + dx, mirroredY + dy)) {
                            GridPosition pos = new GridPosition(p.getX() + dx, mirroredY + dy);
                            grid[pos.getX()][pos.getY()].setTileType(TileType.PRINCESS_TOWER_COMPUTER);
                            towerMap.put(pos, computerTower);
                        }
                    }
                }
                spatialGrid.add(computerTower);
            }
        }

        // Apply user's King tower
        if (layout != null && layout.getKingTowerPosition() != null) {
            GridPosition p = layout.getKingTowerPosition();
            // Place 4x4 tower for user
            Tower tower = new Tower(Tower.TowerType.KING, true);
            tower.setPosition(p);

            for (int dx = 0; dx < 4; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    if (isValidPosition(p.getX() + dx, p.getY() + dy)) {
                        GridPosition pos = new GridPosition(p.getX() + dx, p.getY() + dy);
                        grid[pos.getX()][pos.getY()].setTileType(TileType.KING_TOWER_USER);
                        towerMap.put(pos, tower);
                    }
                }
            }
            spatialGrid.add(tower);

            // Mirror for computer side (4x4)
            int mirroredY = HEIGHT - 4 - p.getY();
            Tower computerTower = new Tower(Tower.TowerType.KING, false);
            GridPosition computerPos = new GridPosition(p.getX(), mirroredY);
            computerTower.setPosition(computerPos);

            for (int dx = 0; dx < 4; dx++) {
                for (int dy = 0; dy < 4; dy++) {
                    if (isValidPosition(p.getX() + dx, mirroredY + dy)) {
                        GridPosition pos = new GridPosition(p.getX() + dx, mirroredY + dy);
                        grid[pos.getX()][pos.getY()].setTileType(TileType.KING_TOWER_COMPUTER);
                        towerMap.put(pos, computerTower);
                    }
                }
            }
            spatialGrid.add(computerTower);
        }
    }

    // Checks if the given coordinates are within valid arena bounds.
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    // Checks if the given position is within valid arena bounds.
    public boolean isValidPosition(GridPosition position) {
        if (position == null) {
            return false;
        }
        return isValidPosition(position.getX(), position.getY());
    }

    // Gets the GridCell at the specified position.
    public GridCell getCell(GridPosition position) {
        if (!isValidPosition(position)) {
            return null;
        }
        return grid[position.getX()][position.getY()];
    }

    // Gets the GridCell at the specified coordinates (convenience method).
    public GridCell getCell(int x, int y) {
        if (!isValidPosition(x, y)) {
            return null;
        }
        return grid[x][y];
    }

    // Checks if a unit can be placed at the specified position.
    public boolean canPlaceUnit(GridPosition position) {
        GridCell cell = getCell(position);
        return cell != null && cell.canPlaceUnit();
    }

    // Places a unit at the specified position.
    public void placeUnit(GridPosition position, Object unit) {
        GridCell cell = getCell(position);
        if (cell == null) {
            throw new IllegalArgumentException("Invalid position: " + position);
        }
        cell.setOccupant(unit);
    }

    // Removes the unit at the specified position.
    public void removeUnit(GridPosition position) {
        GridCell cell = getCell(position);
        if (cell != null) {
            cell.clearOccupant();
        }
    }

    // Gets all adjacent positions to the specified position (4-directional:
    // up,down, left, right).
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

    // Gets all adjacent positions including diagonals (8-directional)
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

    // Gets all cells in the grid as a flat list.
    public List<GridCell> getAllCells() {
        List<GridCell> cells = new ArrayList<>();
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                cells.add(grid[x][y]);
            }
        }
        return cells;
    }

    // Gets all cells that are currently occupied by units.
    public List<GridCell> getOccupiedCells() {
        return getAllCells().stream()
                .filter(GridCell::isOccupied)
                .collect(Collectors.toList());
    }

    // Gets all cells within a rectangular region.
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

    // Gets all cells within a certain distance from a center position.
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

    // Gets the arena layout.
    public ArenaLayout getLayout() {
        return layout;
    }

    // Gets the tower at the specified position.
    public Tower getTowerAt(int x, int y) {
        if (!isValidPosition(x, y))
            return null;
        return towerMap.get(grid[x][y].getPosition());
    }

    // Returns a set of all unique active towers on the board.
    public java.util.Set<Tower> getAllTowers() {
        return new java.util.HashSet<>(towerMap.values());
    }

    // Retrieves towers filtered by type and side.
    public List<Tower> getTowersByType(Tower.TowerType type, boolean isPlayerSide) {
        return towerMap.values().stream()
                .distinct()
                .filter(t -> t.getType() == type && t.isPlayerSide() == isPlayerSide)
                .collect(Collectors.toList());
    }

    // Removes a tower from the arena.Clears the tower from the map and resets the
    // tiles to GRASS
    public void removeTower(Tower tower) {
        if (tower == null)
            return;

        // Find all positions associated with this tower
        List<GridPosition> positionsToRemove = new ArrayList<>();
        for (java.util.Map.Entry<GridPosition, Tower> entry : towerMap.entrySet()) {
            if (entry.getValue() == tower) {
                positionsToRemove.add(entry.getKey());
            }
        }
        spatialGrid.remove(tower);

        // Remove from map and reset grid cells
        for (GridPosition pos : positionsToRemove) {
            towerMap.remove(pos);
            GridCell cell = getCell(pos);
            if (cell != null) {
                cell.setTileType(TileType.GRASS);
            }
        }
    }

    // Removes all towers that have 0 or less health
    public void removeDeadTowers() {
        // Collect dead towers first to avoid concurrent modification
        java.util.Set<Tower> deadTowers = new java.util.HashSet<>();
        for (Tower t : towerMap.values()) {
            if (t.getCurrentHealth() <= 0) {
                deadTowers.add(t);
            }
        }

        for (Tower t : deadTowers) {
            removeTower(t);
        }
    }

    // Checks if the player's King Tower is alive.
    public boolean isPlayerKingAlive() {
        return getAllTowers().stream()
                .anyMatch(t -> t.getType() == Tower.TowerType.KING && t.isPlayerSide() && t.isAlive());
    }

    // Checks if the bot's King Tower is alive.
    public boolean isBotKingAlive() {
        return getAllTowers().stream()
                .anyMatch(t -> t.getType() == Tower.TowerType.KING && !t.isPlayerSide() && t.isAlive());
    }

    public Tower getKingTower(boolean isPlayer) {
        return getTowersByType(Tower.TowerType.KING, isPlayer).stream().findFirst().orElse(null);
    }

    // Occupies the footprint of a building on the grid
    public void occupyFootprint(Building b) {
        if (b == null)
            return;

        for (int dx = 0; dx < b.getWidth(); dx++) {
            for (int dy = 0; dy < b.getHeight(); dy++) {
                int gx = b.getPosition().getX() + dx;
                int gy = b.getPosition().getY() + dy;
                GridPosition pos = GridPosition.tryCreate(gx, gy);
                if (pos != null) {
                    GridCell cell = getCell(pos);
                    if (cell != null) {
                        cell.forceSetOccupant(b);
                    }
                }
            }
        }
    }

    // Frees the footprint of a building from the grid
    public void freeFootprint(Building b) {
        if (b == null)
            return;

        for (int dx = 0; dx < b.getWidth(); dx++) {
            for (int dy = 0; dy < b.getHeight(); dy++) {
                int gx = b.getPosition().getX() + dx;
                int gy = b.getPosition().getY() + dy;
                GridPosition pos = GridPosition.tryCreate(gx, gy);
                if (pos != null) {
                    GridCell cell = getCell(pos);
                    if (cell != null) {
                        if (cell.isOccupied() && cell.getOccupant() == b) {
                            try {
                                cell.clearOccupant();
                            } catch (IllegalStateException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
    }

    public com.kuroyale.model.logic.SpatialGrid getSpatialGrid() {
        return spatialGrid;
    }
}
