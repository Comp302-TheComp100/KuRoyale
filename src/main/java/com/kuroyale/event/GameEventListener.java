package com.kuroyale.event;

/* Interface for listening to game-related events.
 * Implements the Observer pattern to decouple GameState from secondary services. */
public interface GameEventListener {
        default void onCardPlayed(boolean isPlayer, com.kuroyale.model.entities.Card card,
                        java.util.List<com.kuroyale.model.entities.ICombatant> spawnedUnits) {
        }

        default void onTowerDestroyed(boolean isPlayerTower, com.kuroyale.model.entities.Tower tower) {
        }

        default void onElixirSpent(boolean isPlayer, int amount) {
        }

        default void onBuildingProduction(com.kuroyale.model.entities.Building building, String resource, int amount) {
        }

        default void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.GridPosition center,
                        double radius,
                        double duration) {
        }

        default void onComboTriggered(com.kuroyale.model.enums.ComboType combo,
                        java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
        }

        default void onTowerDamaged(com.kuroyale.model.entities.Tower tower) {
        }
}
