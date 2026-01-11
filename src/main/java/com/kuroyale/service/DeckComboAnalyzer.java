package com.kuroyale.service;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.enums.ComboType;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes a deck to determine which combos can be achieved with the current
 * cards.
 * This is used in the deck builder to show available combos to the player.
 */
public class DeckComboAnalyzer {

    /**
     * Returns a list of all combo types that can be achieved with the cards in the
     * deck.
     */
    public List<ComboType> getAvailableCombos(Deck deck) {
        List<ComboType> available = new ArrayList<>();
        List<Card> cards = deck.getCards();

        if (cards.isEmpty()) {
            return available;
        }

        // Check each combo type
        if (hasTankSupport(cards))
            available.add(ComboType.TANK_SUPPORT);
        if (hasSpellSynergy(cards))
            available.add(ComboType.SPELL_SYNERGY);
        if (hasSwarmAttack(cards))
            available.add(ComboType.SWARM_ATTACK);
        if (hasBuildingDefense(cards))
            available.add(ComboType.BUILDING_DEFENSE);
        if (hasAirAssault(cards))
            available.add(ComboType.AIR_ASSAULT);
        if (hasRoyalCombo(cards))
            available.add(ComboType.ROYAL_COMBO);
        if (hasSiegeMode(cards))
            available.add(ComboType.SIEGE_MODE);
        if (hasRushAttack(cards))
            available.add(ComboType.RUSH_ATTACK);

        return available;
    }

    /**
     * Returns detailed info about a combo including requirements.
     */
    public String getComboRequirements(ComboType combo) {
        switch (combo) {
            case TANK_SUPPORT:
                return "Requires: Giant/Knight + Ranged (Musketeer/Archers/Spear Goblins/Wizard)";
            case SPELL_SYNERGY:
                return "Requires: Any 2 different spells";
            case SWARM_ATTACK:
                return "Requires: Any 2 swarm cards (3+ units)";
            case BUILDING_DEFENSE:
                return "Requires: Any 2 buildings";
            case AIR_ASSAULT:
                return "Requires: Minions + Minion Horde";
            case ROYAL_COMBO:
                return "Requires: Knight + Archers";
            case SIEGE_MODE:
                return "Requires: Mortar + defensive building";
            case RUSH_ATTACK:
                return "Requires: Hog Rider + low-cost card (1-2 elixir)";
            default:
                return "";
        }
    }

    /**
     * Info about a near-complete combo missing one card.
     */
    public static class NearComboInfo {
        public final ComboType combo;
        public final String missingCard;

        public NearComboInfo(ComboType combo, String missingCard) {
            this.combo = combo;
            this.missingCard = missingCard;
        }
    }

    /**
     * Returns combos that are missing just one card to complete.
     */
    public List<NearComboInfo> getNearCompleteCombos(Deck deck, List<ComboType> alreadyAvailable) {
        List<NearComboInfo> nearComplete = new ArrayList<>();
        List<Card> cards = deck.getCards();

        // Tank + Support
        if (!alreadyAvailable.contains(ComboType.TANK_SUPPORT)) {
            boolean hasTank = cards.stream().anyMatch(this::isTank);
            boolean hasRanged = cards.stream().anyMatch(this::isRangedTroop);
            if (hasTank && !hasRanged) {
                nearComplete
                        .add(new NearComboInfo(ComboType.TANK_SUPPORT, "Add: Musketeer/Archers/Spear Goblins/Wizard"));
            } else if (!hasTank && hasRanged) {
                nearComplete.add(new NearComboInfo(ComboType.TANK_SUPPORT, "Add: Giant or Knight"));
            }
        }

        // Spell Synergy
        if (!alreadyAvailable.contains(ComboType.SPELL_SYNERGY)) {
            long spellCount = cards.stream().filter(c -> c.getType() == CardType.SPELL).count();
            if (spellCount == 1) {
                nearComplete.add(new NearComboInfo(ComboType.SPELL_SYNERGY, "Add: Any spell"));
            }
        }

        // Swarm Attack
        if (!alreadyAvailable.contains(ComboType.SWARM_ATTACK)) {
            long swarmCount = cards.stream().filter(this::isSwarm).count();
            if (swarmCount == 1) {
                nearComplete.add(new NearComboInfo(ComboType.SWARM_ATTACK, "Add: Swarm card (3+ units)"));
            }
        }

        // Building Defense
        if (!alreadyAvailable.contains(ComboType.BUILDING_DEFENSE)) {
            long buildingCount = cards.stream().filter(c -> c.getType() == CardType.BUILDING).count();
            if (buildingCount == 1) {
                nearComplete.add(new NearComboInfo(ComboType.BUILDING_DEFENSE, "Add: Any building"));
            }
        }

        // Air Assault
        if (!alreadyAvailable.contains(ComboType.AIR_ASSAULT)) {
            boolean hasMinions = cards.stream().anyMatch(c -> c.getName().equals("Minions"));
            boolean hasHorde = cards.stream().anyMatch(c -> c.getName().equals("Minion Horde"));
            if (hasMinions && !hasHorde) {
                nearComplete.add(new NearComboInfo(ComboType.AIR_ASSAULT, "Add: Minion Horde"));
            } else if (!hasMinions && hasHorde) {
                nearComplete.add(new NearComboInfo(ComboType.AIR_ASSAULT, "Add: Minions"));
            }
        }

        // Royal Combo
        if (!alreadyAvailable.contains(ComboType.ROYAL_COMBO)) {
            boolean hasKnight = cards.stream().anyMatch(c -> c.getName().equals("Knight"));
            boolean hasArchers = cards.stream().anyMatch(c -> c.getName().equals("Archers"));
            if (hasKnight && !hasArchers) {
                nearComplete.add(new NearComboInfo(ComboType.ROYAL_COMBO, "Add: Archers"));
            } else if (!hasKnight && hasArchers) {
                nearComplete.add(new NearComboInfo(ComboType.ROYAL_COMBO, "Add: Knight"));
            }
        }

        // Siege Mode
        if (!alreadyAvailable.contains(ComboType.SIEGE_MODE)) {
            boolean hasMortar = cards.stream().anyMatch(c -> c.getName().equals("Mortar"));
            boolean hasDefensive = cards.stream().anyMatch(this::isDefensiveBuilding);
            if (hasMortar && !hasDefensive) {
                nearComplete.add(new NearComboInfo(ComboType.SIEGE_MODE, "Add: Cannon/Tesla/Bomb Tower/Inferno Tower"));
            } else if (!hasMortar && hasDefensive) {
                nearComplete.add(new NearComboInfo(ComboType.SIEGE_MODE, "Add: Mortar"));
            }
        }

        // Rush Attack
        if (!alreadyAvailable.contains(ComboType.RUSH_ATTACK)) {
            boolean hasHog = cards.stream().anyMatch(c -> c.getName().equals("Hog Rider"));
            boolean hasLowCost = cards.stream().anyMatch(c -> c.getCost() <= 2);
            if (hasHog && !hasLowCost) {
                nearComplete.add(new NearComboInfo(ComboType.RUSH_ATTACK, "Add: 1-2 elixir card"));
            } else if (!hasHog && hasLowCost) {
                nearComplete.add(new NearComboInfo(ComboType.RUSH_ATTACK, "Add: Hog Rider"));
            }
        }

        return nearComplete;
    }

