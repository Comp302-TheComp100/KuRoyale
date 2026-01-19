package com.kuroyale.model.strategy.deck;

import com.kuroyale.model.entities.Deck;

/**
 * Strategy Pattern: Defines different strategies for building a deck.
 * Allows switching between deck sources (current deck, random, or custom
 * build).
 * 
 * GRASP: Polymorphism - using interface to enable different deck building
 * behaviors.
 */
public interface DeckBuildingStrategy {

    /**
     * Build and return a deck. May return null if user input is required.
     * 
     * @return The built deck, or null if deck needs to be built via UI.
     */
    Deck buildDeck();

    /**
     * Get the display name for this strategy.
     * 
     * @return Human-readable name for UI display.
     */
    String getDisplayName();

    /**
     * Check if this strategy requires user input through UI.
     * 
     * @return true if user must select cards, false if deck is auto-generated.
     */
    boolean requiresUserInput();
}
