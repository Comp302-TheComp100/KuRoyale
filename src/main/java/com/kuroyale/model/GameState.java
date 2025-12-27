package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

//Central game state manager.Holds references to player and bot states, arena, and manages the game loop updates.
public class GameState {
    private final Hand playerHand;
    private final ElixirManager playerElixir;

    private final ElixirManager botElixir;
    private final BotLogic bot;

    private final Arena arena;
    private final List<PlacedCard> placedCards;
    private final List<Troop> activeTroops;
    private final List<Building> activeBuildings;
    private final List<SpellEffect> activeSpellEffects;
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
        this.activeSpellEffects = new ArrayList<>();
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

    // Restores tower health from saved data
    public void restoreTowerHealth(SavedGameState.SavedTower savedTower) {
        // Find the matching tower in the arena
        java.util.Map<Tower, java.util.List<GridCell>> groups = new java.util.HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType tt = cell.getTileType();
            boolean isTowerTile = tt == TileType.PRINCESS_TOWER_USER ||
                    tt == TileType.PRINCESS_TOWER_COMPUTER ||
                    tt == TileType.KING_TOWER_USER ||
                    tt == TileType.KING_TOWER_COMPUTER;
            if (!isTowerTile)
                continue;

            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null)
                continue;

            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }

        // Match saved tower to actual tower by position and type
        for (java.util.Map.Entry<Tower, java.util.List<GridCell>> entry : groups.entrySet()) {
            Tower tower = entry.getKey();
            java.util.List<GridCell> cells = entry.getValue();

            if (cells.isEmpty())
                continue;

            // Get tower position (top-left)
            int minX = cells.stream().mapToInt(c -> c.getPosition().getX()).min().orElse(0);
            int minY = cells.stream().mapToInt(c -> c.getPosition().getY()).min().orElse(0);

            // Check if player side matches
            TileType firstTileType = cells.get(0).getTileType();
            boolean isPlayerSide = firstTileType == TileType.PRINCESS_TOWER_USER
                    || firstTileType == TileType.KING_TOWER_USER;

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
                    } else {
                        // For bot win or draw, playerWon remains false
                        playerWon = false;
                    }
                }
            }
        }
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
        updateBuildingsCombat(deltaTime);
        updateTowersCombat(deltaTime);
        updateSpellEffects(deltaTime);
    }

    private void cleanupEntities() {
        // Cleanup destroyed buildings and free their occupied tiles
        if (!activeBuildings.isEmpty()) {
            java.util.Iterator<Building> it = activeBuildings.iterator();
            while (it.hasNext()) {
                Building b = it.next();
                if (!b.isAlive()) {
                    arena.freeFootprint(b);
                    it.remove();
                }
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

    // Generalize combat logic for any structure (Building or Tower)
    private void processStructureCombat(ICombatant structure, double deltaTime) {
        if (!structure.isAlive())
            return;

        // Find nearest valid target
        Troop best = null;
        double bestDist = Double.MAX_VALUE;
        GridPosition center = structure.getCenterPosition();
        if (center == null)
            return;

        double range = structure.getRange();

        for (Troop t : activeTroops) {
            if (!t.isAlive())
                continue;
            // Friendly fire check
            if (t.isPlayerSide() == structure.isPlayerSide())
                continue;

            // Check targeting rules (Ground/Air) via ICombatant
            if (!structure.canTarget(t))
                continue;

            double dist = center.getEuclideanDistanceTo(t.getPosition());
            if (dist <= range && dist < bestDist) {
                bestDist = dist;
                best = t;
            }
        }

        // Handle Attack and Cooldown
        double cd = structure.getAttackCooldown() - deltaTime;
        if (best != null) {
            if (cd <= 0) {
                if (structure.isAreaEffect()) {
                    // Area effect (splash around target)
                    // Use a default small splash radius (e.g., 1 tile) or define in ICombatant if
                    // variable
                    combatService.applyAreaDamage(this, best.getPosition(), 1.0, structure.getDamage(),
                            structure.getTargetType(), structure.isPlayerSide());
                } else {
                    // Single target
                    combatService.applyDamage(structure, best);
                }
                structure.setAttackCooldown(Math.max(0.1, structure.getHitSpeed()));
            } else {
                structure.setAttackCooldown(cd);
            }
        } else {
            // No target, cooldown just ticks down
            structure.setAttackCooldown(Math.max(0.0, cd));
        }
    }

    private void updateBuildingsCombat(double deltaTime) {
        for (Building b : activeBuildings) {
            processStructureCombat(b, deltaTime);
        }

        // Remove dead troops post building attacks
        activeTroops.removeIf(t -> !t.isAlive());

        // Trigger death explosion for area-effect buildings (e.g., Bomb Tower)
        // Check for buildings that died this frame?
        // Logic in original code checked activeBuildings for dead ones before cleanup.
        // We moved cleanup to cleanupEntities(), which runs AFTER handleCombat.
        // So dead buildings are still in activeBuildings list here but isAlive() is
        // false.
        // We need to iterate and check dead ones for death damage.
        for (Building b : activeBuildings) {
            if (!b.isAlive() && b.isAreaEffect()) { // Assuming death damage is tied to isAreaEffect like Bomb Tower
                // The original code did this. We should replicate or improve.
                // We need to ensure we don't trigger this multiple times.
                // The original code removed them right after.
                // Here cleanup is later.
                // Problem: If we don't remove them, we might trigger death damage multiple
                // times if update() runs twice before cleanup?
                // No, cleanupEntities runs in same frame.
                GridPosition center = b.getCenterPosition();
                if (center != null) {
                    combatService.applyAreaDamage(this, center, 1.0, b.getDamage(), b.getTargetType(),
                            b.isPlayerSide());
                }
            }
        }
    }

    private void updateTowersCombat(double deltaTime) {
        java.util.Set<Tower> towers = arena.getAllTowers();
        for (Tower t : towers) {
            processStructureCombat(t, deltaTime);
        }
        // Remove any dead troops after tower attacks
        activeTroops.removeIf(t -> !t.isAlive());
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

    public int getPlayerDamageTaken() {
        int damage = 0;
        // Since we don't know easily which tower belongs to whom without position,
        // we have to be careful. But getTowerAt uses grid position.

        // Better approach: Iterate all cells, if it's a player tower tile, check the
        // tower.
        // But towers are multi-tile.

        // We can just iterate the unique towers and check their type/side?
        // Tower class doesn't store "side". It stores "Type" (King/Princess).
        // Side is determined by placement location in Arena (TileType).

        // So we must iterate cells or towerMap.
        // Arena doesn't expose towerMap keys directly.
        // Let's iterate all cells to find unique towers belonging to player.
        java.util.Set<Tower> playerTowers = new java.util.HashSet<>();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = arena.getCell(x, y);
                TileType tt = cell.getTileType();
                if (tt == TileType.PRINCESS_TOWER_USER || tt == TileType.KING_TOWER_USER) {
                    Tower t = arena.getTowerAt(x, y);
                    if (t != null)
                        playerTowers.add(t);
                }
            }
        }

        for (Tower t : playerTowers) {
            damage += (t.getMaxHealth() - t.getCurrentHealth());
        }
        return damage;
    }

    /*
     * public boolean placeCard(boolean isPlayer, int handIndex, int x, int y) {
     * // 1. Basic Validation (Player Specific)
     * if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
     * return false;
     * }
     * 
     * // Validate side (Player can only deploy on bottom half), unless it's a spell
     * boolean isSpell = false;
     * Card pendingCard = null;
     * if (isPlayer) {
     * pendingCard = playerHand.getCard(handIndex);
     * if (pendingCard != null && pendingCard.getType() == CardType.SPELL) {
     * isSpell = true;
     * }
     * }
     * 
     * // Validate terrain (Grass or Bridge only) - unless it's a spell
     * if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
     * return false;
     * }
     * 
     * if (isPlayer && !isSpell && y < Arena.HEIGHT / 2) {
     * return false;
     * }
     * 
     * if (isPlayer) {
     * Card card = pendingCard != null ? pendingCard :
     * playerHand.getCard(handIndex);
     * if (card == null)
     * return false;
     * 
     * // 2. Cost Calculation (Challenge Logic)
     * int cost = card.getCost();
     * if (activeChallenge == ChallengeType.SPELL_BARRAGE && card.getType() ==
     * CardType.SPELL) {
     * cost = Math.max(1, cost - 1);
     * }
     * 
     * // 3. Elixir Check & Spend
     * if (playerElixir.spend(cost)) {
     * // 4. Play Card & Spawn
     * playerHand.playCard(handIndex);
     * boolean success = spawnUnit(true, card, x, y);
     * 
     * // Track Elixir Spent (Quest) - Moved here to ensure it only triggers on
     * // successful spend
     * if (success) {
     * com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
     * .updateProgress(com.kuroyale.model.QuestType.SPEND_ELIXIR, cost);
     * }
     * return success;
     * }
     * }
     * return false;
     * }
     */

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

            // --- CRITICAL FIX: TRANSACTIONAL LOGIC ---

            // Adım A: İksir yetiyor mu KONTROL ET (Ama harcama!)
            // Not: ElixirManager'da 'getCurrentElixir()' metodu olduğunu varsayıyorum.
            if (playerElixir.getCurrentElixir() >= cost) {

                // Adım B: Birimi koymayı DENE (Bina çakışması vb. burada kontrol edilir)
                boolean success = spawnUnit(true, card, x, y);

                // Adım C: Sadece başarılıysa HARCA ve KARTI SİL
                if (success) {
                    playerElixir.spend(cost); // Şimdi düşüyoruz
                    playerHand.playCard(handIndex); // Kartı elden çıkarıyoruz

                    // Quest Updates (Sadece başarılı işlemde tetiklenir)
                    com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                            .updateProgress(com.kuroyale.model.QuestType.SPEND_ELIXIR, cost);
                }

                return success;
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

    // Overload for direct card placement (used by Bot)
    public void placeCard(boolean isPlayer, Card card, int x, int y) {
        // Delegating strictly to spawnUnit.
        // Note: Bot elixir is already spent in BotLogic.
        spawnUnit(isPlayer, card, x, y);
    }

    /**
     * Unified logic for spawning units (Troops, Buildings, Spells).
     * Handles physical creation, specialized validation (building footprint), and
     * Quest/Achievement tracking.
     * 
     * @return true if spawn was successful (e.g. building footprint valid), false
     *         otherwise.
     * 
     *         private boolean spawnUnit(boolean isPlayer, Card card, int x, int y)
     *         {
     *         if (card == null)
     *         return false;
     * 
     *         // 1. Specific Validation & Creation
     *         if (card.getType() == CardType.BUILDING) {
     *         // Validate Footprint
     *         int bw = Math.max(1, card.getFootprintWidthTiles());
     *         int bh = Math.max(1, card.getFootprintHeightTiles());
     *         // Prevent exceeding bounds and enforce margin
     *         int mx = Math.max(0, bw - 1);
     *         int my = Math.max(0, bh - 1);
     * 
     *         if (x < mx || y < my || (x + bw) > (Arena.WIDTH - mx) || (y + bh) >
     *         (Arena.HEIGHT - my)) {
     *         return false;
     *         }
     * 
     *         // Validate all cells in footprint
     *         for (int dx = 0; dx < bw; dx++) {
     *         for (int dy = 0; dy < bh; dy++) {
     *         GridCell c = arena.getCell(x + dx, y + dy);
     *         if (c == null || c.isOccupied() || !c.isWalkable()) {
     *         return false;
     *         }
     *         }
     *         }
     * 
     *         // Create Building
     *         GridPosition topLeft = GridPosition.tryCreate(x, y);
     *         if (topLeft != null) {
     *         Building building = new Building(topLeft, bw, bh, isPlayer,
     *         card.getHp(), card.getImagePath(),
     *         card.getLifetime());
     *         building.configureCombatFromCard(card);
     *         arena.occupyFootprint(building);
     *         activeBuildings.add(building);
     *         }
     *         } else if (card.getType() == CardType.TROOP) {
     *         int count = Math.max(1, card.getCount());
     *         for (int i = 0; i < count; i++) {
     *         GridPosition spawn = GridPosition.tryCreate(x, y);
     *         if (spawn != null) {
     *         Troop troop = new Troop(card, spawn, isPlayer);
     *         activeTroops.add(troop);
     *         }
     *         }
     *         } else if (card.getType() == CardType.SPELL) {
     *         applySpellEffect(isPlayer, card, x, y);
     *         }
     * 
     *         // 2. Add to Placed History
     *         placedCards.add(new PlacedCard(card, x, y, isPlayer));
     * 
     *         // 3. Quests & Achievements (Player Only)
     *         if (isPlayer) {
     *         // Track specialized Quests
     *         if (card.getType() == CardType.SPELL) {
     *         com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
     *         .updateProgress(com.kuroyale.model.QuestType.PLAY_SPELL_CARDS, 1);
     *         } else if (card.getType() == CardType.TROOP) {
     *         com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
     *         .updateProgress(com.kuroyale.model.QuestType.DEPLOY_TROOP_CARDS, 1);
     *         // Track Swarm Troops (Army Builder)
     *         if (card.getCount() > 1) {
     *         com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
     *         .updateProgress(com.kuroyale.model.AchievementType.ARMY_BUILDER,
     *         card.getCount());
     *         }
     *         } else if (card.getType() == CardType.BUILDING) {
     *         com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
     *         .updateProgress(com.kuroyale.model.QuestType.PLAY_BUILDING_CARDS, 1);
     *         }
     *         }
     * 
     *         return true;
     *         }
     */
    /**
     * Unified logic for spawning units (Troops, Buildings, Spells).
     */

    private boolean spawnUnit(boolean isPlayer, Card card, int x, int y) {
        if (card == null)
            return false;

        // --- FIX 2: Askerlerin dağılması için ofset haritası (Spiral/Grid mantığı) ---
        // {dx, dy} -> Merkez, Sağ, Sol, Aşağı, Yukarı, Sağ-Alt, Sol-Üst...
        final int[][] OFFSETS = {
                { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
                { 1, 1 }, { -1, -1 }, { 1, -1 }, { -1, 1 },
                { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 }, { 2, 2 }, { -2, -2 }
        };

        // 1. Specific Validation & Creation
        if (card.getType() == CardType.BUILDING) {
            // Validate Footprint
            int bw = Math.max(1, card.getFootprintWidthTiles());
            int bh = Math.max(1, card.getFootprintHeightTiles());

            // --- FIX 1: Bina Sınır Kontrolü (Düzeltildi) ---
            // Eski 'mx/my' kodları yerine basit sınır kontrolü:
            // X veya Y sıfırdan küçükse VEYA (X + Genişlik) Arena'yı taşıyorsa HATA.
            if (x < 0 || y < 0 || (x + bw) > Arena.WIDTH || (y + bh) > Arena.HEIGHT) {
                return false;
            }

            // Validate all cells in footprint
            for (int dx = 0; dx < bw; dx++) {
                for (int dy = 0; dy < bh; dy++) {
                    GridCell c = arena.getCell(x + dx, y + dy);
                    // Bina sadece boş ve yürünebilir (çim) alana konabilir
                    if (c == null || c.isOccupied() || !c.isWalkable()) {
                        return false;
                    }
                }
            }

            // Create Building
            GridPosition topLeft = GridPosition.tryCreate(x, y);
            if (topLeft != null) {
                Building building = new Building(topLeft, bw, bh, isPlayer, card.getHp(), card.getImagePath(),
                        card.getLifetime());
                building.configureCombatFromCard(card);
                arena.occupyFootprint(building);
                activeBuildings.add(building);
            }
        } else if (card.getType() == CardType.TROOP) {
            int count = Math.max(1, card.getCount());

            for (int i = 0; i < count; i++) {
                // --- FIX 2 UYGULAMASI ---
                // Eğer çok fazla asker varsa (offset dizisinden fazla), fazlalıklar merkezde
                // (0,0) doğsun.
                int[] offset = (i < OFFSETS.length) ? OFFSETS[i] : OFFSETS[0];

                int spawnX = x + offset[0];
                int spawnY = y + offset[1];

                // Hedef nokta harita içinde mi?
                boolean isValidPos = (spawnX >= 0 && spawnX < Arena.WIDTH && spawnY >= 0 && spawnY < Arena.HEIGHT);

                // Eğer harita içindeyse, orası yürünebilir mi (Nehir/Bina değil mi)?
                if (isValidPos) {
                    GridCell cell = arena.getCell(spawnX, spawnY);
                    if (cell == null || !cell.isWalkable()) {
                        isValidPos = false; // Nehir veya duvarsa oraya doğmasın
                    }
                }

                // Eğer offset noktası geçersizse (örn: nehre denk geldi), askeri ana merkeze
                // (x,y) koy.
                if (!isValidPos) {
                    spawnX = x;
                    spawnY = y;
                }

                GridPosition spawn = GridPosition.tryCreate(spawnX, spawnY);
                if (spawn != null) {
                    Troop troop = new Troop(card, spawn, isPlayer);
                    activeTroops.add(troop);
                }
            }
        } else if (card.getType() == CardType.SPELL) {
            applySpellEffect(isPlayer, card, x, y);
        }

        // 2. Add to Placed History
        placedCards.add(new PlacedCard(card, x, y, isPlayer));

        // 3. Quests & Achievements (Player Only)
        if (isPlayer) {
            // ... (Buradaki kodlar aynı kalacak, Quest logic) ...
            if (card.getType() == CardType.SPELL) {
                com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                        .updateProgress(com.kuroyale.model.QuestType.PLAY_SPELL_CARDS, 1);
            } else if (card.getType() == CardType.TROOP) {
                com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                        .updateProgress(com.kuroyale.model.QuestType.DEPLOY_TROOP_CARDS, 1);
                if (card.getCount() > 1) {
                    com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                            .updateProgress(com.kuroyale.model.AchievementType.ARMY_BUILDER, card.getCount());
                }
            } else if (card.getType() == CardType.BUILDING) {
                com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                        .updateProgress(com.kuroyale.model.QuestType.PLAY_BUILDING_CARDS, 1);
            }
        }

        return true;
    }

    public Hand getPlayerHand() {
        return playerHand;
    }

    // Apply spell effects: simple AoE damage around target (affects enemy troops,
    // buildings, and towers)
    private void applySpellEffect(boolean isPlayer, Card spell, int x, int y) {
        // Use card damage and range as radius in tiles
        double radius = Math.max(0, spell.getRange());
        double damage = Math.max(0, spell.getDamage());
        GridPosition center = GridPosition.tryCreate(x, y);
        if (center == null)
            return;

        combatService.applyAreaDamage(this, center, radius, damage, TargetType.BOTH, isPlayer);
        // Track effect for UI for 1 second
        activeSpellEffects.add(new SpellEffect(GridPosition.tryCreate(x, y), (int) radius, isPlayer, 1.0));
    }

    /**
     * Apply circular area damage originating from a troop attack.
     * Center is derived from the primary target to keep targeting logic unchanged.
     */
    public void applyAreaDamageFromTroop(Troop attacker, Troop primaryTarget) {
        if (attacker == null || primaryTarget == null)
            return;
        GridPosition center = primaryTarget.getPosition();
        applyAreaDamageFromTroopInternal(attacker, center);
    }

    public void applyAreaDamageFromTroop(Troop attacker, Building primaryTarget) {
        if (attacker == null || primaryTarget == null)
            return;
        GridPosition center = primaryTarget.getCenterPosition();
        applyAreaDamageFromTroopInternal(attacker, center);
    }

    public void applyAreaDamageFromTroop(Troop attacker, Tower primaryTarget) {
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

        combatService.applyAreaDamage(this, center, radius, damage, targetType, attacker.isPlayerSide());
        // Short-lived visual ring for this AoE, rendered via
        // BattleArenaView.renderSpellEffects
        activeSpellEffects.add(new SpellEffect(center, (int) radius, attacker.isPlayerSide(), 0.3));
    }

    /**
     * Apply circular area damage originating from a building attack or death
     * explosion.
     * Only damages enemy troops and respects the building's targeting rules.
     */

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

    // Trigger death explosion for area-effect buildings (e.g., Bomb Tower)

    /*
     * Checks for destroyed towers and updates scores accordingly.
     * Princess towers: +1 point to the attacker
     * King towers: Set attacker's score to 3 and end the game
     */
    /*
     * Checks for destroyed towers and updates scores accordingly.
     * Princess towers: +1 point to the attacker
     * King towers: Set attacker's score to 3 and end the game
     */
    private void checkAndScoreDestroyedTowers() {
        // Optimization: Iterate unique towers directly instead of scanning all grid
        // cells (O(1) vs O(N))
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

                    // Track King Tower destruction
                    com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                            .updateProgress(com.kuroyale.model.QuestType.DESTROY_KING_TOWER, 1);
                    com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                            .updateProgress(com.kuroyale.model.QuestType.DESTROY_CROWN_TOWERS, 1);
                    com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                            .updateProgress(com.kuroyale.model.AchievementType.TOWER_HUNTER, 1);
                }
            } else {
                // Princess tower destroyed: +1 point to attacker
                if (isPlayerTower) {
                    // Player's princess tower destroyed by bot
                    botScore++;
                } else {
                    // Bot's princess tower destroyed by player
                    playerScore++;

                    // Track Princess Tower destruction
                    com.kuroyale.util.ServiceFactory.getInstance().getQuestService()
                            .updateProgress(com.kuroyale.model.QuestType.DESTROY_CROWN_TOWERS, 1);
                    com.kuroyale.util.ServiceFactory.getInstance().getAchievementService()
                            .updateProgress(com.kuroyale.model.AchievementType.TOWER_HUNTER, 1);
                }
            }
        }
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