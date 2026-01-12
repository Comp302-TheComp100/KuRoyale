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
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("pause-menu");
        this.setMaxSize(500, 400);

        Label title = new Label("PAUSED");
        title.getStyleClass().add("pause-menu-title");

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
        exitBtn.getStyleClass().add("pause-menu-button-small");

        this.getChildren().addAll(title, resumeBtn, saveResumeBtn, saveExitBtn, exitBtn);
    }

    private Button createMenuButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("pause-menu-button");
        btn.setOnAction(e -> action.run());
        return btn;
    }
}
