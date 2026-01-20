package com.kuroyale.model.state;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.arena.Vector2;
import com.kuroyale.model.dto.*;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.ElixirManager;
import com.kuroyale.service.battle.logic.BotLogic;
import com.kuroyale.event.GameEventBus;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Represents the Model in the Model-View-Controller (MVC) architectural
 * pattern.
 * <p>
 * This class encapsulates the core business logic and state of the game,
 * including
 * player and bot status, elixir management, and the arena state. It notifies
 * observers (via the {@link GameEventBus}) of state changes, decoupling the
 * internal representation from the user interface.
 * </p>
 */
public class GameState extends AbstractGameState {
    private final Hand playerHand;
    private final ElixirManager playerElixir, botElixir;
    private final BotLogic bot;

    // Scores are specific here because PvPGameState splits them differently
    // (player1/player2 vs player/bot)
    // could abstract this but let's keep it simple.
    private int playerScore = 0, botScore = 0;

    private boolean playerWon = false, isDraw = false;

    // Track towers that have already been scored to avoid double-counting
    // This could be moved to Abstract if common, but let's keep it here for now as
    // scoring is here.
    private Set<Tower> scoredTowers = new HashSet<>();

    // Challenge Context
    private ChallengeType activeChallenge;

    // Network mode flag - when true, bot AI is disabled (opponent is a real player)
    private boolean networkMode = false;

