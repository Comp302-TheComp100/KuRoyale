package com.kuroyale.event;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Tower;
import java.util.ArrayList;
import java.util.List;

/* Event Bus for broadcasting game events to registered listeners.
 * Facilitates loose coupling between the game engine (GameState) and
 * auxiliary systems like Quests and Achievements.*/
public class GameEventBus {
    private static GameEventBus instance;
    private final List<GameEventListener> listeners = new ArrayList<>();

    private GameEventBus() {
    }

    public static synchronized GameEventBus getInstance() {
        if (instance == null) {
            instance = new GameEventBus();
        }
        return instance;
    }

    public void subscribe(GameEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unsubscribe(GameEventListener listener) {
        listeners.remove(listener);
    }

    public void publishCardPlayed(boolean isPlayer, Card card,
            java.util.List<com.kuroyale.model.entities.ICombatant> spawnedUnits) {
        // Create a copy to avoid ConcurrentModificationException if a listener
        // unsubscribes during notify
        new ArrayList<>(listeners).forEach(l -> l.onCardPlayed(isPlayer, card, spawnedUnits));
    }

    public void publishSpellCast(boolean isPlayer, Card spell, com.kuroyale.model.entities.GridPosition center) {
        new ArrayList<>(listeners).forEach(l -> l.onSpellCast(isPlayer, spell, center));
    }

    public void publishTowerDestroyed(boolean isPlayerTower, Tower tower) {
        new ArrayList<>(listeners).forEach(l -> l.onTowerDestroyed(isPlayerTower, tower));
    }

    public void publishElixirSpent(boolean isPlayer, int amount) {
        new ArrayList<>(listeners).forEach(l -> l.onElixirSpent(isPlayer, amount));
    }

    public void publishBuildingProduction(com.kuroyale.model.entities.Building building, String resource, int amount) {
        new ArrayList<>(listeners).forEach(l -> l.onBuildingProduction(building, resource, amount));
    }

    public void publishAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.Vector2 center,
            double radius, double duration, String effectType) {
        new ArrayList<>(listeners).forEach(l -> l.onAreaEffect(isPlayerSource, center, radius, duration, effectType));
    }

    public void publishAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.GridPosition center,
            double radius, double duration, String effectType) {
        publishAreaEffect(isPlayerSource, com.kuroyale.model.entities.Vector2.fromGridPosition(center), radius,
                duration,
                effectType);
    }

    public void publishComboTriggered(com.kuroyale.model.enums.ComboType combo,
            java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
        new ArrayList<>(listeners).forEach(l -> l.onComboTriggered(combo, affectedUnits));
    }

    public void publishTowerDamaged(Tower tower) {
        new ArrayList<>(listeners).forEach(l -> l.onTowerDamaged(tower));
    }

    public void publishSpellDamageDealt(boolean isPlayer, int damage) {
        new ArrayList<>(listeners).forEach(l -> l.onSpellDamageDealt(isPlayer, damage));
    }<<<<<<<

    Updated upstream=======

    public void publishMatchStart() {
        new ArrayList<>(listeners).forEach(l -> l.onMatchStart());
    }

    public void publishMatchEnd(boolean playerWon) {
        new ArrayList<>(listeners).forEach(l -> l.onMatchEnd(playerWon));
    }

    public void publishEmojiPlayed(boolean isPlayer, String emojiName) {
        new ArrayList<>(listeners).forEach(l -> l.onEmojiPlayed(isPlayer, emojiName));
    }>>>>>>>

    Stashed changes
}