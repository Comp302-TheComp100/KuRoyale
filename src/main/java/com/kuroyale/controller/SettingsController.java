package com.kuroyale.controller;

import java.io.IOException;
import com.kuroyale.model.logic.SettingsModel;
import com.kuroyale.util.SoundEffectUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;

public class SettingsController {

    @FXML
    private StackPane root;
    @FXML
    private Slider musicSlider;
    @FXML
    private Slider sfxSlider;
    @FXML
    private CheckBox buttonSoundsCheckBox;
    @FXML
    private Button backButton;

    private final SettingsModel model = new SettingsModel();
    private final com.kuroyale.util.SceneLoader sceneLoader = new com.kuroyale.util.SceneLoader();

    @FXML
    private void initialize() {
        // Initialize sliders with current values from Model
        musicSlider.setValue(model.getMusicVolume());
        sfxSlider.setValue(model.getSFXVolume());
        buttonSoundsCheckBox.setSelected(model.isButtonSoundsEnabled());

        // Add listeners to update Model when sliders change
        musicSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setMusicVolume(newVal.doubleValue());
        });

        sfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            model.setSFXVolume(newVal.doubleValue());
        });

        buttonSoundsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            model.setButtonSoundsEnabled(newVal);
        });

        // Add hover effects to back button
        addHoverEffects(backButton);
    }

    private void addHoverEffects(Button button) {
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });

        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        try {
            sceneLoader.load(backButton, "/fxml/main-menu.fxml", "KU Royale", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
