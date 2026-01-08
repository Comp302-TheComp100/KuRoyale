package com.kuroyale.util;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.GridPosition;

/*Utility class for the Arena Coordinate System.
 * Provides methods for coordinate validation, conversion, and deployment logic.
 * Coordinate System Specification:
 * - Grid Size: 18 (Width) x 32 (Height)
 * - Origin (0, 0): Top-Left corner
 * - User Side: Bottom half (y > 16)
 * - Computer Side: Top half (y < 16)
 * - River: Rows 15 and 16  */
public class CoordinateSystem {

    public static final int TILE_SIZE = 18; // Pixels per tile

    //Checks if a coordinate is within the arena bounds.
    public static boolean isValid(int x, int y) {
        return x >= 0 && x < Arena.WIDTH && y >= 0 && y < Arena.HEIGHT;
    }

    //Converts screen pixel coordinates to grid coordinates.
    public static GridPosition pixelToGrid(double pixelX, double pixelY) {
        int x = (int) (pixelX / TILE_SIZE);
        int y = (int) (pixelY / TILE_SIZE);

        if (isValid(x, y)) {
            return new GridPosition(x, y);
        }
        return null;
    }

    //Converts grid coordinates to top-left pixel coordinates.
    public static double[] gridToPixel(int x, int y) {
        return new double[] { x * TILE_SIZE, y * TILE_SIZE };
    }

    //Checks if a position is valid for card deployment based on the player side.
    public static boolean isDeployableSide(int x, int y, boolean isUser) {
        if (!isValid(x, y))
            return false;

        // User side: y > 16 (Rows 17-31)
        // Computer side: y < 15 (Rows 0-14)
        // Bridge/River area (15, 16) is  neutral or restricted on game state.

        if (isUser) {
            return y > 16;
        } else {
            return y < 15;
        }
    }

    //Gets the center pixel coordinate of a tile.
    public static double[] getTileCenter(int x, int y) {
        return new double[] { (x * TILE_SIZE) + (TILE_SIZE / 2.0), (y * TILE_SIZE) + (TILE_SIZE / 2.0) };
    }
}
