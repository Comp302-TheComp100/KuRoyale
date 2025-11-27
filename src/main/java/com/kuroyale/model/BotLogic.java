package com.kuroyale.model;

import java.util.Random;

/**
 * Basic AI for the opponent.
 * Strategy: Waits for full elixir, then places a random affordable unit at the
 * bridge.
 */
public class BotLogic {
    private final ElixirManager elixirManager;
    private final Hand hand;
    private final Random random;
    private double timeSinceLastMove;
    private static final double MOVE_DELAY = 2.0; // Seconds between moves

    public BotLogic(Deck deck) {
        this.elixirManager = new ElixirManager();
        this.hand = new Hand(deck);
        this.random = new Random();
        this.timeSinceLastMove = 0;
    }

    /**
     * Updates bot state and decides on moves.
     * 
     * @param deltaTime Time passed in seconds
     * @param gameState Current game state (for checking valid placements)
     * @return A Move object if the bot makes a move, null otherwise
     */
    public Move update(double deltaTime, GameState gameState) {
        elixirManager.update(deltaTime);
        timeSinceLastMove += deltaTime;

        // Simple Logic:
        // 1. Wait until elixir is high (>= 7) or full
        // 2. Wait for move delay
        // 3. Pick a random card from hand
        // 4. If affordable, place it at a random valid position (top half of arena)

        if (timeSinceLastMove >= MOVE_DELAY && elixirManager.getCurrentElixir() >= 7.0) {
            return attemptMove();
        }
        return null;
    }

    private Move attemptMove() {
        // Try to find an affordable card
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card card = hand.getCard(i);
            if (card != null && elixirManager.canAfford(card.getCost())) {
                // Found affordable card

                // Pick random position in top half (opponent side)
                // Arena is 18x32. Top half is y < 16.
                // Spawn area usually y < 14
                int x = random.nextInt(Arena.WIDTH);
                int y = random.nextInt(14);

                // Execute move
                elixirManager.spend(card.getCost());
                hand.playCard(i);
                timeSinceLastMove = 0;

                return new Move(card, x, y);
            }
        }
        return null;
    }

    public ElixirManager getElixirManager() {
        return elixirManager;
    }

    public Hand getHand() {
        return hand;
    }

    public static class Move {
        public final Card card;
        public final int x;
        public final int y;

        public Move(Card card, int x, int y) {
            this.card = card;
            this.x = x;
            this.y = y;
        }
    }
}
