package com.kuroyale.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.GridPosition;
import com.kuroyale.repository.UserRepository;

/**
 * Unit tests for ArenaService grid operations.
 */
public class ArenaServiceGridTest {

    private ArenaService arenaService;
    private Arena arena;

    @BeforeEach
    public void setUp() {
        UserRepository mockRepo = null; // No persistence needed for these tests
        arenaService = new ArenaService(mockRepo);

        ArenaLayout layout = arenaService.createDefaultLayout();
        arena = arenaService.createArena(layout);
    }

    @Test
    public void testIsValidPlacement() {
        GridPosition validPos = new GridPosition(5, 10);
        assertTrue(arenaService.isValidPlacement(arena, validPos));
    }

    @Test
    public void testIsValidPlacementOnBridge() {
        GridPosition bridgePos = new GridPosition(8, 15);
        // Note: Without bridges in default layout, this might be water
        // But the logic should handle it correctly
        boolean result = arenaService.isValidPlacement(arena, bridgePos);
        // Water at river, so should be false
        assertFalse(result);
    }

    @Test
    public void testIsValidPlacementOnWater() {
        GridPosition waterPos = new GridPosition(5, 15); // River position
        assertFalse(arenaService.isValidPlacement(arena, waterPos));
    }

    @Test
    public void testGetValidPlacementPositions() {
        List<GridPosition> validPositions = arenaService.getValidPlacementPositions(arena);

        // Should have many valid positions (all grass cells)
        assertTrue(validPositions.size() > 0);

        // Should not include water cells
        GridPosition waterPos = new GridPosition(5, 15);
        assertFalse(validPositions.contains(waterPos));
    }

    @Test
    public void testGetValidPlacementPositionsInRegion() {
        GridPosition topLeft = new GridPosition(5, 5);
        GridPosition bottomRight = new GridPosition(10, 10);

        List<GridPosition> validPositions = arenaService.getValidPlacementPositionsInRegion(
                arena, topLeft, bottomRight);

        // All cells in this region should be grass (not river)
        assertEquals(36, validPositions.size()); // 6x6 region
    }

    @Test
    public void testCountOccupiedCells() {
        // Initially should be 0
        assertEquals(0, arenaService.countOccupiedCells(arena));

        // Place some units
        arena.placeUnit(new GridPosition(5, 10), "Knight");
        arena.placeUnit(new GridPosition(6, 12), "Archer");

        assertEquals(2, arenaService.countOccupiedCells(arena));
    }

    @Test
    public void testFindUnitsInRange() {
        GridPosition center = new GridPosition(5, 10);

        // Place units around center
        arena.placeUnit(new GridPosition(5, 10), "Knight");
        arena.placeUnit(new GridPosition(5, 11), "Archer"); // Distance 1
        arena.placeUnit(new GridPosition(7, 12), "Giant"); // Distance 4
        arena.placeUnit(new GridPosition(10, 10), "Wizard"); // Distance 5

        List<GridPosition> inRange = arenaService.findUnitsInRange(arena, center, 2);

        // Should find Knight (self) and Archer
        assertEquals(2, inRange.size());
    }

    @Test
    public void testGetOccupiedPositions() {
        arena.placeUnit(new GridPosition(5, 10), "Knight");
        arena.placeUnit(new GridPosition(6, 12), "Archer");

        List<GridPosition> occupied = arenaService.getOccupiedPositions(arena);

        assertEquals(2, occupied.size());
        assertTrue(occupied.contains(new GridPosition(5, 10)));
        assertTrue(occupied.contains(new GridPosition(6, 12)));
    }

    @Test
    public void testHasPathToTarget() {
        GridPosition start = new GridPosition(5, 10);
        GridPosition target = new GridPosition(10, 20);

        assertTrue(arenaService.hasPathToTarget(arena, start, target));
    }

    @Test
    public void testHasPathToTargetBothWalkable() {
        GridPosition waterStart = new GridPosition(5, 15); // Water
        GridPosition target = new GridPosition(10, 20); // Grass

        // Start is water, so no path
        assertFalse(arenaService.hasPathToTarget(arena, waterStart, target));
    }

    @Test
    public void testGetWalkablePath() {
        GridPosition start = new GridPosition(5, 5);
        GridPosition end = new GridPosition(5, 8);

        List<GridPosition> path = arenaService.getWalkablePath(arena, start, end);

        // Should have a straight vertical path
        assertTrue(path.size() > 0);
        assertEquals(start, path.get(0));
        assertEquals(end, path.get(path.size() - 1));
    }

    @Test
    public void testGetWalkablePathBlocked() {
        GridPosition start = new GridPosition(5, 14); // Just before river
        GridPosition end = new GridPosition(5, 17); // After river

        List<GridPosition> path = arenaService.getWalkablePath(arena, start, end);

        // Path should be blocked by river
        assertEquals(0, path.size());
    }

    @Test
    public void testGetWalkableAdjacentPositions() {
        GridPosition pos = new GridPosition(5, 10);

        List<GridPosition> walkable = arenaService.getWalkableAdjacentPositions(arena, pos);

        // Should have 4 walkable adjacent positions (up, down, left, right)
        assertEquals(4, walkable.size());
    }

    @Test
    public void testGetWalkableAdjacentPositionsNearRiver() {
        GridPosition pos = new GridPosition(5, 14); // Just before river

        List<GridPosition> walkable = arenaService.getWalkableAdjacentPositions(arena, pos);

        // Up should be blocked by river (y=15)
        // Should have 3 walkable (down, left, right)
        assertEquals(3, walkable.size());
    }

    @Test
    public void testNullHandling() {
        assertFalse(arenaService.isValidPlacement(null, new GridPosition(5, 10)));
        assertFalse(arenaService.isValidPlacement(arena, null));
        assertEquals(0, arenaService.countOccupiedCells(null));
        assertEquals(0, arenaService.findUnitsInRange(null, new GridPosition(5, 10), 2).size());
    }
}
