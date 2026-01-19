package com.kuroyale.view.menu;

import com.kuroyale.model.dto.SavedGameState;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Custom View component for a Saved Game Entry.
public class SavedGameEntryView extends HBox {

        public interface SavedGameListener {
                void onLoad();

                void onDelete();
        }

        public SavedGameEntryView(SavedGameState savedGame, SavedGameListener listener) {
                super(20);
                this.setAlignment(Pos.CENTER_LEFT);
                this.setPrefHeight(120);

                String baseStyle = "-fx-background-color: rgba(50, 50, 50, 0.9); " +
                                "-fx-padding: 20; " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: #888; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 10;";
                String hoverStyle = "-fx-background-color: rgba(70, 70, 70, 0.9); " +
                                "-fx-padding: 20; " +
                                "-fx-background-radius: 10; " +
                                "-fx-border-color: #4CAF50; " +
                                "-fx-border-width: 2; " +
                                "-fx-border-radius: 10;";

                this.setStyle(baseStyle);
                this.setOnMouseEntered(e -> this.setStyle(hoverStyle));
                this.setOnMouseExited(e -> this.setStyle(baseStyle));

                VBox infoBox = new VBox(8);
                infoBox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                Label dateLabel = new Label("Saved: " + savedGame.getFormattedSaveTime());
                dateLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #aaaaaa;");

                Label playerLabel = new Label("Player: " + savedGame.getPlayerUsername());
                playerLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white; -fx-font-weight: bold;");

                Label timeLabel = new Label("Time Remaining: " + savedGame.getFormattedTimeRemaining());
                timeLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4CAF50;");

                Label scoreLabel = new Label(
                                String.format("Score: %d - %d", savedGame.getPlayerScore(), savedGame.getBotScore()));
                scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

                Label elixirLabel = new Label(String.format("Elixir: %.1f/10", savedGame.getPlayerElixir()));
                elixirLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #bb86fc;");

                Label unitsLabel = new Label(
                                String.format("Units: %d troops, %d buildings", savedGame.getActiveTroops().size(),
                                                savedGame.getActiveBuildings().size()));
                unitsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffab40;");

                infoBox.getChildren().addAll(dateLabel, playerLabel, timeLabel, scoreLabel, elixirLabel, unitsLabel);

                VBox buttonBox = new VBox(10);
                buttonBox.setAlignment(Pos.CENTER);
                buttonBox.setMinWidth(200);

                Button loadButton = new Button("LOAD GAME");
                loadButton.setStyle(
                                "-fx-font-size: 16px; -fx-padding: 10 30; -fx-background-color: #4CAF50; -fx-text-fill: white;");
                loadButton.setPrefWidth(180);
                loadButton.setOnAction(e -> {
                        if (listener != null)
                                listener.onLoad();
                });

                Button deleteButton = new Button("🗑️ DELETE");
                String deleteBaseStyle = "-fx-font-size: 14px; -fx-padding: 8 30; -fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;";
                String deleteHoverStyle = "-fx-font-size: 14px; -fx-padding: 8 30; -fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-color: #b71c1c; -fx-border-width: 1;";
                deleteButton.setStyle(deleteBaseStyle);
                deleteButton.setPrefWidth(180);
                deleteButton.setOnMouseEntered(e -> deleteButton.setStyle(deleteHoverStyle));
                deleteButton.setOnMouseExited(e -> deleteButton.setStyle(deleteBaseStyle));
                deleteButton.setOnAction(e -> {
                        if (listener != null)
                                listener.onDelete();
                });

                buttonBox.getChildren().addAll(loadButton, deleteButton);
                this.getChildren().addAll(infoBox, buttonBox);
        }
}
