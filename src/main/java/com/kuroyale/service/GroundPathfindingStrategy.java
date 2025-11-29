package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class GroundPathfindingStrategy implements PathfindingStrategy {
    @Override
    public Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination) {
        Deque<GridPosition> path = new ArrayDeque<>();
        if (destination == null || troop.getPosition().equals(destination)) return path;
        // Simple BFS for now (replaceable with A* later)
        GridPosition start = troop.getPosition();
        Queue<GridPosition> queue = new ArrayDeque<>();
        Map<GridPosition, GridPosition> prev = new HashMap<>();
        queue.add(start);
        prev.put(start, null);
        while (!queue.isEmpty()) {
            GridPosition current = queue.poll();
            if (current.equals(destination)) break;
            for (GridPosition neighbor : arena.getAdjacentPositions(current)) {
                if (!prev.containsKey(neighbor)) {
                    GridCell cell = arena.getCell(neighbor);
                    if (cell != null && cell.isWalkable()) {
                        prev.put(neighbor, current);
                        queue.add(neighbor);
                    }
                }
            }
        }
        if (!prev.containsKey(destination)) return path; // no path
        GridPosition cur = destination;
        while (cur != null) { path.addFirst(cur); cur = prev.get(cur); }
        return path;
    }
}
