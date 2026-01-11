package com.kuroyale.service;

import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.enums.ComboType;
// import com.kuroyale.model.logic.GameState; // Removed unused import

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

public class ComboService implements GameEventListener {

    private static final long COMBO_WINDOW_MS = 5000; // 5 seconds

    private static class PlayedCardEvent {
        final Card card;
        final long timestamp;
        final List<ICombatant> spawnedUnits;

        PlayedCardEvent(Card card, long timestamp, List<ICombatant> spawnedUnits) {
            this.card = card;
            this.timestamp = timestamp;
            this.spawnedUnits = spawnedUnits;
        }
    }

    private final Deque<PlayedCardEvent> playerPlayedCards = new ArrayDeque<>();
    private final Deque<PlayedCardEvent> opponentPlayedCards = new ArrayDeque<>();

    // Track unique combos for both sides separately for stats
    private final java.util.Set<ComboType> playerUniqueCombos = new java.util.HashSet<>();
    private final java.util.Set<ComboType> opponentUniqueCombos = new java.util.HashSet<>();

    // Track last trigger time per combo type (globally or per player? Usually per
    // player is better but to keep it simple let's use a composite key or just
    // separate maps if needed.
    // Actually, simply using separate maps for cooldowns is safer.)
    private final java.util.Map<ComboType, Long> playerComboCooldowns = new java.util.HashMap<>();
    private final java.util.Map<ComboType, Long> opponentComboCooldowns = new java.util.HashMap<>();

    private com.kuroyale.model.logic.IBattleState gameState; // Reference to apply global effects like Elixir refund

    public ComboService() {
        GameEventBus.getInstance().subscribe(this);
    }

    public void setGameState(com.kuroyale.model.logic.IBattleState gameState) {
        this.gameState = gameState;
    }

    public void cleanup() {
        GameEventBus.getInstance().unsubscribe(this);
        playerUniqueCombos.clear();
        opponentUniqueCombos.clear();
        playerComboCooldowns.clear();
        opponentComboCooldowns.clear();
        playerPlayedCards.clear();
        opponentPlayedCards.clear();
    }

    public int getUniqueComboCount() {
        return playerUniqueCombos.size();
    }

    public java.util.Set<ComboType> getTriggeredCombos() {
        return new java.util.HashSet<>(playerUniqueCombos);
    }

    // New method for opponent stats if needed (e.g. PvP end screen)
    public int getOpponentUniqueComboCount() {
        return opponentUniqueCombos.size();
    }

    public java.util.Set<ComboType> getOpponentTriggeredCombos() {
        return new java.util.HashSet<>(opponentUniqueCombos);
    }

    @Override
    public void onCardPlayed(boolean isPlayer, Card card, List<ICombatant> spawnedUnits) {
        long now = System.currentTimeMillis();

        Deque<PlayedCardEvent> targetList = isPlayer ? playerPlayedCards : opponentPlayedCards;

        pruneOldEvents(targetList, now);

        // Check for combos
        checkCombos(isPlayer, card, spawnedUnits, targetList, now);

        // Add current event
        targetList.addLast(new PlayedCardEvent(card, now, spawnedUnits));
    }

    private void pruneOldEvents(Deque<PlayedCardEvent> list, long now) {
        while (!list.isEmpty() && (now - list.peekFirst().timestamp > COMBO_WINDOW_MS)) {
            list.removeFirst();
        }
    }

