package com.kuroyale.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * Utility class for playing sound effects throughout the application
 * Maintains a single MediaPlayer instance to prevent overlapping sounds
 */
public class SoundEffectUtil {

    private static MediaPlayer buttonClickPlayer;
    private static Media buttonClickMedia;

    /**
     * Plays the button click sound effect
     * Stops any currently playing sound before starting a new one to prevent overlapping
     * Always stops and restarts immediately to fix delay issues
     */
    public static void playButtonClick() {
        try {
            // Lazy initialization: create Media and MediaPlayer on first use
            if (buttonClickMedia == null) {
                String soundPath = SoundEffectUtil.class.getResource("/sfx/button_click.mp3").toExternalForm();
                buttonClickMedia = new Media(soundPath);
                buttonClickPlayer = new MediaPlayer(buttonClickMedia);
            }

            // Always stop the player regardless of status to fix delay issues
            // This ensures immediate restart when clicked again
            buttonClickPlayer.stop();

            // Reset to beginning and play immediately
            buttonClickPlayer.seek(buttonClickPlayer.getStartTime());
            buttonClickPlayer.play();
        } catch (Exception e) {
            // Silently fail if sound cannot be played
            e.printStackTrace();
        }
    }
}

