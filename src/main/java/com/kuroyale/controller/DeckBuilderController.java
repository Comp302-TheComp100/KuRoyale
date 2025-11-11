package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kuroyale.model.Card;
import com.kuroyale.model.Deck;
import com.kuroyale.view.CardInfoDialog;
import com.kuroyale.view.CardView;
import com.kuroyale.view.DeckSlotView;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the deck builder screen
 */
public class DeckBuilderController {

    @FXML
    private GridPane deckSlotsContainer;

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
    private CardView selectedCardView;
    private HBox cardButtonsBox;

    @FXML
    private void initialize() {
        deck = new Deck();
        deckSlots = new ArrayList<>();

        // Create 8 deck slots
        createDeckSlots();

        // Load all 28 cards
        loadAllCards();
    }

    private void createDeckSlots() {
        int index = 0;
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                DeckSlotView slot = new DeckSlotView();
                slot.setOnMouseClicked(event -> handleDeckSlotClick(slot));
                deckSlots.add(slot);
                deckSlotsContainer.add(slot, col, row);
                index++;
            }
        }
    }

    private void loadAllCards() {
        List<Card> allCards = Card.getAllCards();

        int column = 0;
        int row = 0;

        for (Card card : allCards) {
            // Create container for card + buttons
            VBox cardContainer = new VBox(5);
            cardContainer.setAlignment(Pos.TOP_CENTER);

            CardView cardView = new CardView(card);
            cardContainer.getChildren().add(cardView);

            cardView.setOnMouseClicked(event -> {
                handleCardClick(cardView, cardContainer);
                event.consume();
            });

            cardsGrid.add(cardContainer, column, row);

            column++;
            if (column >= 4) { // 4 cards per row
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

    private void handleCardClick(CardView cardView, VBox cardContainer) {
        Card card = cardView.getCard();

        // If this card is already selected, deselect it
        if (selectedCardView == cardView && cardButtonsBox != null) {
            cardContainer.getChildren().remove(cardButtonsBox);
            cardButtonsBox = null;
            selectedCardView = null;
            return;
        }

        // Remove buttons from previously selected card
        if (selectedCardView != null && cardButtonsBox != null) {
            VBox prevContainer = (VBox) selectedCardView.getParent();
            if (prevContainer != null) {
                prevContainer.getChildren().remove(cardButtonsBox);
            }
        }

        selectedCardView = cardView;

        // Create buttons with image backgrounds
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(5, 0, 0, 0));

        Button infoButton = createImageButton("/images/button_blue.png", "INFO");
        infoButton.setOnAction(e -> showCardInfo(card));

        Button useRemoveButton;
        if (deck.contains(card)) {
            useRemoveButton = createImageButton("/images/button_red.png", "REMOVE");
            useRemoveButton.setOnAction(e -> {
                removeCardFromDeck(card);
                cardContainer.getChildren().remove(buttonsBox);
                cardButtonsBox = null;
                selectedCardView = null;
            });
        } else {
            useRemoveButton = createImageButton("/images/button_yellow.png", "USE");
            useRemoveButton.setOnAction(e -> {
                addCardToDeck(card);
                cardContainer.getChildren().remove(buttonsBox);
                cardButtonsBox = null;
                selectedCardView = null;
            });
        }

        buttonsBox.getChildren().addAll(infoButton, useRemoveButton);
        cardContainer.getChildren().add(buttonsBox);
        cardButtonsBox = buttonsBox;
    }

    private Button createImageButton(String imagePath, String text) {
        Button button = new Button();
        button.setPrefSize(70, 35);
        button.setMinSize(70, 35);
        button.setMaxSize(70, 35);

        try {
            Image buttonImage = new Image(getClass().getResourceAsStream(imagePath));
            ImageView buttonImageView = new ImageView(buttonImage);
            buttonImageView.setFitWidth(70);
            buttonImageView.setFitHeight(35);
            buttonImageView.setPreserveRatio(false);
            button.setGraphic(buttonImageView);

            // Make button transparent and show text on top
            button.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-border-width: 0;");
            button.setText(text);
            button.setTextFill(javafx.scene.paint.Color.WHITE);
            button.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 11));
            button.setContentDisplay(javafx.scene.control.ContentDisplay.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to colored background
            button.setText(text);
            String color = imagePath.contains("blue") ? "#3b82f6"
                    : imagePath.contains("yellow") ? "#fbbf24" : "#ef4444";
            button.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; " +
                    "-fx-cursor: hand; -fx-background-color: " + color + "; -fx-background-radius: 5;");
        }

        return button;
    }

    private void showCardInfo(Card card) {
        CardInfoDialog infoDialog = new CardInfoDialog(card, () -> {
            rootPane.getChildren().remove(rootPane.getChildren().size() - 1);
        });

        rootPane.getChildren().add(infoDialog);
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
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
