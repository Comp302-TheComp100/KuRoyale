package com.kuroyale.view.battle;

import com.kuroyale.model.logic.ElixirManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/*Visual elixir meter.
 * Shows current/max elixir and an animated progress bar.*/
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
        this.setMinWidth(220); // Ensure it has a minimum width
        this.setMinHeight(60); // Ensure it has a minimum height

        // HBox to contain label and x2 indicator
        HBox labelContainer = new HBox(10);
        labelContainer.setAlignment(Pos.CENTER);

        // Elixir Label
        this.elixirLabel = new Label();
        this.elixirLabel.getStyleClass().add("elixir-label");
        this.elixirLabel.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 16px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.5, 0, 0);");

        // x2 indicator
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
        // Remove inline style that might conflict with CSS class
        // this.progressBar.setStyle("-fx-accent: #FF00FF;");

        this.getChildren().addAll(labelContainer, progressBar);
        update();
    }

    public void setDoubleElixirActive(boolean active) {
        doubleElixirLabel.setVisible(active);
    }

    private boolean lastDoubleElixirState = false;

    public void update() {
        double current = elixirManager.getCurrentElixir();
        double max = ElixirManager.MAX_ELIXIR;
        boolean isDoubleElixir = elixirManager.isDoubleElixir();

        // Optimized: Only update text if value changed integer-wise (simplification for
        // display)
        // or just update label text always (cheap), but style is expensive.
        // Let's update text always for smooth float changes, but style only on state
        // change.

        elixirLabel.setText(String.format("%d / %d", (int) current, (int) max));
        progressBar.setProgress(current / max);

        // Only update style if state changed
        if (isDoubleElixir != lastDoubleElixirState) {
            lastDoubleElixirState = isDoubleElixir;
            if (isDoubleElixir) {
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

    // Shows a +N indicator with purple text when elixir is produced (e.g. from
    // Elixir Collector)
    public void showProductionIndicator(int amount) {
        Label indicator = new Label("+" + amount);
        indicator.setStyle(
                "-fx-font-weight: bold; -fx-text-fill: #9932CC; -fx-font-size: 20px; -fx-effect: dropshadow(one-pass-box, black, 2, 0.8, 0, 0);");

        // Add to progressBar parent (this VBox)
        this.getChildren().add(indicator);

        // Animate: move up and fade out
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(1.0), indicator);
        tt.setByY(-30);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.seconds(1.0), indicator);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
        pt.setOnFinished(e -> this.getChildren().remove(indicator));
        pt.play();
    }
}
