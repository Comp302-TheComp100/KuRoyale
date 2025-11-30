package com.kuroyale.view;

import com.kuroyale.model.ElixirManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Visual elixir meter.
 * Shows current/max elixir and an animated progress bar.
 */
public class ElixirBar extends VBox {
    private final ProgressBar progressBar;
    private final Label elixirLabel;
    private final Label doubleElixirLabel;
    private final ElixirManager elixirManager;

    public ElixirBar(ElixirManager elixirManager) {
        this.elixirManager = elixirManager;

        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);
        this.getStyleClass().add("elixir-bar-container");

        // HBox to contain label and x2 indicator
        HBox labelContainer = new HBox(10);
        labelContainer.setAlignment(Pos.CENTER);

        // Label: "7 / 10"
        this.elixirLabel = new Label();
        this.elixirLabel.getStyleClass().add("elixir-label");
        this.elixirLabel.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        // x2 indicator (visible by default now)
        this.doubleElixirLabel = new Label("x1");
        this.doubleElixirLabel.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 24px; -fx-effect: dropshadow(one-pass-box, black, 3, 0.8, 0, 0);");
        this.doubleElixirLabel.setVisible(true);

        labelContainer.getChildren().addAll(elixirLabel, doubleElixirLabel);

        // Progress Bar
        this.progressBar = new ProgressBar(0);
        this.progressBar.setPrefWidth(200);
        this.progressBar.setPrefHeight(20);
        this.progressBar.getStyleClass().add("elixir-progress-bar");
        // Style the bar color to pink like Clash Royale
        this.progressBar.setStyle("-fx-accent: #FF00FF;");

        this.getChildren().addAll(labelContainer, progressBar);

        update();
    }

    public void setDoubleElixirActive(boolean active) {
        // doubleElixirLabel.setVisible(active); // No longer hiding it
    }

    public void update() {
        double current = elixirManager.getCurrentElixir();
        double max = ElixirManager.MAX_ELIXIR;

        // Update label
        elixirLabel.setText(String.format("%d / %d", (int) current, (int) max));

        // Update bar
        progressBar.setProgress(current / max);

        // Update speed indicator
        if (elixirManager.isDoubleElixir()) {
            doubleElixirLabel.setText("x2");
            doubleElixirLabel.setStyle(
                    "-fx-font-weight: bold; -fx-text-fill: #FF00FF; -fx-font-size: 24px; -fx-effect: dropshadow(one-pass-box, black, 3, 0.8, 0, 0);");
        } else {
            doubleElixirLabel.setText("x1");
            doubleElixirLabel.setStyle(
                    "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 24px; -fx-effect: dropshadow(one-pass-box, black, 3, 0.8, 0, 0);");
        }
    }
}
