package com.kuroyale.model;

//Represents a card in the game with all its properties
public class Card {
    private final String name;
    private final int cost;
    private final CardType type;
    private final int hp;
    private final int damage;
    private final double hitSpeed;
    private final double range;
    private final SpeedType speed;
    private final TargetType target;
    // Indicates whether the unit itself is airborne (flies over terrain)
    private final boolean airUnit;
    private final boolean areaEffect;
    private final String description;
    private final int count; // For swarm troops
    private final int lifetime; // For buildings

    public Card(String name, int cost, CardType type, int hp, int damage, double hitSpeed,
            double range, SpeedType speed, TargetType target, boolean airUnit, boolean areaEffect,
            String description, int count, int lifetime) {
        this.name = name;
        this.cost = cost;
        this.type = type;
        this.hp = hp;
        this.damage = damage;
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
    public String getName() {return name;}
    public int getCost() {return cost;}
    public CardType getType() {return type;}
    public int getHp() {return hp;}
    public int getDamage() {return damage;}
    public double getHitSpeed() {return hitSpeed;}
    public double getRange() {return range;}
    public SpeedType getSpeed() {return speed;}
    public TargetType getTarget() {return target;}
    public boolean isAirUnit() {return airUnit;}
    public boolean isAreaEffect() {return areaEffect;}
    public String getDescription() {return description;}
    public int getCount() {return count;}
    public int getLifetime() {
        return lifetime;
    }

    public String getImagePath() {
        return "/images/cards/" + name.toLowerCase(java.util.Locale.ENGLISH).replace(" ", "_").replace(".", "") + ".png";}

    //Calculates damage per second (DPS)
    public double getDPS() {
        if (hitSpeed == 0)
            return 0;
        return damage / hitSpeed;
    }

    @Override
    public String toString() {return name + " (Cost: " + cost + ")";}

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
    public int hashCode() {return name.hashCode();}
}