package com.kuroyale.service;

import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.enums.ComboType;
import com.kuroyale.model.logic.GameState;

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

    private final Deque<PlayedCardEvent> playedCards = new ArrayDeque<>();
    private final java.util.Set<ComboType> uniqueCombosTriggered = new java.util.HashSet<>();
    // Track last trigger time per combo type to prevent re-triggering within same
    // 5-second window
    private final java.util.Map<ComboType, Long> comboCooldowns = new java.util.HashMap<>();
    private GameState gameState; // Reference to apply global effects like Elixir refund

    public ComboService() {
        GameEventBus.getInstance().subscribe(this);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void cleanup() {
        GameEventBus.getInstance().unsubscribe(this);
        uniqueCombosTriggered.clear();
        comboCooldowns.clear();
    }

    public int getUniqueComboCount() {
        return uniqueCombosTriggered.size();
    }

    public java.util.Set<ComboType> getTriggeredCombos() {
        return new java.util.HashSet<>(uniqueCombosTriggered);
    }

    @Override
    public void onCardPlayed(boolean isPlayer, Card card, List<ICombatant> spawnedUnits) {
        if (!isPlayer)
            return;

        long now = System.currentTimeMillis();
        pruneOldEvents(now);

        // Check for combos
        checkCombos(card, spawnedUnits, now);

        // Add current event
        playedCards.addLast(new PlayedCardEvent(card, now, spawnedUnits));
    }

    private void pruneOldEvents(long now) {
        while (!playedCards.isEmpty() && (now - playedCards.peekFirst().timestamp > COMBO_WINDOW_MS)) {
            playedCards.removeFirst();
        }
    }

    private void checkCombos(Card currentCard, List<ICombatant> currentUnits, long now) {
        // Iterate backwards to find the most recent matching pair
        java.util.Iterator<PlayedCardEvent> it = playedCards.descendingIterator();

        while (it.hasNext()) {
            PlayedCardEvent prevEvent = it.next();

            // Avoid triggering with itself (shouldn't happen as we add after check)

            if (checkTankSupport(prevEvent, currentCard, currentUnits))
                return;
            if (checkSpellSynergy(prevEvent, currentCard))
                return;
            if (checkSwarmAttack(prevEvent, currentCard, currentUnits))
                return;
            if (checkBuildingDefense(prevEvent, currentCard, currentUnits))
                return;
            if (checkAirAssault(prevEvent, currentCard, currentUnits))
                return;
            if (checkRoyalCombo(prevEvent, currentCard, currentUnits))
                return;
            if (checkSiegeMode(prevEvent, currentCard, currentUnits))
                return;
            if (checkRushAttack(prevEvent, currentCard, currentUnits))
                return;
        }
    }

    private boolean canTriggerCombo(ComboType type, long now) {
        Long lastTrigger = comboCooldowns.get(type);
        return lastTrigger == null || (now - lastTrigger > COMBO_WINDOW_MS);
    }

    private void triggerCombo(ComboType type, List<ICombatant> affectedUnits) {
        long now = System.currentTimeMillis();
        if (!canTriggerCombo(type, now)) {
            return; // Combo already triggered within this 5-second window
        }
        comboCooldowns.put(type, now);
        uniqueCombosTriggered.add(type);
        GameEventBus.getInstance().publishComboTriggered(type, affectedUnits);
    }

    // 1. Tank + Support
    private boolean checkTankSupport(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
        boolean prevIsTank = isTank(prev.card);
        boolean currIsRanged = isRangedTroop(curr);

        if (prevIsTank && currIsRanged) {
            applyDamageBuff(currUnits, 0.15); // +15%
            triggerCombo(ComboType.TANK_SUPPORT, currUnits);
            return true;
        }
        return false;
    }

    // 2. Spell Synergy
    private boolean checkSpellSynergy(PlayedCardEvent prev, Card curr) {
        if (prev.card.getType() == CardType.SPELL && curr.getType() == CardType.SPELL) {
            // Refund 1 Elixir
            if (gameState != null) {
                gameState.getPlayerElixir().refund(1);
            }
            triggerCombo(ComboType.SPELL_SYNERGY, new ArrayList<>());
            return true;
        }
        return false;
    }

    // 3. Swarm Attack
    private boolean checkSwarmAttack(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
        if (isSwarm(prev.card) && isSwarm(curr)) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);
            // Effect: +10% Movement Speed (Not directly supported by Unit yet, but let's
            // assume valid or skip logic)
            // Implementation: We might need to add speed modifier to ICombatant/Troop.
            // For now, let's just trigger visual and maybe skip actual mechanic if too
            // hard,
            // OR cast to Troop and set speed.

            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Troop) {
                    ((com.kuroyale.model.entities.Troop) u).modifySpeed(1.10);
                }
            }
            triggerCombo(ComboType.SWARM_ATTACK, allUnits);
            return true;
        }
        return false;
    }

    // 4. Building Defense
    private boolean checkBuildingDefense(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
        if (prev.card.getType() == CardType.BUILDING && curr.getType() == CardType.BUILDING) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Building) {
                    ((com.kuroyale.model.entities.Building) u)
                            .heal((int) (((com.kuroyale.model.entities.Building) u).getMaxHealth() * 0.20));
                }
            }
            triggerCombo(ComboType.BUILDING_DEFENSE, allUnits);
            return true;
        }
        return false;
    }

    // 5. Air Assault
    private boolean checkAirAssault(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
        boolean minions1 = prev.card.getName().contains("Minion");
        boolean minions2 = curr.getName().contains("Minion");

        if (minions1 && minions2) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);
            applyDamageBuff(allUnits, 0.15);
            triggerCombo(ComboType.AIR_ASSAULT, allUnits);
            return true;
        }
        return false;
    }

    // 6. Royal Combo
    private boolean checkRoyalCombo(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
        boolean knightPlayed = prev.card.getName().equals("Knight") || curr.getName().equals("Knight");
        boolean archersPlayed = prev.card.getName().equals("Archers") || curr.getName().equals("Archers");

        if (knightPlayed && archersPlayed) {
            // Find the Knight instance
            List<ICombatant> knightUnits = new ArrayList<>();
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            for (ICombatant u : allUnits) {
                // How to check name from unit? Unit doesn't have name field usually, Card does.
                // Troop has getBaseCard().
                if (u instanceof com.kuroyale.model.entities.Troop) {
                    if (((com.kuroyale.model.entities.Troop) u).getBaseCard().getName().equals("Knight")) {
                        ((com.kuroyale.model.entities.Troop) u).heal(100);
                        knightUnits.add(u);
                    }
                }
            }

            if (!knightUnits.isEmpty()) {
                triggerCombo(ComboType.ROYAL_COMBO, knightUnits);
                return true;
            }
        }
        return false;
    }

    // 7. Siege Mode
    private boolean checkSiegeMode(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
        boolean mortarPlayed = prev.card.getName().equals("Mortar") || curr.getName().equals("Mortar");
        boolean defensePlayed = isDefensiveBuilding(prev.card) || isDefensiveBuilding(curr);

        // Ensure they are different cards or just satisfy the condition
        if (mortarPlayed && defensePlayed) {
            List<ICombatant> allUnits = new ArrayList<>(prev.spawnedUnits);
            allUnits.addAll(currUnits);

            List<ICombatant> mortars = new ArrayList<>();
            for (ICombatant u : allUnits) {
                if (u instanceof com.kuroyale.model.entities.Building) {
                    com.kuroyale.model.entities.Building b = (com.kuroyale.model.entities.Building) u;
                    // Check if it corresponds to Mortar (via image path or similar?)
                    // Building doesn't keep Card reference easily visible in all versions.
                    // But we know which batch it came from.
                    boolean fromMortarCard = false;
                    // We can infer from the event batch
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
                triggerCombo(ComboType.SIEGE_MODE, mortars);
                return true;
            }
        }
        return false;
    }

    // 8. Rush Attack
    private boolean checkRushAttack(PlayedCardEvent prev, Card curr, List<ICombatant> currUnits) {
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
                triggerCombo(ComboType.RUSH_ATTACK, hogs);
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
