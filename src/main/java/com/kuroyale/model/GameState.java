package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Central game state manager.
 * Holds references to player and bot states, arena, and manages the game loop
 * updates.
 */
public class GameState {
    private final Hand playerHand;
    private final ElixirManager playerElixir;

    private final Hand botHand;
    private final ElixirManager botElixir;
    private final BotLogic bot;

    private final Arena arena;
    private final List<PlacedCard> placedCards;
    private final List<Troop> activeTroops;
    private final com.kuroyale.service.TroopMovementService troopMovementService = new com.kuroyale.service.TroopMovementService();

    private double gameTime = 180.0; // 3 minutes
    private int playerScore = 0;
    private int botScore = 0;

    private boolean isDoubleElixir = false;
    private boolean isGameOver = false;

    public GameState(Deck playerDeck, Deck botDeck, Arena arena) {
        this.playerHand = new Hand(playerDeck);
        this.playerElixir = new ElixirManager();

        this.bot = new BotLogic(botDeck);
        this.botHand = bot.getHand();
        this.botElixir = bot.getElixirManager();

        this.arena = arena;
        this.placedCards = new ArrayList<>();
        this.activeTroops = new ArrayList<>();
    }

    public void update(double deltaTime) {
        if (gameTime > 0) {
            gameTime -= deltaTime;

            // Check for Double Elixir (Last 60 seconds)
            if (gameTime <= 60.0 && !isDoubleElixir) {
                isDoubleElixir = true;
                playerElixir.setDoubleElixir(true);
                botElixir.setDoubleElixir(true);
            }

            if (gameTime <= 0) {
                gameTime = 0;
                if (!isGameOver) {
                    isGameOver = true;
                }
            }
        }

        playerElixir.update(deltaTime);
        botElixir.update(deltaTime); // Ensure bot elixir is also updated

        // Update Bot
        if (!isGameOver) {
            BotLogic.Move botMove = bot.update(deltaTime, this);
            if (botMove != null) {
                placeCard(false, botMove.card, botMove.x, botMove.y);
            }
        }

        // Update placed cards (lifetimes, movement, etc. - future work)
        troopMovementService.updateTroops(deltaTime, this, activeTroops);
    }

    public boolean isDoubleElixir() {
        return isDoubleElixir;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean placeCard(boolean isPlayer, int handIndex, int x, int y) {
        // Validate position
        if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
            return false;
        }

        // Check if card is a spell (can be placed anywhere)
        boolean isSpell = false;
        if (isPlayer) {
            Card card = playerHand.getCard(handIndex);
            if (card != null && card.getType() == CardType.SPELL) {
                isSpell = true;
            }
        }

        // Validate terrain (Grass or Bridge only) - UNLESS it's a spell
        if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
            return false;
        }

        // Validate side (Player can only deploy on bottom half) - UNLESS it's a spell
        if (!isSpell && isPlayer && y < Arena.HEIGHT / 2) {
            return false;
        }

        if (isPlayer) {
            Card card = playerHand.getCard(handIndex);
            if (card != null && playerElixir.spend(card.getCost())) {
                playerHand.playCard(handIndex);
                placedCards.add(new PlacedCard(card, x, y, isPlayer));
                return true;
            }
        } else {
            // Bot placement is handled by BotLogic, but we record it here
            // BotLogic already spent elixir and played card
            // We just need to add to placedCards
            // But wait, placeCard is called with card object for bot in update()
            // Let's overload or adjust
        }
        return false;
    }

    public double getGameTime() {
        return gameTime;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public int getBotScore() {
        return botScore;
    }

    // Overload for direct card placement (used by Bot)
    public void placeCard(boolean isPlayer, Card card, int x, int y) {
        placedCards.add(new PlacedCard(card, x, y, isPlayer));
        if (card.getType() == CardType.TROOP) {
            int count = Math.max(1, card.getCount());
            for (int i = 0; i < count; i++) {
                GridPosition spawn = GridPosition.tryCreate(x, y);
                if (spawn != null) {
                    Troop troop = new Troop(card, spawn, isPlayer);
                    activeTroops.add(troop);
                }
            }
        }
    }

    public Hand getPlayerHand() {
        return playerHand;
    }

    public ElixirManager getPlayerElixir() {
        return playerElixir;
    }

    public Arena getArena() {
        return arena;
    }

    public List<PlacedCard> getPlacedCards() {
        return placedCards;
    }

    public List<Troop> getActiveTroops() { return activeTroops; }

    // Inner class to track placed units
    public static class PlacedCard {
        public final Card card;
        public final int x;
        public final int y;
        public final boolean isPlayer; // true = player, false = bot

        public PlacedCard(Card card, int x, int y, boolean isPlayer) {
            this.card = card;
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }
    }
}
