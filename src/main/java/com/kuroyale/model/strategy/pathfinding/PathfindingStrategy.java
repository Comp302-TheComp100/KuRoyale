package com.kuroyale.model.strategy.pathfinding;

import java.util.Deque;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.entities.Troop;

/**
 * Defines the interface for the **Strategy** pattern used in pathfinding.
 * <p>
 * This interface encapsulates the troop movement algorithm, allowing different
 * movement behaviors
 * (e.g., ground vs. air) to be interchangeable at runtime.
 * </p>
 */
public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}
