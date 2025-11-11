package com.kuroyale.controller;

import com.kuroyale.model.Card;
import com.kuroyale.model.Deck;
import com.kuroyale.view.CardView;
import com.kuroyale.view.DeckSlotView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the deck builder screen
 */
public class DeckBuilderController {
    
    @FXML
    private HBox deckSlotsContainer;
    
    @FXML
    private GridPane cardsGrid;
    
    @FXML
    private ScrollPane cardsScrollPane;
    
    @FXML
    private StackPane rootPane;
    
    @FXML
    private Button backButton;
    
    private Deck deck;
    private List<DeckSlotView> deckSlots;
    private StackPane overlayPane;
    private CardView selectedCardView;
    
    @FXML
    private void initialize() {
        deck = new Deck();
        deckSlots = new ArrayList<>();
        
        // Create 8 deck slots
        createDeckSlots();
        
        // Load all 28 cards
        loadAllCards();
        
        // Create overlay pane (initially hidden)
        overlayPane = new StackPane();
        overlayPane.getStyleClass().add("overlay-background");
        overlayPane.setVisible(false);
        overlayPane.setOnMouseClicked(event -> {
            if (event.getTarget() == overlayPane) {
                hideOverlay();
            }
        });
        
        // Add overlay to root
        if (!rootPane.getChildren().contains(overlayPane)) {
            rootPane.getChildren().add(overlayPane);
        }
    }
    
    private void createDeckSlots() {
        for (int i = 0; i < 8; i++) {
            DeckSlotView slot = new DeckSlotView();
            slot.setOnMouseClicked(event -> handleDeckSlotClick(slot));
            deckSlots.add(slot);
            deckSlotsContainer.getChildren().add(slot);
        }
    }
    
    private void loadAllCards() {
        List<Card> allCards = Card.getAllCards();
        
        int column = 0;
        int row = 0;
        
        for (Card card : allCards) {
            CardView cardView = new CardView(card);
            cardView.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1) {
                    showCardOverlay(cardView);
                }
                event.consume();
            });
            
            cardsGrid.add(cardView, column, row);
            
            column++;
            if (column >= 4) {
                column = 0;
                row++;
            }
        }
    }
    
    private void handleDeckSlotClick(DeckSlotView slot) {
        if (!slot.isEmpty()) {
            // Remove card from deck
            Card cardToRemove = slot.getCard();
            deck.removeCard(cardToRemove);
            slot.clear();
        }
    }
    
    private void showCardOverlay(CardView cardView) {
        selectedCardView = cardView;
        Card card = cardView.getCard();
        
        // Create enlarged card view
        CardView enlargedCard = new CardView(card);
        enlargedCard.setPrefSize(240, 300);
        enlargedCard.setMinSize(240, 300);
        enlargedCard.setMaxSize(240, 300);
        
        // Create buttons container
        HBox buttonsBox = new HBox(20);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(15, 0, 0, 0));
        
        Button infoButton = new Button("INFO");
        infoButton.getStyleClass().add("overlay-button");
        infoButton.setOnAction(e -> {
            // Info action - could show detailed stats in future
            System.out.println("Info for: " + card.getName());
        });
        
        Button useRemoveButton = new Button(deck.contains(card) ? "REMOVE" : "USE");
        useRemoveButton.getStyleClass().addAll("overlay-button", 
                deck.contains(card) ? "overlay-button-remove" : "overlay-button-use");
        useRemoveButton.setOnAction(e -> {
            if (deck.contains(card)) {
                removeCardFromDeck(card);
            } else {
                addCardToDeck(card);
            }
            hideOverlay();
        });
        
        buttonsBox.getChildren().addAll(infoButton, useRemoveButton);
        
        // Create card container
        VBox cardContainer = new VBox(10);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.getStyleClass().add("overlay-card-container");
        cardContainer.setPadding(new Insets(20));
        cardContainer.getChildren().addAll(enlargedCard, buttonsBox);
        
        overlayPane.getChildren().clear();
        overlayPane.getChildren().add(cardContainer);
        overlayPane.setVisible(true);
    }
    
    private void hideOverlay() {
        overlayPane.setVisible(false);
        overlayPane.getChildren().clear();
    }
    
    private void addCardToDeck(Card card) {
        if (deck.isFull()) {
            System.out.println("Deck is full!");
            return;
        }
        
        if (deck.addCard(card)) {
            // Find first empty slot and add card
            for (DeckSlotView slot : deckSlots) {
                if (slot.isEmpty()) {
                    slot.setCard(card);
                    break;
                }
            }
        }
    }
    
    private void removeCardFromDeck(Card card) {
        if (deck.removeCard(card)) {
            // Find and clear the slot with this card
            for (DeckSlotView slot : deckSlots) {
                if (!slot.isEmpty() && slot.getCard().equals(card)) {
                    slot.clear();
                    break;
                }
            }
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

