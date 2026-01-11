package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;

import com.kuroyale.event.GameEventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/* Game state for Local Player vs Player battles.
 * Replaces BotLogic with second human player controls.
 * Uses TurnManager for turn-based gameplay.*/
public class PvPGameState implements IBattleState {

    // Player 1 (left side - bottom half of arena)
    private final Hand player1Hand, player2Hand;
    private final ElixirManager player1Elixir, player2Elixir;

    private final Arena arena;
    private final TurnManager turnManager;
    private final List<PlacedCard> placedCards;
    private final List<Troop> activeTroops;
    private final List<Building> activeBuildings;
    private final com.kuroyale.service.TroopMovementService troopMovementService = new com.kuroyale.service.TroopMovementService();
    private final com.kuroyale.service.CombatService combatService = new com.kuroyale.service.CombatService();

    private double gameTime = 180.0; // 3 minutes
    private int player1Score = 0, player2Score = 0;

    private boolean isDoubleElixir = false, isGameOver = false;
    private TurnManager.Turn winner = null;

    private Set<Tower> scoredTowers = new HashSet<>();
    private java.util.function.Function<String, Card> cardCatalog;

    public PvPGameState(Deck player1Deck, Deck player2Deck, Arena arena) {
        this.player1Hand = new Hand(player1Deck);
        this.player1Elixir = new ElixirManager();

        this.player2Hand = new Hand(player2Deck);
        this.player2Elixir = new ElixirManager();

        this.arena = arena;
        this.turnManager = new TurnManager();
        this.placedCards = new ArrayList<>();
        this.activeTroops = new ArrayList<>();
        this.activeBuildings = new ArrayList<>();

        // Initialize Combo Service
        this.comboService.setGameState(this);
    }

    private final com.kuroyale.service.ComboService comboService = new com.kuroyale.service.ComboService();

    public com.kuroyale.service.ComboService getComboService() {
        return comboService;
    }

    public void setCardCatalog(java.util.function.Function<String, Card> cardCatalog) {
        this.cardCatalog = cardCatalog;
    }

    public void cleanup() {
        if (comboService != null) {
            comboService.cleanup();
        }
    }

    public void update(double deltaTime) {
        updateGameTimer(deltaTime);

        // Both players' elixir regenerates simultaneously (not turn-based)
        player1Elixir.update(deltaTime);
        player2Elixir.update(deltaTime);

        updateEntities(deltaTime);
        handleCombat(deltaTime);
        cleanupEntities();
        checkWinConditions();
    }

    private void updateGameTimer(double deltaTime) {
        if (gameTime > 0) {
            gameTime -= deltaTime;

            // Double Elixir in last 60 seconds
            if (gameTime <= 60.0 && !isDoubleElixir) {
                isDoubleElixir = true;
                player1Elixir.setDoubleElixir(true);
                player2Elixir.setDoubleElixir(true);
            }

            if (gameTime <= 0) {
                gameTime = 0;
                if (!isGameOver) {
                    isGameOver = true;
                    if (player1Score > player2Score) {
                        winner = TurnManager.Turn.PLAYER_1;
                    } else if (player2Score > player1Score) {
                        winner = TurnManager.Turn.PLAYER_2;
                    } else {
                        // Tiebreaker: Compare lowest HP towers
                        double p1MinHP = getLowestTowerHealth(true);
                        double p2MinHP = getLowestTowerHealth(false);

                        if (p1MinHP < p2MinHP) {
                            winner = TurnManager.Turn.PLAYER_2; // Player 1 has weaker tower
                        } else if (p2MinHP < p1MinHP) {
                            winner = TurnManager.Turn.PLAYER_1; // Player 2 has weaker tower
                        } else {
                            winner = null; // Draw
                        }
                    }
                }
            }
        }
    }

    private double getLowestTowerHealth(boolean isPlayer1Side) {
        return arena.getAllTowers().stream()
                .filter(t -> t.isPlayerSide() == isPlayer1Side && t.isAlive())
                .mapToDouble(Tower::getCurrentHealth)
                .min()
                .orElse(0.0);
    }

    private void updateEntities(double deltaTime) {
        troopMovementService.updateTroops(deltaTime, this, activeTroops);

        for (Building b : activeBuildings) {
            if (b.isAlive()) {
                b.update(deltaTime);
            }
        }
    }

    private void handleCombat(double deltaTime) {
        combatService.update(deltaTime, this);
        activeTroops.removeIf(t -> !t.isAlive());
    }

    private void cleanupEntities() {
        java.util.Iterator<Building> it = activeBuildings.iterator();
        while (it.hasNext()) {
            Building b = it.next();
            if (!b.isAlive()) {
                arena.getSpatialGrid().remove(b);
                it.remove();
            }
        }

        if (!isGameOver) {
            checkAndScoreDestroyedTowers();
        }
        arena.removeDeadTowers();
    }

