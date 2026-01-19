package com.kuroyale.view.battle.ui;

import com.kuroyale.model.logic.ElixirManager;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/*Visual elixir meter.
 * Shows current/max elixir and an animated progress bar.*/
public class ElixirBarView extends VBox {
    private final ProgressBar progressBar;
    private final Label elixirLabel;
    private final Label doubleElixirLabel;
    private final ElixirManager elixirManager;

    public ElixirBarView(ElixirManager elixirManager) {
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.elixirManager = elixirManager;

        this.setAlignment(Pos.CENTER);
        this.setSpacing(5);
        this.getStyleClass().add("elixir-bar-container");
        this.setMinWidth(220); // Ensure it has a minimum width
        this.setMaxWidth(220); // Ensure it does not stretch beyond minimum width
        this.setMinHeight(60); // Ensure it has a minimum height
        this.setMaxHeight(60); // Ensure it does not stretch beyond minimum height

        // HBox to contain label and x2 indicator
        HBox labelContainer = new HBox(10);
        labelContainer.setAlignment(Pos.CENTER);

        // Elixir Label
        this.elixirLabel = new Label();
        this.elixirLabel.getStyleClass().add("elixir-label");

        // x2 indicator
        this.doubleElixirLabel = new Label("x1");
        this.doubleElixirLabel.getStyleClass().add("double-elixir-label");
        this.doubleElixirLabel.setVisible(true);

        labelContainer.getChildren().addAll(elixirLabel, doubleElixirLabel);

        // Progress Bar
        this.progressBar = new ProgressBar(0);
        this.progressBar.getStyleClass().add("elixir-progress-bar");

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

        elixirLabel.setText(String.format("%d / %d", (int) current, (int) max));
        progressBar.setProgress(current / max);

        // Only update style if state changed
        if (isDoubleElixir != lastDoubleElixirState) {
            lastDoubleElixirState = isDoubleElixir;
            if (isDoubleElixir) {
                doubleElixirLabel.setText("x2");
                if (!doubleElixirLabel.getStyleClass().contains("double-elixir-active")) {
                    doubleElixirLabel.getStyleClass().add("double-elixir-active");
                }
            } else {
                doubleElixirLabel.setText("x1");
                doubleElixirLabel.getStyleClass().remove("double-elixir-active");
            }
        }
    }

    // Shows a +N indicator with purple text when elixir is produced (e.g. from
    // Elixir Collector)
    public void showProductionIndicator(int amount) {
        Label indicator = new Label("+" + amount);
        indicator.getStyleClass().add("production-indicator");

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