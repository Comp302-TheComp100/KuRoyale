package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;

import java.util.Random;

//Basic AI for the opponent. Waits for full elixir, then places a random affordable unit at the bridge.
public class BotLogic {
    private final ElixirManager elixirManager;
    private final Hand hand;
    private final Random random;
    private double timeSinceLastMove;
    private static final double MOVE_DELAY = 2.0; // Seconds between moves

    // Tracking for combo logic
    private Card lastPlayedCard;
    private double timeSinceLastPlayed; // Seconds
    private static final double COMBO_WINDOW = 5.0;

    public BotLogic(Deck deck) {
        this.elixirManager = new ElixirManager();
        this.hand = new Hand(deck);
        this.random = new Random();
        this.timeSinceLastMove = 0;
        this.timeSinceLastPlayed = 100.0; // Start with no recent play
    }

    // Updates bot state and decides on moves
    public Move update(double deltaTime, GameState gameState) {
        timeSinceLastMove += deltaTime;
        timeSinceLastPlayed += deltaTime;

        // 1. Wait until elixir is high (>= 7) or full
        // 2. Wait for move delay
        // 3. Pick a smart card from hand (prioritizing combos)
        // 4. If affordable, place it

        if (timeSinceLastMove >= MOVE_DELAY && elixirManager.getCurrentElixir() >= 7.0) {
            return attemptMove(gameState);
        }
        return null;
    }

    private Move attemptMove(GameState gameState) {
        // 1. Identify affordable cards
        java.util.List<Integer> affordableIndices = new java.util.ArrayList<>();
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card card = hand.getCard(i);
            if (card != null && elixirManager.canAfford(card.getCost())) {
                affordableIndices.add(i);
            }
        }

        if (affordableIndices.isEmpty()) {
            return null;
        }

        int bestIndex = -1;

        // 2. Check for Combos if within window
        if (lastPlayedCard != null && timeSinceLastPlayed <= COMBO_WINDOW) {
            bestIndex = findComboCard(affordableIndices);
        }

        // 3. Fallback: Random selection
        if (bestIndex == -1) {
            bestIndex = affordableIndices.get(random.nextInt(affordableIndices.size()));
        }

        // Execute move
        Card cardToPlay = hand.getCard(bestIndex);

        // Pick random position in top half (opponent side)
        // Arena is 18x32. Top half is y < 16. Spawn area y < 14
        // Be smarter for buildings? For now random is fine.
        int x = random.nextInt(Arena.WIDTH);
        int y = random.nextInt(14);

        // Try to find a valid position (retry a few times if needed)
        for (int attempt = 0; attempt < 10; attempt++) {
            if (gameState.getArena().isValidPosition(x, y) &&
                    gameState.getArena().getCell(x, y).canPlaceUnit()) {

                elixirManager.spend(cardToPlay.getCost());
                hand.playCard(bestIndex);
                timeSinceLastMove = 0;

                // Construct fake "spawned units" or just skip if logic doesn't strictly require
                // it for this tracking
                // But we must update lastPlayedCard
                lastPlayedCard = cardToPlay;
                timeSinceLastPlayed = 0;

                if (bestIndex != -1) { // If it was a combo choice
                    System.out.println("BOT: Played " + cardToPlay.getName() + " (Combo attempt with "
                            + lastPlayedCard.getName() + ")");
                }

                return new Move(cardToPlay, x, y);
            }
            // Retry
            x = random.nextInt(Arena.WIDTH);
            y = random.nextInt(14);
        }

        return null;
    }

    private int findComboCard(java.util.List<Integer> indices) {
        // Priority 1: Siege Mode (Mortar + Defensive Building)
        for (int i : indices) {
            if (checkSiegeMode(hand.getCard(i)))
                return i;
        }

        // Priority 2: Air Assault (Minion + Minion)
        for (int i : indices) {
            if (checkAirAssault(hand.getCard(i)))
                return i;
        }

        // Priority 3: Other Combos (Tank+Support, etc.)
        for (int i : indices) {
            if (checkOtherCombos(hand.getCard(i)))
                return i;
        }

        return -1;
    }

    // --- Combo Check Helpers ---

    private boolean checkSiegeMode(Card current) {
        boolean mortarPlayed = lastPlayedCard.getName().equals("Mortar") || current.getName().equals("Mortar");
        boolean defensePlayed = isDefensiveBuilding(lastPlayedCard) || isDefensiveBuilding(current);
        return mortarPlayed && defensePlayed;
    }

    private boolean checkAirAssault(Card current) {
        return lastPlayedCard.getName().contains("Minion") && current.getName().contains("Minion");
    }

    private boolean checkOtherCombos(Card current) {
        // Tank + Support
        if (isTank(lastPlayedCard) && isRangedTroop(current))
            return true;

        // Swarm
        if (isSwarm(lastPlayedCard) && isSwarm(current))
            return true;

        // Building Defense
        if (lastPlayedCard.getType() == com.kuroyale.model.enums.CardType.BUILDING &&
                current.getType() == com.kuroyale.model.enums.CardType.BUILDING)
            return true;

        // Royal Combo
        boolean knight = lastPlayedCard.getName().equals("Knight") || current.getName().equals("Knight");
        boolean archers = lastPlayedCard.getName().equals("Archers") || current.getName().equals("Archers");
        if (knight && archers)
            return true;

        return false;
    }

    // --- Helpers from ComboService (Duplicated simplified) ---
    private boolean isDefensiveBuilding(Card c) {
        return c.getType() == com.kuroyale.model.enums.CardType.BUILDING &&
                !c.getName().equals("Mortar") && !c.getName().equals("X-Bow");
    }

    private boolean isTank(Card c) {
        return c.getName().equals("Giant") || c.getName().equals("Knight") ||
                c.getName().equals("Golem") || c.getName().equals("P.E.K.K.A");
    }

    private boolean isRangedTroop(Card c) {
        String n = c.getName();
        return n.equals("Musketeer") || n.equals("Archers") || n.equals("Spear Goblins") ||
                n.equals("Wizard") || n.equals("Witch");
    }

    private boolean isSwarm(Card c) {
        return c.getCount() >= 3;
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
