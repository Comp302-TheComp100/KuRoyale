package com.kuroyale.model.arena;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*Represents the persistent configuration of an arena.
 * Information Expert: Knows the layout configuration (bridges, towers, etc.). */
public class ArenaLayout implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<GridPosition> bridgePositions;
    private List<GridPosition> princessTowerPositions; // User's Princess towers (max 2)
    private GridPosition kingTowerPosition; // User's King tower
    private String gridColor; // Hex color for the grid background
    private String gridColorAlt; // Secondary Hex color

    public ArenaLayout(String name) {
        this.name = name;
        this.bridgePositions = new ArrayList<>();
        this.princessTowerPositions = new ArrayList<>();
        this.princessTowerPositions = new ArrayList<>();
        this.kingTowerPosition = null;
        this.gridColor = null; // Default null implies standard grass
        this.gridColorAlt = null; // Default null
    }

    public String getGridColor() {
        return gridColor;
    }

    public void setGridColor(String gridColor) {
        this.gridColor = gridColor;
    }

    public String getGridColorAlt() {
        return gridColorAlt;
    }

    public void setGridColorAlt(String gridColorAlt) {
        this.gridColorAlt = gridColorAlt;
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

    public void setBridgePositions(List<GridPosition> bridgePositions) {
        this.bridgePositions = bridgePositions;
    }

    public void addBridgePosition(int x, int y) {
        this.bridgePositions.add(new GridPosition(x, y));
    }

    public List<GridPosition> getPrincessTowerPositions() {
        return princessTowerPositions;
    }

    public void setPrincessTowerPositions(List<GridPosition> princessTowerPositions) {
        this.princessTowerPositions = princessTowerPositions;
    }

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

    public ArenaLayout copy() {
        ArenaLayout copy = new ArenaLayout(this.name);
        for (GridPosition p : this.bridgePositions) {
            copy.addBridgePosition(p.getX(), p.getY());
        }
        for (GridPosition p : this.princessTowerPositions) {
            copy.addPrincessTowerPosition(p.getX(), p.getY());
        }
        if (this.kingTowerPosition != null) {
            copy.setKingTowerPosition(this.kingTowerPosition.getX(), this.kingTowerPosition.getY());
        }
        copy.setGridColor(this.gridColor);
        copy.setGridColorAlt(this.gridColorAlt);
        return copy;
    }
}
