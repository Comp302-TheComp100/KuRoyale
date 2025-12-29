package com.kuroyale.view.battle;

import com.kuroyale.model.entities.ICombatant;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

public class ProjectileRenderer {
    private final Pane unitLayer;
    private final Map<ICombatant, Node> activeProjectiles = new HashMap<>();
    private final int TILE_SIZE;

    public ProjectileRenderer(Pane unitLayer, int tileSize) {
        this.unitLayer = unitLayer;
        this.TILE_SIZE = tileSize;
    }

    // Reusable set to avoid per-frame allocation
    private final Set<ICombatant> currentAttackers = new HashSet<>();

    public void render(List<? extends ICombatant> combatants) {
        currentAttackers.clear();

        for (ICombatant combatant : combatants) {
            if (!combatant.isAlive())
                continue;

            ICombatant target = combatant.getTarget();
            if (target != null && target.isAlive()) {
                currentAttackers.add(combatant);
                renderProjectile(combatant, target);
            }
        }

        // Cleanup inactive projectiles
        cleanup(currentAttackers);
    }

    private void cleanup(Set<ICombatant> currentAttackers) {
        activeProjectiles.entrySet().removeIf(entry -> {
            if (!currentAttackers.contains(entry.getKey())) {
                unitLayer.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void renderProjectile(ICombatant attacker, ICombatant target) {
        // Calculate costs less than layout lookups
        double duration = Math.max(0.15, attacker.getHitSpeed());
        double cooldown = attacker.getAttackCooldown();
        double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));

        // Get Positions using Math (No layout bounds lookups)
        // Attacker Center
        com.kuroyale.model.entities.GridPosition startPos = attacker.getCenterPosition();
        // Since getCenterPosition returns a grid coordinate (potentially fractional if
        // we had it, but mostly integer),
        // we can assume it maps to tile coordinates.
        // However, ICombatant.getCenterPosition() usually returns the logic grid pos.
        // Let's rely on standard logic: (x + 0.5) * TILE_SIZE

        double sx = (startPos.getX() + 0.5) * TILE_SIZE;
        double sy = (startPos.getY() + 0.5) * TILE_SIZE;

        // Target Center
        // Target Center - Precise calculation
        com.kuroyale.model.entities.GridPosition targetGridPos = target.getPosition();

        // Use target's center if available or calculate from top-left
        double tx, ty;
        if (targetGridPos != null) {
            tx = (targetGridPos.getX() + target.getWidth() / 2.0) * TILE_SIZE;
            ty = (targetGridPos.getY() + target.getHeight() / 2.0) * TILE_SIZE;
        } else {
            return;
        }

        // Interpolate
        double px = sx + (tx - sx) * phase;
        double py = sy + (ty - sy) * phase;

        // Create or Update Visual
        Node projNode = activeProjectiles.get(attacker);
        if (projNode == null) {
            Circle dot = new Circle(2.5);
            dot.setFill(attacker.isPlayerSide() ? Color.LIGHTSKYBLUE : Color.ORANGERED);
            dot.setStroke(Color.color(0, 0, 0, 0.35));
            dot.setStrokeWidth(0.8);
            projNode = dot;
            unitLayer.getChildren().add(projNode);
            activeProjectiles.put(attacker, projNode);
        }

        projNode.setLayoutX(px);
        projNode.setLayoutY(py);

        if (!unitLayer.getChildren().contains(projNode)) {
            unitLayer.getChildren().add(projNode);
        }
    }
}
