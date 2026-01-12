package com.kuroyale.model.logic;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.Building;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.GridPosition;

import java.util.List;

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
}
