package com.kuroyale.view;

import com.kuroyale.model.Card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Custom JavaFX component for displaying a deck slot
 */
public class DeckSlotView extends StackPane {
    private Card card;
    private final VBox content;
    private final Label placeholderLabel;
    private final VBox cardContent;

    public DeckSlotView() {
        // Set size
        setPrefSize(120, 160);
        setMinSize(120, 160);
        setMaxSize(120, 160);

        // Apply empty slot style
        getStyleClass().add("deck-slot");

        // Create placeholder (+ icon)
        placeholderLabel = new Label("+");
        placeholderLabel.setFont(new Font(48));
        placeholderLabel.setStyle("-fx-text-fill: #94a3b8;");

        // Create card content container
        cardContent = new VBox(5);
        cardContent.setAlignment(Pos.CENTER);
        cardContent.setPadding(new Insets(10));
        cardContent.setVisible(false);

        content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(placeholderLabel, cardContent);

        getChildren().add(content);

        // Add hover effect
        setOnMouseEntered(e -> {
            if (card != null) {
                setStyle("-fx-cursor: hand;");
            }
        });

        setOnMouseExited(e -> setStyle(""));
    }

    /**
     * Sets the card for this slot
     */
    public void setCard(Card newCard) {
        this.card = newCard;

        if (card == null) {
            // Show empty state
            getStyleClass().remove("deck-slot-filled");
            placeholderLabel.setVisible(true);
            cardContent.setVisible(false);
            cardContent.getChildren().clear();
        } else {
            // Show filled state
            if (!getStyleClass().contains("deck-slot-filled")) {
                getStyleClass().add("deck-slot-filled");
            }
            placeholderLabel.setVisible(false);
            cardContent.setVisible(true);

            // Update card content
            updateCardDisplay();
        }
    }

    private void updateCardDisplay() {
        content.getChildren().clear();

        // Make card fill entire slot, overlaid with elixir cost
        javafx.scene.image.ImageView cardImage = new javafx.scene.image.ImageView();
        cardImage.setFitWidth(120);
        cardImage.setFitHeight(160);
        cardImage.setPreserveRatio(false); // Fill entire slot

        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    getClass().getResourceAsStream(card.getImagePath()));
            cardImage.setImage(image);
        } catch (Exception e) {
            // Fallback placeholder
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(120, 160);
            String color = card.getType().toString().equals("TROOP") ? "#f97316"
                    : card.getType().toString().equals("BUILDING") ? "#92400e" : "#8b5cf6";
            placeholder.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");

            Label placeholderLabel = new Label(card.getName().substring(0, Math.min(3, card.getName().length())));
            placeholderLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
            placeholder.getChildren().add(placeholderLabel);
            content.getChildren().add(placeholder);
            return;
        }

        // Elixir cost overlay
        StackPane costPane = new StackPane();
        costPane.setAlignment(javafx.geometry.Pos.CENTER);
        costPane.setPrefSize(35, 35);
        costPane.setMinSize(35, 35);
        costPane.setMaxSize(35, 35);

        try {
            javafx.scene.image.ImageView elixirIcon = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/images/card_elixir.png")));
            elixirIcon.setFitWidth(35);
            elixirIcon.setFitHeight(35);
            elixirIcon.setPreserveRatio(true);

            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane.setAlignment(costLabel, javafx.geometry.Pos.CENTER);

            costPane.getChildren().addAll(elixirIcon, costLabel);
        } catch (Exception e) {
            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
            costPane.getChildren().add(costLabel);
        }

        StackPane imageContainer = new StackPane();
        imageContainer.getChildren().addAll(cardImage, costPane);

        // Position cost at top left
        StackPane.setAlignment(costPane, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(costPane, new javafx.geometry.Insets(-3, 0, 0, -3)); // Push into corner

        content.getChildren().add(imageContainer);
    }

    /**
     * Gets the current card in this slot
     */
    public Card getCard() {
        return card;
    }

    /**
     * Checks if the slot is empty
     */
    public boolean isEmpty() {
        return card == null;
    }

    /**
     * Clears the slot
     */
    public void clear() {
        card = null;
        getStyleClass().remove("deck-slot-filled");
        placeholderLabel.setVisible(true);
        cardContent.setVisible(false);
        content.getChildren().clear();
        content.getChildren().addAll(placeholderLabel, cardContent);
    }
}
