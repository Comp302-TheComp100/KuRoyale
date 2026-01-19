package com.kuroyale.model.strategy.pathfinding;

import com.kuroyale.model.entities.*;

import java.util.*;

public class AirDirectPathfindingStrategy implements PathfindingStrategy {
    @Override
    public Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination) {
        Deque<GridPosition> path = new ArrayDeque<>();
        if (destination == null)
            return path;
        GridPosition start = troop.getPosition();
        int dx = destination.getX() - start.getX();
        int dy = destination.getY() - start.getY();
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        for (int i = 1; i <= steps; i++) {
            int x = start.getX() + (int) Math.round((double) dx * i / steps);
            int y = start.getY() + (int) Math.round((double) dy * i / steps);
            GridPosition p = GridPosition.tryCreate(x, y);
            if (p != null)
                path.add(p);
        }
        return path;
    }
}
