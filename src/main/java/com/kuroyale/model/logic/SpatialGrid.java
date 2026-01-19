package com.kuroyale.model.logic;

import java.util.*;

import com.kuroyale.model.core.entities.GridPosition;
import com.kuroyale.model.core.entities.ICombatant;
import com.kuroyale.model.core.entities.Vector2;

/*A spatial partitioning grid (Bin-Lattice) to optimize spatial queries.
 * The arena is divided into larger buckets (cells). Entities are stored in these buckets based on their position.
 * This allows for O(1) mostly for adding/removing and O(k) for querying nearby entities rather than O(N). */
public class SpatialGrid {
    private static final int BUCKET_SIZE = 4; // Each bucket covers 4x4 tiles
    private final int width;
    private final int height;
    private final int cols;
    private final int rows;

    // Grid of buckets. Each bucket is a generic collection of ICombatant.
    // Using Set to prevent duplicates if an entity spans multiple buckets
    private final List<Set<ICombatant>> buckets;

    // Keep track of entity bucket indices to allow fast removal/updates
    // A map from Entity -> List of bucket indices it resides in
    private final Map<ICombatant, Integer> entityBucketMap;

    public SpatialGrid(int arenaWidth, int arenaHeight) {
        this.width = arenaWidth;
        this.height = arenaHeight;
        this.cols = (int) Math.ceil((double) width / BUCKET_SIZE);
        this.rows = (int) Math.ceil((double) height / BUCKET_SIZE);

        int totalBuckets = cols * rows;
        this.buckets = new ArrayList<>(totalBuckets);
        for (int i = 0; i < totalBuckets; i++) {
            buckets.add(new HashSet<>());
        }

        this.entityBucketMap = new HashMap<>();
    }

    // Adds an entity to the spatial grid based on its center world position
    public void add(ICombatant entity) {
        if (entity == null)
            return;

        Vector2 worldPos = entity.getCenterWorldPosition();
        if (worldPos == null)
            return;

        int bucketIndex = getBucketIndex(worldPos);
        if (bucketIndex != -1) {
            buckets.get(bucketIndex).add(entity);
            entityBucketMap.put(entity, bucketIndex);
        }
    }

    // Removes an entity from the grid.
    public void remove(ICombatant entity) {
        if (entity == null)
            return;

        Integer bucketIndex = entityBucketMap.remove(entity);
        if (bucketIndex != null) {
            buckets.get(bucketIndex).remove(entity);
        }
    }

    /*
     * Updates an entity's position in the grid.
     * Should be called whenever an entity moves significantly (changes buckets).
     */
    public void update(ICombatant entity) {
        if (entity == null)
            return;

        Vector2 worldPos = entity.getCenterWorldPosition();
        if (worldPos == null)
            return;

        int newBucketIndex = getBucketIndex(worldPos);
        Integer oldBucketIndex = entityBucketMap.get(entity);

        if (oldBucketIndex != null && oldBucketIndex == newBucketIndex) {
            return;
        }

        // Remove from old bucket
        if (oldBucketIndex != null) {
            buckets.get(oldBucketIndex).remove(entity);
        }

        // Add to new bucket
        if (newBucketIndex != -1) {
            buckets.get(newBucketIndex).add(entity);
            entityBucketMap.put(entity, newBucketIndex);
        } else {
            // Moved out of bounds? Just remove it.
            entityBucketMap.remove(entity);
        }
    }

    /**
     * Retrieves all entities in the buckets overlapping the query radius around the
     * center.
     * Uses Vector2 for sub-tile precision.
     */
    public List<ICombatant> getNearby(Vector2 center, double radius) {
        List<ICombatant> results = new ArrayList<>();
        if (center == null)
            return results;

        int centerCol = (int) (center.getX() / BUCKET_SIZE);
        int centerRow = (int) (center.getY() / BUCKET_SIZE);
        int radiusInBuckets = (int) Math.ceil(radius / BUCKET_SIZE);

        int minCol = Math.max(0, centerCol - radiusInBuckets);
        int maxCol = Math.min(cols - 1, centerCol + radiusInBuckets);
        int minRow = Math.max(0, centerRow - radiusInBuckets);
        int maxRow = Math.min(rows - 1, centerRow + radiusInBuckets);

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                int bucketIndex = r * cols + c;
                results.addAll(buckets.get(bucketIndex));
            }
        }

        return results;
    }

    /**
     * Retrieves all entities in the buckets overlapping the query radius around the
     * center.
     * 
     * @deprecated Use getNearby(Vector2, double) for sub-tile precision.
     */
    @Deprecated
    public List<ICombatant> getNearby(GridPosition center, double radius) {
        if (center == null)
            return new ArrayList<>();
        return getNearby(new Vector2(center.getX() + 0.5, center.getY() + 0.5), radius);
    }

    private int getBucketIndex(Vector2 pos) {
        if (pos == null)
            return -1;
        int col = (int) (pos.getX() / BUCKET_SIZE);
        int row = (int) (pos.getY() / BUCKET_SIZE);

        if (col < 0 || col >= cols || row < 0 || row >= rows)
            return -1;

        return row * cols + col;
    }

    @Deprecated
    private int getBucketIndex(GridPosition pos) {
        if (pos == null)
            return -1;
        return getBucketIndex(new Vector2(pos.getX() + 0.5, pos.getY() + 0.5));
    }

    public void clear() {
        for (Set<ICombatant> bucket : buckets) {
            bucket.clear();
        }
        entityBucketMap.clear();
    }

    /**
     * Retrieves all entities at a specific grid position.
     * This is faster than getNearby for single-tile checks.
     */
    public List<ICombatant> getAt(GridPosition pos) {
        List<ICombatant> results = new ArrayList<>();
        if (pos == null)
            return results;

        int bucketIndex = getBucketIndex(pos);
        if (bucketIndex != -1) {
            for (ICombatant entity : buckets.get(bucketIndex)) {
                if (entity.getPosition().equals(pos)) {
                    results.add(entity);
                }
            }
        }
        return results;
    }
}
