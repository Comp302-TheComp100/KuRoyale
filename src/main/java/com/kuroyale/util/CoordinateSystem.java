package com.kuroyale.util;

import com.kuroyale.model.Arena;
import com.kuroyale.model.GridPosition;

/**
 * Utility class for the Arena Coordinate System.
 * Provides methods for coordinate validation, conversion, and deployment logic.
 * 
 * Coordinate System Specification:
 * - Grid Size: 18 (Width) x 32 (Height)
 * - Origin (0, 0): Top-Left corner
 * - User Side: Bottom half (y > 16)
 * - Computer Side: Top half (y < 16)
 * - River: Rows 15 and 16 (0-indexed: 15, 16? No, height is 32. 0-15 is top,
 * 16-31 is bottom. River is usually middle.)
 * - Wait, Arena.HEIGHT = 32. Middle is 16.
 * - Let's assume River is at y=15 and y=16 based on previous code.
 */
public class CoordinateSystem {

    public static final int TILE_SIZE = 18; // Pixels per tile

    /**
     * Checks if a coordinate is within the arena bounds.
     * 
     * @param x Grid X (0-17)
     * @param y Grid Y (0-31)
     * @return true if valid
     */
    public static boolean isValid(int x, int y) {
        return x >= 0 && x < Arena.WIDTH && y >= 0 && y < Arena.HEIGHT;
    }

    /**
     * Converts screen pixel coordinates to grid coordinates.
     * 
     * @param pixelX Screen X
     * @param pixelY Screen Y
     * @return GridPosition or null if out of bounds
     */
    public static GridPosition pixelToGrid(double pixelX, double pixelY) {
        int x = (int) (pixelX / TILE_SIZE);
        int y = (int) (pixelY / TILE_SIZE);

        if (isValid(x, y)) {
            return new GridPosition(x, y);
        }
        return null;
    }

    /**
     * Converts grid coordinates to top-left pixel coordinates.
     * 
     * @param x Grid X
     * @param y Grid Y
     * @return double[] {pixelX, pixelY}
     */
    public static double[] gridToPixel(int x, int y) {
        return new double[] { x * TILE_SIZE, y * TILE_SIZE };
    }

    /**
     * Checks if a position is valid for card deployment based on the player side.
     * Does NOT check for obstacles (water/buildings) - that requires Arena state.
     * 
     * @param x      Grid X
     * @param y      Grid Y
     * @param isUser true for User (Bottom), false for Computer (Top)
     * @return true if the coordinate is on the correct side
     */
    public static boolean isDeployableSide(int x, int y, boolean isUser) {
        if (!isValid(x, y))
            return false;

        // River is typically 15-16.
        // User side: y > 16 (Rows 17-31)
        // Computer side: y < 15 (Rows 0-14)
        // Bridge/River area (15, 16) is usually neutral or restricted depending on game
        // state.

        if (isUser) {
            return y > 16;
        } else {
            return y < 15;
        }
    }

    /**
     * Gets the center pixel coordinate of a tile.
     * 
     * @param x Grid X
     * @param y Grid Y
     * @return double[] {centerX, centerY}
     */
    public static double[] getTileCenter(int x, int y) {
        return new double[] { (x * TILE_SIZE) + (TILE_SIZE / 2.0), (y * TILE_SIZE) + (TILE_SIZE / 2.0) };
    }
}