    // 1. Tank + Support: Giant/Knight + Musketeer/Archers/Spear Goblins/Wizard
    private boolean hasTankSupport(List<Card> cards) {
        boolean hasTank = false;
        boolean hasRanged = false;

        for (Card c : cards) {
            if (isTank(c))
                hasTank = true;
            if (isRangedTroop(c))
                hasRanged = true;
        }

        return hasTank && hasRanged;
    }

    // 2. Spell Synergy: Any 2 different spells
    private boolean hasSpellSynergy(List<Card> cards) {
        int spellCount = 0;
        for (Card c : cards) {
            if (c.getType() == CardType.SPELL) {
                spellCount++;
                if (spellCount >= 2)
                    return true;
            }
        }
        return false;
    }

    // 3. Swarm Attack: Any 2 swarm cards (count >= 3)
    private boolean hasSwarmAttack(List<Card> cards) {
        int swarmCount = 0;
        for (Card c : cards) {
            if (isSwarm(c)) {
                swarmCount++;
                if (swarmCount >= 2)
                    return true;
            }
        }
        return false;
    }

    // 4. Building Defense: Any 2 buildings
    private boolean hasBuildingDefense(List<Card> cards) {
        int buildingCount = 0;
        for (Card c : cards) {
            if (c.getType() == CardType.BUILDING) {
                buildingCount++;
                if (buildingCount >= 2)
                    return true;
            }
        }
        return false;
    }

    // 5. Air Assault: Minions + Minion Horde
    private boolean hasAirAssault(List<Card> cards) {
        boolean hasMinions = false;
        boolean hasHorde = false;

        for (Card c : cards) {
            if (c.getName().equals("Minions"))
                hasMinions = true;
            if (c.getName().equals("Minion Horde"))
                hasHorde = true;
        }

        return hasMinions && hasHorde;
    }

    // 6. Royal Combo: Knight + Archers
    private boolean hasRoyalCombo(List<Card> cards) {
        boolean hasKnight = false;
        boolean hasArchers = false;

        for (Card c : cards) {
            if (c.getName().equals("Knight"))
                hasKnight = true;
            if (c.getName().equals("Archers"))
                hasArchers = true;
        }

        return hasKnight && hasArchers;
    }

    // 7. Siege Mode: Mortar + defensive building (Cannon, Tesla, Bomb Tower,
    // Inferno Tower)
    private boolean hasSiegeMode(List<Card> cards) {
        boolean hasMortar = false;
        boolean hasDefensiveBuilding = false;

        for (Card c : cards) {
            if (c.getName().equals("Mortar"))
                hasMortar = true;
            if (isDefensiveBuilding(c))
                hasDefensiveBuilding = true;
        }

        return hasMortar && hasDefensiveBuilding;
    }

    // 8. Rush Attack: Hog Rider + any low-cost card (1-2 elixir)
    private boolean hasRushAttack(List<Card> cards) {
        boolean hasHog = false;
        boolean hasLowCost = false;

        for (Card c : cards) {
            if (c.getName().equals("Hog Rider"))
                hasHog = true;
            if (c.getCost() <= 2)
                hasLowCost = true;
        }

        return hasHog && hasLowCost;
    }

    // --- Helper methods (matching ComboService logic) ---

    private boolean isTank(Card c) {
        String name = c.getName();
        return name.equals("Giant") || name.equals("Knight") ||
                name.equals("Golem") || name.equals("P.E.K.K.A");
    }

    private boolean isRangedTroop(Card c) {
        String name = c.getName();
        return name.equals("Musketeer") || name.equals("Archers") ||
                name.equals("Spear Goblins") || name.equals("Wizard") ||
                name.equals("Witch");
    }

    private boolean isSwarm(Card c) {
        return c.getCount() >= 3;
    }

    private boolean isDefensiveBuilding(Card c) {
        if (c.getType() != CardType.BUILDING)
            return false;
        String name = c.getName();
        // Defensive buildings (not siege weapons)
        return name.equals("Cannon") || name.equals("Tesla") ||
                name.equals("Bomb Tower") || name.equals("Inferno Tower");
    }
}
