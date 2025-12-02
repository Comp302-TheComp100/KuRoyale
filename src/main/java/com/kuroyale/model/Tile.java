package com.kuroyale.model;

/** Represents a single cell on the arena grid.
 * Information Expert: Knows its own coordinates and type.
 * @deprecated This class is kept for backward compatibility only.
 *Use {@link GridCell} for new code as it provides enhanced functionality including occupancy tracking and position management.*/
@Deprecated
public class Tile {
    private final int x;
    private final int y;
    private TileType type;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }

    public TileType getType() {
        return type;
    }
    public void setType(TileType type) {
        this.type = type;
    }
}
