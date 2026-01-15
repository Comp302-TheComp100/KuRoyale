package com.kuroyale.util;

import java.util.prefs.Preferences;

public class SettingsManager {
    private static final String PREF_MUSIC_VOLUME = "music_volume";
    private static final String PREF_SFX_VOLUME = "sfx_volume";
    private static final String PREF_BUTTON_SOUNDS = "button_sounds_enabled";

    private static final Preferences prefs = Preferences.userNodeForPackage(SettingsManager.class);

    // Private constructor to prevent instantiation
    private SettingsManager() {
    }

    public static double getMusicVolume() {
        return prefs.getDouble(PREF_MUSIC_VOLUME, 0.5); // Default 0.5
    }

    public static void setMusicVolume(double volume) {
        prefs.putDouble(PREF_MUSIC_VOLUME, volume);
    }

    public static double getSFXVolume() {
        return prefs.getDouble(PREF_SFX_VOLUME, 0.5); // Default 0.5
    }

    public static void setSFXVolume(double volume) {
        prefs.putDouble(PREF_SFX_VOLUME, volume);
    }

    public static boolean isButtonSoundsEnabled() {
        return prefs.getBoolean(PREF_BUTTON_SOUNDS, true); // Default true
    }

    public static void setButtonSoundsEnabled(boolean enabled) {
        prefs.putBoolean(PREF_BUTTON_SOUNDS, enabled);
    }
}
