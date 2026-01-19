package com.kuroyale.view.battle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;

/**
 * Emoji selection panel with a 4x2 grid layout.
 * Displays 8 emojis that can be clicked to play.
 * Opens downward when emoji button is clicked.
 */
public class EmojiPanel extends VBox {

    private static final int EMOJI_SIZE = 65;
    private static final int GRID_GAP = 10;
    private static final int PADDING = 12;

    // Emoji names matching the files in /gifs/emojis/
    private static final String[] EMOJI_NAMES = {
            "emoji_1",
            "emoji_2",
            "emoji_3",
            "emoji_4",
            "king emote",
            "sticker emote",
            "yanan emoji",
            "ağlayan kral emoji"
    };

    private Consumer<String> onEmojiSelected;

    public EmojiPanel() {
        getStyleClass().add("emoji-panel");
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(PADDING));
        setSpacing(5);
        setVisible(false); // Hidden by default

        // Create grid for emojis (4x2)
        GridPane emojiGrid = new GridPane();
        emojiGrid.setHgap(GRID_GAP);
        emojiGrid.setVgap(GRID_GAP);
        emojiGrid.setAlignment(Pos.CENTER);

        // Load and add 8 emojis (4 rows, 2 columns)
        for (int i = 0; i < EMOJI_NAMES.length; i++) {
            String emojiName = EMOJI_NAMES[i];
            StackPane emojiItem = createEmojiItem(emojiName);

            int row = i / 2;
            int col = i % 2;
            emojiGrid.add(emojiItem, col, row);
        }

        getChildren().add(emojiGrid);

        // Set preferred size (2 columns, 4 rows)
        double totalWidth = (EMOJI_SIZE * 2) + GRID_GAP + (PADDING * 2);
        double totalHeight = (EMOJI_SIZE * 4) + (GRID_GAP * 3) + (PADDING * 2);
        setPrefSize(totalWidth, totalHeight);
        setMaxSize(totalWidth, totalHeight);
    }

    private StackPane createEmojiItem(String emojiName) {
        StackPane container = new StackPane();
        container.getStyleClass().add("emoji-item");
        container.setPrefSize(EMOJI_SIZE, EMOJI_SIZE);

        try {
            // Try to load the emoji GIF
            String emojiPath = "/gifs/emojis/" + emojiName + ".gif";
            Image emojiImage = new Image(getClass().getResourceAsStream(emojiPath));

            ImageView imageView = new ImageView(emojiImage);
            imageView.setFitWidth(EMOJI_SIZE);
            imageView.setFitHeight(EMOJI_SIZE);
            imageView.setPreserveRatio(true);

            container.getChildren().add(imageView);

        } catch (Exception e) {
            // If GIF not found, show placeholder text
            javafx.scene.control.Label placeholder = new javafx.scene.control.Label(emojiName);
            placeholder.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
            container.getChildren().add(placeholder);
        }

        // Add click handler
        container.setOnMouseClicked(e -> {
            if (onEmojiSelected != null) {
                onEmojiSelected.accept(emojiName);
                setVisible(false); // Hide panel after selection
            }
        });

        // Hover effect
        container.setOnMouseEntered(e -> {
            container.setScaleX(1.1);
            container.setScaleY(1.1);
        });
        container.setOnMouseExited(e -> {
            container.setScaleX(1.0);
            container.setScaleY(1.0);
        });

        return container;
    }

    public void setOnEmojiSelected(Consumer<String> handler) {
        this.onEmojiSelected = handler;
    }

    public void toggle() {
        setVisible(!isVisible());
    }
}
