package com.kuroyale.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GridPosition class.
 */
public class GridPositionTest {

    @Test
    public void testValidPositionCreation() {
        GridPosition pos = new GridPosition(0, 0);
        assertEquals(0, pos.getX());
        assertEquals(0, pos.getY());
        assertTrue(pos.isValid());
    }

    @Test
    public void testValidPositionMax() {
        GridPosition pos = new GridPosition(17, 31);
        assertEquals(17, pos.getX());
        assertEquals(31, pos.getY());
        assertTrue(pos.isValid());
    }

    @Test
    public void testInvalidPositionNegative() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridPosition(-1, 0);
        });
    }

    @Test
    public void testInvalidPositionTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> {
            new GridPosition(18, 32);
        });
    }

    @Test
    public void testTryCreateValid() {
        GridPosition pos = GridPosition.tryCreate(5, 10);
        assertNotNull(pos);
        assertEquals(5, pos.getX());
        assertEquals(10, pos.getY());
    }

    @Test
    public void testTryCreateInvalid() {
        GridPosition pos = GridPosition.tryCreate(-1, 0);
        assertNull(pos);
    }

    @Test
    public void testEquality() {
        GridPosition pos1 = new GridPosition(5, 10);
        GridPosition pos2 = new GridPosition(5, 10);
        GridPosition pos3 = new GridPosition(6, 10);

        assertEquals(pos1, pos2);
        assertNotEquals(pos1, pos3);
        assertEquals(pos1.hashCode(), pos2.hashCode());
    }

    @Test
    public void testManhattanDistance() {
        GridPosition pos1 = new GridPosition(0, 0);
        GridPosition pos2 = new GridPosition(3, 4);

        assertEquals(7, pos1.getDistanceTo(pos2));
        assertEquals(7, pos2.getDistanceTo(pos1)); // Symmetric
    }

    @Test
    public void testEuclideanDistance() {
        GridPosition pos1 = new GridPosition(0, 0);
        GridPosition pos2 = new GridPosition(3, 4);

        assertEquals(5.0, pos1.getEuclideanDistanceTo(pos2), 0.001);
    }

    @Test
    public void testIsAdjacent() {
        GridPosition center = new GridPosition(5, 5);
        GridPosition adjacent1 = new GridPosition(5, 6); // Below
        GridPosition adjacent2 = new GridPosition(6, 6); // Diagonal
        GridPosition notAdjacent = new GridPosition(5, 7); // Two away
        GridPosition self = new GridPosition(5, 5); // Same position

        assertTrue(center.isAdjacentTo(adjacent1));
        assertTrue(center.isAdjacentTo(adjacent2));
        assertFalse(center.isAdjacentTo(notAdjacent));
        assertFalse(center.isAdjacentTo(self)); // Not adjacent to self
    }

    @Test
    public void testToString() {
        GridPosition pos = new GridPosition(5, 10);
        assertEquals("(5, 10)", pos.toString());
    }
}
