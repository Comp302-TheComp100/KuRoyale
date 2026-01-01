package com.kuroyale.model.entities;

import com.kuroyale.model.enums.ChallengeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChallengeTest {

    private TestChallenge challenge;

    // Concrete implementation of abstract Challenge class for testing
    private static class TestChallenge extends Challenge {
        public TestChallenge(int id, ChallengeType type, String description, String rules) {
            super(id, type, description, rules);
        }

        @Override
        public List<String> validateDeck(List<Card> deck) {
            return null; // Not testing this
        }
    }

    @BeforeEach
    void setUp() {
        // Use a dummy challenge type
        challenge = new TestChallenge(1, ChallengeType.SWARM_MASTER, "Test Description", "Test Rules");
    }

    /**
     * Test Case 1: Failed attempt.
     * Verifies that when won is false:
     * - attempts increases by 1
     * - completions does not change
     * - completed status remains false
     * - starsEarned does not change
     */
    @Test
    void testRecordAttemptFailure() {
        int initialAttempts = challenge.getAttempts();
        int initialCompletions = challenge.getCompletions();
        int initialStars = challenge.getStarsEarned();

        challenge.recordAttempt(false, 100, 1000);

        assertEquals(initialAttempts + 1, challenge.getAttempts(), "Attempts should increment on failure");
        assertEquals(initialCompletions, challenge.getCompletions(), "Completions should not increment on failure");
        assertFalse(challenge.isCompleted(), "Challenge should not be marked completed on failure");
        assertEquals(initialStars, challenge.getStarsEarned(), "Stars should not change on failure");
    }

    /**
     * Test Case 2: Successful high score attempt.
     * Verifies that when won is true and performance is good (3 stars):
     * - attempts increases by 1
     * - completions increases by 1
     * - completed status becomes true
     * - starsEarned updates to 3
     */
    @Test
    void testRecordAttemptSuccessHighStars() {
        int initialAttempts = challenge.getAttempts();
        int initialCompletions = challenge.getCompletions();

        // 3 stars condition: damageTaken == 0 OR time <= threeStarTimeSeconds (60)
        // Let's use 0 damage to guarantee 3 stars regardless of time
        challenge.recordAttempt(true, 50, 0);

        assertEquals(initialAttempts + 1, challenge.getAttempts(), "Attempts should increment on success");
        assertEquals(initialCompletions + 1, challenge.getCompletions(), "Completions should increment on success");
        assertTrue(challenge.isCompleted(), "Challenge should be marked completed on success");
        assertEquals(3, challenge.getStarsEarned(), "Should earn 3 stars for perfect run");
    }

    /**
     * Test Case 3: Better score updates stars.
     * Verifies that stars update when a better result is achieved.
     * Scenario:
     * 1. First attempt: 1 star
     * 2. Second attempt: 2 stars -> should update to 2
     */
    @Test
    void testRecordAttemptImprovement() {
        // First attempt: 1 star (slow time, took damage)
        // 2 star time is 120s, 3 star is 60s
        challenge.recordAttempt(true, 130, 100);
        assertEquals(1, challenge.getStarsEarned(), "Should have 1 star initially");

        // Second attempt: 2 stars (faster time, but not 3 star time)
        challenge.recordAttempt(true, 100, 100);
        assertEquals(2, challenge.getStarsEarned(), "Stars should update to 2 when performance improves");
    }

    /**
     * Test Case 4: Worse score does not update stars.
     * Verifies that stars do NOT decrease when a worse result is achieved.
     * Scenario:
     * 1. First attempt: 3 stars
     * 2. Second attempt: 1 star -> should remain 3
     */
    @Test
    void testRecordAttemptNoRegression() {
        // First attempt: 3 stars
        challenge.recordAttempt(true, 30, 0);
        assertEquals(3, challenge.getStarsEarned(), "Should have 3 stars initially");

        // Second attempt: 1 star
        challenge.recordAttempt(true, 150, 500);
        assertEquals(3, challenge.getStarsEarned(), "Stars should not decrease when performance is worse");
    }
}
