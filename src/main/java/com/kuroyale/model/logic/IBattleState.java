package com.kuroyale.model.logic;

import java.util.List;

import com.kuroyale.model.core.entities.Arena;
import com.kuroyale.model.core.entities.Building;
import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.GridPosition;
import com.kuroyale.model.core.entities.Troop;

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

    List<com.kuroyale.model.core.entities.Projectile> getProjectiles();

    void addProjectile(com.kuroyale.model.core.entities.Projectile p);
}
