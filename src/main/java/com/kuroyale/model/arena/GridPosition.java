package com.kuroyale.model.arena;

import java.io.Serializable;
import java.util.Objects;

/*Information Expert: Knows how to validate and compare positions.
 * x ranges from 0 to 17 (18 columns)
 * y ranges from 0 to 31 (32 rows)
 * Origin (0, 0) is at top left corner */
public class GridPosition implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MIN_X = 0;
    public static final int MAX_X = Arena.WIDTH - 1;
    public static final int MIN_Y = 0;
    public static final int MAX_Y = Arena.HEIGHT - 1;

    private final int x;
    private final int y;

    // Creates a new grid position with validation.
    public GridPosition(int x, int y) {
        if (!isValidCoordinate(x, y)) {
            throw new IllegalArgumentException(
                    String.format("Invalid grid position: (%d, %d). Valid range: x[%d-%d], y[%d-%d]",
                            x, y, MIN_X, MAX_X, MIN_Y, MAX_Y));
        }
        this.x = x;
        this.y = y;
    }

    // Static factory method to create a position without throwing exceptions.
    public static GridPosition tryCreate(int x, int y) {
        if (isValidCoordinate(x, y)) {
            return new GridPosition(x, y);
        }
        return null;
    }

    // Checks if the given coordinates are within valid arena bounds.
    private static boolean isValidCoordinate(int x, int y) {
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y;
    }

    // Checks if this position is within valid arena bounds.
    public boolean isValid() {
        return isValidCoordinate(x, y);
    }

    // Calculates distance to another position.
    public int getDistanceTo(GridPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot calculate distance to null position");
        }
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    // Calculates Euclidean distance to another position.
    public double getEuclideanDistanceTo(GridPosition other) {
        if (other == null) {
            throw new IllegalArgumentException("Cannot calculate distance to null position");
        }
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Checks if this position is adjacent to another (including diagonals).
    public boolean isAdjacentTo(GridPosition other) {
        if (other == null) {
            return false;
        }
        return Math.abs(this.x - other.x) <= 1 && Math.abs(this.y - other.y) <= 1 && !this.equals(other);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        GridPosition that = (GridPosition) obj;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }
}
