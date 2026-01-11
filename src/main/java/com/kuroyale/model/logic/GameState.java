package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.event.GameEventBus;

import java.util.ArrayList;
import java.util.List;

//Central game state manager.Holds references to player and bot states, arena, and manages the game loop updates.
public class GameState implements IBattleState {
    private final Hand playerHand;
    private final ElixirManager playerElixir, botElixir;
    private final BotLogic bot;
    private java.util.function.Function<String, Card> cardCatalog;

    private final Arena arena;
    private final List<PlacedCard> placedCards;
    private final List<Troop> activeTroops;
    private final List<Building> activeBuildings;
    private final com.kuroyale.service.TroopMovementService troopMovementService = new com.kuroyale.service.TroopMovementService();
    private final com.kuroyale.service.CombatService combatService = new com.kuroyale.service.CombatService();

    private double gameTime = 180.0; // 3 minutes
    private int playerScore = 0, botScore = 0;

    private boolean isDoubleElixir = false, isGameOver = false, playerWon = false, isDraw = false;

    // Track towers that have already been scored to avoid double-counting
    private java.util.Set<Tower> scoredTowers = new java.util.HashSet<>();

    // Challenge Context
    private ChallengeType activeChallenge;

    public GameState(Deck playerDeck, Deck botDeck, Arena arena) {
        this.playerHand = new Hand(playerDeck);
        this.playerElixir = new ElixirManager();

        this.bot = new BotLogic(botDeck);
        // Removed: this.botHand = bot.getHand();
        this.botElixir = bot.getElixirManager();

        this.arena = arena;
        this.placedCards = new ArrayList<>();
        this.activeTroops = new ArrayList<>();
        this.activeBuildings = new ArrayList<>();
    }

    public void setCardCatalog(java.util.function.Function<String, Card> cardCatalog) {
        this.cardCatalog = cardCatalog;
    }

    public Card getCardByName(String name) {
        if (cardCatalog != null) {
            return cardCatalog.apply(name);
        }
        return null;
    }

    public void setActiveChallenge(ChallengeType activeChallenge) {
        this.activeChallenge = activeChallenge;
    }

    // Restores game state from saved data
    public void restoreFromSaved(double gameTime, boolean isDoubleElixir, int playerScore, int botScore,
            double playerElixir, double botElixir) {
        this.gameTime = gameTime;
        this.isDoubleElixir = isDoubleElixir;
        this.playerScore = playerScore;
        this.botScore = botScore;

        // Restore elixir
        this.playerElixir.setCurrentElixir(playerElixir);
        this.botElixir.setCurrentElixir(botElixir);

        if (isDoubleElixir) {
            this.playerElixir.setDoubleElixir(true);
            this.botElixir.setDoubleElixir(true);
        }
    }

    /*
     * Restores the player's hand and draw pile from saved card data.
     * This ensures the exact same cards appear in hand after loading a saved game.
     */
    public void restorePlayerHand(List<Card> handCards, List<Card> drawPileCards) {
        playerHand.restoreFromSaved(handCards, drawPileCards);
    }

    // Restores tower health from saved data
    public void restoreTowerHealth(SavedGameState.SavedTower savedTower) {
        // Find the matching tower in the arena efficiently
        Tower.TowerType type = Tower.TowerType.valueOf(savedTower.getTowerType());
        java.util.List<Tower> towers = arena.getTowersByType(type, savedTower.isPlayerSide());

        for (Tower tower : towers) {
            GridPosition pos = tower.getPosition();
            if (pos != null && pos.getX() == savedTower.getGridX() && pos.getY() == savedTower.getGridY()) {
                tower.setCurrentHealth(savedTower.getCurrentHealth());
                break;
            }
        }
    }

    public void update(double deltaTime) {
        updateGameTimer(deltaTime);

        playerElixir.update(deltaTime);
        botElixir.update(deltaTime);

        if (!isGameOver) {
            updateBot(deltaTime);
        }

        updateEntities(deltaTime);
        handleCombat(deltaTime);
        cleanupEntities();
        checkWinConditions();
    }

