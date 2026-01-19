package com.kuroyale.view.battle.effects;

/**
 * Tracks active visual effects with their remaining duration.
 * Shared across all effect classes.
 */
public class ActiveSpellVisual {
    public final javafx.scene.Node node;
    public double timeRemaining;

    public ActiveSpellVisual(javafx.scene.Node node, double timeRemaining) {
        this.node = node;
        this.timeRemaining = timeRemaining;
    }
}
