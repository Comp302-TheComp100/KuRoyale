package com.kuroyale.view;

import com.kuroyale.model.ICombatant;
import com.kuroyale.model.Troop;
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
    private final java.util.function.BiFunction<Integer, Integer, Node> gridCellProvider;
    private final int TILE_SIZE;

    public ProjectileRenderer(Pane unitLayer, java.util.function.BiFunction<Integer, Integer, Node> gridCellProvider,
            int tileSize) {
        this.unitLayer = unitLayer;
        this.gridCellProvider = gridCellProvider;
        this.TILE_SIZE = tileSize;
    }

    public void render(List<? extends ICombatant> combatants) {
        Set<ICombatant> currentAttackers = new HashSet<>();

        for (ICombatant combatant : combatants) {
            if (!combatant.isAlive())
                continue;

            Troop target = combatant.getTarget();
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

    private void renderProjectile(ICombatant attacker, Troop target) {
        // Calculate Phase based on cooldown
        // Duration of attack animation (approximate)
        double duration = Math.max(0.15, attacker.getHitSpeed());

        // Cooldown counts down from Max to 0.
        // When Cooldown = HitSpeed, attack starts?
        // Logic in GameState:
        // if (cd <= 0) { setAttackCooldown(HitSpeed); FIRE }
        // So cooldown goes HitSpeed -> 0.
        // Phase 0.0 (start) -> 1.0 (hit)
        // Phase = 1.0 - (cooldown / duration)
        // Note: AttackCooldown might be higher than HitSpeed if logic differs, but
        // usually it resets to HitSpeed.

        double cooldown = attacker.getAttackCooldown();
        // Clamp to valid range
        double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));

        // Get Positions
        // Attacker Center
        com.kuroyale.model.GridPosition startPos = attacker.getCenterPosition();
        Node startTimeNode = gridCellProvider.apply(startPos.getX(), startPos.getY());
        if (startTimeNode == null)
            return;

        javafx.geometry.Bounds startBounds = startTimeNode.getBoundsInParent();
        double sx = startBounds.getMinX() + startBounds.getWidth() / 2.0;
        double sy = startBounds.getMinY() + startBounds.getHeight() / 2.0;

        // Target Center
        com.kuroyale.model.GridPosition targetPos = target.getPosition();
        if (targetPos == null)
            return;

        Node targetNode = gridCellProvider.apply(targetPos.getX(), targetPos.getY());
        // If target node is null (off grid?), maybe use target.getWorldPosition?
        // Fallback to calculation if Node is null
        double tx, ty;

        if (targetNode != null) {
            javafx.geometry.Bounds targetBounds = targetNode.getBoundsInParent();
            tx = targetBounds.getMinX() + targetBounds.getWidth() / 2.0;
            ty = targetBounds.getMinY() + targetBounds.getHeight() / 2.0;
        } else {
            // Fallback calculation from grid coordinates
            tx = targetPos.getX() * TILE_SIZE + TILE_SIZE / 2.0;
            ty = targetPos.getY() * TILE_SIZE + TILE_SIZE / 2.0;
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

        // Ensure strictly on top? Or UnitLayer is enough.
        if (!unitLayer.getChildren().contains(projNode)) {
            unitLayer.getChildren().add(projNode);
        }
    }
}
