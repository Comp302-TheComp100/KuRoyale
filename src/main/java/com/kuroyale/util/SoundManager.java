package com.kuroyale.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton class to manage sound effects (preloading and playback).
 * Replaces on-the-fly Media creation.
 */
public class SoundManager {
    private static SoundManager instance;
    private final Map<String, Media> soundCache = new HashMap<>();

    private SoundManager() {
        // Preload common sounds
        preload("button_click", "/sfx/button_click.mp3");
        preload("combo", "/musics/combo.mp3");
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * Preloads a sound into the cache.
     * 
     * @param id           Unique identifier for the sound
     * @param resourcePath Classpath to the resource (e.g. "/sfx/sound.mp3")
     */
    public void preload(String id, String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url != null) {
                Media media = new Media(url.toExternalForm());
                soundCache.put(id, media);
            } else {
                System.err.println("Sound resource not found: " + resourcePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Plays a preloaded sound.
     * 
     * @param id Sound identifier
     */
    public void play(String id) {
        if (!AudioManager.getInstance().isButtonSoundsEnabled()) {
            // Respect global "button sounds" or general SFX mute setting.
            // Usually we'd check getSFXVolume(), but checking enabled flag is a quick
            // check.
            if (AudioManager.getInstance().getSFXVolume() <= 0)
                return;
        }

        Media media = soundCache.get(id);
        if (media != null) {
            try {
                MediaPlayer player = new MediaPlayer(media);
                player.setVolume(AudioManager.getInstance().getSFXVolume());
                player.play();

                // Dispose player on finish to free resources
                player.setOnEndOfMedia(() -> {
                    player.dispose();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
