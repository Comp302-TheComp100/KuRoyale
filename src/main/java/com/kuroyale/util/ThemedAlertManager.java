package com.kuroyale.util;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ThemedAlertManager {

    @FXML
    private Label titleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button okButton;
    @FXML
    private Button cancelButton;

    private Stage stage;

    @FXML
    private void initialize() {
        // Sound effect on hover
        if (okButton != null) {
            setupButtonHover(okButton);
        }
        if (cancelButton != null) {
            setupButtonHover(cancelButton);
        }
    }

    private void setupButtonHover(Button button) {
        button.setOnMouseEntered(e -> {
            button.setScaleX(1.05);
            button.setScaleY(1.05);
        });
        button.setOnMouseExited(e -> {
            button.setScaleX(1.0);
            button.setScaleY(1.0);
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTitle(String title) {
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    public void setMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }

    public void setConfirmationMode(boolean confirmationMode) {
        if (cancelButton != null) {
            cancelButton.setVisible(confirmationMode);
            cancelButton.setManaged(confirmationMode);
        }
    }

    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void handleClose() {
        SoundEffectUtil.playButtonClick();
        if (stage != null) {
            stage.close();
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    private void handleCancel() {
        SoundEffectUtil.playButtonClick();
        if (stage != null) {
            stage.close();
        }
        // Cancel does NOT run onClose callback
    }

    /**
     * Shows a themed error alert.
     * 
     * @param title   The title of the alert
     * @param message The message to display
     */
    public static void show(String title, String message) {
        show(null, title, message, null);
    }

    /**
     * Shows a themed error alert with a callback.
     * 
     * @param title   The title of the alert
     * @param message The message to display
     * @param onClose The action to run when the alert is closed
     */
    public static void show(String title, String message, Runnable onClose) {
        show(null, title, message, onClose);
    }

    /**
     * Shows a themed error alert with a callback, centered on the owner window.
     * 
     * @param owner   The owner window of the alert
     * @param title   The title of the alert
     * @param message The message to display
     * @param onClose The action to run when the alert is closed
     */
    public static void show(javafx.stage.Window owner, String title, String message, Runnable onClose) {
        showInternal(owner, title, message, onClose, false);
    }

    /**
     * Shows a themed confirmation alert with OK and Cancel buttons.
     * 
     * @param title     The title of the alert
     * @param message   The message to display
     * @param onConfirm The action to run when the user confirms (clicks OK)
     */
    public static void showConfirmation(String title, String message, Runnable onConfirm) {
        showConfirmation(null, title, message, onConfirm);
    }

    /**
     * Shows a themed confirmation alert with OK and Cancel buttons, centered on the
     * owner window.
     * 
     * @param owner     The owner window of the alert
     * @param title     The title of the alert
     * @param message   The message to display
     * @param onConfirm The action to run when the user confirms (clicks OK)
     */
    public static void showConfirmation(javafx.stage.Window owner, String title, String message, Runnable onConfirm) {
        showInternal(owner, title, message, onConfirm, true);
    }

    private static void showInternal(javafx.stage.Window owner, String title, String message, Runnable onClose,
            boolean isConfirmation) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    ThemedAlertManager.class.getResource("/fxml/components/themed_alert.fxml"));
            Parent root = loader.load();
            ThemedAlertManager controller = loader.getController();

            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setTitle(title);
            controller.setMessage(message);
            controller.setOnClose(onClose);
            controller.setConfirmationMode(isConfirmation);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            // If owner is null, try to find the active window
            javafx.stage.Window alertOwner = owner;
            if (alertOwner == null) {
                alertOwner = javafx.stage.Window.getWindows().stream()
                        .filter(javafx.stage.Window::isShowing)
                        .findFirst()
                        .orElse(null);
            }

            if (alertOwner != null) {
                stage.initOwner(alertOwner);
                // Center relative to owner after stage is shown to get correct dimensions
                final javafx.stage.Window finalOwner = alertOwner;
                stage.setOnShown(e -> {
                    double x = finalOwner.getX() + (finalOwner.getWidth() - stage.getWidth()) / 2;
                    double y = finalOwner.getY() + (finalOwner.getHeight() - stage.getHeight()) / 2;
                    stage.setX(x);
                    stage.setY(y);
                });
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            if (alertOwner == null) {
                stage.centerOnScreen();
            }

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to system alert if something fails
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    isConfirmation ? javafx.scene.control.Alert.AlertType.CONFIRMATION
                            : javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);

            javafx.stage.Window alertOwner = owner;
            if (alertOwner == null) {
                alertOwner = javafx.stage.Window.getWindows().stream()
                        .filter(javafx.stage.Window::isShowing)
                        .findFirst()
                        .orElse(null);
            }

            if (alertOwner != null) {
                alert.initOwner(alertOwner);
            }
            java.util.Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK && onClose != null) {
                onClose.run();
            }
        }
    }
}
