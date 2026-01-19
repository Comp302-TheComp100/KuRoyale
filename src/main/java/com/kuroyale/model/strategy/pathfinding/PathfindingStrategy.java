package com.kuroyale.model.strategy.pathfinding;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.entities.Troop;
import java.util.Deque;

public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}
