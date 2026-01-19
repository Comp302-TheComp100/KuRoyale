package com.kuroyale.model.strategy.deck;

import com.kuroyale.model.entities.Deck;

/**
 * Strategy Pattern: Marker strategy indicating deck should be built via UI.
 * Returns null from buildDeck() to signal that user must select cards.
 * GRASP: Controller pattern - the UI controller handles actual deck building.
 */
public class CustomDeckStrategy implements DeckBuildingStrategy {

    @Override
    public Deck buildDeck() {
        // Returns null to indicate deck must be built through UI
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Build Deck";
    }

    @Override
    public boolean requiresUserInput() {
        return true;
    }
}
