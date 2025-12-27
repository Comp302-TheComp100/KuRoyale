package com.kuroyale.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable 2D vector for continuous world-space coordinates.
 * Used for smooth troop movement and precise positioning.
 */
public class Vector2 implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final Vector2 ZERO = new Vector2(0, 0);

    private final double x;
    private final double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a Vector2 from a GridPosition (centered on the tile).
     */
    public static Vector2 fromGridPosition(GridPosition pos) {
        if (pos == null)
            return null;
        // Center of the tile
        return new Vector2(pos.getX() + 0.5, pos.getY() + 0.5);
    }

    /**
     * Converts this Vector2 to the nearest GridPosition.
     */
    public GridPosition toGridPosition() {
        int gx = (int) Math.floor(x);
        int gy = (int) Math.floor(y);
        return GridPosition.tryCreate(gx, gy);
    }

    // --- Vector Math Operations ---

    public Vector2 add(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    public Vector2 subtract(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }

    public Vector2 multiply(double scalar) {
        return new Vector2(this.x * scalar, this.y * scalar);
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    /**
     * Returns a normalized (unit length) version of this vector.
     * If length is zero, returns ZERO.
     */
    public Vector2 normalize() {
        double len = length();
        if (len < 1e-9)
            return ZERO;
        return new Vector2(x / len, y / len);
    }

    /**
     * Calculates distance to another Vector2.
     */
    public double distanceTo(Vector2 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculates squared distance (faster, no sqrt).
     */
    public double distanceSquaredTo(Vector2 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx * dx + dy * dy;
    }

    /**
     * Dot product with another vector.
     */
    public double dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }

    /**
     * Linearly interpolate between this and target by t (0-1).
     */
    public Vector2 lerp(Vector2 target, double t) {
        return new Vector2(
                this.x + (target.x - this.x) * t,
                this.y + (target.y - this.y) * t);
    }

    // --- Getters ---

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    // --- Object Overrides ---

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Vector2 other = (Vector2) obj;
        return Double.compare(other.x, x) == 0 && Double.compare(other.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}