    private void checkCombos(boolean isPlayer, Card currentCard, List<ICombatant> currentUnits,
            Deque<PlayedCardEvent> history, long now) {
        // Iterate backwards to find the most recent matching pair
        java.util.Iterator<PlayedCardEvent> it = history.descendingIterator();

        while (it.hasNext()) {
            PlayedCardEvent prevEvent = it.next();

            // Priority 1: Siege Mode & Air Assault
            if (checkSiegeMode(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;
            if (checkAirAssault(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;

            // Priority 2: Royal Combo (Prioritized over generic Tank+Support)
            if (checkRoyalCombo(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;

            // Priority 3: Others
            if (checkTankSupport(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;
            if (checkSpellSynergy(isPlayer, prevEvent, currentCard, now))
                return;
            if (checkSwarmAttack(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;
            if (checkBuildingDefense(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;
            if (checkRushAttack(isPlayer, prevEvent, currentCard, currentUnits, now))
                return;
        }
    }

    private boolean canTriggerCombo(boolean isPlayer, ComboType type, long now) {
        java.util.Map<ComboType, Long> cooldowns = isPlayer ? playerComboCooldowns : opponentComboCooldowns;
        Long lastTrigger = cooldowns.get(type);
        return lastTrigger == null || (now - lastTrigger > COMBO_WINDOW_MS);
    }

    private void triggerCombo(boolean isPlayer, ComboType type, List<ICombatant> affectedUnits) {
        long now = System.currentTimeMillis();
        if (!canTriggerCombo(isPlayer, type, now)) {
            return; // Combo already triggered within this 5-second window
        }

        if (isPlayer) {
            playerComboCooldowns.put(type, now);
            playerUniqueCombos.add(type);
        } else {
            opponentComboCooldowns.put(type, now);
            opponentUniqueCombos.add(type);
        }

        GameEventBus.getInstance().publishComboTriggered(type, affectedUnits);
    }

    // 1. Tank + Support
    private boolean checkTankSupport(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        boolean prevIsTank = isTank(prev.card);
        boolean currIsRanged = isRangedTroop(curr);

        if (prevIsTank && currIsRanged) {
            applyDamageBuff(currUnits, 0.15); // +15%
            triggerCombo(isPlayer, ComboType.TANK_SUPPORT, currUnits);
            return true;
        }
        return false;
    }

    // 2. Spell Synergy
    private boolean checkSpellSynergy(boolean isPlayer, PlayedCardEvent prev, Card curr, long now) {
        if (prev.card.getType() == CardType.SPELL && curr.getType() == CardType.SPELL) {
            // Refund 1 Elixir
            if (gameState != null) {
                gameState.getElixirManager(isPlayer).refund(1);
            }
            triggerCombo(isPlayer, ComboType.SPELL_SYNERGY, new ArrayList<>());
            return true;
        }
        return false;
    }

    // 3. Swarm Attack
    private boolean checkSwarmAttack(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        if (isSwarm(prev.card) && isSwarm(curr)) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);
            // Effect: +10% Movement Speed
            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Troop) {
                    ((com.kuroyale.model.entities.Troop) u).modifySpeed(1.10);
                }
            }
            triggerCombo(isPlayer, ComboType.SWARM_ATTACK, allUnits);
            return true;
        }
        return false;
    }

    // 4. Building Defense
    private boolean checkBuildingDefense(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        if (prev.card.getType() == CardType.BUILDING && curr.getType() == CardType.BUILDING) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Building) {
                    ((com.kuroyale.model.entities.Building) u)
                            .heal((int) (((com.kuroyale.model.entities.Building) u).getMaxHealth() * 0.20));
                }
            }
            triggerCombo(isPlayer, ComboType.BUILDING_DEFENSE, allUnits);
            return true;
        }
        return false;
    }

    // 5. Air Assault
    private boolean checkAirAssault(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        boolean minions1 = prev.card.getName().contains("Minion");
        boolean minions2 = curr.getName().contains("Minion");

        if (minions1 && minions2) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);
            applyDamageBuff(allUnits, 0.15);
            triggerCombo(isPlayer, ComboType.AIR_ASSAULT, allUnits);
            return true;
        }
        return false;
    }

    // 6. Royal Combo
    private boolean checkRoyalCombo(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        boolean knightPlayed = prev.card.getName().equals("Knight") || curr.getName().equals("Knight");
        boolean archersPlayed = prev.card.getName().equals("Archers") || curr.getName().equals("Archers");

        if (knightPlayed && archersPlayed) {
            List<ICombatant> knightUnits = new ArrayList<>();
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Troop) {
                    if (((com.kuroyale.model.entities.Troop) u).getBaseCard().getName().equals("Knight")) {
                        ((com.kuroyale.model.entities.Troop) u).heal(100);
                        knightUnits.add(u);
                    }
                }
            }

            if (!knightUnits.isEmpty()) {
                triggerCombo(isPlayer, ComboType.ROYAL_COMBO, knightUnits);
                return true;
            }
        }
        return false;
    }

    // 7. Siege Mode
    private boolean checkSiegeMode(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        boolean mortarPlayed = prev.card.getName().equals("Mortar") || curr.getName().equals("Mortar");
        boolean defensePlayed = isDefensiveBuilding(prev.card) || isDefensiveBuilding(curr);

        if (mortarPlayed && defensePlayed) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            List<ICombatant> mortars = new ArrayList<>();
            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Building) {
                    com.kuroyale.model.entities.Building b = (com.kuroyale.model.entities.Building) u;
                    boolean fromMortarCard = false;

                    if (prev.spawnedUnits.contains(u) && prev.card.getName().equals("Mortar"))
                        fromMortarCard = true;
                    if (currUnits.contains(u) && curr.getName().equals("Mortar"))
                        fromMortarCard = true;

                    if (fromMortarCard) {
                        b.buffRange(2.0);
                        mortars.add(b);
                    }
                }
            }

            if (!mortars.isEmpty()) {
                triggerCombo(isPlayer, ComboType.SIEGE_MODE, mortars);
                return true;
            }
        }
        return false;
    }

    // 8. Rush Attack
    private boolean checkRushAttack(boolean isPlayer, PlayedCardEvent prev, Card curr, List<ICombatant> currUnits,
            long now) {
        boolean hogPlayed = prev.card.getName().equals("Hog Rider") || curr.getName().equals("Hog Rider");
        boolean lowCostPlayed = prev.card.getCost() <= 2 || curr.getCost() <= 2;

        if (hogPlayed && lowCostPlayed) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            List<ICombatant> hogs = new ArrayList<>();
            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Troop) {
                    com.kuroyale.model.entities.Troop t = (com.kuroyale.model.entities.Troop) u;
                    if (t.getBaseCard().getName().equals("Hog Rider")) {
                        t.modifySpeed(1.20);
                        hogs.add(t);
                    }
                }
            }

            if (!hogs.isEmpty()) {
                triggerCombo(isPlayer, ComboType.RUSH_ATTACK, hogs);
                return true;
            }
        }
        return false;
    }

    // Helpers
    private boolean isTank(Card c) {
        return c.getName().equals("Giant") || c.getName().equals("Knight") || c.getName().equals("Golem")
                || c.getName().equals("P.E.K.K.A");
    }

    private boolean isRangedTroop(Card c) {
        String n = c.getName();
        return n.equals("Musketeer") || n.equals("Archers") || n.equals("Spear Goblins") || n.equals("Wizard")
                || n.equals("Witch");
    }

    private boolean isSwarm(Card c) {
        return c.getCount() >= 3;
    }

    private boolean isDefensiveBuilding(Card c) {
        return c.getType() == CardType.BUILDING && !c.getName().equals("Mortar") && !c.getName().equals("X-Bow"); // Mortar
                                                                                                                  // is
                                                                                                                  // siege
    }

    private void applyDamageBuff(List<ICombatant> units, double percent) {
        for (ICombatant u : units) {
            if (u instanceof com.kuroyale.model.entities.Troop) {
                ((com.kuroyale.model.entities.Troop) u).buffDamage(percent);
            }
        }
    }
}
