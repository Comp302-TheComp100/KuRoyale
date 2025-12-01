package com.kuroyale.service;

import com.kuroyale.model.Arena;
import com.kuroyale.model.GridPosition;
import com.kuroyale.model.Troop;
import java.util.Deque;

public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}
