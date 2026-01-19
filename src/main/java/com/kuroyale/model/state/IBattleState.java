package com.kuroyale.model.state;

import java.util.List;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.arena.GridPosition;
import com.kuroyale.model.entities.Building;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.logic.ElixirManager;

/**
 * Common interface for accessing and subtly modifying battle-related
 * information.
 */
public interface IBattleState {
    Arena getArena();

    List<Troop> getActiveTroops();

    List<Building> getActiveBuildings();

    Card getCardByName(String name);

    GridPosition getFrontPosition(Building b);

    boolean spawnTroopDirectly(boolean isPlayerSide, Card card, int x, int y, int count);

    ElixirManager getElixirManager(boolean isPlayerSide);

    List<com.kuroyale.model.entities.Projectile> getProjectiles();

    void addProjectile(com.kuroyale.model.entities.Projectile p);

    // New common methods for GameState/PvPGameState unification
    void update(double deltaTime);

    boolean isGameOver();

    boolean isDoubleElixir();

    boolean isTiebreakerMode();

    double getGameTime();
}
