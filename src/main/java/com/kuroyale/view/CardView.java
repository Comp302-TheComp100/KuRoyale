package com.kuroyale.view;

import com.kuroyale.model.Card;
import com.kuroyale.util.StyleHelper;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

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

        // Apply card view style
        StyleHelper.applyCardViewStyle(this);

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
            costLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
            pane.getChildren().add(costLabel);
        }

        return pane;
    }

    private ImageView createCardImage() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(120);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false); // Fill entire card space
        imageView.setSmooth(true); // Better image quality

        try {
            String imagePath = card.getImagePath();
            Image image = new Image(getClass().getResourceAsStream(imagePath), 120, 160, false, true);
            imageView.setImage(image);
        } catch (Exception e) {
            // Image failed to load - return empty ImageView
            // The card will still display with elixir cost overlay
        }

        return imageView;
    }

    public Card getCard() {
        return card;
    }
}
