package com.kuroyale.controller;

import java.io.IOException;

import com.kuroyale.util.AudioManager;
import com.kuroyale.util.SoundEffectUtil;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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

    @FXML
    private void initialize() {
        // Initialize sliders with current values from AudioManager
        AudioManager audioManager = AudioManager.getInstance();
        musicSlider.setValue(audioManager.getMusicVolume());
        sfxSlider.setValue(audioManager.getSFXVolume());
        buttonSoundsCheckBox.setSelected(audioManager.isButtonSoundsEnabled());

        // Add listeners to update AudioManager when sliders change
        musicSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setMusicVolume(newVal.doubleValue());
        });

        sfxSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setSFXVolume(newVal.doubleValue());
        });

        buttonSoundsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setButtonSoundsEnabled(newVal);
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
