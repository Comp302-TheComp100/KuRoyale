package com.kuroyale.util.ui;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

/* Singleton class for managing and caching game assets (images, audio).
 * Prevents redundant loading of resources. */
public class GameAssets {
    private static GameAssets instance;

    // Image Cache
    private final Map<String, Image> imageCache = new HashMap<>();

    // Asset Keys
    public static final String PRINCESS_TOWER_USER = "princess_tower_user";
    public static final String PRINCESS_TOWER_COMPUTER = "princess_tower_computer";
    public static final String KING_TOWER_USER = "king_tower_user";
    public static final String KING_TOWER_COMPUTER = "king_tower_computer";

    // Asset Paths
    private static final String ATH_PRINCESS_TOWER_USER = "/images/tower_archer_blue.png";
    private static final String PATH_PRINCESS_TOWER_COMPUTER = "/images/tower_archer_red.png";
    private static final String PATH_KING_TOWER_USER = "/images/Clash_Royale_icon_King_Tower_Blue.png";
    private static final String PATH_KING_TOWER_COMPUTER = "/images/Clash_Royale_icon_King_Tower_Red.png";

    private GameAssets() {
        // Private constructor
    }

    public static synchronized GameAssets getInstance() {
        if (instance == null) {
            instance = new GameAssets();
        }
        return instance;
    }

    // Preloads core battle assets.
    public void loadBattleAssets() {
        getImage(PRINCESS_TOWER_USER, ATH_PRINCESS_TOWER_USER);
        getImage(PRINCESS_TOWER_COMPUTER, PATH_PRINCESS_TOWER_COMPUTER);
        getImage(KING_TOWER_USER, PATH_KING_TOWER_USER);
        getImage(KING_TOWER_COMPUTER, PATH_KING_TOWER_COMPUTER);
    }

    // Gets a cached image or loads it if not present.
    public Image getImage(String key, String path) {
        if (!imageCache.containsKey(key)) {
            try {
                if (path == null) {
                    System.err.println("Asset path missing for key: " + key);
                    return null;
                }
                Image img = new Image(getClass().getResourceAsStream(path));
                if (img.isError()) {
                    System.err.println("Failed to load image: " + path);
                } else {
                    imageCache.put(key, img);
                }
            } catch (Exception e) {
                System.err.println("Exception loading image " + path + ": " + e.getMessage());
                return null;
            }
        }
        return imageCache.get(key);
    }

    // Convenience methods for common assets
    public Image getPrincessTowerUser() {
        return getImage(PRINCESS_TOWER_USER, ATH_PRINCESS_TOWER_USER);
    }

    public Image getPrincessTowerComputer() {
        return getImage(PRINCESS_TOWER_COMPUTER, PATH_PRINCESS_TOWER_COMPUTER);
    }

    public Image getKingTowerUser() {
        return getImage(KING_TOWER_USER, PATH_KING_TOWER_USER);
    }

    public Image getKingTowerComputer() {
        return getImage(KING_TOWER_COMPUTER, PATH_KING_TOWER_COMPUTER);
    }
}
