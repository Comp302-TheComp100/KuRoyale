package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

/*Represents a single cell in the arena grid with full state information.
 * Information Expert: Knows its own state (position, tile type, occupancy).
 * High Cohesion: Focused on managing cell-level state.*/
public class GridCell {
    private final GridPosition position;
    private TileType tileType;
    private boolean occupied;
    private Object occupant; // Reference to the occupying unit

    // Creates a new grid cell.
    public GridCell(GridPosition position, TileType tileType) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (tileType == null) {
            throw new IllegalArgumentException("TileType cannot be null");
        }
        this.position = position;
        this.tileType = tileType;
        this.occupied = false;
        this.occupant = null;
    }

    // Convenience constructor using x, y coordinates.
    public GridCell(int x, int y, TileType tileType) {
        this(new GridPosition(x, y), tileType);
    }

    /*
     * Checks if a unit can be placed on this cell.
     * A cell is valid for placement if:
     * - It's not currently occupied
     * - It's grass
     */
    public boolean canPlaceUnit() {
        if (occupied) {
            return false;
        }
        // Can place on grass, but not on bridges or water
        return tileType == TileType.GRASS;
    }

    // Places a unit on this cell.
    public void setOccupant(Object unit) {
        if (!canPlaceUnit()) {
            throw new IllegalStateException(
                    String.format("Cannot place unit at %s: occupied=%b, tileType=%s",
                            position, occupied, tileType));
        }
        this.occupant = unit;
        this.occupied = true;
    }

    // Forces occupant on this cell without validating canPlaceUnit (for building
    // footprints)
    public void forceSetOccupant(Object occupant) {
        this.occupant = occupant;
        this.occupied = true;
    }

    // Removes the unit from this cell.
    public void clearOccupant() {
        this.occupant = null;
        this.occupied = false;
    }

    // Checks if this cell is currently occupied by a unit.
    public boolean isOccupied() {
        return occupied;
    }

    // Gets the unit occupying this cell.
    public Object getOccupant() {
        return occupant;
    }

    // Gets the position of this cell.
    public GridPosition getPosition() {
        return position;
    }

    // Gets the x-coordinate (convenience method).
    public int getX() {
        return position.getX();
    }

    // Gets the y-coordinate (convenience method).
    public int getY() {
        return position.getY();
    }

    // Gets the terrain type of this cell.
    public TileType getTileType() {
        return tileType;
    }

    /*
     * Sets the terrain type of this cell.
     * Note: This will not affect occupancy (occupied cells remain occupied).
     */
    public void setTileType(TileType tileType) {
        if (tileType == null) {
            throw new IllegalArgumentException("TileType cannot be null");
        }
        this.tileType = tileType;
    }

    /*
     * Checks if this cell is walkable (for pathfinding).
     * A cell is walkable if it's grass, bridge, or road.
     * Water and Buildings are not walkable.
     */
    public boolean isWalkable() {
        return tileType == TileType.GRASS || tileType == TileType.BRIDGE || tileType == TileType.ROAD;
    }

    @Override
    public String toString() {
        return String.format("GridCell[pos=%s, type=%s, occupied=%b]", position, tileType, occupied);
    }
}
