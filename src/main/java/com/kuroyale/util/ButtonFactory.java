package com.kuroyale.util;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

//Factory class for creating styled buttons with images & Centralizes button creation logic to avoid code duplication
public class ButtonFactory {

    private static final int BUTTON_WIDTH = 58;
    private static final int BUTTON_HEIGHT = 27;

    public enum ButtonType {
        INFO("/images/button_blue.png", "#3b82f6"),
        ACTION("/images/button_yellow.png", "#fbbf24"),
        REMOVE("/images/button_red.png", "#ef4444");

        private final String imagePath;
        private final String fallbackColor;

        ButtonType(String imagePath, String fallbackColor) {
            this.imagePath = imagePath;
            this.fallbackColor = fallbackColor;
        }

        public String getImagePath() {return imagePath;}
        public String getFallbackColor() {return fallbackColor;}
    }

    public static Button createButton(ButtonType type, String text) {
        Button button = new Button();
        button.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMaxSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        try {
            Image buttonImage = new Image(ButtonFactory.class.getResourceAsStream(type.getImagePath()));
            ImageView buttonImageView = new ImageView(buttonImage);
            buttonImageView.setFitWidth(BUTTON_WIDTH);
            buttonImageView.setFitHeight(BUTTON_HEIGHT);
            buttonImageView.setPreserveRatio(false);
            button.setGraphic(buttonImageView);

            // Make button transparent and show text on top
            button.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-border-width: 0;");
            button.setText(text);
            button.setTextFill(javafx.scene.paint.Color.WHITE);
            button.setFont(javafx.scene.text.Font.font("Clash", javafx.scene.text.FontWeight.BOLD, 8));
            button.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        } catch (Exception e) {
            // Fallback to colored background
            button.setText(text);
            button.setStyle(
                    "-fx-font-family: 'Clash'; -fx-font-size: 8px; -fx-font-weight: bold; -fx-text-fill: white; " +
                            "-fx-cursor: hand; -fx-background-color: " + type.getFallbackColor()
                            + "; -fx-background-radius: 5;");
        }

        return button;
    }
    //Creates a inforamtion button
    public static Button createInfoButton() {return createButton(ButtonType.INFO, "INFO");}
    //Creates a use button
    public static Button createUseButton() {return createButton(ButtonType.ACTION, "USE");}
    //Creates a replace button
    public static Button createReplaceButton() {return createButton(ButtonType.ACTION, "REPLACE");}
    //Creates a remove button
    public static Button createRemoveButton() {return createButton(ButtonType.REMOVE, "REMOVE");}
}