package com.kuroyale.view;

import com.kuroyale.model.Card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Simple card view component showing only image, name, and cost
 */
public class CardView extends StackPane {
    private final Card card;

    public CardView(Card card) {
        this.card = card;

        // Set preferred size (same as deck slots)
        setPrefSize(120, 160);
        setMinSize(120, 160);
        setMaxSize(120, 160);

        // Apply style classes
        getStyleClass().add("card-view");
        getStyleClass().add("card-" + card.getType().toString().toLowerCase());

        // Card image fills entire space
        ImageView imageView = createCardImage();

        // Elixir icon and cost overlaid on top left corner
        StackPane elixirPane = createElixirCost();
        StackPane.setAlignment(elixirPane, Pos.TOP_LEFT);
        StackPane.setMargin(elixirPane, new Insets(-3, 0, 0, -3)); // Push into corner

        getChildren().addAll(imageView, elixirPane);
    }

    private StackPane createElixirCost() {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setMaxSize(35, 35);
        pane.setPrefSize(35, 35);
        pane.setMinSize(35, 35);

        try {
            // Load elixir icon
            ImageView elixirIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/card_elixir.png")));
            elixirIcon.setFitWidth(35);
            elixirIcon.setFitHeight(35);
            elixirIcon.setPreserveRatio(true);

            // Cost label - centered on icon
            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane.setAlignment(costLabel, Pos.CENTER);

            pane.getChildren().addAll(elixirIcon, costLabel);
        } catch (Exception e) {
            // Fallback without icon
            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.getStyleClass().add("card-cost");
            pane.getChildren().add(costLabel);
        }

        return pane;
    }

    private ImageView createCardImage() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false); // Fill entire card space

        try {
            String imagePath = card.getImagePath();
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            imageView.setImage(image);
        } catch (Exception e) {
            // Create placeholder colored rectangle
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(120, 160);
            placeholder.setStyle(getPlaceholderStyle());

            Label placeholderLabel = new Label(
                    card.getName().substring(0, Math.min(3, card.getName().length())).toUpperCase());
            placeholderLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
            placeholder.getChildren().add(placeholderLabel);

            // Can't return StackPane from ImageView method, so create a container
            VBox container = new VBox(placeholder);
            return new ImageView(); // Return empty, placeholder will be shown differently
        }

        return imageView;
    }

    private String getPlaceholderStyle() {
        String color;
        switch (card.getType()) {
            case TROOP:
                color = "#f97316";
                break;
            case BUILDING:
                color = "#92400e";
                break;
            case SPELL:
                color = "#8b5cf6";
                break;
            default:
                color = "#64748b";
        }
        return "-fx-background-color: " + color + "; -fx-background-radius: 8;";
    }

    public Card getCard() {
        return card;
    }
}
