package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

//Represents a card in the game with all its properties
public class Card {
    private final String name;
    private final int cost;
    private final CardType type;
    private final Rarity rarity;
    private final int baseHp;
    private final int baseDamage;
    private final double hitSpeed;
    private final double range;
    private final SpeedType speed;
    private final TargetType target;
    // Indicates whether the unit itself is flying
    private final boolean airUnit;
    private final boolean areaEffect;
    private final String description;
    private final int count; // For swarm troops
    private final int lifetime; // For buildings
    public static final int MAX_LEVEL = 3;
    public static final int MIN_LEVEL = 1;
    private int level = 1;
    // Optional footprint for buildings (tiles). Defaults to 2x2 if unset.
    private int footprintWidthTiles = 2;
    private int footprintHeightTiles = 2;

    // Spawning properties
    private String spawnUnitName;
    private int spawnUnitCount;
    private String deathSpawnUnitName;
    private int deathSpawnUnitCount;

    public Card(String name, int cost, CardType type, Rarity rarity, int hp, int damage, double hitSpeed,
            double range, SpeedType speed, TargetType target, boolean airUnit, boolean areaEffect,
            String description, int count, int lifetime) {
        this.name = name;
        this.cost = cost;
        this.type = type;
        this.rarity = rarity;
        this.baseHp = hp;
        this.baseDamage = damage;
        this.hitSpeed = hitSpeed;
        this.range = range;
        this.speed = speed;
        this.target = target;
        this.airUnit = airUnit;
        this.areaEffect = areaEffect;
        this.description = description;
        this.count = count;
        this.lifetime = lifetime;
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public CardType getType() {
        return type;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public int getBaseDamage() {
        return baseDamage;
    }

    public int getHp() {
        if (baseHp <= 0) {
            // Spells and non-HP cards keep 0
            return 0;
        }
        double multiplier = 1.0 + (Math.max(1, level) - 1) * 0.10;
        return (int) Math.round(baseHp * multiplier);
    }

    public int getDamage() {
        double multiplier = 1.0 + (Math.max(1, level) - 1) * 0.10;
        return (int) Math.round(baseDamage * multiplier);
    }

    public double getHitSpeed() {
        return hitSpeed;
    }

    public double getRange() {
        return range;
    }

    public SpeedType getSpeed() {
        return speed;
    }

    public TargetType getTarget() {
        return target;
    }

    public boolean isAirUnit() {
        return airUnit;
    }

    public boolean isAreaEffect() {
        return areaEffect;
    }

    public String getDescription() {
        return description;
    }

    public int getCount() {
        return count;
    }

    public int getLifetime() {
        return lifetime;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
    }

    // Building footprint accessors (no-op for troops/spells)
    public int getFootprintWidthTiles() {
        return footprintWidthTiles;
    }

    public int getFootprintHeightTiles() {
        return footprintHeightTiles;
    }

    public void setFootprintTiles(int width, int height) {
        if (width > 0)
            this.footprintWidthTiles = width;
        if (height > 0)
            this.footprintHeightTiles = height;
    }

    public String getSpawnUnitName() {
        return spawnUnitName;
    }

    public void setSpawnUnitName(String spawnUnitName) {
        this.spawnUnitName = spawnUnitName;
    }

    public int getSpawnUnitCount() {
        return spawnUnitCount;
    }

    public void setSpawnUnitCount(int spawnUnitCount) {
        this.spawnUnitCount = spawnUnitCount;
    }

    public String getDeathSpawnUnitName() {
        return deathSpawnUnitName;
    }

    public void setDeathSpawnUnitName(String deathSpawnUnitName) {
        this.deathSpawnUnitName = deathSpawnUnitName;
    }

    public int getDeathSpawnUnitCount() {
        return deathSpawnUnitCount;
    }

    public void setDeathSpawnUnitCount(int deathSpawnUnitCount) {
        this.deathSpawnUnitCount = deathSpawnUnitCount;
    }

    public String getImagePath() {
        return "/images/cards/" + name.toLowerCase(java.util.Locale.ENGLISH).replace(" ", "_").replace(".", "")
                + ".png";
    }

    // Calculates damage per second (DPS)
    public double getDPS() {
        if (hitSpeed == 0)
            return 0;
        return getDamage() / hitSpeed;
    }

    @Override
    public String toString() {
        return name + " (Cost: " + cost + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Card card = (Card) obj;
        return name.equals(card.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    // Logic moved from Views
    public int calculateUpgradeCost() {
        if (level >= MAX_LEVEL) {
            return 0;
        }

        switch (rarity) {
            case COMMON:
                return level == 1 ? 200 : 500;
            case RARE:
                return level == 1 ? 400 : 1000;
            case EPIC:
                return level == 1 ? 800 : 2000;
            case LEGENDARY:
                return level == 1 ? 1500 : 4000;
            default:
                return 0;
        }
    }

    public static int calculateStatForLevel(int baseStat, int level) {
        if (baseStat <= 0) {
            return 0;
        }
        double multiplier = 1.0 + (Math.max(1, level) - 1) * 0.10;
        return (int) Math.round(baseStat * multiplier);
    }
}
