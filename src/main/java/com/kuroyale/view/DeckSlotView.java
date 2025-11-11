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
        cardContent.getChildren().clear();
        
        // Card name
        Label nameLabel = new Label(card.getName());
        nameLabel.setFont(new Font(12));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(100);
        nameLabel.setAlignment(Pos.CENTER);
        
        // Elixir cost
        Label costLabel = new Label(String.valueOf(card.getCost()));
        costLabel.setFont(new Font(24));
        costLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #fbbf24;");
        
        // Cost background
        StackPane costPane = new StackPane(costLabel);
        costPane.setStyle("-fx-background-color: rgba(139, 43, 226, 0.8); -fx-background-radius: 25; -fx-pref-width: 50; -fx-pref-height: 50;");
        
        // Type label
        Label typeLabel = new Label(card.getType().toString());
        typeLabel.setFont(new Font(9));
        typeLabel.setStyle("-fx-text-fill: #cbd5e1;");
        
        cardContent.getChildren().addAll(costPane, nameLabel, typeLabel);
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
        setCard(null);
    }
}

