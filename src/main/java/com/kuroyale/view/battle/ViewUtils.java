package com.kuroyale.view.battle;

import com.kuroyale.model.enums.TileType;
import com.kuroyale.util.config.GameColors;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class ViewUtils {

    // Prevent instantiation
    private ViewUtils() {
    }

    public static Paint getTileColor(TileType type, int x, int y, String userGridColor1, String userGridColor2) {
        // Treat towers as grass base
        if (isTower(type)) {
            type = TileType.GRASS;
        }

        switch (type) {
            case GRASS:
                try {
                    Color c1 = null;
                    if (userGridColor1 != null && !userGridColor1.isEmpty()) {
                        c1 = Color.valueOf(userGridColor1);
                    }

                    Color c2 = null;
                    if (userGridColor2 != null && !userGridColor2.isEmpty()) {
                        c2 = Color.valueOf(userGridColor2);
                    }

                    if (c1 != null) {
                        if ((x + y) % 2 == 0) {
                            return c1;
                        } else {
                            if (c2 != null) {
                                return c2;
                            } else {
                                // Fallback: derived dark (15% darker) if no secondary provided
                                return c1.deriveColor(0, 1, 0.85, 1);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Fallback to default
                }
                return ((x + y) % 2 == 0) ? GameColors.GRASS_LIGHT : GameColors.GRASS_DARK;
            case WATER:
                return GameColors.WATER;
            case BRIDGE:
                return GameColors.BRIDGE;
            case ROAD:
                return GameColors.ROAD;
            case PRINCESS_TOWER_USER:
                return GameColors.TOWER_PRINCESS_USER; // Fallback only
            case PRINCESS_TOWER_COMPUTER:
                return GameColors.TOWER_PRINCESS_ENEMY; // Fallback only
            case KING_TOWER_USER:
                return GameColors.TOWER_KING_USER; // Fallback only
            case KING_TOWER_COMPUTER:
                return GameColors.TOWER_KING_ENEMY; // Fallback only
            default:
                return GameColors.DEFAULT;
        }
    }

    // Deprecated / Compatibility
    public static Paint getTileColor(TileType type, int x, int y, String userGridColor) {
        return getTileColor(type, x, y, userGridColor, null);
    }

    public static Paint getTileColor(TileType type, int x, int y) {
        return getTileColor(type, x, y, null);
    }

    private static boolean isTower(TileType type) {
        return type == TileType.PRINCESS_TOWER_USER ||
                type == TileType.PRINCESS_TOWER_COMPUTER ||
                type == TileType.KING_TOWER_USER ||
                type == TileType.KING_TOWER_COMPUTER;
    }
}
