package com.kuroyale.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.GridCell;
import com.kuroyale.model.GridPosition;
import com.kuroyale.model.User;
import com.kuroyale.repository.UserRepository;

/* Service for managing Arena operations.
 * Pure Fabrication: Manages arena operations without being a domain entityitself.
 * Creator: Creates Arena instances from ArenaLayout.
 * Low Coupling: Coordinates between Arena and User without duplicating Arena's logic.*/
public class ArenaService {
    private final UserRepository userRepository;
    private ArenaLayout savedLayout; // Fallback for when no user is logged in
    private User currentUser; // Track the current user

    public ArenaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    //Sets the current user for arena operations.
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }


    //Creates a default arena layout.
    public ArenaLayout createDefaultLayout() {
        ArenaLayout layout = new ArenaLayout("Default Arena");
        // Add a central 2x2 bridge across the river (y = 15,16)
        int midXStart = Math.max(0, (Arena.WIDTH / 2) - 1);
        layout.addBridgePosition(midXStart, 15);
        layout.addBridgePosition(midXStart + 1, 15);
        layout.addBridgePosition(midXStart, 16);
        layout.addBridgePosition(midXStart + 1, 16);

        // Place two Princess towers on player's side (bottom half), 3x3 each
        // Left princess
        int princessLeftX = 2;
        int princessY = Arena.HEIGHT - 6; // leave margin from bottom
        layout.addPrincessTowerPosition(princessLeftX, princessY);
        // Right princess
        int princessRightX = Arena.WIDTH - 5; // 3x3 top-left fits within width
        layout.addPrincessTowerPosition(princessRightX, princessY);

        // Place one King tower centered at bottom, 4x4
        int kingX = (Arena.WIDTH / 2) - 2; // top-left of 4x4
        int kingY = Arena.HEIGHT - 6; // align roughly with princess towers
        layout.setKingTowerPosition(kingX, kingY);

        return layout;
    }

    // Creates an Arena instance from a layout.
    public Arena createArena(ArenaLayout layout) {
        if (layout == null) {
            layout = createDefaultLayout();
        }
        return new Arena(layout);
    }

    // Saves an arena layout to the current user's profile.
    public void saveArenaLayout(ArenaLayout layout) throws IOException {
        if (currentUser != null && userRepository != null) {
            // Save to current user's profile
            currentUser.setArenaLayout(layout);
            userRepository.save(currentUser);
            System.out.println("Saving arena layout to user profile: " + layout.getName());
        } else {
            // Fallback to in-memory storage if no user logged in
            this.savedLayout = layout;
            System.out.println("Saving arena layout (in-memory): " + layout.getName());
        }
    }

    // Loads the arena layout from the current user's profile.
    public ArenaLayout loadArenaLayout() {
        // Try to load from current user first
        if (currentUser != null && currentUser.hasArenaLayout()) {
            return currentUser.getArenaLayout();
        }

        // Fallback to in-memory storage
        if (savedLayout == null) {
            savedLayout = createDefaultLayout();
        }
        return savedLayout;
    }

    // placement validation combining all rules. Checks if a unit can be placed at the given position.
    public boolean isValidPlacement(Arena arena, GridPosition position) {
        if (arena == null || position == null) {
            return false;
        }
        return arena.canPlaceUnit(position);
    }

    //Gets all positions where units can currently be placed.
    public List<GridPosition> getValidPlacementPositions(Arena arena) {
        List<GridPosition> validPositions = new ArrayList<>();
        if (arena == null) {
            return validPositions;
        }

        return arena.getAllCells().stream().filter(GridCell::canPlaceUnit).map(GridCell::getPosition).collect(Collectors.toList());
    }

    //Gets all positions where units can be placed in a specific region.
    public List<GridPosition> getValidPlacementPositionsInRegion(
            Arena arena, GridPosition topLeft, GridPosition bottomRight) {
        if (arena == null) {
            return new ArrayList<>();
        }

        return arena.getCellsInRegion(topLeft, bottomRight).stream().filter(GridCell::canPlaceUnit).map(GridCell::getPosition).collect(Collectors.toList());
    }

    // Counts the number of occupied cells in the arena.
    public int countOccupiedCells(Arena arena) {
        if (arena == null) {
            return 0;
        }
        return arena.getOccupiedCells().size();
    }

    //Finds all units within a certain range from a central position.
    public List<GridPosition> findUnitsInRange(Arena arena, GridPosition center, int range) {
        if (arena == null || center == null) {
            return new ArrayList<>();
        }

        return arena.getCellsInRadius(center, range).stream().filter(GridCell::isOccupied).map(GridCell::getPosition).collect(Collectors.toList());
    }

    // Gets all occupied positions in the arena.
    public List<GridPosition> getOccupiedPositions(Arena arena) {
        if (arena == null) {
            return new ArrayList<>();
        }

        return arena.getOccupiedCells().stream().map(GridCell::getPosition).collect(Collectors.toList());
    }

    //hecks if there's a walkable path between two positions.
    public boolean hasPathToTarget(Arena arena, GridPosition start, GridPosition target) {
        if (arena == null || start == null || target == null) {
            return false;
        }

        // Simple check: both positions must be walkable
        GridCell startCell = arena.getCell(start);
        GridCell targetCell = arena.getCell(target);

        if (startCell == null || targetCell == null) {
            return false;
        }

        return startCell.isWalkable() && targetCell.isWalkable();
    }

    //Gets a simple walkable path between two positions. Placehoder until a* algorithm
    public List<GridPosition> getWalkablePath(Arena arena, GridPosition start, GridPosition end) {
        List<GridPosition> path = new ArrayList<>();

        if (arena == null || start == null || end == null) {
            return path;
        }

        // Simple straight-line path
        int x0 = start.getX();
        int y0 = start.getY();
        int x1 = end.getX();
        int y1 = end.getY();

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            GridPosition pos = GridPosition.tryCreate(x, y);
            if (pos != null) {
                GridCell cell = arena.getCell(pos);
                if (cell != null && cell.isWalkable()) {
                    path.add(pos);
                } else {
                    // Path blocked, return empty list
                    return new ArrayList<>();
                }
            }

            if (x == x1 && y == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
        return path;
    }

    //Gets all walkable positions adjacent to a given position.
    public List<GridPosition> getWalkableAdjacentPositions(Arena arena, GridPosition position) {
        if (arena == null || position == null) {
            return new ArrayList<>();
        }

        return arena.getAdjacentPositions(position).stream().filter(pos -> {
                    GridCell cell = arena.getCell(pos);
                    return cell != null && cell.isWalkable() && !cell.isOccupied();})
                    .collect(Collectors.toList());
    }
}