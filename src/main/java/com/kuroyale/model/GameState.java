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
    private final List<Building> activeBuildings;
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
        this.activeBuildings = new ArrayList<>();
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

        // Cleanup destroyed buildings (future: decay timers)
        activeBuildings.removeIf(b -> !b.isAlive());
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
        Card pendingCard = null;
        if (isPlayer) {
            pendingCard = playerHand.getCard(handIndex);
            if (pendingCard != null && pendingCard.getType() == CardType.SPELL) {
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
            Card card = pendingCard != null ? pendingCard : playerHand.getCard(handIndex);
            if (card != null && playerElixir.spend(card.getCost())) {
                playerHand.playCard(handIndex);
                if (card.getType() == CardType.TROOP) {
                    placedCards.add(new PlacedCard(card, x, y, isPlayer));
                    int count = Math.max(1, card.getCount());
                    for (int i = 0; i < count; i++) {
                        GridPosition spawn = GridPosition.tryCreate(x, y);
                        if (spawn != null) {
                            Troop troop = new Troop(card, spawn, isPlayer);
                            activeTroops.add(troop);
                        }
                    }
                } else if (card.getType() == CardType.BUILDING) {
                    // Building footprint from card metadata (defaults 3x3)
                    int bw = Math.max(1, card.getFootprintWidthTiles());
                    int bh = Math.max(1, card.getFootprintHeightTiles());
                    // Prevent exceeding bounds and enforce margin: (building size - 1)
                    int mx = Math.max(0, bw - 1);
                    int my = Math.max(0, bh - 1);
                    if (x < mx || y < my || (x + bw) > (Arena.WIDTH - mx) || (y + bh) > (Arena.HEIGHT - my)) {
                        return false; // invalid placement; do not accept
                    }
                    // Validate all cells in footprint are placeable (not water/tower/occupied)
                    for (int dx = 0; dx < bw; dx++) {
                        for (int dy = 0; dy < bh; dy++) {
                            GridCell c = arena.getCell(x + dx, y + dy);
                            if (c == null || c.isOccupied() || !c.isWalkable()) {
                                return false; // invalid footprint region
                            }
                        }
                    }
                    GridPosition topLeft = GridPosition.tryCreate(x, y);
                    if (topLeft != null) {
                        placedCards.add(new PlacedCard(card, x, y, isPlayer));
                        Building building = new Building(topLeft, bw, bh, isPlayer, card.getHp(), card.getImagePath());
                        occupyFootprint(building);
                        activeBuildings.add(building);
                    }
                }
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
        if (card.getType() == CardType.TROOP) {
            placedCards.add(new PlacedCard(card, x, y, isPlayer));
            int count = Math.max(1, card.getCount());
            for (int i = 0; i < count; i++) {
                GridPosition spawn = GridPosition.tryCreate(x, y);
                if (spawn != null) {
                    Troop troop = new Troop(card, spawn, isPlayer);
                    activeTroops.add(troop);
                }
            }
        } else if (card.getType() == CardType.BUILDING) {
            int bw = Math.max(1, card.getFootprintWidthTiles());
            int bh = Math.max(1, card.getFootprintHeightTiles());
            int mx = Math.max(0, bw - 1);
            int my = Math.max(0, bh - 1);
            if (x < mx || y < my || (x + bw) > (Arena.WIDTH - mx) || (y + bh) > (Arena.HEIGHT - my)) {
                return; // ignore invalid bot placement
            }
            for (int dx = 0; dx < bw; dx++) {
                for (int dy = 0; dy < bh; dy++) {
                    GridCell c = arena.getCell(x + dx, y + dy);
                    if (c == null || c.isOccupied() || !c.isWalkable()) {
                        return; // invalid area
                    }
                }
            }
            GridPosition topLeft = GridPosition.tryCreate(x, y);
            if (topLeft != null) {
                placedCards.add(new PlacedCard(card, x, y, isPlayer));
                Building building = new Building(topLeft, bw, bh, isPlayer, card.getHp(), card.getImagePath());
                occupyFootprint(building);
                activeBuildings.add(building);
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

    public List<Troop> getActiveTroops() {
         return activeTroops; 
    }

    public List<Building> getActiveBuildings() {
        return activeBuildings;
    }

    private void occupyFootprint(Building b) {
        for (int dx = 0; dx < b.getWidth(); dx++) {
            for (int dy = 0; dy < b.getHeight(); dy++) {
                int gx = b.getPosition().getX() + dx;
                int gy = b.getPosition().getY() + dy;
                GridPosition pos = GridPosition.tryCreate(gx, gy);
                if (pos != null) {
                    GridCell cell = arena.getCell(pos);
                    if (cell != null) {
                        // Mark as occupied to block placement and pathfinding
                        try {
                            cell.setOccupant(b);
                        } catch (IllegalStateException e) {
                            // ignore if invalid (e.g., water); future: adjust placement
                        }
                    }
                }
            }
        }
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
