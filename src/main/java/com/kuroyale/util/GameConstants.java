package com.kuroyale.util;

public class GameConstants {
    // Grid Dimensions
    public static final int TILE_SIZE = 18;

    // Tower Dimensions
    public static final int PRINCESS_TOWER_SIZE = 3;
    public static final int KING_TOWER_SIZE = 4;

    // Tower Health
    public static final double PRINCESS_TOWER_HEALTH = 1400.0;
    public static final double KING_TOWER_HEALTH = 2400.0;

    // Limits
    public static final int MAX_BRIDGES = 3;
    public static final int BRIDGE_WIDTH = 2; // 2x2
    public static final int MAX_BRIDGE_TILES = 12; // 3 bridges * 4 tiles
    public static final int MAX_PRINCESS_TOWERS = 2;
    public static final int MAX_KING_TOWERS = 1;

    // Map Boundaries & Positions
    public static final int RIVER_ROW_1 = 15;
    public static final int RIVER_ROW_2 = 16;
    public static final int USER_SIDE_BOUNDARY_Y = 16; // Towers must be > 16

    // Colors (can be used if needed, though often in view)

    private GameConstants() {
        // Prevent instantiation
    }
}
