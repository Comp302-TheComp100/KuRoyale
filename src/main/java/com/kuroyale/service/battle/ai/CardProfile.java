package com.kuroyale.service.battle.ai;

import java.util.EnumSet;

public final class CardProfile {
    private final EnumSet<CardRole> roles;

    public CardProfile(EnumSet<CardRole> roles) {
        this.roles = roles != null ? EnumSet.copyOf(roles) : EnumSet.noneOf(CardRole.class);
    }

    public static CardProfile of(CardRole primary, CardRole... extra) {
        EnumSet<CardRole> set = EnumSet.noneOf(CardRole.class);
        if (primary != null) {
            set.add(primary);
        }
        if (extra != null) {
            for (CardRole r : extra) {
                if (r != null) {
                    set.add(r);
                }
            }
        }
        return new CardProfile(set);
    }

    public boolean hasRole(CardRole role) {
        return role != null && roles.contains(role);
    }

    public EnumSet<CardRole> getRoles() {
        return EnumSet.copyOf(roles);
    }
}
