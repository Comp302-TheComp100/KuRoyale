package com.kuroyale.model.strategy.pathfinding;

import java.util.Deque;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.entities.Troop;

public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}
