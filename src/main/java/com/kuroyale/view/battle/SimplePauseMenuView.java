package com.kuroyale.view.battle;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Simplified Pause Menu for PvP battles.
 * Only provides Resume and Exit options (no save functionality).
 * 
 * Uses Strategy Pattern: Different pause menu for different game modes.
 */
public class SimplePauseMenuView extends VBox {

    public interface SimplePauseMenuListener {
        void onResume();

        void onExit();
    }

    public SimplePauseMenuView(SimplePauseMenuListener listener) {
        super(30);
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.setAlignment(Pos.CENTER);
        this.getStyleClass().add("pause-menu");
        this.setMaxSize(500, 300);

        Label title = new Label("PAUSED");
        title.getStyleClass().add("pause-menu-title");

        Button resumeBtn = createMenuButton("RESUME", () -> {
            if (listener != null)
                listener.onResume();
        });

        Button exitBtn = createMenuButton("EXIT", () -> {
            if (listener != null)
                listener.onExit();
        });

        this.getChildren().addAll(title, resumeBtn, exitBtn);
    }

    private Button createMenuButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.getStyleClass().add("pause-menu-button");
        btn.setOnAction(e -> action.run());
        return btn;
    }
}
