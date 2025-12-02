package com.kuroyale.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/*Utility class for playing sound effects throughout the application
 * Maintains a single MediaPlayer instance to prevent overlapping sounds */
public class SoundEffectUtil {

    private static MediaPlayer buttonClickPlayer;
    private static Media buttonClickMedia;

    /*Plays the button click sound effect
     * Stops any currently playing sound before starting a new one to prevent overlapping */
    public static void playButtonClick() {
        AudioManager audioManager = AudioManager.getInstance();
        if (!audioManager.isButtonSoundsEnabled()) {
            return;
        }

        try {
            // create Media and MediaPlayer on first use
            if (buttonClickMedia == null) {
                String soundPath = SoundEffectUtil.class.getResource("/sfx/button_click.mp3").toExternalForm();
                buttonClickMedia = new Media(soundPath);
                buttonClickPlayer = new MediaPlayer(buttonClickMedia);
            }

            // Set volume from AudioManager
            buttonClickPlayer.setVolume(audioManager.getSFXVolume());
            // Always stop the player regardless of status to fix delay issues
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
