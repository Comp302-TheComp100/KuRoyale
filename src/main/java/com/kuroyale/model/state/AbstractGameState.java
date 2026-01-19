package com.kuroyale.model.state;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.GridCell;
import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.arena.Vector2;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.service.battle.core.CombatService;
import com.kuroyale.service.battle.core.TroopMovementService;
import com.kuroyale.event.GameEventBus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract base class for game states (Single Player, PvP).
 * Manages common state like entities, arena, and core update loops.
 */
public abstract class AbstractGameState implements IBattleState {

    protected final Arena arena;
    protected final List<Troop> activeTroops;
    protected final List<Building> activeBuildings;
    protected final List<Projectile> activeProjectiles;
    protected final List<PlacedCard> placedCards;

    // Services
    protected final TroopMovementService troopMovementService = new TroopMovementService();
    protected final CombatService combatService = new CombatService();

    // Game Loop State
    protected double gameTime = 180.0; // 3 minutes standard
    protected boolean isDoubleElixir = false;
    protected boolean isGameOver = false;
    protected boolean isTiebreakerMode = false;
    protected static final double TIEBREAKER_DRAIN_RATE = 100.0;

    // Card Catalog for spawning lookups
    protected java.util.function.Function<String, Card> cardCatalog;

    public AbstractGameState(Arena arena) {
        this.arena = arena;
        this.activeTroops = new ArrayList<>();
        this.activeBuildings = new ArrayList<>();
        this.activeProjectiles = new ArrayList<>();
        this.placedCards = new ArrayList<>();
    }

    @Override
    public Arena getArena() {
        return arena;
    }

    @Override
    public List<Troop> getActiveTroops() {
        return activeTroops;
    }

    @Override
    public List<Building> getActiveBuildings() {
        return activeBuildings;
    }

    @Override
    public List<Projectile> getProjectiles() {
        return activeProjectiles;
    }

    @Override
    public void addProjectile(Projectile p) {
        activeProjectiles.add(p);
    }

    @Override
    public Card getCardByName(String name) {
        if (cardCatalog != null) {
            return cardCatalog.apply(name);
        }
        return null;
    }

    public void setCardCatalog(java.util.function.Function<String, Card> cardCatalog) {
        this.cardCatalog = cardCatalog;
    }

    public List<PlacedCard> getPlacedCards() {
        return placedCards;
    }

    // -- Common Game Loop Methods --

    protected void updateEntities(double deltaTime) {
        // Update placed cards/troops
        troopMovementService.updateTroops(deltaTime, this, activeTroops);

        // Update buildings (lifetime depreciation)
        for (Building b : activeBuildings) {
            if (b.isAlive()) {
                b.update(deltaTime);
            }
        }
    }

    protected void handleCombat(double deltaTime) {
        combatService.update(deltaTime, this);

        // Remove dead troops post combat
        activeTroops.removeIf(t -> !t.isAlive());
    }

    protected void cleanupEntities() {
        // Cleanup destroyed buildings from the list
        Iterator<Building> it = activeBuildings.iterator();
        while (it.hasNext()) {
            Building b = it.next();
            if (!b.isAlive()) {
                arena.freeFootprint(b); // Clear occupied cells so new cards can be placed
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

    /**
     * Tiebreaker mode: All remaining towers lose health rapidly.
     */
    protected void updateTiebreakerMode(double deltaTime) {
        double drainAmount = TIEBREAKER_DRAIN_RATE * deltaTime;

        // Drain all living towers
        for (com.kuroyale.model.entities.Tower tower : arena.getAllTowers()) {
            if (tower.isAlive()) {
                double newHealth = tower.getCurrentHealth() - drainAmount;
                tower.setCurrentHealth((int) Math.max(0, newHealth));
            }
        }

        // Concrete classes must implement score checking logic since
        // score variables are specific to them (playerScore vs player1Score etc)
        // or we could abstract score checking too, but let's keep it simple first.
        checkTiebreakerWinCondition();
    }

    /**
     * Clears all troops and buildings.
     * Called when entering tiebreaker mode to ensure only tower health matters.
     */
    protected void clearArenaUnits() {
        // Remove all troops from spatial grid
        for (Troop troop : activeTroops) {
            arena.getSpatialGrid().remove(troop);
        }
        activeTroops.clear();

        // Remove all buildings from spatial grid
        for (Building building : activeBuildings) {
            arena.getSpatialGrid().remove(building);
        }
        activeBuildings.clear();

        // Clear projectiles too
        activeProjectiles.clear();
    }

    public void removeTroop(Troop troop) {
        if (troop != null) {
            arena.getSpatialGrid().remove(troop);
            activeTroops.remove(troop);
        }
    }

    public void removeBuilding(Building building) {
        if (building != null) {
            arena.getSpatialGrid().remove(building);
            activeBuildings.remove(building);
        }
    }

    // -- Getters for State --

    public double getGameTime() {
        return gameTime;
    }

    public boolean isDoubleElixir() {
        return isDoubleElixir;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public boolean isTiebreakerMode() {
        return isTiebreakerMode;
    }

    // -- Abstract Methods dependent of logic --

    protected abstract void checkAndScoreDestroyedTowers();

    protected abstract void checkTiebreakerWinCondition();

    @Override
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

        // Check if spawn position lands on river AND is not walkable (not a bridge)
        // If it's on a bridge, spawn normally so troops can walk across
        if (spawnY == com.kuroyale.util.config.GameConstants.RIVER_ROW_1 ||
                spawnY == com.kuroyale.util.config.GameConstants.RIVER_ROW_2) {
            com.kuroyale.model.arena.GridCell frontCell = arena.getCell(spawnX, spawnY);
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
                    spawnY = com.kuroyale.util.config.GameConstants.RIVER_ROW_2 + 1; // y = 17
                } else {
                    // Bot side: move above the river (y < 15)
                    spawnY = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 1; // y = 14
                }
            }
        }

        return GridPosition.tryCreate(spawnX, spawnY);
    }

    // -- Spawn Logic --

    protected java.util.List<ICombatant> spawnUnit(boolean isPlayer, Card card, int x, int y) {
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
            GameEventBus.getInstance().publishCardPlayed(isPlayer, card, spawnedUnits);
        }

        return spawnedUnits;
    }

