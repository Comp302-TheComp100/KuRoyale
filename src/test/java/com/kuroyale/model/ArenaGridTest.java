package com.kuroyale.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for Arena grid functionality.
 */
public class ArenaGridTest {

    private Arena arena;
    private ArenaLayout layout;

    @BeforeEach
    public void setUp() {
        // Create a layout with some bridges
        layout = new ArenaLayout("Test Arena");
        List<GridPosition> bridges = new ArrayList<>();
        bridges.add(new GridPosition(8, 15)); // Bridge at river
        bridges.add(new GridPosition(9, 16)); // Bridge at river
        layout.setBridgePositions(bridges);

        arena = new Arena(layout);
    }

    @Test
    public void testArenaGridInitialization() {
        assertNotNull(arena);
        assertEquals(Arena.WIDTH, 18);
        assertEquals(Arena.HEIGHT, 32);
    }

    @Test
    public void testGetCell() {
        GridPosition pos = new GridPosition(5, 10);
        GridCell cell = arena.getCell(pos);

        assertNotNull(cell);
        assertEquals(pos, cell.getPosition());
        assertEquals(TileType.GRASS, cell.getTileType());
    }

    @Test
    public void testGetCellByCoordinates() {
        GridCell cell = arena.getCell(5, 10);

        assertNotNull(cell);
        assertEquals(5, cell.getX());
        assertEquals(10, cell.getY());
    }

    @Test
    public void testGetCellInvalid() {
        GridPosition invalidPos = GridPosition.tryCreate(-1, 0);
        GridCell cell = arena.getCell(invalidPos);

        assertNull(cell);
    }

    @Test
    public void testRiverInitialization() {
        // River should be at y=15 and y=16
        GridCell riverCell1 = arena.getCell(5, 15);
        GridCell riverCell2 = arena.getCell(5, 16);

        assertEquals(TileType.WATER, riverCell1.getTileType());
        assertEquals(TileType.WATER, riverCell2.getTileType());
    }

    @Test
    public void testBridgeOverridesRiver() {
        // Bridge at (8, 15) should override water
        GridCell bridgeCell = arena.getCell(8, 15);
        assertEquals(TileType.BRIDGE, bridgeCell.getTileType());
        assertTrue(bridgeCell.isWalkable());
    }

    @Test
    public void testCanPlaceUnit() {
        GridPosition grassPos = new GridPosition(5, 10);
        assertTrue(arena.canPlaceUnit(grassPos));
    }

    @Test
    public void testCannotPlaceUnitOnWater() {
        GridPosition waterPos = new GridPosition(5, 15);
        assertFalse(arena.canPlaceUnit(waterPos));
    }

    @Test
    public void testPlaceUnit() {
        GridPosition pos = new GridPosition(5, 10);
        String unit = "Knight";

        arena.placeUnit(pos, unit);

        GridCell cell = arena.getCell(pos);
        assertTrue(cell.isOccupied());
        assertEquals(unit, cell.getOccupant());
    }

    @Test
    public void testRemoveUnit() {
        GridPosition pos = new GridPosition(5, 10);
        arena.placeUnit(pos, "Knight");

        GridCell cell = arena.getCell(pos);
        assertTrue(cell.isOccupied());

        arena.removeUnit(pos);

        assertFalse(cell.isOccupied());
    }

    @Test
    public void testGetAdjacentPositions() {
        GridPosition center = new GridPosition(5, 5);
        List<GridPosition> adjacent = arena.getAdjacentPositions(center);

        assertEquals(4, adjacent.size()); // Up, Down, Left, Right

        assertTrue(adjacent.contains(new GridPosition(5, 4))); // Up
        assertTrue(adjacent.contains(new GridPosition(5, 6))); // Down
        assertTrue(adjacent.contains(new GridPosition(4, 5))); // Left
        assertTrue(adjacent.contains(new GridPosition(6, 5))); // Right
    }

    @Test
    public void testGetAdjacentPositionsCorner() {
        GridPosition corner = new GridPosition(0, 0);
        List<GridPosition> adjacent = arena.getAdjacentPositions(corner);

        assertEquals(2, adjacent.size()); // Only Down and Right
    }

    @Test
    public void testGetAdjacentPositionsWithDiagonals() {
        GridPosition center = new GridPosition(5, 5);
        List<GridPosition> adjacent = arena.getAdjacentPositionsWithDiagonals(center);

        assertEquals(8, adjacent.size()); // All 8 directions
    }

    @Test
    public void testGetAllCells() {
        List<GridCell> allCells = arena.getAllCells();

        assertEquals(Arena.WIDTH * Arena.HEIGHT, allCells.size());
        assertEquals(18 * 32, allCells.size());
    }

    @Test
    public void testGetOccupiedCells() {
        // Initially no occupied cells
        assertEquals(0, arena.getOccupiedCells().size());

        // Place some units
        arena.placeUnit(new GridPosition(5, 10), "Knight");
        arena.placeUnit(new GridPosition(6, 12), "Archer");

        List<GridCell> occupied = arena.getOccupiedCells();
        assertEquals(2, occupied.size());
    }

    @Test
    public void testGetCellsInRegion() {
        GridPosition topLeft = new GridPosition(5, 5);
        GridPosition bottomRight = new GridPosition(7, 7);

        List<GridCell> cells = arena.getCellsInRegion(topLeft, bottomRight);

        // 3x3 region
        assertEquals(9, cells.size());
    }

    @Test
    public void testGetCellsInRadius() {
        GridPosition center = new GridPosition(5, 5);
        int radius = 2;

        List<GridCell> cells = arena.getCellsInRadius(center, radius);

        // Should include all cells within Manhattan distance 2
        assertTrue(cells.size() > 0);

        // Center should be included
        assertTrue(cells.stream().anyMatch(c -> c.getPosition().equals(center)));
    }

    @Test
    public void testBackwardCompatibilityGetTile() {
        @SuppressWarnings("deprecation")
        Tile tile = arena.getTile(5, 10);

        assertNotNull(tile);
        assertEquals(5, tile.getX());
        assertEquals(10, tile.getY());
    }
}
