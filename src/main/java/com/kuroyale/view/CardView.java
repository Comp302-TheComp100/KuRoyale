package com.kuroyale.view;

import com.kuroyale.model.entities.Card;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

//Simple card view component showing only image, name, and cost
public class CardView extends StackPane {
    private final Card card;

    public CardView(Card card) {
        this.card = card;

        // Set preferred size (same as deck slots)
        setPrefSize(90, 120);
        setMinSize(90, 120);
        setMaxSize(90, 120);
        getStyleClass().add("card-view");
        
        String rarityColor = getRarityColor();
        setStyle("-fx-border-color: " + rarityColor + "; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8;");
        
        // Add programmatic hover effects for scale transforms
        setOnMouseEntered(e -> {
            setScaleX(1.02);
            setScaleY(1.02);
        });
        
        setOnMouseExited(e -> {
            setScaleX(1.0);
            setScaleY(1.0);
        });

        // Card image fills entire space
        ImageView imageView = createCardImage();

        // Elixir icon and cost overlaid on top left corner
        StackPane elixirPane = createElixirCost();
        StackPane.setAlignment(elixirPane, Pos.TOP_LEFT);
        StackPane.setMargin(elixirPane, new Insets(-3, 0, 0, -3)); // Push into corner

        Label levelLabel = createLevelIndicator();
        StackPane.setAlignment(levelLabel, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(levelLabel, new Insets(0, 3, 3, 0));

        getChildren().addAll(imageView, elixirPane, levelLabel);
    }

    private StackPane createElixirCost() {
        StackPane pane = new StackPane();
        pane.setAlignment(Pos.CENTER);
        pane.setMaxSize(35, 35);
        pane.setPrefSize(35, 35);
        pane.setMinSize(35, 35);

        try {
            // Load elixir icon
            ImageView elixirIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/cost.png")));
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
        imageView.setFitWidth(90);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);

        try {
            String imagePath = card.getImagePath();
            Image image = new Image(getClass().getResourceAsStream(imagePath), 90, 120, false, true);
            imageView.setImage(image);
        } catch (Exception e) {
            // Image failed to load - return empty ImageView
        }
        return imageView;
    }

    private Label createLevelIndicator() {
        int level = card.getLevel();
        String stars = "";
        for (int i = 0; i < level; i++) {
            stars += "★";
        }
        
        Label levelLabel = new Label(stars);
        levelLabel.setStyle("-fx-font-size: 14px; " +
                "-fx-font-weight: bold; " +
                "-fx-text-fill: #fbbf24; " +
                "-fx-background-color: rgba(0, 0, 0, 0.6); " +
                "-fx-padding: 1 3 1 3; " +
                "-fx-background-radius: 3;");
        
        return levelLabel;
    }

    private String getRarityColor() {
        switch (card.getRarity()) {
            case COMMON:
                return "#9ca3af"; // Gray
            case RARE:
                return "#3b82f6"; // Blue
            case EPIC:
                return "#8b5cf6"; // Purple
            case LEGENDARY:
                return "#f59e0b"; // Orange/Gold
            default:
                return "#cbd5e1"; // Default gray
        }
    }

    public Card getCard() {
        return card;
    }

    public void refreshLevelIndicator() {
        // Level indicator is always the last child (index 2: imageView, elixirPane, levelLabel)
        if (getChildren().size() >= 3) {
            javafx.scene.Node lastChild = getChildren().get(getChildren().size() - 1);
            if (lastChild instanceof Label) {
                Label levelLabel = (Label) lastChild;
                int level = card.getLevel();
                String stars = "";
                for (int i = 0; i < level; i++) {
                    stars += "★";
                }
                levelLabel.setText(stars);
            }
        }
    }
}
