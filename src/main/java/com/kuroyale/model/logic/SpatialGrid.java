package com.kuroyale.model.logic;

import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.entities.ICombatant;

import java.util.*;

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

    // Adds an entity to the spatial grid based on its center position
    public void add(ICombatant entity) {
        if (entity == null || entity.getPosition() == null)
            return;

        int bucketIndex = getBucketIndex(entity.getCenterPosition());
        if (bucketIndex != -1) {
            buckets.get(bucketIndex).add(entity);
            entityBucketMap.put(entity, bucketIndex);
        }
    }

    //Removes an entity from the grid.
    public void remove(ICombatant entity) {
        if (entity == null)
            return;

        Integer bucketIndex = entityBucketMap.remove(entity);
        if (bucketIndex != null) {
            buckets.get(bucketIndex).remove(entity);
        }
    }

    /* Updates an entity's position in the grid.
     * Should be called whenever an entity moves significantly (changes buckets).*/
    public void update(ICombatant entity) {
        if (entity == null || entity.getPosition() == null)
            return;

        int newBucketIndex = getBucketIndex(entity.getCenterPosition());
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

    // Retrieves all entities in the buckets overlapping the query radius around the center.
    public List<ICombatant> getNearby(GridPosition center, double radius) {
        List<ICombatant> results = new ArrayList<>();
        if (center == null)
            return results;

        // Determine range of buckets to check
        // Convert radius to bucket units
        int centerCol = center.getX() / BUCKET_SIZE;
        int centerRow = center.getY() / BUCKET_SIZE;

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

    private int getBucketIndex(GridPosition pos) {
        if (pos == null)
            return -1;
        int col = pos.getX() / BUCKET_SIZE;
        int row = pos.getY() / BUCKET_SIZE;

        if (col < 0 || col >= cols || row < 0 || row >= rows)
            return -1;

        return row * cols + col;
    }

    public void clear() {
        for (Set<ICombatant> bucket : buckets) {
            bucket.clear();
        }
        entityBucketMap.clear();
    }
}
