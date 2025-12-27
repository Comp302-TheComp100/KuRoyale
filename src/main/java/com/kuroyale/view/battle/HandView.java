package com.kuroyale.view.battle;

import com.kuroyale.view.CardView;

import com.kuroyale.model.Card;
import com.kuroyale.model.ElixirManager;
import com.kuroyale.model.Hand;
import javafx.geometry.Pos;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/*Visual representation of the player's 4-card hand.
 * Highlights affordable cards and handles selection.*/
public class HandView extends VBox {
    private final Hand hand;
    private final ElixirManager elixirManager;
    private final List<CardView> cardViews;
    private int selectedIndex = -1;
    private Consumer<Integer> onCardSelected;

    public HandView(Hand hand, ElixirManager elixirManager) {
        this.hand = hand;
        this.elixirManager = elixirManager;
        this.cardViews = new ArrayList<>();

        this.setAlignment(Pos.CENTER);
        this.setSpacing(10);
        this.getStyleClass().add("hand-container");
        // Make background semi-transparent and compact
        this.setStyle("-fx-background-color: rgba(79, 35, 24, 0.68); -fx-background-radius: 15; -fx-padding: 5;");

        initializeCards();
    }

    private void initializeCards() {
        this.getChildren().clear();
        cardViews.clear();

        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card card = hand.getCard(i);
            CardView view = new CardView(card); // Assuming CardView exists and takes Card
            // We might need to adjust CardView or create a wrapper if CardView is too
            // complex

            // Make it smaller for hand view
            view.setPrefWidth(60);
            view.setPrefHeight(80);

            final int index = i;
            view.setOnMouseClicked(e -> handleCardClick(index));

            cardViews.add(view);
            this.getChildren().add(view);
        }
    }

    public void setOnCardSelected(Consumer<Integer> onCardSelected) {
        this.onCardSelected = onCardSelected;
    }

    private void handleCardClick(int index) {
        if (selectedIndex == index) {
            // Deselect
            selectedIndex = -1;
        } else {
            // Select if affordable
            Card card = hand.getCard(index);
            if (card != null && elixirManager.canAfford(card.getCost())) {
                selectedIndex = index;
            } else {
                return;
            }
        }

        if (onCardSelected != null) {
            onCardSelected.accept(selectedIndex);
        }
        update();
    }

    public void update() {
        // Refresh cards if they changed
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card card = hand.getCard(i);
            CardView view = cardViews.get(i);

            // Check if card object changed
            if (view.getCard() != card) { // Assuming CardView has getCard() or check equality
                // Re-create or update view
                CardView newView = new CardView(card);
                newView.setPrefWidth(60);
                newView.setPrefHeight(80);
                final int index = i;
                newView.setOnMouseClicked(e -> handleCardClick(index));

                this.getChildren().set(i, newView);
                cardViews.set(i, newView);
                view = newView;
            }

            // Update visual state (affordable, selected)
            boolean affordable = card != null && elixirManager.canAfford(card.getCost());
            boolean selected = (i == selectedIndex);

            String rarityBorder = getRarityBorderStyle(card);

            if (selected) {
                view.setStyle(rarityBorder
                        + "-fx-effect: dropshadow(three-pass-box, gold, 10, 0, 0, 0); -fx-translate-y: -10;");
                view.setEffect(null);
            } else if (affordable) {
                view.setStyle(rarityBorder);
                view.setEffect(null);
            } else {
                // Dim unavailable cards
                ColorAdjust dim = new ColorAdjust();
                dim.setBrightness(-0.5);
                dim.setSaturation(-0.5);
                view.setEffect(dim);
                view.setStyle(rarityBorder);
            }
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void clearSelection() {
        selectedIndex = -1;
        update();
    }

    private String getRarityBorderStyle(Card card) {
        if (card == null) {
            return "";
        }

        String rarityColor;
        switch (card.getRarity()) {
            case COMMON:
                rarityColor = "#9ca3af"; // Gray
                break;
            case RARE:
                rarityColor = "#3b82f6"; // Blue
                break;
            case EPIC:
                rarityColor = "#8b5cf6"; // Purple
                break;
            case LEGENDARY:
                rarityColor = "#f59e0b"; // Orange/Gold
                break;
            default:
                rarityColor = "#cbd5e1"; // Default gray
        }

        return "-fx-border-color: " + rarityColor + "; " +
                "-fx-border-width: 3; " +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; ";
    }
}