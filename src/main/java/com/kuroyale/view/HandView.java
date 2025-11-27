package com.kuroyale.view;

import com.kuroyale.model.Card;
import com.kuroyale.model.ElixirManager;
import com.kuroyale.model.Hand;
import javafx.geometry.Pos;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visual representation of the player's 4-card hand.
 * Highlights affordable cards and handles selection.
 */
public class HandView extends HBox {
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
            // For now, assuming CardView is usable. If not, we'll fix it.

            // Make it smaller for hand view
            view.setPrefWidth(80);
            view.setPrefHeight(100);

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
                // Shake animation or sound for "cannot afford"
                return;
            }
        }

        if (onCardSelected != null) {
            onCardSelected.accept(selectedIndex);
        }
        update();
    }

    public void update() {
        // Refresh cards if they changed (e.g. after play)
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card card = hand.getCard(i);
            CardView view = cardViews.get(i);

            // Check if card object changed (cycled)
            if (view.getCard() != card) { // Assuming CardView has getCard() or we check equality
                // Re-create or update view
                // For simplicity, let's just re-initialize if needed, but better to update
                // content
                // Since CardView might be complex, let's just replace the view in children
                CardView newView = new CardView(card);
                newView.setPrefWidth(80);
                newView.setPrefHeight(100);
                final int index = i;
                newView.setOnMouseClicked(e -> handleCardClick(index));

                this.getChildren().set(i, newView);
                cardViews.set(i, newView);
                view = newView;
            }

            // Update visual state (affordable, selected)
            boolean affordable = card != null && elixirManager.canAfford(card.getCost());
            boolean selected = (i == selectedIndex);

            if (selected) {
                view.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 10, 0, 0, 0); -fx-translate-y: -10;");
                view.setEffect(null);
            } else if (affordable) {
                view.setStyle("");
                view.setEffect(null);
            } else {
                // Dim unavailable cards
                ColorAdjust dim = new ColorAdjust();
                dim.setBrightness(-0.5);
                dim.setSaturation(-0.5);
                view.setEffect(dim);
                view.setStyle("");
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
}
