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

    public GameState(Deck playerDeck, Deck botDeck, Arena arena) {
        this.playerHand = new Hand(playerDeck);
        this.playerElixir = new ElixirManager();

        this.bot = new BotLogic(botDeck);
        this.botHand = bot.getHand();
        this.botElixir = bot.getElixirManager();

        this.arena = arena;
        this.placedCards = new ArrayList<>();
    }

    public void update(double deltaTime) {
        playerElixir.update(deltaTime);

        // Update Bot
        BotLogic.Move botMove = bot.update(deltaTime, this);
        if (botMove != null) {
            placeCard(false, botMove.card, botMove.x, botMove.y);
        }

        // Update placed cards (lifetimes, movement, etc. - future work)
    }

    public boolean placeCard(boolean isPlayer, int handIndex, int x, int y) {
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

    // Overload for direct card placement (used by Bot)
    public void placeCard(boolean isPlayer, Card card, int x, int y) {
        placedCards.add(new PlacedCard(card, x, y, isPlayer));
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
