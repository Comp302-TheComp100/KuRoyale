package com.kuroyale.view;

import com.kuroyale.model.Card;
import com.kuroyale.model.CardType;
import com.kuroyale.util.StyleHelper;

import javafx.css.PseudoClass;
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
    private static final PseudoClass FILLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("filled");
    private static final PseudoClass HIGHLIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlight");
    
    private Card card;
    private final VBox content;
    private final Label placeholderLabel;
    private final VBox cardContent;
    private StackPane highlightOverlay; // Yellow border overlay for replace mode
    private boolean isHighlighted = false;

    public DeckSlotView() {
        // Set size
        setPrefSize(90, 120);
        setMinSize(90, 120);
        setMaxSize(90, 120);

        // Apply empty slot style
        StyleHelper.applyDeckSlotEmptyStyle(this);

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

        // Add hover effect with PseudoClass-based styling
        setOnMouseEntered(e -> {
            if (isHighlighted) {
                StyleHelper.applyDeckSlotReplaceHighlightHoverStyle(this);
            } else {
                StyleHelper.applyDeckSlotHoverStyle(this);
            }
        });

        setOnMouseExited(e -> {
            if (isHighlighted) {
                StyleHelper.applyDeckSlotReplaceHighlightStyle(this);
            } else {
                if (card != null) {
                    StyleHelper.applyDeckSlotFilledStyle(this);
                } else {
                    StyleHelper.applyDeckSlotEmptyStyle(this);
                }
            }
        });
    }

    /**
     * Sets the card for this slot
     */
    public void setCard(Card newCard) {
        this.card = newCard;

        if (card == null) {
            // Show empty state
            pseudoClassStateChanged(FILLED_PSEUDO_CLASS, false);
            StyleHelper.applyDeckSlotEmptyStyle(this);
            placeholderLabel.setVisible(true);
            cardContent.setVisible(false);
            cardContent.getChildren().clear();
        } else {
            // Show filled state
            pseudoClassStateChanged(FILLED_PSEUDO_CLASS, true);
            StyleHelper.applyDeckSlotFilledStyle(this);
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
        cardImage.setFitWidth(90);
        cardImage.setFitHeight(120);
        cardImage.setPreserveRatio(false); // Fill entire slot
        cardImage.setSmooth(true); // Better image quality
        cardImage.setStyle("-fx-background-color: transparent;");

        try {
            javafx.scene.image.Image image = new javafx.scene.image.Image(
                    getClass().getResourceAsStream(card.getImagePath()), 90, 120, false, true);
            cardImage.setImage(image);
        } catch (Exception e) {
            // Fallback placeholder
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(90, 120);
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
        costPane.setPrefSize(30, 30);
        costPane.setMinSize(30, 30);
        costPane.setMaxSize(30, 30);
        costPane.setStyle("-fx-background-color: transparent;");

        try {
            javafx.scene.image.ImageView elixirIcon = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/images/cost.png")));
            elixirIcon.setFitWidth(30);
            elixirIcon.setFitHeight(30);
            elixirIcon.setPreserveRatio(true);

            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
            StackPane.setAlignment(costLabel, javafx.geometry.Pos.CENTER);

            costPane.getChildren().addAll(elixirIcon, costLabel);
        } catch (Exception e) {
            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
            costPane.getChildren().add(costLabel);
        }

        // Create highlight overlay (initially hidden)
        highlightOverlay = new StackPane();
        highlightOverlay.setPrefSize(90, 120);
        highlightOverlay.setMinSize(90, 120);
        highlightOverlay.setMaxSize(90, 120);
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
        pseudoClassStateChanged(FILLED_PSEUDO_CLASS, false);
        StyleHelper.applyDeckSlotEmptyStyle(this);
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
            isHighlighted = true;
            pseudoClassStateChanged(HIGHLIGHT_PSEUDO_CLASS, true);
            StyleHelper.applyDeckSlotReplaceHighlightStyle(this);
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
        isHighlighted = false;
        pseudoClassStateChanged(HIGHLIGHT_PSEUDO_CLASS, false);
        // Restore normal filled style
        if (!isEmpty()) {
            StyleHelper.applyDeckSlotFilledStyle(this);
        }
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
