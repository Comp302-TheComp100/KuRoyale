package com.kuroyale.service.battle.ai;

import com.kuroyale.model.entities.Card;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CardProfiles {
    private static final Map<String, CardProfile> PROFILES_BY_NAME;

    static {
        Map<String, CardProfile> m = new HashMap<>();

        m.put("Knight", CardProfile.of(CardRole.TANK, CardRole.DPS));
        m.put("Musketeer", CardProfile.of(CardRole.RANGED_DPS, CardRole.DPS));
        m.put("Mini P.E.K.K.A", CardProfile.of(CardRole.DPS));
        m.put("Giant", CardProfile.of(CardRole.TANK, CardRole.WIN_CONDITION));
        m.put("Hog Rider", CardProfile.of(CardRole.WIN_CONDITION, CardRole.DPS));
        m.put("Bomber", CardProfile.of(CardRole.SPLASH_TROOP, CardRole.DPS));
        m.put("Valkyrie", CardProfile.of(CardRole.SPLASH_TROOP, CardRole.TANK));
        m.put("Wizard", CardProfile.of(CardRole.SPLASH_TROOP, CardRole.RANGED_DPS));
        m.put("Skeletons", CardProfile.of(CardRole.SWARM));
        m.put("Goblins", CardProfile.of(CardRole.SWARM, CardRole.DPS));
        m.put("Spear Goblins", CardProfile.of(CardRole.SWARM, CardRole.RANGED_DPS));
        m.put("Archers", CardProfile.of(CardRole.RANGED_DPS));
        m.put("Minions", CardProfile.of(CardRole.DPS));
        m.put("Minion Horde", CardProfile.of(CardRole.SWARM, CardRole.DPS));
        m.put("Barbarians", CardProfile.of(CardRole.SWARM, CardRole.DPS));

        m.put("Cannon", CardProfile.of(CardRole.DEFENSIVE_BUILDING));
        m.put("Tesla", CardProfile.of(CardRole.DEFENSIVE_BUILDING, CardRole.RANGED_DPS));
        m.put("Mortar", CardProfile.of(CardRole.SIEGE_BUILDING));
        m.put("Bomb Tower", CardProfile.of(CardRole.DEFENSIVE_BUILDING, CardRole.SPLASH_TROOP));
        m.put("Inferno Tower", CardProfile.of(CardRole.DEFENSIVE_BUILDING, CardRole.DPS));
        m.put("Tombstone", CardProfile.of(CardRole.DEFENSIVE_BUILDING, CardRole.SPAWNER_BUILDING));
        m.put("Goblin Hut", CardProfile.of(CardRole.SPAWNER_BUILDING, CardRole.DEFENSIVE_BUILDING));
        m.put("Barbarian Hut", CardProfile.of(CardRole.SPAWNER_BUILDING));
        m.put("Elixir Collector", CardProfile.of(CardRole.ECONOMY_BUILDING));

        m.put("Zap", CardProfile.of(CardRole.AOE_SPELL));
        m.put("Arrows", CardProfile.of(CardRole.AOE_SPELL));
        m.put("Fireball", CardProfile.of(CardRole.AOE_SPELL));
        m.put("Rocket", CardProfile.of(CardRole.AOE_SPELL));

        PROFILES_BY_NAME = Collections.unmodifiableMap(m);
    }

    private CardProfiles() {
    }

    public static CardProfile get(Card card) {
        if (card == null || card.getName() == null) {
            return null;
        }
        return PROFILES_BY_NAME.get(card.getName());
    }

    public static boolean hasRole(Card card, CardRole role) {
        CardProfile p = get(card);
        return p != null && p.hasRole(role);
    }
}
