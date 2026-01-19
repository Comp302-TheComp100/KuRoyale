package com.kuroyale.model.strategy.pathfinding;

import java.util.Deque;

import com.kuroyale.model.core.entities.Arena;
import com.kuroyale.model.core.entities.GridPosition;
import com.kuroyale.model.core.entities.Troop;

public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}
