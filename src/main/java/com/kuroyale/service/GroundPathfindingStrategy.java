package com.kuroyale.service;

import com.kuroyale.model.*;
import java.util.*;

public class GroundPathfindingStrategy implements PathfindingStrategy {
    @Override
    public Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination) {
        Deque<GridPosition> path = new ArrayDeque<>();
        if (arena == null || troop == null || destination == null) return path;

        // If destination is not walkable, pick nearest walkable cell
        GridCell destCell = arena.getCell(destination);
        if (destCell == null || !destCell.isWalkable()) {
            destination = findNearestWalkable(arena, destination);
            if (destination == null) return path; // No valid reachable target
        }

        GridPosition start = troop.getPosition();
        if (start == null || start.equals(destination)) return path;

        // A* search
        Map<GridPosition, GridPosition> cameFrom = new HashMap<>();
        Map<GridPosition, Integer> gScore = new HashMap<>();
        Map<GridPosition, Integer> fScore = new HashMap<>();

        Comparator<GridPosition> byFScore = Comparator.comparingInt(p -> fScore.getOrDefault(p, Integer.MAX_VALUE));
        PriorityQueue<GridPosition> openSet = new PriorityQueue<>(byFScore);

        gScore.put(start, 0);
        fScore.put(start, heuristic(start, destination));
        openSet.add(start);

        Set<GridPosition> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            GridPosition current = openSet.poll();
            if (current.equals(destination)) {
                // reconstruct path
                GridPosition cur = current;
                while (cur != null) { path.addFirst(cur); cur = cameFrom.get(cur); }
                return path;
            }

            closedSet.add(current);

            for (GridPosition neighbor : arena.getAdjacentPositions(current)) { // 4-dir for ground
                if (closedSet.contains(neighbor)) continue;
                GridCell cell = arena.getCell(neighbor);
                if (cell == null || !cell.isWalkable() || cell.isOccupied()) continue;

                int tentativeG = gScore.getOrDefault(current, Integer.MAX_VALUE - 1) + 1; // cost 1 per move

                boolean isBetter = tentativeG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE);
                if (isBetter) {
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    fScore.put(neighbor, tentativeG + heuristic(neighbor, destination));
                    // Update openSet priority; if already present, remove and re-add
                    if (openSet.contains(neighbor)) {
                        openSet.remove(neighbor);
                    }
                    openSet.add(neighbor);
                }
            }
        }

        // No path found
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

    // heuristic distance suitable for 4-directional grid movement
    private int heuristic(GridPosition a, GridPosition b) {
        if (a == null || b == null) return Integer.MAX_VALUE / 4;
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }
}