    private void updateGameTimer(double deltaTime) {
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
                    if (playerScore > botScore) {
                        playerWon = true;
                    } else if (botScore > playerScore) {
                        // Bot wins
                        playerWon = false;
                    } else {
                        // Tiebreaker: Compare lowest HP towers
                        // The side with the single lowest health tower loses.
                        double playerMinHP = getLowestTowerHealth(true);
                        double botMinHP = getLowestTowerHealth(false);

                        if (playerMinHP < botMinHP) {
                            // Player has the weakest tower -> Player loses
                            playerWon = false;
                        } else if (botMinHP < playerMinHP) {
                            // Bot has the weakest tower -> Player wins
                            playerWon = true;
                        } else {
                            // Equal lowest HP -> True Draw (very rare)
                            isDraw = true;
                            playerWon = false;
                        }
                    }
                }
            }
        }
    }

    private double getLowestTowerHealth(boolean isPlayer) {
        return arena.getAllTowers().stream()
                .filter(t -> t.isPlayerSide() == isPlayer && t.isAlive())
                .mapToDouble(Tower::getCurrentHealth)
                .min()
                .orElse(0.0);
    }

    private void updateBot(double deltaTime) {
        BotLogic.Move botMove = bot.update(deltaTime, this);
        if (botMove != null) {
            placeCard(false, botMove.card, botMove.x, botMove.y);
        }
    }

    private void updateEntities(double deltaTime) {
        // Update placed cards/troops
        troopMovementService.updateTroops(deltaTime, this, activeTroops);

        // Update buildings (lifetime depreciation)
        for (Building b : activeBuildings) {
            if (b.isAlive()) {
                b.update(deltaTime);
            }
        }
    }

    private void handleCombat(double deltaTime) {
        combatService.update(deltaTime, this);

        // Remove dead troops post combat
        activeTroops.removeIf(t -> !t.isAlive());
    }

    private void cleanupEntities() {
        // Cleanup destroyed buildings from the list
        java.util.Iterator<Building> it = activeBuildings.iterator();
        while (it.hasNext()) {
            Building b = it.next();
            if (!b.isAlive()) {
                arena.getSpatialGrid().remove(b);
                it.remove();
            }
        }

        // Check for destroyed towers and update scores (must be before
        // removeDeadTowers)
        if (!isGameOver) {
            checkAndScoreDestroyedTowers();
        }

        // Cleanup destroyed towers
        arena.removeDeadTowers();
    }

    private void checkWinConditions() {
        if (!isGameOver) {
            boolean playerKingAlive = arena.isPlayerKingAlive();
            boolean botKingAlive = arena.isBotKingAlive();

            if (!playerKingAlive) {
                isGameOver = true;
                playerWon = false;
            } else if (!botKingAlive) {
                isGameOver = true;
                playerWon = true;
            }
        }
    }

    public boolean isDoubleElixir() {
        return isDoubleElixir;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isPlayerWinner() {
        return playerWon;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public int getPlayerDamageTaken() {
        int damage = 0;
        // Optimization: Use getAllTowers() which is O(1-6) instead of scanning the grid
        // O(N)
        for (Tower t : arena.getAllTowers()) {
            if (t.isPlayerSide()) {
                damage += (int) (t.getMaxHealth() - t.getCurrentHealth());
            }
        }
        return damage;
    }

    public boolean placeCard(boolean isPlayer, int handIndex, int x, int y) {
        // 1. Basic Validation (Player Specific)
        if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
            return false;
        }

        // Validate side (Player can only deploy on bottom half), unless it's a spell
        boolean isSpell = false;
        Card pendingCard = null;
        if (isPlayer) {
            pendingCard = playerHand.getCard(handIndex);
            if (pendingCard != null && pendingCard.getType() == CardType.SPELL) {
                isSpell = true;
            }
        }

        // Validate terrain (Grass or Bridge only) - unless it's a spell
        if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
            return false;
        }

        if (isPlayer && !isSpell && y < Arena.HEIGHT / 2) {
            return false;
        }

        if (isPlayer) {
            Card card = pendingCard != null ? pendingCard : playerHand.getCard(handIndex);
            if (card == null)
                return false;

            // 2. Cost Calculation (Challenge Logic)
            int cost = card.getCost();
            if (activeChallenge == ChallengeType.SPELL_BARRAGE && card.getType() == CardType.SPELL) {
                cost = Math.max(1, cost - 1);
            }

            if (playerElixir.getCurrentElixir() >= cost) {
                java.util.List<ICombatant> spawnedUnits = spawnUnit(true, card, x, y);

                if (spawnedUnits != null) {
                    playerElixir.spend(cost);
                    playerHand.playCard(handIndex);

                    // Notify listeners that elixir was spent
                    GameEventBus.getInstance().publishElixirSpent(true, cost);
                }

                return spawnedUnits != null;
            } else {
                // Yetersiz iksir
                return false;
            }
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

    public boolean spawnTroopDirectly(boolean isPlayer, Card card, int x, int y, int count) {
        if (card == null)
            return false;

        // Temporarily override the card's count for this specific spawn if requested
        Card spawnCard = card;
        if (count > 0 && count != card.getCount()) {
            spawnCard = new Card(card.getName(), card.getCost(), card.getType(), card.getRarity(),
                    card.getBaseHp(), card.getBaseDamage(), card.getHitSpeed(), card.getRange(),
                    card.getSpeed(), card.getTarget(), card.isAirUnit(), card.isAreaEffect(),
                    card.getDescription(), count, card.getLifetime());
            spawnCard.setLevel(card.getLevel());
        }

        return spawnTroopGroup(isPlayer, spawnCard, x, y) != null;
    }

    public GridPosition getFrontPosition(Building b) {
        if (b == null)
            return null;
        int bw = b.getWidth(), bh = b.getHeight(), x = b.getPosition().getX(), y = b.getPosition().getY();

        int spawnX = x + bw / 2, spawnY;

        if (b.isPlayerSide()) {
            spawnY = y - 1; // "Above" the building for player
        } else {
            spawnY = y + bh; // "Below" the building for bot
        }

        return GridPosition.tryCreate(spawnX, spawnY);
    }

    // Overload for direct card placement (used by Bot)
    public void placeCard(boolean isPlayer, Card card, int x, int y) {
        spawnUnit(isPlayer, card, x, y);
    }

    private java.util.List<ICombatant> spawnUnit(boolean isPlayer, Card card, int x, int y) {
        if (card == null)
            return null;

        java.util.List<ICombatant> spawnedUnits = null;
        if (card.getType() == CardType.BUILDING) {
            spawnedUnits = spawnBuilding(isPlayer, card, x, y);
        } else if (card.getType() == CardType.TROOP) {
            spawnedUnits = spawnTroopGroup(isPlayer, card, x, y);
        } else if (card.getType() == CardType.SPELL) {
            applySpellEffect(isPlayer, card, x, y);
            spawnedUnits = new java.util.ArrayList<>();
        }

        if (spawnedUnits != null) {
            // 2. Add to Placed History
            placedCards.add(new PlacedCard(card, x, y, isPlayer));

            // 3. Quests & Achievements (Player Only - listeners will filter)
            // Correction: ComboService needs this for Bot too.
            // Quests should filter by isPlayer themselves if needed.
            GameEventBus.getInstance().publishCardPlayed(isPlayer, card, spawnedUnits);
        }

        return spawnedUnits;
    }

    private java.util.List<ICombatant> spawnBuilding(boolean isPlayer, Card card, int x, int y) {
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
            Building building = new Building(topLeft, bw, bh, isPlayer, card.getHp(), card.getImagePath(),
                    card.getLifetime());
            building.configureCombatFromCard(card);

            // Set initial spawn delay if it's a spawner
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

    private java.util.List<ICombatant> spawnTroopGroup(boolean isPlayer, Card card, int x, int y) {
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
                Troop troop = new Troop(card, spawn, isPlayer);
                activeTroops.add(troop);
                arena.getSpatialGrid().add(troop);
                spawned.add(troop);
            }
        }
        return spawned;
    }

    public Hand getPlayerHand() {
        return playerHand;
    }

    // Apply spell effects: simple AoE damage around target (affects enemy troops,
    // buildings, and towers)
    private void applySpellEffect(boolean isPlayer, Card spell, int x, int y) {
        double radius = Math.max(0, spell.getRange());
        double damage = Math.max(0, spell.getDamage());
        GridPosition center = GridPosition.tryCreate(x, y);
        if (center == null)
            return;

        combatService.applyAreaDamage(this, center, radius, damage, TargetType.BOTH, isPlayer, true,
                spell.getStunDuration());
    }

    /*
     * Apply circular area damage originating from a troop attack. Center is derived
     * from the primary target to keep targeting logic unchanged.
     */
    public void applyAreaDamageFromTroop(Troop attacker, ICombatant primaryTarget) {
        if (attacker == null || primaryTarget == null)
            return;
        GridPosition center = primaryTarget.getCenterPosition();
        applyAreaDamageFromTroopInternal(attacker, center);
    }

    private void applyAreaDamageFromTroopInternal(Troop attacker, GridPosition center) {
        if (attacker == null || center == null)
            return;
        // Fixed small splash for troops (e.g. 1.0) or use card property if exits
        double radius = 1.0;
        double damage = attacker.getCombatStats() != null ? attacker.getCombatStats().getDamage() : 0;
        TargetType targetType = attacker.getBaseCard() != null ? attacker.getBaseCard().getTarget() : TargetType.BOTH;

        combatService.applyAreaDamage(this, center, radius, damage, targetType, attacker.isPlayerSide(), false, 0.0);
    }

    /*
     * Checks for destroyed towers and updates scores accordingly. Princess towers:
     * +1 point to the attacker. King towers: Set attacker's score to 3 and end the
     * game.
     */
    private void checkAndScoreDestroyedTowers() {
        java.util.Set<Tower> towers = arena.getAllTowers();

        for (Tower tower : towers) {
            // Skip if already scored or still alive
            if (scoredTowers.contains(tower) || tower.isAlive())
                continue;

            // Mark as scored to avoid double-counting
            scoredTowers.add(tower);

            boolean isPlayerTower = tower.isPlayerSide();
            boolean isKingTower = tower.getType() == Tower.TowerType.KING;

            // Update scores based on tower type and ownership
            if (isKingTower) {
                // King tower destroyed: set score to 3 and end game
                if (isPlayerTower) {
                    // Player's king tower destroyed by bot
                    botScore = 3;
                    isGameOver = true;
                    playerWon = false;
                } else {
                    // Bot's king tower destroyed by player
                    playerScore = 3;
                    isGameOver = true;
                    playerWon = true;
                }
            } else {
                // Princess tower destroyed: +1 point to attacker
                if (isPlayerTower) {
                    // Player's princess tower destroyed by bot
                    botScore++;
                } else {
                    // Bot's princess tower destroyed by player
                    playerScore++;
                }
            }

            // Notify listeners about tower destruction
            GameEventBus.getInstance().publishTowerDestroyed(isPlayerTower, tower);
        }
    }

    public ElixirManager getPlayerElixir() {
        return playerElixir;
    }

    public ElixirManager getBotElixir() {
        return botElixir;
    }

    public ElixirManager getElixirManager(boolean isPlayer) {
        return isPlayer ? playerElixir : botElixir;
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

    // Inner class to track placed units
    public static class PlacedCard {
        public final Card card;
        public final int x, y;
        public final boolean isPlayer;

        public PlacedCard(Card card, int x, int y, boolean isPlayer) {
            this.card = card;
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }
    }
}