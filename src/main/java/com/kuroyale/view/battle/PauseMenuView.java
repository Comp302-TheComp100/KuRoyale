package com.kuroyale.view.battle;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Custom View component for the Pause Menu in Battle.
 */
public class PauseMenuView extends VBox {

    public interface PauseMenuListener {
        void onResume();

        void onSaveAndResume();

        void onSaveAndExit();

        void onExitWithoutSaving();
    }

    public PauseMenuView(PauseMenuListener listener) {
        super(30);
        this.setAlignment(Pos.CENTER);
        this.setStyle(
                "-fx-background-color: #2a2a2a; -fx-padding: 50; -fx-background-radius: 20; -fx-border-color: white; -fx-border-width: 3;");
        this.setMaxSize(500, 400);

        Label title = new Label("PAUSED");
        title.setStyle("-fx-font-size: 42px; -fx-text-fill: white; -fx-font-weight: bold;");

        Button resumeBtn = createMenuButton("RESUME", () -> {
            if (listener != null)
                listener.onResume();
        });
        Button saveResumeBtn = createMenuButton("SAVE & RESUME", () -> {
            if (listener != null)
                listener.onSaveAndResume();
        });
        Button saveExitBtn = createMenuButton("SAVE & EXIT", () -> {
            if (listener != null)
                listener.onSaveAndExit();
        });
        Button exitBtn = createMenuButton("EXIT WITHOUT SAVING", () -> {
            if (listener != null)
                listener.onExitWithoutSaving();
        });

        // Slightly smaller style for exit without saving
        exitBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 30; -fx-min-width: 300;");

        this.getChildren().addAll(title, resumeBtn, saveResumeBtn, saveExitBtn, exitBtn);
    }

    private Button createMenuButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-size: 20px; -fx-padding: 15 50; -fx-min-width: 300;");
        btn.setOnAction(e -> action.run());
        return btn;
    }
}
