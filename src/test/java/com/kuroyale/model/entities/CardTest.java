package com.kuroyale.model.entities;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Card class, specifically testing the level up system
 * and the calculateUpgradeCost() method.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CardTest {

    private final CardFactory cardFactory = new CardFactory();

    /**
     * Test 1: Verify upgrade cost from level 1 to 2 for all rarities.
     * Expected costs:
     * - COMMON: 200
     * - RARE: 400
     * - EPIC: 800
     * - LEGENDARY: 1500
     */
    @Test
    @Order(1)
    void testCalculateUpgradeCost_Level1To2_AllRarities() {
        System.out.println("\n=== Test: Level 1 to 2 Upgrade Costs ===");
        
        // Test COMMON rarity (level 1 -> 2)
        Card knight = cardFactory.createKnight();
        knight.setLevel(1);
        int knightCost = knight.calculateUpgradeCost();
        System.out.println("Testing COMMON card: " + knight.getName() + " (Level " + knight.getLevel() + 
                ") -> Expected: 200, Actual: " + knightCost);
        assertEquals(200, knightCost,
                "COMMON card at level 1 should cost 200 to upgrade");

        // Test RARE rarity (level 1 -> 2)
        Card musketeer = cardFactory.createMusketeer();
        musketeer.setLevel(1);
        int musketeerCost = musketeer.calculateUpgradeCost();
        System.out.println("Testing RARE card: " + musketeer.getName() + " (Level " + musketeer.getLevel() + 
                ") -> Expected: 400, Actual: " + musketeerCost);
        assertEquals(400, musketeerCost,
                "RARE card at level 1 should cost 400 to upgrade");

        // Test EPIC rarity (level 1 -> 2)
        Card giant = cardFactory.createGiant();
        giant.setLevel(1);
        int giantCost = giant.calculateUpgradeCost();
        System.out.println("Testing EPIC card: " + giant.getName() + " (Level " + giant.getLevel() + 
                ") -> Expected: 800, Actual: " + giantCost);
        assertEquals(800, giantCost,
                "EPIC card at level 1 should cost 800 to upgrade");

        // Test LEGENDARY rarity (level 1 -> 2)
        Card hogRider = cardFactory.createHogRider();
        hogRider.setLevel(1);
        int hogRiderCost = hogRider.calculateUpgradeCost();
        System.out.println("Testing LEGENDARY card: " + hogRider.getName() + " (Level " + hogRider.getLevel() + 
                ") -> Expected: 1500, Actual: " + hogRiderCost);
        assertEquals(1500, hogRiderCost,
                "LEGENDARY card at level 1 should cost 1500 to upgrade");
        
        System.out.println("✓ All Level 1 to 2 upgrade cost tests passed!\n");
    }

    /**
     * Test 2: Verify upgrade cost from level 2 to 3 for all rarities.
     * Expected costs:
     * - COMMON: 500
     * - RARE: 1000
     * - EPIC: 2000
     * - LEGENDARY: 4000
     */
    @Test
    @Order(2)
    void testCalculateUpgradeCost_Level2To3_AllRarities() {
        System.out.println("\n=== Test: Level 2 to 3 Upgrade Costs ===");
        
        // Test COMMON rarity (level 2 -> 3)
        Card bomber = cardFactory.createBomber();
        bomber.setLevel(2);
        int bomberCost = bomber.calculateUpgradeCost();
        System.out.println("Testing COMMON card: " + bomber.getName() + " (Level " + bomber.getLevel() + 
                ") -> Expected: 500, Actual: " + bomberCost);
        assertEquals(500, bomberCost,
                "COMMON card at level 2 should cost 500 to upgrade");

        // Test RARE rarity (level 2 -> 3)
        Card miniPEKKA = cardFactory.createMiniPEKKA();
        miniPEKKA.setLevel(2);
        int miniPEKKACost = miniPEKKA.calculateUpgradeCost();
        System.out.println("Testing RARE card: " + miniPEKKA.getName() + " (Level " + miniPEKKA.getLevel() + 
                ") -> Expected: 1000, Actual: " + miniPEKKACost);
        assertEquals(1000, miniPEKKACost,
                "RARE card at level 2 should cost 1000 to upgrade");

        // Test EPIC rarity (level 2 -> 3)
        Card giant = cardFactory.createGiant();
        giant.setLevel(2);
        int giantCost = giant.calculateUpgradeCost();
        System.out.println("Testing EPIC card: " + giant.getName() + " (Level " + giant.getLevel() + 
                ") -> Expected: 2000, Actual: " + giantCost);
        assertEquals(2000, giantCost,
                "EPIC card at level 2 should cost 2000 to upgrade");

        // Test LEGENDARY rarity (level 2 -> 3)
        Card hogRider = cardFactory.createHogRider();
        hogRider.setLevel(2);
        int hogRiderCost = hogRider.calculateUpgradeCost();
        System.out.println("Testing LEGENDARY card: " + hogRider.getName() + " (Level " + hogRider.getLevel() + 
                ") -> Expected: 4000, Actual: " + hogRiderCost);
        assertEquals(4000, hogRiderCost,
                "LEGENDARY card at level 2 should cost 4000 to upgrade");
        
        System.out.println("✓ All Level 2 to 3 upgrade cost tests passed!\n");
    }

    /**
     * Test 3: Verify that cards at MAX_LEVEL (3) return 0 cost for upgrade.
     * This should work regardless of rarity.
     */
    @Test
    @Order(3)
    void testCalculateUpgradeCost_MaxLevel_ReturnsZero() {
        System.out.println("\n=== Test: Max Level (Level 3) Returns Zero Cost ===");
        
        // Test COMMON rarity at max level
        Card knight = cardFactory.createKnight();
        knight.setLevel(Card.MAX_LEVEL);
        int knightCost = knight.calculateUpgradeCost();
        System.out.println("Testing COMMON card: " + knight.getName() + " (Level " + knight.getLevel() + 
                ", MAX_LEVEL) -> Expected: 0, Actual: " + knightCost);
        assertEquals(0, knightCost,
                "COMMON card at MAX_LEVEL should return 0 upgrade cost");

        // Test RARE rarity at max level
        Card musketeer = cardFactory.createMusketeer();
        musketeer.setLevel(Card.MAX_LEVEL);
        int musketeerCost = musketeer.calculateUpgradeCost();
        System.out.println("Testing RARE card: " + musketeer.getName() + " (Level " + musketeer.getLevel() + 
                ", MAX_LEVEL) -> Expected: 0, Actual: " + musketeerCost);
        assertEquals(0, musketeerCost,
                "RARE card at MAX_LEVEL should return 0 upgrade cost");

        // Test EPIC rarity at max level
        Card giant = cardFactory.createGiant();
        giant.setLevel(Card.MAX_LEVEL);
        int giantCost = giant.calculateUpgradeCost();
        System.out.println("Testing EPIC card: " + giant.getName() + " (Level " + giant.getLevel() + 
                ", MAX_LEVEL) -> Expected: 0, Actual: " + giantCost);
        assertEquals(0, giantCost,
                "EPIC card at MAX_LEVEL should return 0 upgrade cost");

        // Test LEGENDARY rarity at max level
        Card hogRider = cardFactory.createHogRider();
        hogRider.setLevel(Card.MAX_LEVEL);
        int hogRiderCost = hogRider.calculateUpgradeCost();
        System.out.println("Testing LEGENDARY card: " + hogRider.getName() + " (Level " + hogRider.getLevel() + 
                ", MAX_LEVEL) -> Expected: 0, Actual: " + hogRiderCost);
        assertEquals(0, hogRiderCost,
                "LEGENDARY card at MAX_LEVEL should return 0 upgrade cost");
        
        System.out.println("✓ All MAX_LEVEL tests passed! Cards at max level correctly return 0 cost.\n");
    }
}
