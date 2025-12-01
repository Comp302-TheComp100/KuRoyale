package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

/*Represents the persistent configuration of an arena.
 * Information Expert: Knows the layout configuration (bridges, towers, etc.).
 */
public class ArenaLayout {
    private String name;
    private List<GridPosition> bridgePositions;
    private List<GridPosition> princessTowerPositions; // User's Princess towers (max 2)
    private GridPosition kingTowerPosition; // User's King tower

    public ArenaLayout(String name) {
        this.name = name;
        this.bridgePositions = new ArrayList<>();
        this.princessTowerPositions = new ArrayList<>();
        this.kingTowerPosition = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GridPosition> getBridgePositions() {
        return bridgePositions;
    }

    public void setBridgePositions(List<GridPosition> bridgePositions) {this.bridgePositions = bridgePositions;}

    public void addBridgePosition(int x, int y) {
        this.bridgePositions.add(new GridPosition(x, y));
    }

    public List<GridPosition> getPrincessTowerPositions() {
        return princessTowerPositions;
    }

    public void setPrincessTowerPositions(List<GridPosition> princessTowerPositions) {this.princessTowerPositions = princessTowerPositions;}

    public void addPrincessTowerPosition(int x, int y) {
        this.princessTowerPositions.add(new GridPosition(x, y));
    }

    public GridPosition getKingTowerPosition() {
        return kingTowerPosition;
    }

    public void setKingTowerPosition(GridPosition kingTowerPosition) {
        this.kingTowerPosition = kingTowerPosition;
    }

    public void setKingTowerPosition(int x, int y) {
        this.kingTowerPosition = new GridPosition(x, y);
    }
}
