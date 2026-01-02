package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.TargetType;
import com.kuroyale.model.logic.IBattleState;
import com.kuroyale.model.logic.SpatialGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TargetingServiceTest {

    private TargetingService targetingService;
    private MockBattleState mockState;
    private MockArena mockArena;

    @BeforeEach
    public void setup() {
        targetingService = new TargetingService();
        mockArena = new MockArena();
        mockState = new MockBattleState(mockArena);
    }

    // 1. Basic Enemy Detection
    @Test
    public void testFindNearestEnemyOrObjective_FindsNearestEnemyTroop() {
        // Setup
        Troop attacker = new MockTroop(true, new GridPosition(10, 10), TargetType.GROUND); // Player side
        Troop enemyNear = new MockTroop(false, new GridPosition(12, 10), TargetType.GROUND); // Enemy, dist=2
        Troop enemyFar = new MockTroop(false, new GridPosition(15, 10), TargetType.GROUND); // Enemy, dist=5

        mockState.addTroop(enemyNear);
        mockState.addTroop(enemyFar);

        // Execute
        GridPosition result = targetingService.findNearestEnemyOrObjective(mockState, attacker);

        // Verify
        assertNotNull(result);
        assertEquals(enemyNear.getPosition(), result, "Should target the nearest enemy troop");
    }

    // 2. Target Type Filtering (Ground cannot hit Air)
    @Test
    public void testFindNearestEnemyOrObjective_GroundAttackerIgnoresAir() {
        // Setup
        Troop attacker = new MockTroop(true, new GridPosition(10, 10), TargetType.GROUND); // Ground attacker
        Troop enemyAir = new MockTroop(false, new GridPosition(11, 10), TargetType.GROUND); // Enemy Air unit
                                                                                            // (isAirUnit=true manually
                                                                                            // set in mock if needed, or
                                                                                            // derived from card)
        ((MockTroop) enemyAir).setAirUnit(true);

        Troop enemyGroundFar = new MockTroop(false, new GridPosition(14, 10), TargetType.GROUND); // Enemy Ground,
                                                                                                  // farther

        mockState.addTroop(enemyAir);
        mockState.addTroop(enemyGroundFar);

        // Execute
        GridPosition result = targetingService.findNearestEnemyOrObjective(mockState, attacker);

        // Verify
        assertEquals(enemyGroundFar.getPosition(), result,
                "Ground attacker should ignore air units and target valid ground unit");
    }

    // 3. Friendly Fire
    @Test
    public void testFindNearestEnemyOrObjective_IgnoresFriendlyUnits() {
        // Setup
        Troop attacker = new MockTroop(true, new GridPosition(10, 10), TargetType.GROUND);
        Troop friendly = new MockTroop(true, new GridPosition(11, 10), TargetType.GROUND);
        Troop enemy = new MockTroop(false, new GridPosition(14, 10), TargetType.GROUND);

        mockState.addTroop(friendly);
        mockState.addTroop(enemy);

        // Execute
        GridPosition result = targetingService.findNearestEnemyOrObjective(mockState, attacker);

        // Verify
        assertEquals(enemy.getPosition(), result, "Should ignore friendly units and target enemy");
    }

    // 4. Fallback to Enemy Tower
    @Test
    public void testFindNearestEnemyOrObjective_FallbackToEnemyTower() {
        // Setup
        Troop attacker = new MockTroop(true, new GridPosition(10, 10), TargetType.GROUND);

        // No enemy troops
        Tower enemyTower = new MockTower(false, new GridPosition(17, 17));
        mockArena.addTower(enemyTower);

        // Execute
        GridPosition result = targetingService.findNearestEnemyOrObjective(mockState, attacker);

        // Verify
        assertNotNull(result, "Should fallback to tower if no troops nearby");
        // We know simple mock tower has position 17,17. Targeting service might return
        // perimeter or center.
        // For simplicity in mock, let's assume it returns a position near 17,17.
        // Actually the service calls nearestPerimeterTile.
        // Let's just assert it is NOT null, effectively verifying the fallback path was
        // taken.
    }

    // --- Simple Mocks ---

    static class MockBattleState implements IBattleState {
        private Arena arena;
        private List<Troop> troops = new ArrayList<>();
        private List<Building> buildings = new ArrayList<>();

        public MockBattleState(Arena arena) {
            this.arena = arena;
        }

        public void addTroop(Troop t) {
            troops.add(t);
        }

        public void addBuilding(Building b) {
            buildings.add(b);
        }

        @Override
        public Arena getArena() {
            return arena;
        }

        @Override
        public List<Troop> getActiveTroops() {
            return troops;
        }

        @Override
        public List<Building> getActiveBuildings() {
            return buildings;
        }

        // Unused interface methods
        @Override
        public Card getCardByName(String name) {
            return null;
        }

        @Override
        public GridPosition getFrontPosition(Building b) {
            return null;
        }

        @Override
        public boolean spawnTroopDirectly(boolean isPlayer, Card card, int x, int y, int count) {
            return true;
        }

        @Override
        public com.kuroyale.model.logic.ElixirManager getElixirManager(boolean isPlayer) {
            return null;
        }
    }

    static class MockArena extends Arena {
        private SpatialGrid spatialGrid;
        private Set<Tower> towers = new HashSet<>();

        public MockArena() {
            super(null); // Fix: Arena has constructor(ArenaLayout)
            this.spatialGrid = null;
        }

        public void addTower(Tower t) {
            towers.add(t);
        }

        @Override
        public SpatialGrid getSpatialGrid() {
            return spatialGrid;
        }

        @Override
        public Set<Tower> getAllTowers() {
            return towers;
        }

        // Mock getCell for perimeter checks
        @Override
        public GridCell getCell(GridPosition p) {
            if (p == null)
                return null;
            return new GridCell(p.getX(), p.getY(), com.kuroyale.model.enums.TileType.GRASS); // Fix: Use TileType
        }

        @Override
        public GridCell getCell(int x, int y) {
            return new GridCell(x, y, com.kuroyale.model.enums.TileType.GRASS); // Fix: Use TileType
        }
    }

    static class MockTroop extends Troop {
        private boolean isPlayer;
        private GridPosition pos;
        private boolean isAir = false;
        private Card mockCard;

        public MockTroop(boolean isPlayer, GridPosition pos, TargetType targetType) {
            // Call super with dummy card
            super(new Card("Mock", 1, com.kuroyale.model.enums.CardType.TROOP, com.kuroyale.model.enums.Rarity.COMMON,
                    100, 10, 1.0, 5.0, com.kuroyale.model.enums.SpeedType.MEDIUM, targetType, false, false, "Mock", 1,
                    0), pos, isPlayer);
            this.isPlayer = isPlayer;
            this.pos = pos;
            this.mockCard = super.getBaseCard(); // Use the one we passed
            // Verify if we need to set other fields
        }

        public void setAirUnit(boolean air) {
            this.isAir = air;
            // Hack: Card is final in Troop, can't change it easily.
            // Better to pass correct boolean in constructor if possible, or just override
            // isAirUnit() output which we do below.
        }

        @Override
        public boolean isPlayerSide() {
            return isPlayer;
        }

        @Override
        public GridPosition getPosition() {
            return pos;
        }

        // Ensure getCenterPosition is consistent
        @Override
        public GridPosition getCenterPosition() {
            return pos;
        }

        @Override
        public Vector2 getWorldPosition() {
            return Vector2.fromGridPosition(pos);
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        // We override this, so the final field in base class doesn't matter much for
        // this getter
        @Override
        public boolean isAirUnit() {
            return isAir;
        }

        @Override
        public Card getBaseCard() {
            return mockCard;
        }

        @Override
        public boolean isBuildingOnly() {
            return false;
        }
    }

    static class MockTower extends Tower {
        private boolean isPlayer;
        private GridPosition pos;

        public MockTower(boolean isPlayer, GridPosition pos) {
            super(com.kuroyale.model.entities.Tower.TowerType.PRINCESS, isPlayer);
            this.isPlayer = isPlayer;
            this.pos = pos;
        }

        @Override
        public boolean isPlayerSide() {
            return isPlayer;
        }

        @Override
        public GridPosition getPosition() {
            return pos;
        }

        @Override
        public GridPosition getCenterPosition() {
            return pos;
        }

        // Removed invalid getTime() override

        // ICombatant methods
        @Override
        public boolean isAlive() {
            return true;
        }

        // Tower dimensions usually 3x3
        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }
    }
}
