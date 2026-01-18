package com.kuroyale.model.logic;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.dto.NetworkGameStateSnapshot;
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
    private final List<Projectile> activeProjectiles;
    private final com.kuroyale.service.TroopMovementService troopMovementService = new com.kuroyale.service.TroopMovementService();
    private final com.kuroyale.service.CombatService combatService = new com.kuroyale.service.CombatService();

    private double gameTime = 180.0; // 3 minutes
    private int playerScore = 0, botScore = 0;

    private boolean isDoubleElixir = false, isGameOver = false, playerWon = false, isDraw = false;
    private boolean isTiebreakerMode = false; // Tiebreaker: all towers drain health
    private static final double TIEBREAKER_DRAIN_RATE = 100.0; // HP per second

    // Track towers that have already been scored to avoid double-counting
    private java.util.Set<Tower> scoredTowers = new java.util.HashSet<>();

    // Challenge Context
    private ChallengeType activeChallenge;

    // Network mode flag - when true, bot AI is disabled (opponent is a real player)
    private boolean networkMode = false;

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
        this.activeProjectiles = new ArrayList<>();
    }

    @Override
    public List<Projectile> getProjectiles() {
        return activeProjectiles;
    }

    @Override
    public void addProjectile(Projectile p) {
        activeProjectiles.add(p);
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

    /**
     * Enables network mode - disables bot AI so opponent is controlled by real
     * player.
     */
    public void setNetworkMode(boolean networkMode) {
        this.networkMode = networkMode;
    }

    public boolean isNetworkMode() {
        return networkMode;
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

        // Only run bot AI if NOT in network mode, NOT game over, and NOT in tiebreaker
        if (!isGameOver && !networkMode && !isTiebreakerMode) {
            updateBot(deltaTime);
        }

        updateEntities(deltaTime);
        handleCombat(deltaTime);
        cleanupEntities();
        checkWinConditions();
    }

    private void updateGameTimer(double deltaTime) {
        // Handle tiebreaker mode: all towers drain health until one reaches 0
        if (isTiebreakerMode) {
            updateTiebreakerMode(deltaTime);
            return;
        }

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
                    if (playerScore > botScore) {
                        isGameOver = true;
                        playerWon = true;
                    } else if (botScore > playerScore) {
                        isGameOver = true;
                        playerWon = false;
                    } else {
                        // Equal scores: Enter tiebreaker mode
                        // All remaining towers start losing health rapidly
                        isTiebreakerMode = true;
                        // Clear all troops and buildings so they don't affect tiebreaker
                        clearArenaUnits();
                    }
                }
            }
        }
    }

    /**
     * Tiebreaker mode: All remaining towers lose health rapidly.
     * The first tower(s) to reach 0 health determines the loser.
     * If towers on both sides die simultaneously, count crowns for both.
     */
    private void updateTiebreakerMode(double deltaTime) {
        double drainAmount = TIEBREAKER_DRAIN_RATE * deltaTime;

        // Drain all living towers
        for (Tower tower : arena.getAllTowers()) {
            if (tower.isAlive()) {
                double newHealth = tower.getCurrentHealth() - drainAmount;
                tower.setCurrentHealth((int) Math.max(0, newHealth));
            }
        }

        // Count all towers that reached 0 health and score them
        int playerTowersDied = 0;
        int botTowersDied = 0;
        boolean anyKingDied = false;
        boolean playerKingDied = false;
        boolean botKingDied = false;

        for (Tower tower : arena.getAllTowers()) {
            if (tower.getCurrentHealth() <= 0) {
                boolean isPlayerTower = tower.isPlayerSide();
                boolean isKingTower = tower.getType() == Tower.TowerType.KING;

                if (isKingTower) {
                    anyKingDied = true;
                    if (isPlayerTower) {
                        playerKingDied = true;
                    } else {
                        botKingDied = true;
                    }
                }

                if (isPlayerTower) {
                    playerTowersDied++;
                } else {
                    botTowersDied++;
                }
            }
        }

        // If any towers died, end the game
        if (playerTowersDied > 0 || botTowersDied > 0) {
            // Award crowns based on what died
            if (anyKingDied) {
                // King tower death = 3 crowns
                if (playerKingDied) {
                    botScore = 3;
                }
                if (botKingDied) {
                    playerScore = 3;
                }
            } else {
                // Princess towers = 1 crown each
                botScore += playerTowersDied;
                playerScore += botTowersDied;
            }

            // End the game
            isGameOver = true;

            // Determine winner based on final scores
            if (playerScore > botScore) {
                playerWon = true;
            } else if (botScore > playerScore) {
                playerWon = false;
            } else {
                // Still tied after simultaneous deaths = draw
                isDraw = true;
                playerWon = false;
            }
        }
    }

    /**
     * Clears all troops and buildings from the arena.
     * Called when entering tiebreaker mode to ensure only tower health matters.
     */
    private void clearArenaUnits() {
        // Remove all troops from spatial grid
        for (Troop troop : activeTroops) {
            arena.getSpatialGrid().remove(troop);
        }
        activeTroops.clear();

        // Remove all buildings from spatial grid and clear footprints
        for (Building building : activeBuildings) {
            arena.getSpatialGrid().remove(building);
        }
        activeBuildings.clear();

        // Clear projectiles too
        activeProjectiles.clear();
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
    
    /**
     * Places a card for the opponent (network player 2) using their hand and elixir.
     * Validates placement on the TOP half (unless spell).
     */
    public boolean placeOpponentCard(String cardName, int x, int y) {
        if (cardName == null) return false;
        Hand botHand = bot.getHand();
        int handIndex = -1;
        Card card = null;
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card candidate = botHand.getCard(i);
            if (candidate != null && cardName.equals(candidate.getName())) {
                handIndex = i;
                card = candidate;
                break;
            }
        }
        
        // Fallback: allow card by name even if not in hand (prevents input drops)
        if (card == null) {
            card = getCardByName(cardName);
        }
        if (card == null) {
            return false;
        }
        
        return placeOpponentCardInternal(card, handIndex, x, y);
    }
    
    private boolean placeOpponentCardInternal(Card card, int handIndex, int x, int y) {
        if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
            return false;
        }
        
        boolean isSpell = card.getType() == CardType.SPELL;
        
        if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
            return false;
        }
        
        // Opponent can only deploy on TOP half (y < Arena.HEIGHT / 2) unless spell
        if (!isSpell && y >= Arena.HEIGHT / 2) {
            return false;
        }
        
        int cost = card.getCost();
        if (botElixir.getCurrentElixir() >= cost) {
            java.util.List<ICombatant> spawnedUnits = spawnUnit(false, card, x, y);
            if (spawnedUnits != null) {
                botElixir.spend(cost);
                if (handIndex >= 0) {
                    bot.getHand().playCard(handIndex);
                }
                GameEventBus.getInstance().publishElixirSpent(false, cost);
                return true;
            }
        }
        
        return false;
    }

    public double getGameTime() {
        return gameTime;
    }

    /**
     * Sets the game time (used for network sync where host is authoritative).
     */
    public void setGameTime(double gameTime) {
        this.gameTime = Math.max(0, gameTime);
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
                    card.getDescription(), count, card.getLifetime(), card.getWidth(), card.getHeight());
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

        // Check if spawn position lands on river AND is not walkable (not a bridge)
        // If it's on a bridge, spawn normally so troops can walk across
        if (spawnY == com.kuroyale.util.GameConstants.RIVER_ROW_1 ||
                spawnY == com.kuroyale.util.GameConstants.RIVER_ROW_2) {
            GridCell frontCell = arena.getCell(spawnX, spawnY);
            // Only shift if the cell is not walkable (water, not bridge)
            if (frontCell == null || !frontCell.isWalkable()) {
                // Shift X based on building's position: if on right half, go left; otherwise go
                // right
                int centerX = Arena.WIDTH / 2;
                if (spawnX >= centerX) {
                    spawnX = x - 1; // Spawn to the left of building
                } else {
                    spawnX = x + bw; // Spawn to the right of building
                }
                // Also shift Y off the river to a walkable tile
                if (b.isPlayerSide()) {
                    // Player side: move below the river (y > 16)
                    spawnY = com.kuroyale.util.GameConstants.RIVER_ROW_2 + 1; // y = 17
                } else {
                    // Bot side: move above the river (y < 15)
                    spawnY = com.kuroyale.util.GameConstants.RIVER_ROW_1 - 1; // y = 14
                }
            }
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

        // Center the building on the clicked tile by offsetting top-left position
        int topLeftX = x - (bw / 2);
        int topLeftY = y - (bh / 2);

        if (topLeftX < 0 || topLeftY < 0 || (topLeftX + bw) > Arena.WIDTH || (topLeftY + bh) > Arena.HEIGHT) {
            return null;
        }

        for (int dx = 0; dx < bw; dx++) {
            for (int dy = 0; dy < bh; dy++) {
                GridCell c = arena.getCell(topLeftX + dx, topLeftY + dy);
                if (c == null || c.isOccupied() || !c.isWalkable()) {
                    return null;
                }
            }
        }

        GridPosition topLeft = GridPosition.tryCreate(topLeftX, topLeftY);
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
    
    // Aliases for NetworkGameStateSnapshot compatibility
    public List<Troop> getTroops() {
        return activeTroops;
    }
    
    public List<Building> getBuildings() {
        return activeBuildings;
    }
    
    public boolean isTiebreakerMode() {
        return isTiebreakerMode;
    }
    
    // ==================== Network State Synchronization ====================
    
    /**
     * Applies authoritative state from a network snapshot.
     * This is called by the CLIENT to update its local state to match the host's.
     * 
     * This method performs FULL STATE SYNCHRONIZATION:
     * - Core game values (time, scores, flags)
     * - Elixir values (for display)
     * - Tower health
     * - Troop positions and health
     * - Building positions and health
     * - Projectile positions
     * 
     * @param snapshot The authoritative game state from the host
     */
    public void applyNetworkSnapshot(NetworkGameStateSnapshot snapshot) {
        if (snapshot == null) return;
        
        // Apply core game state
        this.gameTime = snapshot.getGameTime();
        this.playerScore = snapshot.getPlayer1Score();
        this.botScore = snapshot.getPlayer2Score();
        this.isDoubleElixir = snapshot.isDoubleElixir();
        this.isGameOver = snapshot.isGameOver();
        this.isTiebreakerMode = snapshot.isTiebreakerMode();
        
        // Apply winner state
        if (snapshot.isGameOver()) {
            switch (snapshot.getWinner()) {
                case 1 -> { playerWon = true; isDraw = false; }
                case 2 -> { playerWon = false; isDraw = false; }
                case 3 -> { playerWon = false; isDraw = true; }
                default -> { }
            }
        }
        
        // Apply elixir values (client sees snapshot's player1 as "my" elixir after perspective mapping)
        playerElixir.setCurrentElixir(snapshot.getPlayer1Elixir());
        botElixir.setCurrentElixir(snapshot.getPlayer2Elixir());
        
        // Enable double elixir if needed
        if (snapshot.isDoubleElixir() && !playerElixir.isDoubleElixir()) {
            playerElixir.setDoubleElixir(true);
            botElixir.setDoubleElixir(true);
        }
        
        // Apply tower health from snapshot
        applyTowerHealthFromSnapshot(snapshot.getTowers());
        
        // Apply troop state from snapshot
        applyTroopsFromSnapshot(snapshot.getTroops());
        
        // Apply building state from snapshot
        applyBuildingsFromSnapshot(snapshot.getBuildings());
        
        // Apply projectile state from snapshot
        applyProjectilesFromSnapshot(snapshot.getProjectiles());
    }
    
    /**
     * Applies tower health values from the authoritative snapshot.
     * 
     * Matching logic: After perspective mapping, the snapshot's isPlayerSide
     * matches the local tower's isPlayerSide. We match by type AND isPlayerSide.
     */
    private void applyTowerHealthFromSnapshot(List<NetworkGameStateSnapshot.TowerSnapshot> towerSnapshots) {
        if (towerSnapshots == null) return;
        
        for (NetworkGameStateSnapshot.TowerSnapshot ts : towerSnapshots) {
            // Find matching tower in arena by type and side
            // After perspective mapping, isPlayerSide in snapshot matches local tower's isPlayerSide
            for (Tower tower : arena.getAllTowers()) {
                if (tower.getType().name().equals(ts.getType()) && 
                    tower.isPlayerSide() == ts.isPlayerSide()) {
                    tower.setCurrentHealth(ts.getHealth());
                    break;
                }
            }
        }
    }
    
    /**
     * Applies troop state from the authoritative snapshot.
     * This syncs positions and health of troops.
     */
    private void applyTroopsFromSnapshot(List<NetworkGameStateSnapshot.TroopSnapshot> troopSnapshots) {
        if (troopSnapshots == null) return;
        
        // Build a map of snapshot troops by ID for efficient lookup
        java.util.Map<Integer, NetworkGameStateSnapshot.TroopSnapshot> snapshotMap = new java.util.HashMap<>();
        for (NetworkGameStateSnapshot.TroopSnapshot ts : troopSnapshots) {
            snapshotMap.put(ts.getId(), ts);
        }
        
        // Update existing troops and mark for removal if not in snapshot
        java.util.List<Troop> toRemove = new java.util.ArrayList<>();
        java.util.Set<Integer> matchedIds = new java.util.HashSet<>();
        
        for (Troop troop : activeTroops) {
            int troopId = System.identityHashCode(troop);
            NetworkGameStateSnapshot.TroopSnapshot ts = snapshotMap.get(troopId);
            
            if (ts != null) {
                // Update troop state
                troop.setCurrentHealth(ts.getHealth());
                troop.setWorldPosition(ts.getX(), ts.getY());
                troop.setTargetWorldPosition(ts.getTargetX(), ts.getTargetY());
                matchedIds.add(troopId);
            } else {
                // Troop not in snapshot - mark for removal
                toRemove.add(troop);
            }
        }
        
        // Remove troops not in snapshot
        activeTroops.removeAll(toRemove);
        
        // Add new troops from snapshot (troops that exist in snapshot but not locally)
        for (NetworkGameStateSnapshot.TroopSnapshot ts : troopSnapshots) {
            if (!matchedIds.contains(ts.getId())) {
                // Create new troop from snapshot
                Card card = cardCatalog != null ? cardCatalog.apply(ts.getCardName()) : null;
                if (card != null) {
                    GridPosition spawnPos = new GridPosition((int) ts.getX(), (int) ts.getY());
                    Troop newTroop = new Troop(card, spawnPos, ts.isPlayerSide());
                    newTroop.setCurrentHealth(ts.getHealth());
                    newTroop.setWorldPosition(ts.getX(), ts.getY());
                    activeTroops.add(newTroop);
                }
            }
        }
    }
    
    /**
     * Applies building state from the authoritative snapshot.
     */
    private void applyBuildingsFromSnapshot(List<NetworkGameStateSnapshot.BuildingSnapshot> buildingSnapshots) {
        if (buildingSnapshots == null) return;
        
        // Build a map of snapshot buildings by ID
        java.util.Map<Integer, NetworkGameStateSnapshot.BuildingSnapshot> snapshotMap = new java.util.HashMap<>();
        for (NetworkGameStateSnapshot.BuildingSnapshot bs : buildingSnapshots) {
            snapshotMap.put(bs.getId(), bs);
        }
        
        // Update existing buildings and mark for removal if not in snapshot
        java.util.List<Building> toRemove = new java.util.ArrayList<>();
        java.util.Set<Integer> matchedIds = new java.util.HashSet<>();
        
        for (Building building : activeBuildings) {
            int buildingId = System.identityHashCode(building);
            NetworkGameStateSnapshot.BuildingSnapshot bs = snapshotMap.get(buildingId);
            
            if (bs != null) {
                // Update building state
                building.setCurrentHealth(bs.getHealth());
                matchedIds.add(buildingId);
            } else {
                // Building not in snapshot - mark for removal
                toRemove.add(building);
            }
        }
        
        // Remove buildings not in snapshot
        activeBuildings.removeAll(toRemove);
        
        // Add new buildings from snapshot
        for (NetworkGameStateSnapshot.BuildingSnapshot bs : buildingSnapshots) {
            if (!matchedIds.contains(bs.getId())) {
                Card card = cardCatalog != null ? cardCatalog.apply(bs.getCardName()) : null;
                if (card != null) {
                    GridPosition pos = new GridPosition((int) bs.getX(), (int) bs.getY());
                    int bw = Math.max(1, card.getFootprintWidthTiles());
                    int bh = Math.max(1, card.getFootprintHeightTiles());
                    Building newBuilding = new Building(pos, bw, bh, bs.isPlayerSide(), 
                            card.getHp(), card.getImagePath(), card.getLifetime());
                    newBuilding.configureCombatFromCard(card);
                    newBuilding.setCurrentHealth(bs.getHealth());
                    activeBuildings.add(newBuilding);
                }
            }
        }
    }
    
    /**
     * Applies projectile state from the authoritative snapshot.
     * Projectiles are ephemeral so we just sync their positions.
     */
    private void applyProjectilesFromSnapshot(List<NetworkGameStateSnapshot.ProjectileSnapshot> projectileSnapshots) {
        if (projectileSnapshots == null) return;
        
        // For simplicity, just update positions of existing projectiles
        // Projectiles are very short-lived so exact sync isn't critical
        java.util.Map<Integer, NetworkGameStateSnapshot.ProjectileSnapshot> snapshotMap = new java.util.HashMap<>();
        for (NetworkGameStateSnapshot.ProjectileSnapshot ps : projectileSnapshots) {
            snapshotMap.put(ps.getId(), ps);
        }
        
        // Update existing projectiles
        for (Projectile proj : activeProjectiles) {
            int projId = System.identityHashCode(proj);
            NetworkGameStateSnapshot.ProjectileSnapshot ps = snapshotMap.get(projId);
            if (ps != null) {
                proj.setPosition(new Vector2(ps.getX(), ps.getY()));
            }
        }
    }
    
    /**
     * Sets the player score directly (used for network sync).
     */
    public void setPlayerScore(int score) {
        this.playerScore = score;
    }
    
    /**
     * Sets the bot/player2 score directly (used for network sync).
     */
    public void setBotScore(int score) {
        this.botScore = score;
    }
    
    /**
     * Sets the game over state directly (used for network sync).
     */
    public void setGameOver(boolean isGameOver, boolean playerWon, boolean isDraw) {
        this.isGameOver = isGameOver;
        this.playerWon = playerWon;
        this.isDraw = isDraw;
    }
    
    /**
     * Sets the double elixir flag (used for network sync).
     */
    public void setDoubleElixir(boolean isDoubleElixir) {
        this.isDoubleElixir = isDoubleElixir;
        if (isDoubleElixir) {
            playerElixir.setDoubleElixir(true);
            botElixir.setDoubleElixir(true);
        }
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