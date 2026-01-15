package com.kuroyale.view;

import com.kuroyale.util.SoundEffectUtil;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ThemedAlertController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Button okButton;

    private Stage stage;

    @FXML
    private void initialize() {
        // Sound effect on hover
        if (okButton != null) {
            okButton.setOnMouseEntered(e -> {
                okButton.setScaleX(1.05);
                okButton.setScaleY(1.05);
            });
            okButton.setOnMouseExited(e -> {
                okButton.setScaleX(1.0);
                okButton.setScaleY(1.0);
            });
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    ThemedAlertController.class.getResource("/fxml/components/themed_alert.fxml"));
            Parent root = loader.load();
            ThemedAlertController controller = loader.getController();

            Stage stage = new Stage();
            controller.setStage(stage);
            controller.setTitle(title);
            controller.setMessage(message);
            controller.setOnClose(onClose);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.TRANSPARENT);

            if (owner != null) {
                stage.initOwner(owner);
                // Center relative to owner after stage is shown to get correct dimensions
                stage.setOnShown(e -> {
                    double x = owner.getX() + (owner.getWidth() - stage.getWidth()) / 2;
                    double y = owner.getY() + (owner.getHeight() - stage.getHeight()) / 2;
                    stage.setX(x);
                    stage.setY(y);
                });
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            if (owner == null) {
                stage.centerOnScreen();
            }

            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to system alert if something fails
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);
            if (owner != null) {
                alert.initOwner(owner);
            }
            alert.showAndWait();
            if (onClose != null) {
                onClose.run();
            }
        }
    }
}
