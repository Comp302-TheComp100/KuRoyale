package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

import com.kuroyale.service.TargetingService;

//Central game state manager.Holds references to player and bot states, arena, and manages the game loop updates.
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
    private final List<SpellEffect> activeSpellEffects;
    private final TargetingService targetingService = new TargetingService();
    private final com.kuroyale.service.TroopMovementService troopMovementService = new com.kuroyale.service.TroopMovementService();
    private final com.kuroyale.service.CombatService combatService = new com.kuroyale.service.CombatService();

    private double gameTime = 180.0; // 3 minutes
    private int playerScore = 0;
    private int botScore = 0;

    private boolean isDoubleElixir = false;
    private boolean isGameOver = false;
    private boolean playerWon = false;
    
    // Track towers that have already been scored to avoid double-counting
    private java.util.Set<Tower> scoredTowers = new java.util.HashSet<>();

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
        this.activeSpellEffects = new ArrayList<>();
    }
    
    //Restores game state from saved data
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
    
    //Restores tower health from saved data
    public void restoreTowerHealth(SavedGameState.SavedTower savedTower) {
        // Find the matching tower in the arena
        java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType tt = cell.getTileType();
            boolean isTowerTile = tt == TileType.PRINCESS_TOWER_USER || 
                                 tt == TileType.PRINCESS_TOWER_COMPUTER ||
                                 tt == TileType.KING_TOWER_USER || 
                                 tt == TileType.KING_TOWER_COMPUTER;
            if (!isTowerTile) continue;
            
            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null) continue;
            
            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }
        
        // Match saved tower to actual tower by position and type
        for (java.util.Map.Entry<Tower, java.util.List<GridCell>> entry : groups.entrySet()) {
            Tower tower = entry.getKey();
            java.util.List<GridCell> cells = entry.getValue();
            
            if (cells.isEmpty()) continue;
            
            // Get tower position (top-left)
            int minX = cells.stream().mapToInt(c -> c.getPosition().getX()).min().orElse(0);
            int minY = cells.stream().mapToInt(c -> c.getPosition().getY()).min().orElse(0);
            
            // Check if player side matches
            TileType firstTileType = cells.get(0).getTileType();
            boolean isPlayerSide = firstTileType == TileType.PRINCESS_TOWER_USER || firstTileType == TileType.KING_TOWER_USER;
            
            // Check if type and position match
            if (tower.getType().name().equals(savedTower.getTowerType()) &&
                isPlayerSide == savedTower.isPlayerSide() &&
                minX == savedTower.getGridX() &&
                minY == savedTower.getGridY()) {
                tower.setCurrentHealth(savedTower.getCurrentHealth());
                break;
            }
        }
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
                    if (playerScore > botScore) {
                        playerWon = true;
                    } else {
                        // For bot win or draw, playerWon remains false; draw is handled by consumers via scores
                        playerWon = false;
                    }
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

        // Update placed cards
        troopMovementService.updateTroops(deltaTime, this, activeTroops);

        // Update buildings (lifetime depreciation)
        for (Building b : activeBuildings) {
            if (b.isAlive()) {
                b.update(deltaTime);
            }
        }

        updateBuildingsCombat(deltaTime);
        updateTowersCombat(deltaTime);
        updateSpellEffects(deltaTime);

        // Cleanup destroyed buildings and free their occupied tiles
        if (!activeBuildings.isEmpty()) {
            java.util.Iterator<Building> it = activeBuildings.iterator();
            while (it.hasNext()) {
                Building b = it.next();
                if (!b.isAlive()) {
                    freeFootprint(b);
                    it.remove();
                }
            }
        }

        // Check for destroyed towers and update scores (must be before removeDeadTowers)
        if (!isGameOver) {
            checkAndScoreDestroyedTowers();
        }

        // Cleanup destroyed towers
        arena.removeDeadTowers();

        // Check for King Tower destruction
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

    public boolean isDoubleElixir() {return isDoubleElixir;}
    public boolean isGameOver() {return isGameOver;}
    public boolean isPlayerWinner() {return playerWon;}

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

        // Validate terrain (Grass or Bridge only) - unless it's a spell
        if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
            return false;
        }

        // Validate side (Player can only deploy on bottom half) - unless it's a spell
        if (!isSpell && isPlayer && y < Arena.HEIGHT / 2) {
            return false;
        }

        if (isPlayer) {
            Card card = pendingCard != null ? pendingCard : playerHand.getCard(handIndex);
            if (card != null && playerElixir.spend(card.getCost())) {
                playerHand.playCard(handIndex);
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
                } else if (card.getType() == CardType.BUILDING) {
                    // Building footprint from card metadata
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
                        Building building = new Building(topLeft, bw, bh, isPlayer, card.getHp(), card.getImagePath(), card.getLifetime());
                        building.configureCombatFromCard(card);
                        occupyFootprint(building);
                        activeBuildings.add(building);
                    }
                } else if (card.getType() == CardType.SPELL) {
                    // Apply spell immediately at target position
                    applySpellEffect(isPlayer, card, x, y);
                }
                return true;
            }
        } else {
        }
        return false;
    }

    public double getGameTime() {return gameTime;}
    public int getPlayerScore() {return playerScore;}
    public int getBotScore() {return botScore;}

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
        } else if (card.getType() == CardType.BUILDING) {
            int bw = Math.max(1, card.getFootprintWidthTiles());
            int bh = Math.max(1, card.getFootprintHeightTiles());
            if (x < 0 || y < 0 || (x + bw) >= Arena.WIDTH || (y + bh) >= Arena.HEIGHT) {
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
                Building building = new Building(topLeft, bw, bh, isPlayer, card.getHp(), card.getImagePath(), card.getLifetime());
                building.configureCombatFromCard(card);
                occupyFootprint(building);
                activeBuildings.add(building);
            }
        } else if (card.getType() == CardType.SPELL) {
            applySpellEffect(isPlayer, card, x, y);
        }
    }

    public Hand getPlayerHand() {return playerHand;}

    // Apply spell effects: simple AoE damage around target (affects enemy troops, buildings, and towers)
    private void applySpellEffect(boolean isPlayer, Card spell, int x, int y) {
        // Use card damage and range as radius in tiles
        int radius = (int) Math.max(0, Math.round(spell.getRange()));
        int damage = Math.max(0, spell.getDamage());
        GridPosition center = GridPosition.tryCreate(x, y);
        if (center == null)
            return;

        // Damage enemy troops
        for (Troop t : new java.util.ArrayList<>(activeTroops)) {
            if (!t.isAlive())
                continue;
            if (t.isPlayerSide() == isPlayer)
                continue;
            double dist = center.getEuclideanDistanceTo(t.getPosition());
            if (dist <= radius) {
                t.takeDamage(damage);
            }
        }
        activeTroops.removeIf(t -> !t.isAlive());

        // Damage enemy buildings
        for (Building b : new java.util.ArrayList<>(activeBuildings)) {
            if (!b.isAlive())
                continue;
            if (b.isPlayerSide() == isPlayer)
                continue;
            int cx = b.getPosition().getX() + Math.max(0, b.getWidth() - 1) / 2;
            int cy = b.getPosition().getY() + Math.max(0, b.getHeight() - 1) / 2;
            GridPosition bc = GridPosition.tryCreate(cx, cy);
            double dist = bc != null ? center.getEuclideanDistanceTo(bc)
                    : center.getEuclideanDistanceTo(b.getPosition());
            if (dist <= radius) {
                b.takeDamage(damage);
                if (!b.isAlive()) {
                    freeFootprint(b);
                }
            }
        }
        activeBuildings.removeIf(b -> !b.isAlive());

        // Damage enemy towers
        java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType tt = cell.getTileType();
            boolean enemyTowerTile = isPlayer
                    ? (tt == TileType.PRINCESS_TOWER_COMPUTER || tt == TileType.KING_TOWER_COMPUTER)
                    : (tt == TileType.PRINCESS_TOWER_USER || tt == TileType.KING_TOWER_USER);
            if (!enemyTowerTile)
                continue;
            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null || tower.getCurrentHealth() <= 0)
                continue;
            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }
        for (java.util.Map.Entry<Tower, java.util.List<GridCell>> e : groups.entrySet()) {
            java.util.List<GridCell> cells = e.getValue();
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (GridCell c : cells) {
                int px = c.getPosition().getX();
                int py = c.getPosition().getY();
                minX = Math.min(minX, px);
                minY = Math.min(minY, py);
                maxX = Math.max(maxX, px);
                maxY = Math.max(maxY, py);
            }
            int cx = (minX + maxX) / 2;
            int cy = (minY + maxY) / 2;
            GridPosition tc = GridPosition.tryCreate(cx, cy);
            double dist = tc != null ? center.getEuclideanDistanceTo(tc) : 0.0;
            if (dist <= radius) {
                e.getKey().takeDamage(damage);
            }
        }
        // Track effect for UI for 1 second
        activeSpellEffects.add(new SpellEffect(GridPosition.tryCreate(x, y), radius, isPlayer, 1.0));
    }

    /**
     * Apply circular area damage originating from a troop attack.
     * Center is derived from the primary target to keep targeting logic unchanged.
     */
    public void applyAreaDamageFromTroop(Troop attacker, Troop primaryTarget) {
        if (attacker == null || primaryTarget == null || !primaryTarget.isAlive())
            return;
        GridPosition center = primaryTarget.getPosition();
        if (center == null)
            return;
        int radiusTiles = computeAoERadiusTiles(attacker);
        if (radiusTiles <= 0)
            return;
        applyAreaDamageFromTroopInternal(attacker, center, radiusTiles);
    }

    public void applyAreaDamageFromTroop(Troop attacker, Building primaryTarget) {
        if (attacker == null || primaryTarget == null || !primaryTarget.isAlive())
            return;
        GridPosition center = buildingCenter(primaryTarget);
        if (center == null)
            return;
        int radiusTiles = computeAoERadiusTiles(attacker);
        if (radiusTiles <= 0)
            return;
        applyAreaDamageFromTroopInternal(attacker, center, radiusTiles);
    }

    public void applyAreaDamageFromTroop(Troop attacker, Tower primaryTarget) {
        if (attacker == null || primaryTarget == null || primaryTarget.getCurrentHealth() <= 0)
            return;
        GridPosition center = towerCenter(primaryTarget);
        if (center == null)
            return;
        int radiusTiles = computeAoERadiusTiles(attacker);
        if (radiusTiles <= 0)
            return;
        applyAreaDamageFromTroopInternal(attacker, center, radiusTiles);
    }

    private int computeAoERadiusTiles(Troop attacker) {
        // Fixed small splash radius for all area-effect troops
        if (attacker == null)
            return 0;
        return 1;
    }

    private GridPosition buildingCenter(Building b) {
        if (b == null || b.getPosition() == null)
            return null;
        int cx = b.getPosition().getX() + Math.max(0, b.getWidth() - 1) / 2;
        int cy = b.getPosition().getY() + Math.max(0, b.getHeight() - 1) / 2;
        return GridPosition.tryCreate(cx, cy);
    }

    private GridPosition towerCenter(Tower tower) {
        if (tower == null)
            return null;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (GridCell cell : arena.getAllCells()) {
            Tower t = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (t != tower)
                continue;
            int px = cell.getPosition().getX();
            int py = cell.getPosition().getY();
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            maxX = Math.max(maxX, px);
            maxY = Math.max(maxY, py);
        }
        if (minX == Integer.MAX_VALUE)
            return null;
        int cx = (minX + maxX) / 2;
        int cy = (minY + maxY) / 2;
        return GridPosition.tryCreate(cx, cy);
    }

    /**
     * Core AoE implementation shared by all troop-based area attacks.
     * Respects TargetType for air/ground restrictions and can optionally show a short-lived visual ring.
     */
    private void applyAreaDamageFromTroopInternal(Troop attacker, GridPosition center, int radiusTiles) {
        if (attacker == null || center == null || radiusTiles <= 0)
            return;

        int damage = attacker.getCombatStats() != null ? attacker.getCombatStats().getDamage() : 0;
        if (damage <= 0)
            return;

        TargetType targetType = attacker.getBaseCard() != null ? attacker.getBaseCard().getTarget() : TargetType.BOTH;

        // Damage enemy troops, enforcing TargetType via TargetingService
        for (Troop t : new java.util.ArrayList<>(activeTroops)) {
            if (!t.isAlive())
                continue;
            if (t.isPlayerSide() == attacker.isPlayerSide())
                continue;
            if (!targetingService.isValidTarget(attacker, t))
                continue;
            int dx = Math.abs(t.getPosition().getX() - center.getX());
            int dy = Math.abs(t.getPosition().getY() - center.getY());
            // Plus-shaped small splash: center + 4 orthogonal neighbors
            if (dx + dy <= radiusTiles) {
                t.takeDamage(damage);
            }
        }
        activeTroops.removeIf(t -> !t.isAlive());

        // Damage enemy buildings if attacker can hit ground/structures
        if (targetType != TargetType.AIR && targetType != TargetType.NONE && !activeBuildings.isEmpty()) {
            for (Building b : new java.util.ArrayList<>(activeBuildings)) {
                if (!b.isAlive())
                    continue;
                if (b.isPlayerSide() == attacker.isPlayerSide())
                    continue;
                GridPosition bc = buildingCenter(b);
                if (bc == null)
                    continue;
                int dx = Math.abs(bc.getX() - center.getX());
                int dy = Math.abs(bc.getY() - center.getY());
                if (dx + dy <= radiusTiles) {
                    b.takeDamage(damage);
                    if (!b.isAlive()) {
                        freeFootprint(b);
                    }
                }
            }
            activeBuildings.removeIf(b -> !b.isAlive());
        }

        // Damage enemy towers if attacker can hit ground/structures
        if (targetType != TargetType.AIR && targetType != TargetType.NONE) {
            java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
            for (GridCell cell : arena.getAllCells()) {
                TileType tt = cell.getTileType();
                boolean enemyTowerTile = attacker.isPlayerSide()
                        ? (tt == TileType.PRINCESS_TOWER_COMPUTER || tt == TileType.KING_TOWER_COMPUTER)
                        : (tt == TileType.PRINCESS_TOWER_USER || tt == TileType.KING_TOWER_USER);
                if (!enemyTowerTile)
                    continue;
                Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
                if (tower == null || tower.getCurrentHealth() <= 0)
                    continue;
                groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
            }
            for (java.util.Map.Entry<Tower, java.util.List<GridCell>> e : groups.entrySet()) {
                Tower tower = e.getKey();
                java.util.List<GridCell> cells = e.getValue();
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                for (GridCell c : cells) {
                    int px = c.getPosition().getX();
                    int py = c.getPosition().getY();
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px);
                    maxY = Math.max(maxY, py);
                }
                int cx = (minX + maxX) / 2;
                int cy = (minY + maxY) / 2;
                GridPosition tc = GridPosition.tryCreate(cx, cy);
                if (tc == null)
                    continue;
                int dx = Math.abs(tc.getX() - center.getX());
                int dy = Math.abs(tc.getY() - center.getY());
                if (dx + dy <= radiusTiles) {
                    tower.takeDamage(damage);
                }
            }
        }

        // Short-lived visual ring for this AoE, rendered via BattleArenaView.renderSpellEffects
        activeSpellEffects.add(new SpellEffect(center, radiusTiles, attacker.isPlayerSide(), 0.3));
    }

    /**
     * Apply circular area damage originating from a building attack or death explosion.
     * Only damages enemy troops and respects the building's targeting rules.
     */
    private void applyAreaDamageFromBuilding(Building attacker, GridPosition center, int radiusTiles) {
        if (attacker == null || center == null || radiusTiles <= 0)
            return;

        int damage = attacker.getDamage();
        if (damage <= 0)
            return;

        // Damage enemy troops respecting building target rules
        for (Troop t : new java.util.ArrayList<>(activeTroops)) {
            if (!t.isAlive())
                continue;
            if (t.isPlayerSide() == attacker.isPlayerSide())
                continue;
            if (!attacker.canTargetTroop(t))
                continue;
            int dx = Math.abs(t.getPosition().getX() - center.getX());
            int dy = Math.abs(t.getPosition().getY() - center.getY());
            // Plus-shaped small splash: center + 4 orthogonal neighbors
            if (dx + dy <= radiusTiles) {
                t.takeDamage(damage);
            }
        }
        activeTroops.removeIf(t -> !t.isAlive());

        // Optional: also damage nearby enemy buildings/towers if this building can hit ground/structures
        TargetType bt = attacker.getTargetType();
        if (bt != TargetType.AIR && bt != TargetType.NONE) {
            // Buildings
            for (Building b : new java.util.ArrayList<>(activeBuildings)) {
                if (!b.isAlive())
                    continue;
                if (b.isPlayerSide() == attacker.isPlayerSide())
                    continue;
                GridPosition bc = buildingCenter(b);
                if (bc == null)
                    continue;
                int dx = Math.abs(bc.getX() - center.getX());
                int dy = Math.abs(bc.getY() - center.getY());
                if (dx + dy <= radiusTiles) {
                    b.takeDamage(damage);
                    if (!b.isAlive()) {
                        freeFootprint(b);
                    }
                }
            }
            activeBuildings.removeIf(b -> !b.isAlive());

            // Towers
            java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
            for (GridCell cell : arena.getAllCells()) {
                TileType tt = cell.getTileType();
                boolean enemyTowerTile = attacker.isPlayerSide()
                        ? (tt == TileType.PRINCESS_TOWER_COMPUTER || tt == TileType.KING_TOWER_COMPUTER)
                        : (tt == TileType.PRINCESS_TOWER_USER || tt == TileType.KING_TOWER_USER);
                if (!enemyTowerTile)
                    continue;
                Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
                if (tower == null || tower.getCurrentHealth() <= 0)
                    continue;
                groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
            }
            for (java.util.Map.Entry<Tower, java.util.List<GridCell>> e : groups.entrySet()) {
                Tower tower = e.getKey();
                java.util.List<GridCell> cells = e.getValue();
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                for (GridCell c : cells) {
                    int px = c.getPosition().getX();
                    int py = c.getPosition().getY();
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px);
                    maxY = Math.max(maxY, py);
                }
                int cx = (minX + maxX) / 2;
                int cy = (minY + maxY) / 2;
                GridPosition tc = GridPosition.tryCreate(cx, cy);
                if (tc == null)
                    continue;
                int dx = Math.abs(tc.getX() - center.getX());
                int dy = Math.abs(tc.getY() - center.getY());
                if (dx + dy <= radiusTiles) {
                    tower.takeDamage(damage);
                }
            }
        }

        // Short-lived visual ring for this AoE, rendered via BattleArenaView.renderSpellEffects
        activeSpellEffects.add(new SpellEffect(center, radiusTiles, attacker.isPlayerSide(), 0.3));
    }

    private void updateSpellEffects(double deltaTime) {
        if (activeSpellEffects.isEmpty())
            return;
        java.util.Iterator<SpellEffect> it = activeSpellEffects.iterator();
        while (it.hasNext()) {
            SpellEffect se = it.next();
            se.timeRemaining -= deltaTime;
            if (se.timeRemaining <= 0) {
                it.remove();
            }
        }
    }

    public static class SpellEffect {
        public final GridPosition center;
        public final int radiusTiles;
        public final boolean isPlayerSide;
        public double timeRemaining;

        public SpellEffect(GridPosition center, int radiusTiles, boolean isPlayerSide, double timeSeconds) {
            this.center = center;
            this.radiusTiles = radiusTiles;
            this.isPlayerSide = isPlayerSide;
            this.timeRemaining = timeSeconds;
        }
    }

    private void updateBuildingsCombat(double deltaTime) {
        for (Building b : activeBuildings) {
            if (!b.isAlive())
                continue;
            // Find nearest enemy troop within range respecting target type
            Troop best = null;
            double bestDist = Double.MAX_VALUE;
            for (Troop t : activeTroops) {
                if (!t.isAlive())
                    continue;
                if (t.isPlayerSide() == b.isPlayerSide())
                    continue;
                if (!b.canTargetTroop(t))
                    continue;
                // Measure from building center
                int cx = b.getPosition().getX() + Math.max(0, b.getWidth() - 1) / 2;
                int cy = b.getPosition().getY() + Math.max(0, b.getHeight() - 1) / 2;
                GridPosition center = GridPosition.tryCreate(cx, cy);
                double dist = center != null ? center.getEuclideanDistanceTo(t.getPosition())
                        : b.getPosition().getEuclideanDistanceTo(t.getPosition());
                if (dist <= b.getRangeTiles() && dist < bestDist) {
                    bestDist = dist;
                    best = t;
                }
            }
            if (best != null) {
                double cd = b.getAttackCooldown() - deltaTime;
                if (cd <= 0) {
                    if (b.isAreaEffect()) {
                        // Small fixed-radius splash around the primary target troop
                        GridPosition impactCenter = best.getPosition();
                        if (impactCenter != null) {
                            applyAreaDamageFromBuilding(b, impactCenter, 1);
                        }
                    } else {
                        combatService.applyDamage(b, best);
                    }
                    b.setAttackCooldown(Math.max(0.1, b.getHitSpeedSeconds()));
                } else {
                    b.setAttackCooldown(cd);
                }
            } else {
                // Cooldown still ticks down when idle
                double cd = Math.max(0.0, b.getAttackCooldown() - deltaTime);
                b.setAttackCooldown(cd);
            }
        }
        // Remove dead troops post building attacks
        activeTroops.removeIf(t -> !t.isAlive());

        // Trigger death explosion for area-effect buildings (e.g., Bomb Tower)
        if (!activeBuildings.isEmpty()) {
            java.util.List<Building> deadAreaBuildings = new java.util.ArrayList<>();
            for (Building b : activeBuildings) {
                if (!b.isAlive() && b.isAreaEffect()) {
                    deadAreaBuildings.add(b);
                }
            }
            for (Building b : deadAreaBuildings) {
                GridPosition center = buildingCenter(b);
                if (center != null) {
                    applyAreaDamageFromBuilding(b, center, 1);
                }
                // Footprint will be freed in the main update loop where inactive buildings are removed
            }
        }
    }

    /*Checks for destroyed towers and updates scores accordingly.
     * Princess towers: +1 point to the attacker
     * King towers: Set attacker's score to 3 and end the game  */
    private void checkAndScoreDestroyedTowers() {
        // Collect unique towers with inferred side and type
        java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType tt = cell.getTileType();
            boolean isTowerTile = tt == TileType.PRINCESS_TOWER_USER || tt == TileType.PRINCESS_TOWER_COMPUTER
                    || tt == TileType.KING_TOWER_USER || tt == TileType.KING_TOWER_COMPUTER;
            if (!isTowerTile)
                continue;
            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null)
                continue;
            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }
        
        // Process each unique tower
        for (java.util.Map.Entry<Tower, java.util.List<GridCell>> entry : groups.entrySet()) {
            Tower tower = entry.getKey();
            
            // Skip if already scored or still alive
            if (scoredTowers.contains(tower) || tower.getCurrentHealth() > 0)
                continue;
            
            // Determine ownership and type from cells
            boolean isPlayerTower = false;
            boolean isKingTower = false;
            for (GridCell c : entry.getValue()) {
                TileType tt = c.getTileType();
                if (tt == TileType.PRINCESS_TOWER_USER || tt == TileType.KING_TOWER_USER)
                    isPlayerTower = true;
                if (tt == TileType.KING_TOWER_USER || tt == TileType.KING_TOWER_COMPUTER)
                    isKingTower = true;
            }
            
            // Mark as scored to avoid double-counting
            scoredTowers.add(tower);
            
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
        }
    }

    private void updateTowersCombat(double deltaTime) {
        // Collect unique towers with inferred side and footprint size
        java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType tt = cell.getTileType();
            boolean isTowerTile = tt == TileType.PRINCESS_TOWER_USER || tt == TileType.PRINCESS_TOWER_COMPUTER || tt == TileType.KING_TOWER_USER || tt == TileType.KING_TOWER_COMPUTER;
            if (!isTowerTile)
                continue;
            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null)
                continue;
            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }
        for (java.util.Map.Entry<Tower, java.util.List<GridCell>> entry : groups.entrySet()) {
            Tower tower = entry.getKey();
            if (tower.getCurrentHealth() <= 0)
                continue;
            java.util.List<GridCell> cells = entry.getValue();
            // Infer side and center from cells
            boolean isPlayerTower = false; // default
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (GridCell c : cells) {
                TileType tt = c.getTileType();
                if (tt == TileType.PRINCESS_TOWER_USER || tt == TileType.KING_TOWER_USER)
                    isPlayerTower = true;
                int x = c.getPosition().getX();
                int y = c.getPosition().getY();
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
            int cx = (minX + maxX) / 2;
            int cy = (minY + maxY) / 2;
            GridPosition center = GridPosition.tryCreate(cx, cy);

            // Find nearest enemy troop within tower range
            Troop best = null;
            double bestDist = Double.MAX_VALUE;
            for (Troop t : activeTroops) {
                if (!t.isAlive())
                    continue;
                if (t.isPlayerSide() == isPlayerTower)
                    continue;
                // Respect target type
                if (tower.getTargetType() == TargetType.GROUND && t.isAirUnit())
                    continue;
                double dist = center != null ? center.getEuclideanDistanceTo(t.getPosition()) : 0.0;
                if (dist <= Math.round(tower.getRange()) && dist < bestDist) {
                    bestDist = dist;
                    best = t;
                }
            }
            // Attack using cooldown
            double cd = tower.getAttackCooldown() - deltaTime;
            if (best != null) {
                if (cd <= 0) {
                    combatService.applyDamage(tower, best);
                    tower.setAttackCooldown(Math.max(0.1, tower.getHitSpeed()));
                } else {
                    tower.setAttackCooldown(cd);
                }
            } else {
                // tick down
                tower.setAttackCooldown(Math.max(0.0, cd));
            }
        }
        // Remove any dead troops after tower attacks
        activeTroops.removeIf(t -> !t.isAlive());
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
    public List<SpellEffect> getActiveSpellEffects() {
        return activeSpellEffects;
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
                            // ignore if invalid
                        }
                    }
                }
            }
        }
    }

    private void freeFootprint(Building b) {
        for (int dx = 0; dx < b.getWidth(); dx++) {
            for (int dy = 0; dy < b.getHeight(); dy++) {
                int gx = b.getPosition().getX() + dx;
                int gy = b.getPosition().getY() + dy;
                GridPosition pos = GridPosition.tryCreate(gx, gy);
                if (pos != null) {
                    GridCell cell = arena.getCell(pos);
                    if (cell != null) {
                        if (cell.isOccupied() && cell.getOccupant() == b) {
                            try {
                                cell.clearOccupant();
                            } catch (IllegalStateException e) {
                                // ignore if invalid; occupancy will be reset by tile type
                            }
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