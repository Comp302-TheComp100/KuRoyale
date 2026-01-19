package com.kuroyale.model.strategy.pathfinding;

import java.util.Deque;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.entities.Troop;

public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}
