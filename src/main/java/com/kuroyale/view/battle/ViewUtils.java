package com.kuroyale.view.battle;

import com.kuroyale.model.core.enums.TileType;
import com.kuroyale.util.config.GameColors;

import javafx.scene.paint.Paint;

public class ViewUtils {

    // Prevent instantiation
    private ViewUtils() {
    }

    public static Paint getTileColor(TileType type, int x, int y) {
        // Treat towers as grass base
        if (isTower(type)) {
            type = TileType.GRASS;
        }

        switch (type) {
            case GRASS:
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

    private static boolean isTower(TileType type) {
        return type == TileType.PRINCESS_TOWER_USER ||
                type == TileType.PRINCESS_TOWER_COMPUTER ||
                type == TileType.KING_TOWER_USER ||
                type == TileType.KING_TOWER_COMPUTER;
    }
}