    @Override
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

    protected List<com.kuroyale.model.entities.ICombatant> spawnTroopGroup(boolean isPlayerSide, Card card, int x,
            int y) {
        // Reuse the logic from original GameState/PvPGameState
        // This was identical in both
        final int[][] OFFSETS = {
                { 0, 0 }, { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 },
                { 1, 1 }, { -1, -1 }, { 1, -1 }, { -1, 1 },
                { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 }, { 2, 2 }, { -2, -2 }
        };

        int count = Math.max(1, card.getCount());
        List<com.kuroyale.model.entities.ICombatant> spawned = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int[] offset = (i < OFFSETS.length) ? OFFSETS[i] : OFFSETS[0];
            int spawnX = x + offset[0];
            int spawnY = y + offset[1];

            boolean isValidPos = (spawnX >= 0 && spawnX < Arena.WIDTH && spawnY >= 0 && spawnY < Arena.HEIGHT);
            if (isValidPos) {
                com.kuroyale.model.arena.GridCell cell = arena.getCell(spawnX, spawnY);
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
                Troop troop = new Troop(card, spawn, isPlayerSide);
                activeTroops.add(troop);
                arena.getSpatialGrid().add(troop);
                spawned.add(troop);
            }
        }
        return spawned.isEmpty() ? null : spawned;
    }

    protected java.util.List<ICombatant> spawnBuilding(boolean isPlayer, Card card, int x, int y) {
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

    // Apply spell effects: simple AoE damage around target
    protected void applySpellEffect(boolean isPlayer, Card spell, int x, int y) {

        // Check for Projectile-based spells (Fireball, Rocket)
        if ("Fireball".equalsIgnoreCase(spell.getName()) || "Rocket".equalsIgnoreCase(spell.getName())) {
            GridPosition targetGrid = GridPosition.tryCreate(x, y);
            if (targetGrid == null)
                return;

            // Determine Start Position (King Tower)
            Vector2 startPos = new Vector2(Arena.WIDTH / 2.0, isPlayer ? Arena.HEIGHT : 0); // Default bottom/top center

            // Try to find actual King Tower
            Tower kingTower = arena.getKingTower(isPlayer);
            if (kingTower != null && kingTower.getCenterPosition() != null) {
                startPos = new Vector2(kingTower.getCenterPosition().getX() + 0.5,
                        kingTower.getCenterPosition().getY() + 0.5);
            }

            Vector2 targetPos = new Vector2(targetGrid.getX() + 0.5, targetGrid.getY() + 0.5);

            // Create Projectile
            Projectile spellProjectile = new Projectile(kingTower, startPos, targetPos, spell);
            addProjectile(spellProjectile);
            return;
        }

        double radius = Math.max(0, spell.getRange());
        double damage = Math.max(0, spell.getDamage());
        GridPosition center = GridPosition.tryCreate(x, y);
        if (center == null)
            return;

        com.kuroyale.event.GameEventBus.getInstance().publishSpellCast(isPlayer, spell, center);

        Vector2 centerVec = Vector2.fromGridPosition(center);

        combatService.applyAreaDamage(this, centerVec, radius, damage, TargetType.BOTH, isPlayer, true,
                spell.getStunDuration(), spell.getName());
    }

    public void setGameTime(double gameTime) {
        this.gameTime = Math.max(0, gameTime);
    }

    // Inner class for placed cards
    public static class PlacedCard {
        public final Card card;
        public final int x, y;
        public final boolean isPlayer; // Generic "player side" flag (true=bottom/player1, false=top/player2/bot)

        public PlacedCard(Card card, int x, int y, boolean isPlayer) {
            this.card = card;
            this.x = x;
            this.y = y;
            this.isPlayer = isPlayer;
        }
    }
}
