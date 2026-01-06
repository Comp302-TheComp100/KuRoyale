package com.kuroyale.model.logic;

import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.GridPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private GameState gameState;
    private Arena arena;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        System.out.println("Starting test: " + testInfo.getDisplayName());
        // Setup a basic arena and game state
        // We use empty decks for simplicity as we are testing win conditions only
        Deck deck = new Deck();
        ArenaLayout layout = new ArenaLayout("Test Layout");
        // Manually place towers effectively to simulate specific scenarios if needed,
        // but default layout places them standardly.
        // For testing "King Death", we need to damage them.

        // Populate layout with standard positions
        layout.setKingTowerPosition(new GridPosition(8, 28)); // User King
        layout.setPrincessTowerPositions(new ArrayList<>()); // Skip princess for simplicity

        arena = new Arena(layout);
        gameState = new GameState(new Deck(), new Deck(), arena);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        System.out.println("Finished test: " + testInfo.getDisplayName());
    }

    /**
     * Test Case 1: Instant Victory (King Tower Destruction)
     * Expected: Game ends immediately when a King Tower dies.
     */
    @Test
    void testInstantWin_KingDeath() {
        // Setup: Find the player's King Tower and kill it
        assertFalse(gameState.isGameOver(), "Game should start not over");

        Tower playerKing = arena.getAllTowers().stream()
                .filter(t -> t.getType() == Tower.TowerType.KING && t.isPlayerSide())
                .findFirst()
                .orElseThrow();

        // Deal fatal damage
        playerKing.takeDamage(playerKing.getMaxHealth()); // Should die

        // Update game loop (triggers checkWinConditions)
        gameState.update(0.1);

        // Check effects
        assertTrue(gameState.isGameOver(), "Game should be over after King death");
        assertFalse(gameState.isPlayerWinner(), "Player should lose if their King died");
    }

    /**
     * Test Case 2: Victory by Points (Time Limit)
     * Expected: When time runs out, higher score wins.
     */
    @Test
    void testPointsWin_TimeLimit() {
        // Setup: Destroy a bot princess tower to give player score lead
        // We need to simulate score. Direct score manipulation isn't public,
        // so we must destroy a tower.
        // Let's add a bot princess tower manually or assume one exists if we used
        // standard layout.
        // Since we used custom layout in setUp with only Kings, let's manually add one
        // for Bot to die.

        // Actually, we can just kill the Bot King to win? No, that's instant win.
        // We need to win by POINTS.
        // Let's rely on internal score update logic.
        // Easier approach: Use reflection or create a scenario where score increases
        // without ending game.
        // Destroying a Princess Tower increases score by 1 but doesn't end game.

        // But our setUp only has Kings. Let's make a new setup with Princess towers.
        ArenaLayout fullLayout = new ArenaLayout("Full Layout");
        fullLayout.setKingTowerPosition(new GridPosition(8, 28));
        java.util.List<GridPosition> princessPos = new ArrayList<>();
        princessPos.add(new GridPosition(3, 24)); // Player Princess
        fullLayout.setPrincessTowerPositions(princessPos);

        Arena fullArena = new Arena(fullLayout);
        GameState fsGameState = new GameState(new Deck(), new Deck(), fullArena);

        // Find Bot Princess Tower (Mirrored from player's)
        Tower botPrincess = fullArena.getAllTowers().stream()
                .filter(t -> t.getType() == Tower.TowerType.PRINCESS && !t.isPlayerSide())
                .findFirst()
                .orElseThrow();

        // Kill Bot Princess
        botPrincess.takeDamage(botPrincess.getMaxHealth());
        fsGameState.update(0.1); // Process death and scoring

        assertEquals(1, fsGameState.getPlayerScore());
        assertEquals(0, fsGameState.getBotScore());

        // Now simulate time expiring
        // gameTime starts at 180. We need to drain it.
        // Since update takes deltaTime, we can pass a huge deltaTime or loop.
        // Passing 200.0 seconds.
        fsGameState.update(200.0);

        assertTrue(fsGameState.isGameOver(), "Game should be over due to time");
        assertTrue(fsGameState.isPlayerWinner(), "Player should win by points (1-0)");
    }

    /**
     * Test Case 3: Tiebreaker (Lowest HP)
     * Expected: Scores tied, time runs out -> Compare lowest tower HP.
     */
    @Test
    void testTiebreakerWin_LowestHP() {
        // Setup: Both have King Towers only (Score 0-0).
        // Damage Bot King slightly more than Player King.
        // Player King: 3000 -> 2500
        // Bot King: 3000 -> 2400 (Bot is weaker)
        // Result: Player wins.

        Tower playerKing = arena.getAllTowers().stream()
                .filter(t -> t.isPlayerSide() && t.getType() == Tower.TowerType.KING)
                .findFirst().orElseThrow();

        Tower botKing = arena.getAllTowers().stream()
                .filter(t -> !t.isPlayerSide() && t.getType() == Tower.TowerType.KING)
                .findFirst().orElseThrow();

        double maxHp = playerKing.getMaxHealth(); // 2400
        playerKing.setCurrentHealth(2000);
        botKing.setCurrentHealth(1000); // Weaker

        // Verify state strictly before time out
        assertEquals(0, gameState.getPlayerScore());
        assertEquals(0, gameState.getBotScore());

        // Expire time
        gameState.update(200.0);

        assertTrue(gameState.isGameOver());
        assertTrue(gameState.isPlayerWinner(), "Player should win tiebreaker because Bot has lower HP tower");
    }
}
