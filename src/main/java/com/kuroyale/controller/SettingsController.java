package com.kuroyale.controller;

import java.io.IOException;
import com.kuroyale.model.SettingsModel;
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

    @FXML private StackPane root;
    @FXML private Slider musicSlider;
    @FXML private Slider sfxSlider;
    @FXML private CheckBox buttonSoundsCheckBox;
    @FXML private Button backButton;

    private final SettingsModel model = new SettingsModel();

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