package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {

    private Deck deck;
    private Card card1, card2, card3;

    @BeforeEach
    void setUp() {
        deck = new Deck();
        card1 = new Card("C1", 1, CardType.TROOP, Rarity.COMMON, 10, 10, 1.0, 1.0, SpeedType.MEDIUM, TargetType.GROUND,
                false, false, "", 1, 0);
        card2 = new Card("C2", 2, CardType.TROOP, Rarity.COMMON, 10, 10, 1.0, 1.0, SpeedType.MEDIUM, TargetType.GROUND,
                false, false, "", 1, 0);
        card3 = new Card("C3", 3, CardType.TROOP, Rarity.COMMON, 10, 10, 1.0, 1.0, SpeedType.MEDIUM, TargetType.GROUND,
                false, false, "", 1, 0);
    }

    // Test Case 1: Add Card
    @Test
    void testAddCard() {
        assertTrue(deck.addCard(card1));
        assertEquals(1, deck.size());
        assertTrue(deck.contains(card1));
        assertTrue(deck.repOk());
    }

    // Test Case 2: Capacity Limit
    @Test
    void testCapacityLimit() {
        // Add 8 cards
        for (int i = 0; i < 8; i++) {
            deck.addCard(new Card("C" + i, 1, CardType.TROOP, Rarity.COMMON, 10, 10, 1.0, 1.0, SpeedType.MEDIUM,
                    TargetType.GROUND, false, false, "", 1, 0));
        }
        assertTrue(deck.isFull());
        assertTrue(deck.repOk());

        // Try to add 9th
        boolean added = deck.addCard(new Card("Extra", 1, CardType.TROOP, Rarity.COMMON, 10, 10, 1.0, 1.0,
                SpeedType.MEDIUM, TargetType.GROUND, false, false, "", 1, 0));
        assertFalse(added, "Should not add card when full");
        assertEquals(8, deck.size());
        assertTrue(deck.repOk());
    }

    // Test Case 3: Duplicate Prevention
    @Test
    void testDuplicatePrevention() {
        deck.addCard(card1);
        boolean addedAgain = deck.addCard(card1); // Same instance

        assertFalse(addedAgain, "Should not add duplicate card");
        assertEquals(1, deck.size());
        assertTrue(deck.repOk());
    }

    // Test Case 4: Removal
    @Test
    void testRemoveCard() {
        deck.addCard(card1);
        deck.addCard(card2);

        assertTrue(deck.removeCard(card1));
        assertFalse(deck.contains(card1));
        assertEquals(1, deck.size());
        assertTrue(deck.repOk());

        assertFalse(deck.removeCard(card3)); // Not in deck
    }

    // Test Case 5: repOk
    @Test
    void testRepOk() {
        // Empty deck should be valid
        assertTrue(deck.repOk());

        deck.addCard(card1);
        assertTrue(deck.repOk());
    }
}