    private void checkWinConditions() {
        if (!isGameOver) {
            boolean player1KingAlive = arena.isPlayerKingAlive();
            boolean player2KingAlive = arena.isBotKingAlive(); // "Bot" side is Player 2

            if (!player1KingAlive) {
                isGameOver = true;
                winner = TurnManager.Turn.PLAYER_2;
            } else if (!player2KingAlive) {
                isGameOver = true;
                winner = TurnManager.Turn.PLAYER_1;
            }
        }
    }

    // Place a card for a player. In PvP, we check if it's the player's turn.
    public boolean placeCard(boolean isPlayer1, int handIndex, int x, int y) {
        Hand hand = isPlayer1 ? player1Hand : player2Hand;
        ElixirManager elixir = isPlayer1 ? player1Elixir : player2Elixir;

        // Validate position bounds
        if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
            return false;
        }

        Card card = hand.getCard(handIndex);
        if (card == null)
            return false;

        boolean isSpell = card.getType() == CardType.SPELL;

        // Validate terrain
        if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
            return false;
        }

        // Validate side: Player 1 bottom half (y >= HEIGHT/2), Player 2 top half (y <
        // HEIGHT/2)
        if (isPlayer1 && !isSpell && y < Arena.HEIGHT / 2) {
            return false;
        }
        if (!isPlayer1 && !isSpell && y >= Arena.HEIGHT / 2) {
            return false;
        }

        int cost = card.getCost();
        if (elixir.getCurrentElixir() >= cost) {
            boolean success = spawnUnit(isPlayer1, card, x, y);

            if (success) {
                elixir.spend(cost);
                hand.playCard(handIndex);
                GameEventBus.getInstance().publishElixirSpent(isPlayer1, cost);
            }

            return success;
        }

        return false;
    }

    private boolean spawnUnit(boolean isPlayer1, Card card, int x, int y) {
        if (card == null)
            return false;

        java.util.List<ICombatant> spawnedUnits = null;
        if (card.getType() == CardType.BUILDING) {
            spawnedUnits = spawnBuilding(isPlayer1, card, x, y);
        } else if (card.getType() == CardType.TROOP) {
            spawnedUnits = spawnTroopGroup(isPlayer1, card, x, y);
        } else if (card.getType() == CardType.SPELL) {
            applySpellEffect(isPlayer1, card, x, y);
            spawnedUnits = new java.util.ArrayList<>();
        }

        if (spawnedUnits != null) {
            placedCards.add(new PlacedCard(card, x, y, isPlayer1));
            // Publish for both players/bot so ComboService can detect
            GameEventBus.getInstance().publishCardPlayed(isPlayer1, card, spawnedUnits);
            return true;
        }

        return false;
    }

    private java.util.List<ICombatant> spawnBuilding(boolean isPlayer1, Card card, int x, int y) {
        int bw = Math.max(1, card.getFootprintWidthTiles());
        int bh = Math.max(1, card.getFootprintHeightTiles());

        if (x < 0 || y < 0 || (x + bw) > Arena.WIDTH || (y + bh) > Arena.HEIGHT) {
            return null;
        }

        for (int dx = 0; dx < bw; dx++) {
            for (int dy = 0; dy < bh; dy++) {
                GridCell c = arena.getCell(x + dx, y + dy);
                if (c == null || c.isOccupied() || !c.isWalkable()) {
                    return null;
                }
            }
        }

        GridPosition topLeft = GridPosition.tryCreate(x, y);
        if (topLeft != null) {
            Building building = new Building(topLeft, bw, bh, isPlayer1, card.getHp(), card.getImagePath(),
                    card.getLifetime());
            building.configureCombatFromCard(card);

            if (card.getSpawnUnitName() != null) {
                building.setAttackCooldown(1.0);
            }

            arena.occupyFootprint(building);
            activeBuildings.add(building);
            arena.getSpatialGrid().add(building);

            java.util.List<ICombatant> result = new java.util.ArrayList<>();
            result.add(building);
            return result;
        }
        return null;
    }

    private java.util.List<ICombatant> spawnTroopGroup(boolean isPlayer1, Card card, int x, int y) {
        final int[][] OFFSETS = {
                { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
                { 1, 1 }, { -1, -1 }, { 1, -1 }, { -1, 1 },
                { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 }, { 2, 2 }, { -2, -2 }
        };

        int count = Math.max(1, card.getCount());
        java.util.List<ICombatant> spawned = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++) {
            int[] offset = (i < OFFSETS.length) ? OFFSETS[i] : OFFSETS[0];
            int spawnX = x + offset[0];
            int spawnY = y + offset[1];

            boolean isValidPos = (spawnX >= 0 && spawnX < Arena.WIDTH && spawnY >= 0 && spawnY < Arena.HEIGHT);
            if (isValidPos) {
                GridCell cell = arena.getCell(spawnX, spawnY);
                if (cell == null || !cell.isWalkable()) {
                    isValidPos = false;
                }
            }

            if (!isValidPos) {
                spawnX = x;
                spawnY = y;
            }

            GridPosition spawn = GridPosition.tryCreate(spawnX, spawnY);
            if (spawn != null) {
                Troop troop = new Troop(card, spawn, isPlayer1);
                activeTroops.add(troop);
                arena.getSpatialGrid().add(troop);
                spawned.add(troop);
            }
        }
        return spawned.isEmpty() ? null : spawned;
    }

    // Spawns troops directly (used by buildings/spells)
    public boolean spawnTroopDirectly(boolean isPlayerSide, Card card, int x, int y, int count) {
        if (card == null)
            return false;

        Card spawnCard = card;
        if (count > 0 && count != card.getCount()) {
            spawnCard = new Card(card.getName(), card.getCost(), card.getType(), card.getRarity(),
                    card.getBaseHp(), card.getBaseDamage(), card.getHitSpeed(), card.getRange(),
                    card.getSpeed(), card.getTarget(), card.isAirUnit(), card.isAreaEffect(),
                    card.getDescription(), count, card.getLifetime());
            spawnCard.setLevel(card.getLevel());
        }

        return spawnTroopGroup(isPlayerSide, spawnCard, x, y) != null;
    }

    public GridPosition getFrontPosition(Building b) {
        if (b == null)
            return null;
        int bw = b.getWidth(), bh = b.getHeight(), x = b.getPosition().getX(), y = b.getPosition().getY();

        int spawnX = x + bw / 2;
        int spawnY;

        if (b.isPlayerSide()) {
            spawnY = y - 1; // "Above" for player 1 (bottom side)
        } else {
            spawnY = y + bh; // "Below" for player 2 (top side)
        }

        return GridPosition.tryCreate(spawnX, spawnY);
    }

    public Card getCardByName(String name) {
        if (cardCatalog != null) {
            return cardCatalog.apply(name);
        }
        return null;
    }

    public ElixirManager getElixirManager(boolean isPlayerSide) {
        return isPlayerSide ? player1Elixir : player2Elixir;
    }

    private void applySpellEffect(boolean isPlayer1, Card spell, int x, int y) {
        double radius = Math.max(0, spell.getRange());
        double damage = Math.max(0, spell.getDamage());
        GridPosition center = GridPosition.tryCreate(x, y);
        if (center == null)
            return;

        combatService.applyAreaDamage(this, center, radius, damage, TargetType.BOTH, isPlayer1, true,
                spell.getStunDuration());
    }

    private void checkAndScoreDestroyedTowers() {
        Set<Tower> towers = arena.getAllTowers();

        for (Tower tower : towers) {
            if (scoredTowers.contains(tower) || tower.isAlive())
                continue;

            scoredTowers.add(tower);

            boolean isPlayer1Tower = tower.isPlayerSide();
            boolean isKingTower = tower.getType() == Tower.TowerType.KING;

            if (isKingTower) {
                if (isPlayer1Tower) {
                    player2Score = 3;
                    isGameOver = true;
                    winner = TurnManager.Turn.PLAYER_2;
                } else {
                    player1Score = 3;
                    isGameOver = true;
                    winner = TurnManager.Turn.PLAYER_1;
                }
            } else {
                if (isPlayer1Tower) {
                    player2Score++;
                } else {
                    player1Score++;
                }
            }

            GameEventBus.getInstance().publishTowerDestroyed(isPlayer1Tower, tower);
        }
    }

    // Getters
    public Hand getPlayer1Hand() {
        return player1Hand;
    }

    public Hand getPlayer2Hand() {
        return player2Hand;
    }

    public ElixirManager getPlayer1Elixir() {
        return player1Elixir;
    }

    public ElixirManager getPlayer2Elixir() {
        return player2Elixir;
    }

    public TurnManager getTurnManager() {
        return turnManager;
    }

    public Arena getArena() {
        return arena;
    }

    public double getGameTime() {
        return gameTime;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public boolean isDoubleElixir() {
        return isDoubleElixir;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public TurnManager.Turn getWinner() {
        return winner;
    }

    public List<Troop> getActiveTroops() {
        return activeTroops;
    }

    public List<Building> getActiveBuildings() {
        return activeBuildings;
    }

    public List<PlacedCard> getPlacedCards() {
        return placedCards;
    }

    // Inner class for placed cards
    public static class PlacedCard {
        public final Card card;
        public final int x, y;
        public final boolean isPlayer1;

        public PlacedCard(Card card, int x, int y, boolean isPlayer1) {
            this.card = card;
            this.x = x;
            this.y = y;
            this.isPlayer1 = isPlayer1;
        }
    }
}