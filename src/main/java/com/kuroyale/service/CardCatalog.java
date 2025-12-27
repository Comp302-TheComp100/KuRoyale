package com.kuroyale.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kuroyale.model.Card;
import com.kuroyale.model.CardFactory;

/*Service that manages the catalog of available cards in the game
 * Information Expert: CardCatalog knows about all available cards
 * Indirection: Provides interface between controllers and CardFactory
 * Acts as a facade to card creation logic */
public class CardCatalog {

    private final CardFactory cardFactory;
    private final Map<String, Card> cardsByName;
    private final List<Card> allCards;

    //Creates a new CardCatalog. Creator pattern: CardCatalog aggregates Card references
    public CardCatalog() {
        this.cardFactory = new CardFactory();
        this.allCards = cardFactory.getAllCards();
        this.cardsByName = new HashMap<>();

        // Build lookup map for quick card retrieval by name
        for (Card card : allCards) {
            cardsByName.put(card.getName(), card);
        }
    }

    //Gets all available cards in the game
    public List<Card> getAllCards() {
        return allCards;
    }

    //Finds a card by its name. Information Expert: CardCatalog knows all available cards
    public Card getCardByName(String name) {return cardsByName.get(name);}

    public Card createCardWithLevel(String name, int level) {
        Card base = cardsByName.get(name);
        if (base == null) {
            return null;
        }
        Card copy = new Card(
                base.getName(),
                base.getCost(),
                base.getType(),
                base.getRarity(),
                base.getBaseHp(),
                base.getBaseDamage(),
                base.getHitSpeed(),
                base.getRange(),
                base.getSpeed(),
                base.getTarget(),
                base.isAirUnit(),
                base.isAreaEffect(),
                base.getDescription(),
                base.getCount(),
                base.getLifetime());
        copy.setLevel(level);
        return copy;
    }

    //Checks if a card with the given name exists
    public boolean cardExists(String name) {return cardsByName.containsKey(name);}

    //Gets the total number of available cards
    public int getCardCount() {return allCards.size();}
}