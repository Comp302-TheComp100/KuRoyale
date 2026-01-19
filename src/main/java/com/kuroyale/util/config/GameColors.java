package com.kuroyale.util.config;

import javafx.scene.paint.Color;

public class GameColors {
    // Terrain
    public static final Color GRASS_LIGHT = Color.rgb(124, 252, 0); // LawnGreen
    public static final Color GRASS_DARK = Color.rgb(50, 205, 50); // LimeGreen
    public static final Color WATER = Color.LIGHTBLUE;
    public static final Color BRIDGE = Color.SADDLEBROWN;
    public static final Color ROAD = Color.SANDYBROWN;
    public static final Color DEFAULT = Color.GRAY;

    // Teams
    public static final Color PLAYER_TEAM = Color.ROYALBLUE;
    public static final Color ENEMY_TEAM = Color.CRIMSON;

    // Towers (Fallback/Minimap)
    public static final Color TOWER_PRINCESS_USER = Color.HOTPINK;
    public static final Color TOWER_PRINCESS_ENEMY = Color.DEEPPINK;
    public static final Color TOWER_KING_USER = Color.GOLD;
    public static final Color TOWER_KING_ENEMY = Color.ORANGE;

    // Projectiles
    public static final Color PROJECTILE_USER = Color.LIGHTSKYBLUE;
    public static final Color PROJECTILE_ENEMY = Color.ORANGERED;
    public static final Color PROJECTILE_STROKE = Color.color(0, 0, 0, 0.45);

    // UI Elements
    public static final Color HEALTH_BAR_BG = Color.DARKBLUE;
    public static final Color HEALTH_BAR_STROKE = Color.BLACK;
    public static final Color TEXT_FILL = Color.WHITE;
    public static final Color TEXT_STROKE = Color.BLACK;
    public static final Color ELIXIR_BAR = Color.MAGENTA;

    // Overlays
    public static final Color HIGHLIGHT_VALID = Color.rgb(255, 215, 0, 0.3);
    public static final Color HIGHLIGHT_VALID_P2 = Color.rgb(255, 100, 100, 0.3);
    public static final Color HOVER_FILL = Color.color(0.0, 0.8, 1.0, 0.25);
    public static final Color HOVER_STROKE = Color.CYAN;

    // Spell Effects - More visible with higher opacity
    public static final Color AOE_PLAYER = Color.color(0.2, 0.6, 1.0, 0.30);
    public static final Color AOE_ENEMY = Color.color(1.0, 0.3, 0.2, 0.30);
    public static final Color AOE_STROKE = Color.color(1, 1, 1, 0.6);

    private GameColors() {
    }
}
