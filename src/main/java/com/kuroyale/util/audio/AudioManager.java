package com.kuroyale.util.audio;

import com.kuroyale.util.common.SettingsManager;

import javafx.scene.media.MediaPlayer;

//Singleton class to manage audio volumes across the application.
public class AudioManager {
    private static AudioManager instance;

    private double musicVolume;
    private double sfxVolume;
    private boolean buttonSoundsEnabled;

    private MediaPlayer currentMusicPlayer;

    private AudioManager() {
        // Load settings on initialization
        musicVolume = SettingsManager.getMusicVolume();
        sfxVolume = SettingsManager.getSFXVolume();
        buttonSoundsEnabled = SettingsManager.isButtonSoundsEnabled();
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(double volume) {
        this.musicVolume = Math.max(0.0, Math.min(1.0, volume));
        SettingsManager.setMusicVolume(this.musicVolume); // Save
        if (currentMusicPlayer != null) {
            currentMusicPlayer.setVolume(this.musicVolume);
        }
    }

    public double getSFXVolume() {
        return sfxVolume;
    }

    public void setSFXVolume(double volume) {
        this.sfxVolume = Math.max(0.0, Math.min(1.0, volume));
        SettingsManager.setSFXVolume(this.sfxVolume); // Save
    }

    public boolean isButtonSoundsEnabled() {
        return buttonSoundsEnabled;
    }

    public void setButtonSoundsEnabled(boolean enabled) {
        this.buttonSoundsEnabled = enabled;
        SettingsManager.setButtonSoundsEnabled(enabled); // Save
    }

    public void registerMusicPlayer(MediaPlayer player) {
        this.currentMusicPlayer = player;
        if (player != null) {
            player.setVolume(musicVolume);
            // Ensure loop
            player.setCycleCount(MediaPlayer.INDEFINITE);
        }
    }
}
