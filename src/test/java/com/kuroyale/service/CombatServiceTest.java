package com.kuroyale.service;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.IBattleState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CombatServiceTest {

    private CombatService combatService;

    @Mock
    private IBattleState battleState;
    @Mock
    private ICombatant target;

    private Troop attacker;
    private Arena arena;

    @BeforeEach
    void setUp() {
        combatService = new CombatService();

        // Setup real Arena
        ArenaLayout layout = new ArenaLayout("Test");
        arena = new Arena(layout);

        // Setup real Troop
        Card card = createDummyCard();
        attacker = new Troop(card, GridPosition.tryCreate(5, 5), true);

        // Bind arena to battleState
        lenient().when(battleState.getArena()).thenReturn(arena);
    }

    private Card createDummyCard() {
        return new Card(
                "TestTroop", 1, CardType.TROOP, Rarity.COMMON, 100, 10, 1.0,
                5.0, SpeedType.MEDIUM, TargetType.GROUND, false, false,
                "Desc", 1, 0);
    }

    @Test
    void testProcessCombatant_Stunned() {
        // Setup state
        attacker.stun(1.0); // Stun for 1 second
        assertTrue(attacker.isStunned());

        // Execute
        combatService.processCombatant(attacker, battleState, 0.1);

        // Verify
        assertEquals(UnitState.STUNNED, attacker.getUnitState());
        assertTrue(attacker.isStunned());
    }

    @Test
    void testProcessCombatant_Attack() {
        // Setup
        attacker.setAttackCooldown(0.0);
        attacker.setTarget(target);
        attacker.setUnitState(UnitState.ATTACKING); // Assume already attacking

        // Target setup
        when(target.isAlive()).thenReturn(true);
        when(target.getPosition()).thenReturn(GridPosition.tryCreate(5, 6)); // Dist 1.0

        // Execute
        combatService.processCombatant(attacker, battleState, 0.1);

        // Verify
        verify(target).takeDamage(anyInt());

        // Cooldown should be reset to hitSpeed (1.0)
        assertEquals(1.0, attacker.getAttackCooldown(), 0.01);
    }

    @Test
    void testProcessCombatant_FindTarget() {
        // Setup
        attacker.setAttackCooldown(1.0);
        attacker.setTarget(null);
        attacker.setUnitState(UnitState.MOVING);

        // Add a target to the REAL spatial grid
        GridPosition targetPos = GridPosition.tryCreate(5, 6);
        when(target.getPosition()).thenReturn(targetPos);
        when(target.getCenterPosition()).thenReturn(targetPos);
        when(target.isAlive()).thenReturn(true);
        when(target.isPlayerSide()).thenReturn(false); // Enemy

        // We need to inject the mock target into the real spatial grid.
        // SpatialGrid.add(ICombatant c)
        arena.getSpatialGrid().add(target);

        // Execute
        combatService.processCombatant(attacker, battleState, 0.1);

        // Verify
        assertEquals(target, attacker.getTarget());
        assertEquals(UnitState.ATTACKING, attacker.getUnitState());
    }

    @Test
    void testProcessCombatant_CooldownNotReady() {
        // Setup
        attacker.setAttackCooldown(0.5); // Not ready
        attacker.setTarget(target);
        attacker.setUnitState(UnitState.ATTACKING);

        // Target valid
        when(target.isAlive()).thenReturn(true);
        when(target.getPosition()).thenReturn(GridPosition.tryCreate(5, 6));

        // Execute
        combatService.processCombatant(attacker, battleState, 0.1);

        // Verify
        verify(target, never()).takeDamage(anyInt()); // Should NOT attack
        assertEquals(0.4, attacker.getAttackCooldown(), 0.01); // Cooldown decremented
    }

    @Test
    void testProcessCombatant_TargetInvalid_ResetsState() {
        // Setup
        attacker.setAttackCooldown(0.0);
        attacker.setTarget(target);
        attacker.setUnitState(UnitState.ATTACKING);

        // Target DEAD
        when(target.isAlive()).thenReturn(false);

        // Execute
        combatService.processCombatant(attacker, battleState, 0.1);

        // Verify
        // Should attempt to find new target. If none found (spatial grid empty), reset
        // state.
        assertEquals(UnitState.MOVING, attacker.getUnitState());
        assertNull(attacker.getTarget());
    }

    @Test
    void testProcessCombatant_FirstAttackDelay() {
        // Setup: No target initially, cooldown ready
        attacker.setTarget(null);
        attacker.setAttackCooldown(0.0);
        attacker.setUnitState(UnitState.MOVING);

        // Add enemy to grid so it finds one
        GridPosition targetPos = GridPosition.tryCreate(5, 6);
        when(target.getPosition()).thenReturn(targetPos);
        when(target.getCenterPosition()).thenReturn(targetPos);
        when(target.isAlive()).thenReturn(true);
        when(target.isPlayerSide()).thenReturn(false);
        arena.getSpatialGrid().add(target);

        // Execute
        combatService.processCombatant(attacker, battleState, 0.1);

        // Verify
        assertEquals(target, attacker.getTarget());
        // CRITICAL: Should NOT attack immediately. content of first attack delay logic.
        // It sets cooldown to hitSpeed.
        assertEquals(attacker.getHitSpeed(), attacker.getAttackCooldown(), 0.01);
        verify(target, never()).takeDamage(anyInt());
    }
}
