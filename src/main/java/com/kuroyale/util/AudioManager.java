package com.kuroyale.util;

import javafx.scene.media.MediaPlayer;

//Singleton class to manage audio volumes across the application.
public class AudioManager {
    private static AudioManager instance;

    private double musicVolume = 0.5;
    private double sfxVolume = 0.5;
    private boolean buttonSoundsEnabled = true;

    private MediaPlayer currentMusicPlayer;

    private AudioManager() {
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
        if (currentMusicPlayer != null) {
            currentMusicPlayer.setVolume(this.musicVolume);
        }
    }

    public double getSFXVolume() {
        return sfxVolume;
    }

    public void setSFXVolume(double volume) {
        this.sfxVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public boolean isButtonSoundsEnabled() {
        return buttonSoundsEnabled;
    }

    public void setButtonSoundsEnabled(boolean enabled) {
        this.buttonSoundsEnabled = enabled;
    }

    public void registerMusicPlayer(MediaPlayer player) {
        this.currentMusicPlayer = player;
        if (player != null) {
            player.setVolume(musicVolume);
        }
    }
}
