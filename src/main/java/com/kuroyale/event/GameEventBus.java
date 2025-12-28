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

    public void publishCardPlayed(boolean isPlayer, Card card) {
        // Create a copy to avoid ConcurrentModificationException if a listener unsubscribes during notify
        new ArrayList<>(listeners).forEach(l -> l.onCardPlayed(isPlayer, card));
    }

    public void publishTowerDestroyed(boolean isPlayerTower, Tower tower) {
        new ArrayList<>(listeners).forEach(l -> l.onTowerDestroyed(isPlayerTower, tower));
    }

    public void publishElixirSpent(boolean isPlayer, int amount) {
        new ArrayList<>(listeners).forEach(l -> l.onElixirSpent(isPlayer, amount));
    }

    public void publishAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.GridPosition center,
            double radius, double duration) {
        new ArrayList<>(listeners).forEach(l -> l.onAreaEffect(isPlayerSource, center, radius, duration));
    }
}