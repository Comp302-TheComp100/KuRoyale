package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class GroundPathfindingStrategy implements PathfindingStrategy {
    @Override
    public Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination) {
        Deque<GridPosition> path = new ArrayDeque<>();
        if (destination == null) return path;
        // If destination is not walkable (e.g., tower cell), pick nearest walkable cell
        GridCell destCell = arena.getCell(destination);
        if (destCell == null || !destCell.isWalkable()) {
            destination = findNearestWalkable(arena, destination);
            if (destination == null) return path; // No valid reachable target
        }
        if (troop.getPosition().equals(destination)) return path;
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

    private GridPosition findNearestWalkable(Arena arena, GridPosition from) {
        // BFS expanding from 'from' to find first walkable cell
        Queue<GridPosition> q = new ArrayDeque<>();
        Set<GridPosition> seen = new HashSet<>();
        q.add(from);
        seen.add(from);
        while (!q.isEmpty()) {
            GridPosition p = q.poll();
            GridCell c = arena.getCell(p);
            if (c != null && c.isWalkable()) return p;
            for (GridPosition n : arena.getAdjacentPositionsWithDiagonals(p)) {
                if (!seen.contains(n)) { seen.add(n); q.add(n); }
            }
        }
        return null;
    }
}
