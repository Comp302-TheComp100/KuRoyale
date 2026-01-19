package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.config.GameConstants;

/*The Model component for the Arena Design screen.
 * Encapsulates business logic for arena layout operations.*/
public class ArenaDesignModel {

    private final AuthenticationService authService;
    private final ArenaService arenaService;

    public ArenaDesignModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.arenaService = factory.getArenaService();
    }

    // Checks if a user is logged in
    public boolean isLoggedIn() {
        return authService.isLoggedIn();
    }

    // Gets the current logged-in user
    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    // Sets the current user in arena service to load their saved layout
    public void setCurrentUserInArenaService(User user) {
        arenaService.setCurrentUser(user);
    }

    // Loads the arena layout for the current user
    public ArenaLayout loadArenaLayout() {
        return arenaService.loadArenaLayout();
    }

    // Creates an arena from the given layout
    public Arena createArena(ArenaLayout layout) {
        return arenaService.createArena(layout);
    }

    // Saves the arena layout for the current user
    public void saveArenaLayout(ArenaLayout layout) throws IOException {
        arenaService.saveArenaLayout(layout);
    }

    // Validates the arena layout and returns a list of validation errors (empty if
    // valid)
    public List<String> validateLayout(ArenaLayout layout) {
        List<String> errors = new ArrayList<>();

        if (layout == null) {
            errors.add("Layout is null");
            return errors;
        }

        if (layout.getBridgePositions().isEmpty()) {
            errors.add("You must place at least one bridge.");
        }

        if (layout.getPrincessTowerPositions().size() != 2) {
            errors.add("You must place exactly two Princess towers.");
        }

        if (layout.getKingTowerPosition() == null) {
            errors.add("You must place one King tower.");
        }

        return errors;
    }

    // Logic Methods moved from Controller
    public boolean canPlaceBridge(ArenaLayout layout, int x, int y, StringBuilder errorMsg) {
        // Check max bridges
        if (layout.getBridgePositions().size() >= GameConstants.MAX_BRIDGE_TILES) {
            if (errorMsg != null)
                errorMsg.append(
                        "Limit Reached: You can only place a maximum of " + GameConstants.MAX_BRIDGES + " bridges.");
            return false;
        }

        // River is y=15, 16.
        int startY = GameConstants.RIVER_ROW_1;

        // Align X. If X is last column, shift left.
        int startX = x;
        if (startX >= Arena.WIDTH - 1) {
            startX = Arena.WIDTH - 2;
        }

        // Check if space is already occupied
        boolean occupied = false;
        for (int dx = 0; dx < GameConstants.BRIDGE_WIDTH; dx++) {
            for (int dy = 0; dy < 2; dy++) { // Bridge is 2x2
                final int tx = startX + dx;
                final int ty = startY + dy;
                // Check against existing bridges
                if (layout.getBridgePositions().stream().anyMatch(p -> p.getX() == tx && p.getY() == ty)) {
                    occupied = true;
                    break;
                }
            }
        }

        if (occupied) {
            // overlap
            return false;
        }

        return true;
    }

    public boolean canPlacePrincessTower(ArenaLayout layout, int x, int y, StringBuilder errorMsg) {
        // Check max 2 Princess towers
        if (layout.getPrincessTowerPositions().size() >= GameConstants.MAX_PRINCESS_TOWERS) {
            if (errorMsg != null)
                errorMsg.append("Limit Reached: You can only place a maximum of " + GameConstants.MAX_PRINCESS_TOWERS
                        + " Princess towers.");
            return false;
        }

        // Only allow placement in user's bottom half
        if (y <= GameConstants.USER_SIDE_BOUNDARY_Y) {
            return false;
        }

        // Treat drop cell as center -> top-left start for 3x3
        int startX = x - 1;
        int startY = y - 1;

        // Check bounds for 3x3 tower
        if (startX < 0 || startY < 0 || startX + 2 >= Arena.WIDTH || startY + 2 >= Arena.HEIGHT) {
            return false;
        }

        // Check overlap
        boolean occupied = false;

        // Check against existing Princess towers
        for (GridPosition p : layout.getPrincessTowerPositions()) {
            if (isOverlap(startX, startY, GameConstants.PRINCESS_TOWER_SIZE, GameConstants.PRINCESS_TOWER_SIZE,
                    p.getX(), p.getY(), GameConstants.PRINCESS_TOWER_SIZE, GameConstants.PRINCESS_TOWER_SIZE)) {
                occupied = true;
                break;
            }
        }
        // Check against King tower (4x4)
        if (!occupied && layout.getKingTowerPosition() != null) {
            GridPosition k = layout.getKingTowerPosition();
            if (isOverlap(startX, startY, GameConstants.PRINCESS_TOWER_SIZE, GameConstants.PRINCESS_TOWER_SIZE,
                    k.getX(), k.getY(), GameConstants.KING_TOWER_SIZE, GameConstants.KING_TOWER_SIZE)) {
                occupied = true;
            }
        }

        if (occupied) {
            return false;
        }

        return true;
    }

    public boolean canPlaceKingTower(ArenaLayout layout, int x, int y, StringBuilder errorMsg) {
        // Check if King tower already placed
        if (layout.getKingTowerPosition() != null) {
            if (errorMsg != null)
                errorMsg.append("Limit Reached: You can only place " + GameConstants.MAX_KING_TOWERS + " King tower.");
            return false;
        }

        // Only allow placement in user's bottom half
        if (y <= GameConstants.USER_SIDE_BOUNDARY_Y) {
            return false;
        }

        // Treat drop cell as center. top-left start for 4x4
        int startX = x - 2;
        int startY = y - 2;

        // Check bounds for 4x4 tower
        if (startX < 0 || startY < 0 || startX + 3 >= Arena.WIDTH || startY + 3 >= Arena.HEIGHT) {
            return false;
        }

        // Check against Princess towers
        boolean occupied = false;
        for (GridPosition p : layout.getPrincessTowerPositions()) {
            if (isOverlap(startX, startY, GameConstants.KING_TOWER_SIZE, GameConstants.KING_TOWER_SIZE,
                    p.getX(), p.getY(), GameConstants.PRINCESS_TOWER_SIZE, GameConstants.PRINCESS_TOWER_SIZE)) {
                occupied = true;
                break;
            }
        }

        if (occupied) {
            return false;
        }

        return true;
    }

    public void placeBridge(ArenaLayout layout, int x) {
        int startX = x;
        if (startX >= Arena.WIDTH - 1) {
            startX = Arena.WIDTH - 2;
        }
        int startY = GameConstants.RIVER_ROW_1;

        for (int dx = 0; dx < GameConstants.BRIDGE_WIDTH; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                layout.addBridgePosition(startX + dx, startY + dy);
            }
        }
    }

    public void removeBridge(ArenaLayout layout, int x) {
        // Find the start of the 2x2 bridge block
        int blockStart = x;
        while (blockStart > 0) {
            final int checkX = blockStart - 1;
            boolean isBridgeLeft = layout.getBridgePositions().stream()
                    .anyMatch(p -> p.getX() == checkX
                            && (p.getY() == GameConstants.RIVER_ROW_1 || p.getY() == GameConstants.RIVER_ROW_2));
            if (isBridgeLeft) {
                blockStart--;
            } else {
                break;
            }
        }

        int offset = x - blockStart;
        int bridgeStartX = blockStart + (offset / 2) * 2;

        layout.getBridgePositions().removeIf(
                p -> (p.getX() == bridgeStartX || p.getX() == bridgeStartX + 1)
                        && (p.getY() == GameConstants.RIVER_ROW_1 || p.getY() == GameConstants.RIVER_ROW_2));
    }

    public void placePrincessTower(ArenaLayout layout, int x, int y) {
        int startX = x - 1;
        int startY = y - 1;
        layout.addPrincessTowerPosition(startX, startY);
    }

    public void removePrincessTower(ArenaLayout layout, int x, int y) {
        layout.getPrincessTowerPositions().removeIf(p -> p.getX() == x && p.getY() == y);
    }

    public void placeKingTower(ArenaLayout layout, int x, int y) {
        int startX = x - 2;
        int startY = y - 2;
        layout.setKingTowerPosition(startX, startY);
    }

    public void removeKingTower(ArenaLayout layout) {
        layout.setKingTowerPosition(null);
    }

    // Helper to check if two rectangles overlap
    private boolean isOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
}
