package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a card in the game with all its properties
 */
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
    private final boolean areaEffect;
    private final String description;
    private final int count; // For swarm troops (e.g., Skeletons spawn 4)
    private final int lifetime; // For buildings

    public Card(String name, int cost, CardType type, int hp, int damage, double hitSpeed,
            double range, SpeedType speed, TargetType target, boolean areaEffect,
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

    public int getHp() {
        return hp;
    }

    public int getDamage() {
        return damage;
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

    public String getImagePath() {
        return "/images/cards/" + name.toLowerCase(java.util.Locale.ENGLISH).replace(" ", "_").replace(".", "")
                + ".png";
    }

    public double getDPS() {
        if (hitSpeed == 0)
            return 0;
        return damage / hitSpeed;
    }

    // Factory methods for all 28 cards

    // TROOPS (15 cards)

    public static Card createKnight() {
        return new Card("Knight", 3, CardType.TROOP, 600, 75, 1.1, 0,
                SpeedType.MEDIUM, TargetType.GROUND, false,
                "A tough soldier with a sword. Good for soaking up damage.", 1, 0);
    }

    public static Card createMusketeer() {
        return new Card("Musketeer", 4, CardType.TROOP, 340, 100, 1.1, 6.5,
                SpeedType.MEDIUM, TargetType.BOTH, false,
                "A ranged shooter. Can hit ground and air targets.", 1, 0);
    }

    public static Card createMiniPEKKA() {
        return new Card("Mini P.E.K.K.A", 4, CardType.TROOP, 600, 325, 1.8, 0,
                SpeedType.SLOW, TargetType.GROUND, false,
                "A powerful armored warrior. Slow but deals massive damage.", 1, 0);
    }

    public static Card createGiant() {
        return new Card("Giant", 5, CardType.TROOP, 2000, 126, 1.5, 0,
                SpeedType.VERY_SLOW, TargetType.BUILDINGS, false,
                "A huge tank unit. Ignores soldiers and attacks buildings/towers only.", 1, 0);
    }

    public static Card createHogRider() {
        return new Card("Hog Rider", 4, CardType.TROOP, 800, 160, 1.5, 0,
                SpeedType.FAST, TargetType.BUILDINGS, false,
                "Fast unit that rushes toward buildings. Ignores soldiers.", 1, 0);
    }

    public static Card createBomber() {
        return new Card("Bomber", 3, CardType.TROOP, 150, 100, 1.9, 5.0,
                SpeedType.MEDIUM, TargetType.GROUND, true,
                "Throws bombs that explode on impact.", 1, 0);
    }

    public static Card createValkyrie() {
        return new Card("Valkyrie", 4, CardType.TROOP, 880, 120, 1.5, 0,
                SpeedType.MEDIUM, TargetType.GROUND, true,
                "Spins and damages all nearby enemies.", 1, 0);
    }

    public static Card createWizard() {
        return new Card("Wizard", 5, CardType.TROOP, 340, 130, 1.7, 5.0,
                SpeedType.MEDIUM, TargetType.BOTH, true,
                "Shoots fireballs that explode.", 1, 0);
    }

    public static Card createSkeletons() {
        return new Card("Skeletons", 1, CardType.TROOP, 30, 30, 1.0, 0,
                SpeedType.VERY_FAST, TargetType.GROUND, false,
                "Spawns 4 very weak but very fast soldiers.", 4, 0);
    }

    public static Card createGoblins() {
        return new Card("Goblins", 2, CardType.TROOP, 80, 50, 1.1, 0,
                SpeedType.FAST, TargetType.GROUND, false,
                "Spawns 3 fast, weak melee fighters.", 3, 0);
    }

    public static Card createSpearGoblins() {
        return new Card("Spear Goblins", 2, CardType.TROOP, 52, 24, 1.3, 5.5,
                SpeedType.FAST, TargetType.BOTH, false,
                "Spawns 3 ranged goblins (can hit air).", 3, 0);
    }

    public static Card createArchers() {
        return new Card("Archers", 3, CardType.TROOP, 125, 40, 1.2, 5.5,
                SpeedType.MEDIUM, TargetType.BOTH, false,
                "Spawns 2 ranged soldiers (can hit air).", 2, 0);
    }

    public static Card createMinions() {
        return new Card("Minions", 3, CardType.TROOP, 90, 40, 1.0, 2.5,
                SpeedType.VERY_FAST, TargetType.BOTH, false,
                "Spawns 3 flying units that attack from the air.", 3, 0);
    }

    public static Card createMinionHorde() {
        return new Card("Minion Horde", 5, CardType.TROOP, 90, 40, 1.0, 2.5,
                SpeedType.VERY_FAST, TargetType.BOTH, false,
                "Spawns 6 flying units (double the Minions).", 6, 0);
    }

    public static Card createBarbarians() {
        return new Card("Barbarians", 5, CardType.TROOP, 300, 75, 1.5, 0,
                SpeedType.FAST, TargetType.GROUND, false,
                "Spawns 4 tough melee fighters.", 4, 0);
    }

    // BUILDINGS (9 cards)

    public static Card createCannon() {
        return new Card("Cannon", 3, CardType.BUILDING, 400, 60, 0.8, 5.5,
                SpeedType.NONE, TargetType.GROUND, false,
                "Basic defensive tower.", 1, 30);
    }

    public static Card createTesla() {
        return new Card("Tesla", 4, CardType.BUILDING, 400, 64, 1.1, 5.5,
                SpeedType.NONE, TargetType.BOTH, false,
                "Defensive tower that can hit both air and ground.", 1, 40);
    }

    public static Card createMortar() {
        return new Card("Mortar", 4, CardType.BUILDING, 600, 108, 5.0, 11.0,
                SpeedType.NONE, TargetType.GROUND, true,
                "Long-range artillery. Range: 4.5-11 tiles.", 1, 30);
    }

    public static Card createBombTower() {
        return new Card("Bomb Tower", 5, CardType.BUILDING, 900, 100, 1.6, 6.0,
                SpeedType.NONE, TargetType.GROUND, true,
                "Defensive tower with explosive shells.", 1, 40);
    }

    public static Card createInfernoTower() {
        return new Card("Inferno Tower", 5, CardType.BUILDING, 800, 400, 0.4, 6.0,
                SpeedType.NONE, TargetType.BOTH, false,
                "Shoots a laser that grows stronger over time. DMG: 20-400 (ramps up).", 1, 40);
    }

    public static Card createTombstone() {
        return new Card("Tombstone", 3, CardType.BUILDING, 200, 0, 2.9, 0,
                SpeedType.NONE, TargetType.GROUND, false,
                "Spawns 1 Skeleton every 2.9s. When destroyed, spawns 4 more.", 1, 40);
    }

    public static Card createGoblinHut() {
        return new Card("Goblin Hut", 5, CardType.BUILDING, 700, 0, 4.9, 0,
                SpeedType.NONE, TargetType.GROUND, false,
                "Spawns Spear Goblins periodically. Spawns 1 every 4.9s.", 1, 60);
    }

    public static Card createBarbarianHut() {
        return new Card("Barbarian Hut", 7, CardType.BUILDING, 1100, 0, 14.0, 0,
                SpeedType.NONE, TargetType.GROUND, false,
                "Spawns 2 Barbarians periodically. Spawns 2 every 14s.", 1, 60);
    }

    public static Card createElixirCollector() {
        return new Card("Elixir Collector", 5, CardType.BUILDING, 640, 0, 0, 0,
                SpeedType.NONE, TargetType.NONE, false,
                "Generates Elixir over time. Produces 1 Elixir every 10s. Total: 7 Elixir.", 1, 70);
    }

    // SPELLS (4 cards)

    public static Card createZap() {
        return new Card("Zap", 2, CardType.SPELL, 0, 80, 0, 2.5,
                SpeedType.NONE, TargetType.BOTH, true,
                "Small area damage + stuns enemies for 0.5 seconds. Radius: 2.5 tiles.", 1, 0);
    }

    public static Card createArrows() {
        return new Card("Arrows", 3, CardType.SPELL, 0, 115, 0, 4.0,
                SpeedType.NONE, TargetType.BOTH, true,
                "Medium area damage. Good for killing swarms. Radius: 4 tiles.", 1, 0);
    }

    public static Card createFireball() {
        return new Card("Fireball", 4, CardType.SPELL, 0, 325, 0, 2.5,
                SpeedType.NONE, TargetType.BOTH, true,
                "Large area damage. Good for clusters of enemies. Radius: 2.5 tiles.", 1, 0);
    }

    public static Card createRocket() {
        return new Card("Rocket", 6, CardType.SPELL, 0, 700, 0, 2.0,
                SpeedType.NONE, TargetType.BOTH, true,
                "Massive damage in a small area. Expensive but powerful. Radius: 2 tiles.", 1, 0);
    }

    /**
     * Returns a list of all 28 available cards
     */
    public static List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();

        // Troops (15)
        cards.add(createKnight());
        cards.add(createMusketeer());
        cards.add(createMiniPEKKA());
        cards.add(createGiant());
        cards.add(createHogRider());
        cards.add(createBomber());
        cards.add(createValkyrie());
        cards.add(createWizard());
        cards.add(createSkeletons());
        cards.add(createGoblins());
        cards.add(createSpearGoblins());
        cards.add(createArchers());
        cards.add(createMinions());
        cards.add(createMinionHorde());
        cards.add(createBarbarians());

        // Buildings (9)
        cards.add(createCannon());
        cards.add(createTesla());
        cards.add(createMortar());
        cards.add(createBombTower());
        cards.add(createInfernoTower());
        cards.add(createTombstone());
        cards.add(createGoblinHut());
        cards.add(createBarbarianHut());
        cards.add(createElixirCollector());

        // Spells (4)
        cards.add(createZap());
        cards.add(createArrows());
        cards.add(createFireball());
        cards.add(createRocket());

        return cards;
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
}
