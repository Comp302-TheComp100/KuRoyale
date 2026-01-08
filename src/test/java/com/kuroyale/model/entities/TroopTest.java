package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TroopTest {

    private Card groundCard;
    private Card airCard;
    private Card buildingTargetCard;

    @BeforeEach
    void setUp() {
        // Create dummy cards
        groundCard = new Card("Knight", 3, CardType.TROOP, Rarity.COMMON, 100, 10, 1.0, 1.0, SpeedType.MEDIUM,
                TargetType.GROUND, false, false, "Desc", 1, 0);
        airCard = new Card("Minion", 3, CardType.TROOP, Rarity.COMMON, 50, 10, 1.0, 1.0, SpeedType.FAST,
                TargetType.GROUND, true, false, "Desc", 3, 0);
        buildingTargetCard = new Card("Giant", 5, CardType.TROOP, Rarity.RARE, 500, 50, 1.5, 1.0, SpeedType.SLOW,
                TargetType.BUILDINGS, false, false, "Desc", 1, 0);
    }

    private Troop createTroop(Card card, boolean isPlayer) {
        return new Troop(card, new GridPosition(0, 0), isPlayer);
    }

    // Test Case 1: Invalid Target (null)
    @Test
    void testCanTargetNull() {
        Troop troop = createTroop(groundCard, true);
        assertFalse(troop.canTarget(null), "Should not target null");
    }

    // Test Case 2: Type Mismatch (Ground vs Air)
    @Test
    void testCanTargetAirWithGround() {
        Troop groundTroop = createTroop(groundCard, true);
        Troop airTarget = createTroop(airCard, false);

        // Ground card implies TargetType.GROUND
        // In Troop.java: if (tt == TargetType.GROUND && target.isAirUnit()) return false;

        assertFalse(groundTroop.canTarget(airTarget), "Ground targeting unit should not target Air unit");
    }

    // Test Case 3: Specific Targeting (Building Only)
    @Test
    void testCanTargetBuildingOnly() {
        Troop giant = createTroop(buildingTargetCard, true);
        Troop enemyTroop = createTroop(groundCard, false);

        // Create a Building
        // Building constructor: Building(GridPosition pos, int width, int height,
        // boolean isPlayer, int hp, String imagePath, int lifetime)
        Building enemyBuilding = new Building(new GridPosition(5, 5), 3, 3, false, 1000, "path", 30);

        // Giant targets BUILDINGS
        assertFalse(giant.canTarget(enemyTroop), "Building-only targeter should not target troops");
        assertTrue(giant.canTarget(enemyBuilding), "Building-only targeter should target buildings");
    }

    // Test Case 4: Valid Match
    @Test
    void testCanTargetValidEnemy() {
        Troop knight = createTroop(groundCard, true);
        Troop enemyKnight = createTroop(groundCard, false);

        assertTrue(knight.canTarget(enemyKnight), "Should target valid ground enemy");
    }
}
