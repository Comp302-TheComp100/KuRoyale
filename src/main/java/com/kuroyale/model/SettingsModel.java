package com.kuroyale.model;

import com.kuroyale.util.AudioManager;

/*The Model component for the Settings screen.
 * Encapsulates business logic for application settings operations.*/
public class SettingsModel {
    
    private final AudioManager audioManager;
    
    public SettingsModel() {
        this.audioManager = AudioManager.getInstance();
    }
    
    //Gets the current music volume (0.0 to 1.0)
    public double getMusicVolume() {
        return audioManager.getMusicVolume();
    }
    
    //Sets the music volume (0.0 to 1.0)
    public void setMusicVolume(double volume) {
        audioManager.setMusicVolume(volume);
    }
    
    //Gets the current SFX volume (0.0 to 1.0)
    public double getSFXVolume() {
        return audioManager.getSFXVolume();
    }
    
    //Sets the SFX volume (0.0 to 1.0)
    public void setSFXVolume(double volume) {
        audioManager.setSFXVolume(volume);
    }
    
    //Checks if button sounds are enabled
    public boolean isButtonSoundsEnabled() {
        return audioManager.isButtonSoundsEnabled();
    }
    
    //Sets whether button sounds are enabled
    public void setButtonSoundsEnabled(boolean enabled) {
        audioManager.setButtonSoundsEnabled(enabled);
    }
}





