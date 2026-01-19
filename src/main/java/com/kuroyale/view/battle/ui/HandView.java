package com.kuroyale.view.battle.ui;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Hand;
import com.kuroyale.model.logic.ElixirManager;
import com.kuroyale.view.card.CardView;

import javafx.geometry.Pos;

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
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.hand = hand;
        this.elixirManager = elixirManager;
        this.cardViews = new ArrayList<>();

        this.setAlignment(Pos.CENTER);
        this.setSpacing(10);
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

            // Check if state changed before applying heavy styles
            // We store the last state in the node's userData to avoid a separate map
            String currentStateSig = affordable + ":" + selected;
            Object lastStateObj = view.getUserData();

            if (!currentStateSig.equals(lastStateObj)) {
                view.setUserData(currentStateSig);

                // clear old rarity classes
                view.getStyleClass().removeAll("card-common", "card-rare", "card-epic", "card-legendary",
                        "card-default");
                String rarityClass = getRarityStyleClass(card);
                if (!rarityClass.isEmpty()) {
                    view.getStyleClass().add(rarityClass);
                }

                if (selected) {
                    view.setStyle("-fx-effect: dropshadow(three-pass-box, gold, 10, 0, 0, 0); -fx-translate-y: -10;");
                    view.setEffect(null);
                    view.setDimmed(false);
                } else if (affordable) {
                    view.setStyle("");
                    view.setEffect(null);
                    view.setDimmed(false);
                } else {
                    // Dim unavailable cards
                    view.setStyle("");
                    view.setEffect(null);
                    view.setDimmed(true);
                }
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

    private String getRarityStyleClass(Card card) {
        if (card == null) {
            return "";
        }

        switch (card.getRarity()) {
            case COMMON:
                return "card-common";
            case RARE:
                return "card-rare";
            case EPIC:
                return "card-epic";
            case LEGENDARY:
                return "card-legendary";
            default:
                return "card-default";
        }
    }
}
