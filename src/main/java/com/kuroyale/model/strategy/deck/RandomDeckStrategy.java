package com.kuroyale.model.strategy.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.CardCatalog;
import com.kuroyale.model.core.entities.Deck;
import com.kuroyale.util.common.ServiceFactory;

/**
 * Strategy Pattern: Generates a random deck from available cards.
 * GRASP: Pure Fabrication - creates deck without domain knowledge.
 */
public class RandomDeckStrategy implements DeckBuildingStrategy {

    private final CardCatalog cardCatalog;

    public RandomDeckStrategy() {
        this.cardCatalog = ServiceFactory.getInstance().getCardCatalog();
    }

    @Override
    public Deck buildDeck() {
        Deck deck = new Deck();
        List<Card> allCards = new ArrayList<>(cardCatalog.getAllCards());
        Collections.shuffle(allCards);

        for (int i = 0; i < Math.min(8, allCards.size()); i++) {
            deck.addCard(allCards.get(i));
        }
        return deck;
    }

    @Override
    public String getDisplayName() {
        return "Random Deck";
    }

    @Override
    public boolean requiresUserInput() {
        return false;
    }
}
