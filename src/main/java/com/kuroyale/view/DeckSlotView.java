package com.kuroyale.view;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.enums.CardType;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

//Custom JavaFX component for displaying a deck slot
public class DeckSlotView extends StackPane {
    private static final PseudoClass FILLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("filled");
    private static final PseudoClass HIGHLIGHT_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlight");

    private Card card;
    private final VBox content;
    private final Label placeholderLabel;
    private final VBox cardContent;
    private StackPane highlightOverlay;

    public DeckSlotView() {
        // Set size
        setPrefSize(90, 120);
        setMinSize(90, 120);
        setMaxSize(90, 120);
        getStyleClass().add("deck-slot");

        // Create placeholder
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

        // Hover effects for filled deck slots
        // Use same animation as CardView
        setOnMouseEntered(e -> {
            if (!isEmpty()) {
                // Same scale as CardView (1.02)
                setScaleX(1.02);
                setScaleY(1.02);
                // Add CSS class for hover effects (glow)
                getStyleClass().add("deck-slot-hover");
            }
        });

        setOnMouseExited(e -> {
            if (!isEmpty()) {
                // Reset scale to normal
                setScaleX(1.0);
                setScaleY(1.0);
                // Remove hover CSS class
                getStyleClass().remove("deck-slot-hover");
            }
        });
    }

    // Sets the card for this slot
    public void setCard(Card newCard) {
        this.card = newCard;

        if (card == null) {
            // Show empty state
            pseudoClassStateChanged(FILLED_PSEUDO_CLASS, false);
            getStyleClass().remove("filled");
            placeholderLabel.setVisible(true);
            cardContent.setVisible(false);
            cardContent.getChildren().clear();
            setStyle("");
        } else {
            // Show filled state
            pseudoClassStateChanged(FILLED_PSEUDO_CLASS, true);
            if (!getStyleClass().contains("filled")) {
                getStyleClass().add("filled");
            }
            placeholderLabel.setVisible(false);
            cardContent.setVisible(true);
            String rarityColor = getRarityColor();
            setStyle("-fx-border-color: " + rarityColor + "; " +
                    "-fx-border-width: 3; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8;");
            updateCardDisplay();
        }
    }

    private void updateCardDisplay() {
        content.getChildren().clear();

        // Make card fill entire slot, overlaid with elixir cost
        javafx.scene.image.ImageView cardImage = new javafx.scene.image.ImageView();
        cardImage.setFitWidth(90);
        cardImage.setFitHeight(120);
        cardImage.setPreserveRatio(false);
        cardImage.setSmooth(true);
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

        Label levelLabel = createLevelIndicator();

        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: transparent;");
        // Add children in order: cardImage (bottom), highlightOverlay (middle),
        // costPane (top), levelLabel (top)
        imageContainer.getChildren().addAll(cardImage, highlightOverlay, costPane, levelLabel);

        // Position cost at top left
        StackPane.setAlignment(costPane, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(costPane, new javafx.geometry.Insets(-3, 0, 0, -3)); // Push into corner

        StackPane.setAlignment(levelLabel, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(levelLabel, new javafx.geometry.Insets(0, 3, 3, 0));

        content.getChildren().add(imageContainer);
    }

    // Gets the current card in this slot
    public Card getCard() {
        return card;
    }

    // Checks if the slot is empty
    public boolean isEmpty() {
        return card == null;
    }

    public void refreshCardDisplay() {
        if (!isEmpty()) {
            updateCardDisplay();
        }
    }

    // Clears the slot
    public void clear() {
        card = null;
        pseudoClassStateChanged(FILLED_PSEUDO_CLASS, false);
        getStyleClass().remove("filled");
        placeholderLabel.setVisible(true);
        cardContent.setVisible(false);
        content.getChildren().clear();
        content.getChildren().addAll(placeholderLabel, cardContent);
    }

    // Highlights the slot for replace mode
    public void highlightForReplace() {
        if (!isEmpty()) {
            pseudoClassStateChanged(HIGHLIGHT_PSEUDO_CLASS, true);
            if (!getStyleClass().contains("highlight")) {
                getStyleClass().add("highlight");
            }
            // Show the overlay on top of the card image
            if (highlightOverlay != null) {
                highlightOverlay.setVisible(true);
            }
        }
    }

    // Removes highlight from the slot
    public void removeHighlight() {
        pseudoClassStateChanged(HIGHLIGHT_PSEUDO_CLASS, false);
        getStyleClass().remove("highlight");
        // Hide the overlay
        if (highlightOverlay != null) {
            highlightOverlay.setVisible(false);
        }
    }

    // Gets the placeholder color based on card type
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
}
