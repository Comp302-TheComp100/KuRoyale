package com.kuroyale.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.CardFactory;
import com.kuroyale.model.entities.User;

/*Service that manages the catalog of available cards in the game
 * Information Expert: CardCatalog knows about all available cards
 * Indirection: Provides interface between controllers and CardFactory
 * Acts as a facade to card creation logic */
public class CardCatalog {

    private final CardFactory cardFactory;
    private final Map<String, Card> cardsByName;
    private final List<Card> allCards;

    // Creates a new CardCatalog. Creator pattern: CardCatalog aggregates Card
    // references
    public CardCatalog() {
        this.cardFactory = new CardFactory();
        this.allCards = cardFactory.getAllCards();
        this.cardsByName = new HashMap<>();

        // Build lookup map for quick card retrieval by name
        for (Card card : allCards) {
            cardsByName.put(card.getName(), card);
        }
    }

    // Gets all available cards in the game
    public List<Card> getAllCards() {
        return allCards;
    }

    public Set<String> getAllCardNames() {
        Set<String> names = new HashSet<>();
        for (Card card : allCards) {
            if (card != null && card.getName() != null && !card.getName().isEmpty()) {
                names.add(card.getName());
            }
        }
        return names;
    }

    public void applyUserLevels(User user) {
        if (user == null) {
            return;
        }
        for (Card card : allCards) {
            if (card == null) {
                continue;
            }
            card.setLevel(user.getCardLevel(card.getName()));
        }
    }

    // Finds a card by its name. Information Expert: CardCatalog knows all available
    // cards
    public Card getCardByName(String name) {
        return cardsByName.get(name);
    }

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
        copy.setSpawnUnitName(base.getSpawnUnitName());
        copy.setSpawnUnitCount(base.getSpawnUnitCount());
        copy.setDeathSpawnUnitName(base.getDeathSpawnUnitName());
        copy.setDeathSpawnUnitCount(base.getDeathSpawnUnitCount());
        // Copy production properties (for Elixir Collector)
        if (base.getProductionResource() != null) {
            copy.setProduction(base.getProductionResource(), base.getProductionAmount(), base.getProductionInterval());
        }
        // Copy minRange (for Mortar blind spot)
        copy.setMinRange(base.getMinRange());
        copy.setLevel(level);
        return copy;
    }

    // Checks if a card with the given name exists
    public boolean cardExists(String name) {
        return cardsByName.containsKey(name);
    }

    // Gets the total number of available cards
    public int getCardCount() {
        return allCards.size();
    }
}
