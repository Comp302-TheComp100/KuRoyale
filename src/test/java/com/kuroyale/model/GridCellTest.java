package com.kuroyale.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GridCell class.
 */
public class GridCellTest {

    @Test
    public void testCellCreation() {
        GridPosition pos = new GridPosition(5, 10);
        GridCell cell = new GridCell(pos, TileType.GRASS);

        assertEquals(pos, cell.getPosition());
        assertEquals(5, cell.getX());
        assertEquals(10, cell.getY());
        assertEquals(TileType.GRASS, cell.getTileType());
        assertFalse(cell.isOccupied());
        assertNull(cell.getOccupant());
    }

    @Test
    public void testCellCreationWithCoordinates() {
        GridCell cell = new GridCell(3, 7, TileType.BRIDGE);

        assertEquals(3, cell.getX());
        assertEquals(7, cell.getY());
        assertEquals(TileType.BRIDGE, cell.getTileType());
    }

    @Test
    public void testCanPlaceUnitOnGrass() {
        GridCell cell = new GridCell(5, 10, TileType.GRASS);
        assertTrue(cell.canPlaceUnit());
    }

    @Test
    public void testCanPlaceUnitOnBridge() {
        GridCell cell = new GridCell(5, 10, TileType.BRIDGE);
        assertTrue(cell.canPlaceUnit());
    }

    @Test
    public void testCannotPlaceUnitOnWater() {
        GridCell cell = new GridCell(5, 10, TileType.WATER);
        assertFalse(cell.canPlaceUnit());
    }

    @Test
    public void testUnitPlacement() {
        GridCell cell = new GridCell(5, 10, TileType.GRASS);
        String unit = "Knight";

        cell.setOccupant(unit);

        assertTrue(cell.isOccupied());
        assertEquals(unit, cell.getOccupant());
        assertFalse(cell.canPlaceUnit()); // Can't place on occupied cell
    }

    @Test
    public void testCannotPlaceOnOccupiedCell() {
        GridCell cell = new GridCell(5, 10, TileType.GRASS);
        cell.setOccupant("Knight");

        assertThrows(IllegalStateException.class, () -> {
            cell.setOccupant("Archer");
        });
    }

    @Test
    public void testClearOccupant() {
        GridCell cell = new GridCell(5, 10, TileType.GRASS);
        cell.setOccupant("Knight");

        assertTrue(cell.isOccupied());

        cell.clearOccupant();

        assertFalse(cell.isOccupied());
        assertNull(cell.getOccupant());
        assertTrue(cell.canPlaceUnit());
    }

    @Test
    public void testIsWalkableGrass() {
        GridCell cell = new GridCell(5, 10, TileType.GRASS);
        assertTrue(cell.isWalkable());
    }

    @Test
    public void testIsWalkableBridge() {
        GridCell cell = new GridCell(5, 10, TileType.BRIDGE);
        assertTrue(cell.isWalkable());
    }

    @Test
    public void testIsNotWalkableWater() {
        GridCell cell = new GridCell(5, 10, TileType.WATER);
        assertFalse(cell.isWalkable());
    }

    @Test
    public void testSetTileType() {
        GridCell cell = new GridCell(5, 10, TileType.GRASS);
        cell.setTileType(TileType.WATER);

        assertEquals(TileType.WATER, cell.getTileType());
        assertFalse(cell.canPlaceUnit());
    }

    @Test
    public void testNullPositionThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridCell(null, TileType.GRASS);
        });
    }

    @Test
    public void testNullTileTypeThrows() {
        GridPosition pos = new GridPosition(5, 10);
        assertThrows(IllegalArgumentException.class, () -> {
            new GridCell(pos, null);
        });
    }
}
