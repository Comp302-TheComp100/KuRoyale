package com.kuroyale.model.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.service.CardCatalog;
import com.kuroyale.util.ServiceFactory;

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
