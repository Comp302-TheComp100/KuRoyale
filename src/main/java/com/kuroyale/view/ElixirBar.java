package com.kuroyale.view;

import com.kuroyale.model.ElixirManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Visual elixir meter.
 * Shows current/max elixir and an animated progress bar.
 */
public class ElixirBar extends VBox {
    private final ProgressBar progressBar;
    private final Label elixirLabel;
    private final ElixirManager elixirManager;

    public ElixirBar(ElixirManager elixirManager) {
        this.elixirManager = elixirManager;

        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);
        this.getStyleClass().add("elixir-bar-container");

        // Label: "7 / 10"
        this.elixirLabel = new Label();
        this.elixirLabel.getStyleClass().add("elixir-label");
        this.elixirLabel.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        // Progress Bar
        this.progressBar = new ProgressBar(0);
        this.progressBar.setPrefWidth(200);
        this.progressBar.setPrefHeight(20);
        this.progressBar.getStyleClass().add("elixir-progress-bar");
        // Style the bar color to purple/magenta like Clash Royale
        this.progressBar.setStyle("-fx-accent: #D000D0;");

        this.getChildren().addAll(elixirLabel, progressBar);

        update();
    }

    public void update() {
        double current = elixirManager.getCurrentElixir();
        double max = ElixirManager.MAX_ELIXIR;

        // Update label
        elixirLabel.setText(String.format("%d / %d", (int) current, (int) max));

        // Update bar
        progressBar.setProgress(current / max);
    }
}
