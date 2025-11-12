package com.kuroyale.view;

import com.kuroyale.model.Card;
import com.kuroyale.model.CardType;

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
    private StackPane highlightOverlay; // Yellow border overlay for replace mode

    public DeckSlotView() {
        // Set size
        setPrefSize(120, 160);
        setMinSize(120, 160);
        setMaxSize(120, 160);

        // Apply empty slot style
        getStyleClass().add("deck-slot");

        // Create placeholder (+ icon) - transparent to blend with background
        placeholderLabel = new Label("+");
        placeholderLabel.setFont(new Font(48));
        placeholderLabel.setStyle("-fx-text-fill: rgba(148, 163, 184, 0.3);");

        // Create card content container
        cardContent = new VBox(5);
        cardContent.setAlignment(Pos.CENTER);
        cardContent.setPadding(new Insets(10));
        cardContent.setStyle("-fx-background-color: transparent;");
        cardContent.setVisible(false);

        content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: transparent;");
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
        cardImage.setSmooth(true); // Better image quality
        cardImage.setStyle("-fx-background-color: transparent;");

        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    getClass().getResourceAsStream(card.getImagePath()), 120, 160, false, true);
            cardImage.setImage(image);
        } catch (Exception e) {
            // Fallback placeholder
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(120, 160);
            String color = getPlaceholderColor(card.getType());
            placeholder.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");

            Label placeholderLabel = new Label(card.getName().substring(0, Math.min(3, card.getName().length()))
                    .toUpperCase(java.util.Locale.ENGLISH));
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
        costPane.setStyle("-fx-background-color: transparent;");

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

        // Create highlight overlay (initially hidden)
        highlightOverlay = new StackPane();
        highlightOverlay.setPrefSize(120, 160);
        highlightOverlay.setMinSize(120, 160);
        highlightOverlay.setMaxSize(120, 160);
        highlightOverlay.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: #fbbf24; " +
                        "-fx-border-width: 4; " +
                        "-fx-border-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(251, 191, 36, 0.8), 15, 0, 0, 0);");
        highlightOverlay.setVisible(false);
        highlightOverlay.setMouseTransparent(true); // Allow clicks to pass through

        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: transparent;");
        // Add children in order: cardImage (bottom), highlightOverlay (middle),
        // costPane (top)
        imageContainer.getChildren().addAll(cardImage, highlightOverlay, costPane);

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

    /**
     * Highlights the slot for replace mode
     */
    public void highlightForReplace() {
        if (!isEmpty()) {
            getStyleClass().add("deck-slot-replace-highlight");
            // Show the overlay on top of the card image
            if (highlightOverlay != null) {
                highlightOverlay.setVisible(true);
            }
        }
    }

    /**
     * Removes highlight from the slot
     */
    public void removeHighlight() {
        getStyleClass().remove("deck-slot-replace-highlight");
        // Hide the overlay
        if (highlightOverlay != null) {
            highlightOverlay.setVisible(false);
        }
    }

    /**
     * Gets the placeholder color based on card type
     */
    private String getPlaceholderColor(CardType type) {
        switch (type) {
            case TROOP:
                return "#f97316";
            case BUILDING:
                return "#92400e";
            case SPELL:
                return "#8b5cf6";
            default:
                return "#64748b";
        }
    }
}
