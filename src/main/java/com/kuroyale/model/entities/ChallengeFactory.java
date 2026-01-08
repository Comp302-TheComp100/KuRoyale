package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/** Design Pattern: Factory Pattern
 * - Centralizes challenge creation with proper initialization
 * - Each create method returns a specific challenge type with its validation logic
 *
 * Design Pattern: Strategy Pattern (implemented via anonymous inner classes)
 *  Each challenge has unique deck validation rules*/
public class ChallengeFactory {

    // Swarm troop card names
    private static final Set<String> SWARM_CARDS = new HashSet<>(Arrays.asList(
            "Skeletons", "Goblins", "Spear Goblins", "Archers", "Minions", "Minion Horde", "Barbarians"));

    // Spell card names
    private static final Set<String> SPELL_CARDS = new HashSet<>(Arrays.asList(
            "Zap", "Arrows", "Fireball", "Rocket"));

    // Tank/high-HP unit names for Tank Rush
    private static final Set<String> TANK_CARDS = new HashSet<>(Arrays.asList(
            "Giant", "Knight", "Valkyrie", "Mini P.E.K.K.A", "Barbarians"));

    /*Creates the "Swarm Master" challenge.
     * Rules: Must include at least 5 swarm cards. Other cards can be anything.*/
    public Challenge createSwarmMaster() {
        return new Challenge(1, ChallengeType.SWARM_MASTER,
                "Overwhelm your opponent with swarm troops!",
                "• Must include at least 5 swarm cards\n• Other 3 cards can be anything") {

            @Override
            public List<String> validateDeck(List<Card> deck) {
                List<String> errors = new ArrayList<>();
                int swarmCount = 0;

                for (Card card : deck) {
                    if (SWARM_CARDS.contains(card.getName())) {
                        swarmCount++;
                    }
                }

                if (swarmCount < 5) {
                    errors.add("Must have at least 5 swarm cards (you have " + swarmCount + ")");
                }

                return errors;
            }
        };
    }

    /* Creates the "Spell Barrage" challenge.
     * Rules: Must contain all 4 spell cards, spells cost 1 less elixir.*/
    public Challenge createSpellBarrage() {
        return new Challenge(2, ChallengeType.SPELL_BARRAGE,
                "Rain destruction with powerful spells!",
                "• Must include all 4 spell cards\n• Spells cost 1 less Elixir (min 1)") {

            @Override
            public List<String> validateDeck(List<Card> deck) {
                List<String> errors = new ArrayList<>();
                Set<String> foundSpells = new HashSet<>();

                for (Card card : deck) {
                    if (SPELL_CARDS.contains(card.getName())) {
                        foundSpells.add(card.getName());
                    }
                }

                for (String spell : SPELL_CARDS) {
                    if (!foundSpells.contains(spell)) {
                        errors.add("Missing required spell: " + spell);
                    }
                }

                return errors;
            }
        };
    }

    /* Creates the "No Buildings Allowed" challenge.
     * Rules: Cannot use any building cards. */
    public Challenge createNoBuildingsAllowed() {
        return new Challenge(3, ChallengeType.NO_BUILDINGS,
                "Fight without defensive structures!",
                "• No building cards allowed\n• Deck must contain only troops and spells") {

            @Override
            public List<String> validateDeck(List<Card> deck) {
                List<String> errors = new ArrayList<>();

                for (Card card : deck) {
                    if (card.getType() == CardType.BUILDING) {
                        errors.add("'" + card.getName() + "' is a building (not allowed)");
                    }
                }

                return errors;
            }
        };
    }

    /* Creates the "Budget Battle" challenge.
     * Rules: Only cards costing 3 elixir or less.*/
    public Challenge createBudgetBattle() {
        return new Challenge(4, ChallengeType.BUDGET_BATTLE,
                "Win with cheap but effective cards!",
                "• Only cards costing 3 Elixir or less\n• Deck must have 8 valid cards") {

            @Override
            public List<String> validateDeck(List<Card> deck) {
                List<String> errors = new ArrayList<>();

                for (Card card : deck) {
                    if (card.getCost() > 3) {
                        errors.add("'" + card.getName() + "' costs " + card.getCost() + " (max 3 allowed)");
                    }
                }

                if (deck.size() != 8) {
                    errors.add("Deck must have exactly 8 cards (you have " + deck.size() + ")");
                }

                return errors;
            }
        };
    }

    /* Creates the "Tank Rush" challenge.
     * Rules: Only high-HP units, no spells or buildings. */
    public Challenge createTankRush() {
        return new Challenge(5, ChallengeType.TANK_RUSH,
                "Crush your enemies with heavy units!",
                "• Only high-HP units allowed\n• No spells or buildings") {

            @Override
            public List<String> validateDeck(List<Card> deck) {
                List<String> errors = new ArrayList<>();

                for (Card card : deck) {
                    if (!TANK_CARDS.contains(card.getName())) {
                        errors.add("'" + card.getName() + "' is not a tank unit");
                    }
                }

                return errors;
            }

            @Override
            public List<String> getAllowedCardNames() {
                return new ArrayList<>(TANK_CARDS);
            }
        };
    }

    /* Returns all 5 challenges in order.
     * Challenges must be completed in sequence. */
    public List<Challenge> getAllChallenges() {
        List<Challenge> challenges = new ArrayList<>();
        challenges.add(createSwarmMaster());
        challenges.add(createSpellBarrage());
        challenges.add(createNoBuildingsAllowed());
        challenges.add(createBudgetBattle());
        challenges.add(createTankRush());
        return challenges;
    }
}