    public GameState(Deck playerDeck, Deck botDeck, Arena arena) {
        super(arena);
        this.playerHand = new Hand(playerDeck);
        this.playerElixir = new ElixirManager();

        this.bot = new BotLogic(botDeck);
        // Removed: this.botHand = bot.getHand();
        this.botElixir = bot.getElixirManager();

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

    @Override
    public void update(double deltaTime) {
        updateGameTimer(deltaTime);

        playerElixir.update(deltaTime);
        botElixir.update(deltaTime);

        // Only run bot AI if NOT in network mode, NOT game over, and NOT in tiebreaker
        if (!isGameOver && !networkMode && !isTiebreakerMode) {
            updateBot(deltaTime);
        }

        super.updateEntities(deltaTime); // Uses Base implementation
        super.handleCombat(deltaTime); // Uses Base implementation
        super.cleanupEntities(); // Uses Base implementation
        checkWinConditions();
    }

    private void updateGameTimer(double deltaTime) {
        // Handle tiebreaker mode: all towers drain health until one reaches 0
        if (isTiebreakerMode) {
            super.updateTiebreakerMode(deltaTime);
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

    @Override
    protected void checkTiebreakerWinCondition() {
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

    private void updateBot(double deltaTime) {
        BotLogic.Move botMove = bot.update(deltaTime, this);
        if (botMove != null) {
            placeCard(false, botMove.card, botMove.x, botMove.y);
        }
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

    public boolean isPlayerWinner() {
        return playerWon;
    }

    public boolean isDraw() {
        return isDraw;
    }

    public int getPlayerDamageTaken() {
        int damage = 0;
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
     * Sets the scores (used for network sync where host is authoritative).
     */
    public void setScores(int playerScore, int botScore) {
        this.playerScore = playerScore;
        this.botScore = botScore;
    }

    /**
     * Sets the player score directly (used for network sync).
     */
    public void setPlayerScore(int score) {
        this.playerScore = score;
    }

    /**
     * Sets the bot/opponent score directly (used for network sync).
     */
    public void setBotScore(int score) {
        this.botScore = score;
    }

    /**
     * Sets the double elixir state (used for network sync).
     */
    public void setDoubleElixir(boolean isDoubleElixir) {
        this.isDoubleElixir = isDoubleElixir;
        if (isDoubleElixir) {
            playerElixir.setDoubleElixir(true);
            botElixir.setDoubleElixir(true);
        }
    }

    /**
     * Applies a full game state sync from the authoritative host.
     * Used by the client to update its local state.
     */
    public void applyHostStateSync(double gameTime, double playerElixirValue, double botElixirValue,
            int playerScore, int botScore, boolean isDoubleElixir) {
        this.gameTime = Math.max(0, gameTime);
        this.playerScore = playerScore;
        this.botScore = botScore;

        // Sync elixir values - client sees these inverted (their elixir is the "bot"
        // from host's perspective)
        // The client's "player" elixir should match the host's "bot" elixir (since
        // client is the opponent)
        this.playerElixir.setCurrentElixir(botElixirValue); // Client's elixir = Host's opponent elixir
        this.botElixir.setCurrentElixir(playerElixirValue); // Client's opponent = Host's player

        if (isDoubleElixir && !this.isDoubleElixir) {
            this.isDoubleElixir = true;
            this.playerElixir.setDoubleElixir(true);
            this.botElixir.setDoubleElixir(true);
        }
    }

    /**
     * Light update for client - updates movement and visuals but NOT scoring.
     * The authoritative scores and game state come from the host.
     * This keeps the game visually smooth while ensuring consistent outcomes.
     */
    public void updateClientOnly(double deltaTime) {
        // Update elixir regeneration for responsive UI
        playerElixir.update(deltaTime);
        botElixir.update(deltaTime);

        // Update timer locally (will be corrected by host sync)
        if (gameTime > 0) {
            gameTime -= deltaTime;

            // Check for Double Elixir locally (host will confirm)
            if (gameTime <= 60.0 && !isDoubleElixir) {
                isDoubleElixir = true;
                playerElixir.setDoubleElixir(true);
                botElixir.setDoubleElixir(true);
            }
        }

        // Update troop movement for visual smoothness
        troopMovementService.updateTroops(deltaTime, this, activeTroops);

        // Update buildings (lifetime, animations)
        for (Building b : activeBuildings) {
            if (b.isAlive()) {
                b.update(deltaTime);
            }
        }

        // Run combat for visual effects (damage numbers, animations)
        // But do NOT calculate scores - those come from host
        combatService.update(deltaTime, this);

        // Remove dead troops (visual cleanup)
        activeTroops.removeIf(t -> !t.isAlive());

        // Cleanup dead buildings
        java.util.Iterator<Building> it = activeBuildings.iterator();
        while (it.hasNext()) {
            Building b = it.next();
            if (!b.isAlive()) {
                arena.getSpatialGrid().remove(b);
                it.remove();
            }
        }

        // Remove dead towers (visual cleanup - scores come from host)
        arena.removeDeadTowers();

        // NOTE: Do NOT update scores or check win conditions here
        // Those are authoritative from the host
    }

    /**
     * Render-only update for network client.
     * Client receives all entity positions from host and just renders them.
     * No game logic is run locally - this just updates UI elements.
     */
    public void updateRenderOnly(double deltaTime) {
        // Only update elixir for responsive UI (will be corrected by host sync)
        playerElixir.update(deltaTime);
    }

    /**
     * Serializes all troops for network transmission.
     * Format: cardName,worldX,worldY,health,isPlayer,state|cardName,...
     */
    public String serializeTroops() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Troop troop : activeTroops) {
            if (!first)
                sb.append("|");
            first = false;

            Card card = troop.getBaseCard();
            Vector2 pos = troop.getWorldPosition();

            sb.append(card != null ? card.getName() : "Unknown")
                    .append(",").append(String.format("%.2f", pos.getX()))
                    .append(",").append(String.format("%.2f", pos.getY()))
                    .append(",").append(troop.getCurrentHealth())
                    .append(",").append(troop.isPlayerSide())
                    .append(",").append(troop.getUnitState().name());
        }

        return sb.toString();
    }

    /**
     * Serializes all buildings for network transmission.
     * Format: cardName,gridX,gridY,health,isPlayer,lifetime|...
     */
    public String serializeBuildings() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Building building : activeBuildings) {
            if (!first)
                sb.append("|");
            first = false;

            GridPosition pos = building.getPosition();

            sb.append(building.getCardName() != null ? building.getCardName() : "Building")
                    .append(",").append(pos.getX())
                    .append(",").append(pos.getY())
                    .append(",").append(building.getCurrentHealth())
                    .append(",").append(building.isPlayerSide())
                    .append(",").append(String.format("%.1f", building.getRemainingLifetime()));
        }

        return sb.toString();
    }

    /**
     * Clears all troops and buildings - used before applying host state.
     */
    public void clearEntities() {
        // Remove from spatial grid first
        for (Troop troop : activeTroops) {
            arena.getSpatialGrid().remove(troop);
        }
        for (Building building : activeBuildings) {
            arena.getSpatialGrid().remove(building);
        }

        activeTroops.clear();
        activeBuildings.clear();
    }

    /**
     * Clears only buildings - used for smarter network sync.
     */
    public void clearBuildings() {
        for (Building building : activeBuildings) {
            arena.getSpatialGrid().remove(building);
        }
        activeBuildings.clear();
    }

    /**
     * Spawns a troop at exact world position (for network sync).
     * Used by client to recreate host's troop state.
     */
    public void spawnTroopAtPosition(String cardName, double worldX, double worldY, int health, boolean isPlayer,
            String state) {
        Card card = getCardByName(cardName);
        if (card == null) {
            System.err.println("[GameState] Unknown card for troop spawn: " + cardName);
            return;
        }

        // Create troop at approximate grid position
        int gridX = (int) worldX;
        int gridY = (int) worldY;
        GridPosition spawn = GridPosition.tryCreate(
                Math.max(0, Math.min(gridX, Arena.WIDTH - 1)),
                Math.max(0, Math.min(gridY, Arena.HEIGHT - 1)));

        if (spawn != null) {
            Troop troop = new Troop(card, spawn, isPlayer);
            // Set exact world position
            troop.setWorldPosition(new Vector2(worldX, worldY));
            troop.setCurrentHealth(health);

            // Set unit state for proper animation rendering
            if (state != null && !state.isEmpty()) {
                try {
                    troop.setUnitState(com.kuroyale.model.enums.UnitState.valueOf(state));
                } catch (IllegalArgumentException e) {
                    // Default to MOVING if state is invalid
                    troop.setUnitState(com.kuroyale.model.enums.UnitState.MOVING);
                }
            }

            activeTroops.add(troop);
            arena.getSpatialGrid().add(troop);
        }
    }

    /**
     * Spawns a building at position (for network sync).
     */
    public void spawnBuildingAtPosition(String cardName, int gridX, int gridY, int health, boolean isPlayer,
            double lifetime) {
        Card card = getCardByName(cardName);
        if (card == null) {
            System.err.println("[GameState] Unknown card for building spawn: '" + cardName + "' - cardCatalog is "
                    + (cardCatalog != null ? "set" : "NULL"));
            return;
        }

        int bw = Math.max(1, card.getFootprintWidthTiles());
        int bh = Math.max(1, card.getFootprintHeightTiles());

        // Clamp position to valid bounds
        int clampedX = Math.max(0, Math.min(gridX, Arena.WIDTH - bw));
        int clampedY = Math.max(0, Math.min(gridY, Arena.HEIGHT - bh));

        GridPosition pos = GridPosition.tryCreate(clampedX, clampedY);
        if (pos != null) {
            Building building = new Building(pos, bw, bh, isPlayer, card.getHp(), card.getImagePath(),
                    card.getLifetime());
            building.configureCombatFromCard(card);
            building.setCurrentHealth(health);
            building.setRemainingLifetime(lifetime);

            activeBuildings.add(building);
            arena.getSpatialGrid().add(building);
            System.out.println("[GameState] Building '" + cardName + "' spawned at (" + clampedX + ", " + clampedY
                    + ") size " + bw + "x" + bh);
        } else {
            System.err.println(
                    "[GameState] Failed to create GridPosition for building at (" + clampedX + ", " + clampedY + ")");
        }
    }

    /**
     * Spawns a building directly without relying on card catalog (for network
     * sync).
     * This is a fallback method when card lookup fails.
     */
    public boolean spawnBuildingDirect(String cardName, int gridX, int gridY, int health, int maxHealth,
            boolean isPlayer, double lifetime, int width, int height, String imagePath) {

        // First try using card catalog
        Card card = getCardByName(cardName);
        if (card != null) {
            // Use card-based spawn
            int bw = Math.max(1, card.getFootprintWidthTiles());
            int bh = Math.max(1, card.getFootprintHeightTiles());

            int clampedX = Math.max(0, Math.min(gridX, Arena.WIDTH - bw));
            int clampedY = Math.max(0, Math.min(gridY, Arena.HEIGHT - bh));

            GridPosition pos = GridPosition.tryCreate(clampedX, clampedY);
            if (pos != null) {
                Building building = new Building(pos, bw, bh, isPlayer, card.getHp(), card.getImagePath(),
                        card.getLifetime());
                building.configureCombatFromCard(card);
                building.setCurrentHealth(health);
                building.setRemainingLifetime(lifetime);

                activeBuildings.add(building);
                arena.getSpatialGrid().add(building);
                System.out.println("[GameState] Building '" + cardName + "' spawned via card at (" + clampedX + ", "
                        + clampedY + ")");
                return true;
            }
        }

        // Fallback: Create building directly with provided data
        int clampedX = Math.max(0, Math.min(gridX, Arena.WIDTH - width));
        int clampedY = Math.max(0, Math.min(gridY, Arena.HEIGHT - height));

        GridPosition pos = GridPosition.tryCreate(clampedX, clampedY);
        if (pos != null) {
            String imgPath = (imagePath != null && !imagePath.isEmpty()) ? imagePath
                    : "images/cards/building_default.png";
            Building building = new Building(pos, width, height, isPlayer, maxHealth, imgPath, (int) lifetime);
            building.setCardName(cardName);
            building.setCurrentHealth(health);
            building.setRemainingLifetime(lifetime);

            activeBuildings.add(building);
            arena.getSpatialGrid().add(building);
            System.out.println("[GameState] Building '" + cardName + "' spawned DIRECTLY at (" + clampedX + ", "
                    + clampedY + ") size " + width + "x" + height);
            return true;
        }

        System.err.println("[GameState] Failed to spawn building '" + cardName + "' at (" + gridX + ", " + gridY + ")");
        return false;
    }

    public int getPlayerScore() {
        return playerScore;
    }

    public int getBotScore() {
        return botScore;
    }

    // Overload for direct card placement (used by Bot)
    public void placeCard(boolean isPlayer, Card card, int x, int y) {
        spawnUnit(isPlayer, card, x, y);
    }

    // Abstract impl
    @Override
    protected void checkAndScoreDestroyedTowers() {
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

    public Hand getPlayerHand() {
        return playerHand;
    }

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

        Vector2 centerVec = Vector2.fromGridPosition(center);
        combatService.applyAreaDamage(this, centerVec, radius, damage, targetType, attacker.isPlayerSide(), false, 0.0,
                "Generic");
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

    public List<PlacedCard> getPlacedCards() {
        return placedCards;
    }

    /**
     * Alias for getActiveTroops() - used for network sync compatibility.
     */
    public List<Troop> getTroops() {
        return activeTroops;
    }

    /**
     * Alias for getActiveBuildings() - used for network sync compatibility.
     */
    public List<Building> getBuildings() {
        return activeBuildings;
    }

    // ==================== Network State Synchronization ====================
    // (Methods below are kept as is, but could move to separate helper if this file
    // stays too large)
    // ... applyNetworkSnapshot etc were preserved in previous read ...

    // NOTE: The previous read indicated applyNetworkSnapshot and related methods.
    // I am including them here to ensure the file is complete.

    public void applyNetworkSnapshot(NetworkGameStateSnapshot snapshot) {
        if (snapshot == null)
            return;

        // Apply core game state
        this.gameTime = snapshot.getGameTime();
        this.playerScore = snapshot.getPlayer1Score();
        this.botScore = snapshot.getPlayer2Score();
        this.isDoubleElixir = snapshot.isDoubleElixir();
        this.isGameOver = snapshot.isGameOver();
        this.isTiebreakerMode = snapshot.isTiebreakerMode();

        if (snapshot.isGameOver()) {
            switch (snapshot.getWinner()) {
                case 1 -> {
                    playerWon = true;
                    isDraw = false;
                }
                case 2 -> {
                    playerWon = false;
                    isDraw = false;
                }
                case 3 -> {
                    playerWon = false;
                    isDraw = true;
                }
            }
        }

        playerElixir.setCurrentElixir(snapshot.getPlayer1Elixir());
        botElixir.setCurrentElixir(snapshot.getPlayer2Elixir());

        if (snapshot.isDoubleElixir() && !playerElixir.isDoubleElixir()) {
            playerElixir.setDoubleElixir(true);
            botElixir.setDoubleElixir(true);
        }

        applyTowerHealthFromSnapshot(snapshot.getTowers());
        applyTroopsFromSnapshot(snapshot.getTroops());
        applyBuildingsFromSnapshot(snapshot.getBuildings());
        applyProjectilesFromSnapshot(snapshot.getProjectiles());
    }

    private void applyTowerHealthFromSnapshot(List<NetworkGameStateSnapshot.TowerSnapshot> towerSnapshots) {
        if (towerSnapshots == null)
            return;

        for (NetworkGameStateSnapshot.TowerSnapshot ts : towerSnapshots) {
            for (Tower tower : arena.getAllTowers()) {
                if (tower.getType().name().equals(ts.getType()) &&
                        tower.isPlayerSide() == ts.isPlayerSide()) {
                    tower.setCurrentHealth(ts.getHealth());
                    break;
                }
            }
        }
    }

    private void applyTroopsFromSnapshot(List<NetworkGameStateSnapshot.TroopSnapshot> troopSnapshots) {
        if (troopSnapshots == null)
            return;

        java.util.Map<Integer, NetworkGameStateSnapshot.TroopSnapshot> snapshotMap = new java.util.HashMap<>();
        for (NetworkGameStateSnapshot.TroopSnapshot ts : troopSnapshots) {
            snapshotMap.put(ts.getId(), ts);
        }

        java.util.List<Troop> toRemove = new java.util.ArrayList<>();
        java.util.Set<Integer> matchedIds = new java.util.HashSet<>();

        for (Troop troop : activeTroops) {
            int troopId = System.identityHashCode(troop);
            NetworkGameStateSnapshot.TroopSnapshot ts = snapshotMap.get(troopId);

            if (ts != null) {
                troop.setCurrentHealth(ts.getHealth());
                troop.setWorldPosition(ts.getX(), ts.getY());
                troop.setTargetWorldPosition(ts.getTargetX(), ts.getTargetY());
                matchedIds.add(troopId);
            } else {
                toRemove.add(troop);
            }
        }

        activeTroops.removeAll(toRemove);

        // Add new troops
        for (NetworkGameStateSnapshot.TroopSnapshot ts : troopSnapshots) {
            if (!matchedIds.contains(ts.getId())) {
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

    private void applyBuildingsFromSnapshot(List<NetworkGameStateSnapshot.BuildingSnapshot> buildingSnapshots) {
        if (buildingSnapshots == null)
            return;

        java.util.Map<Integer, NetworkGameStateSnapshot.BuildingSnapshot> snapshotMap = new java.util.HashMap<>();
        for (NetworkGameStateSnapshot.BuildingSnapshot bs : buildingSnapshots) {
            snapshotMap.put(bs.getId(), bs);
        }

        java.util.List<Building> toRemove = new java.util.ArrayList<>();
        java.util.Set<Integer> matchedIds = new java.util.HashSet<>();

        for (Building building : activeBuildings) {
            int buildingId = System.identityHashCode(building);
            NetworkGameStateSnapshot.BuildingSnapshot bs = snapshotMap.get(buildingId);

            if (bs != null) {
                building.setCurrentHealth(bs.getHealth());
                matchedIds.add(buildingId);
            } else {
                toRemove.add(building);
            }
        }

        activeBuildings.removeAll(toRemove);

        // Add new buildings
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

    private void applyProjectilesFromSnapshot(List<NetworkGameStateSnapshot.ProjectileSnapshot> projectileSnapshots) {
        if (projectileSnapshots == null)
            return;

        java.util.Map<Integer, NetworkGameStateSnapshot.ProjectileSnapshot> snapshotMap = new java.util.HashMap<>();
        for (NetworkGameStateSnapshot.ProjectileSnapshot ps : projectileSnapshots) {
            snapshotMap.put(ps.getId(), ps);
        }

        for (Projectile proj : activeProjectiles) {
            int projId = System.identityHashCode(proj);
            NetworkGameStateSnapshot.ProjectileSnapshot ps = snapshotMap.get(projId);
            if (ps != null) {
                proj.setPosition(new Vector2(ps.getX(), ps.getY()));
            }
        }
    }

    public void setGameOver(boolean isGameOver, boolean playerWon, boolean isDraw) {
        this.isGameOver = isGameOver;
        this.playerWon = playerWon;
        this.isDraw = isDraw;
    }
}