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
    private final Map<ICombatant, VisualProjectile> activeProjectiles = new HashMap<>();
    private final List<VisualProjectile> orphans = new java.util.ArrayList<>();
    private final int TILE_SIZE;

    public ProjectileRenderer(Pane unitLayer, int tileSize) {
        this.unitLayer = unitLayer;
        this.TILE_SIZE = tileSize;
    }

    private static class VisualProjectile {
        final Node node;
        final ICombatant owner;
        double targetX, targetY;
        double lastPhase = 0.0;

        // For orphans
        boolean isOrphan = false;
        double currentX, currentY;
        double speedPixelsPerSec = 0;

        VisualProjectile(Node node, ICombatant owner) {
            this.node = node;
            this.owner = owner;
        }
    }

    public void render(List<? extends ICombatant> combatants, double deltaTime) {
        Set<ICombatant> currentAttackers = new HashSet<>();

        // 1. Update Active Projectiles
        for (ICombatant combatant : combatants) {
            if (!combatant.isAlive())
                continue;

            ICombatant target = combatant.getTarget();
            if (target != null && target.isAlive()) {
                currentAttackers.add(combatant);
                updateActiveProjectile(combatant, target);
            }
        }

        // 2. Detect died/stopped attackers and convert to orphans
        activeProjectiles.entrySet().removeIf(entry -> {
            ICombatant owner = entry.getKey();
            VisualProjectile vp = entry.getValue();

            if (!currentAttackers.contains(owner)) {
                // Was mid-flight? (0.0 < phase < 1.0)
                // Note: The original logic used phase 0.0 as start, 1.0 as hit.
                // We convert to orphan if it was recently active.
                if (vp.lastPhase < 1.0 && vp.lastPhase > 0.0) {
                    convertToOrphan(vp);
                    return true; // Remove from active map
                } else {
                    // Just remove
                    unitLayer.getChildren().remove(vp.node);
                    return true;
                }
            }
            return false;
        });

        // 3. Update Orphans
        updateOrphans(deltaTime);
    }

    // Legacy support if needed, but we should switch BattleArenaView to call the
    // one with deltaTime
    public void render(List<? extends ICombatant> combatants) {
        render(combatants, 0.016); // Fallback
    }

    private void convertToOrphan(VisualProjectile vp) {
        vp.isOrphan = true;
        // Estimate speed based on progress?
        // Or just pick a standard speed. Standard arrow is fast.
        // Let's calculate remaining distance and expected remaining time.
        // But for simplicity, let's say 10 tiles/sec
        vp.speedPixelsPerSec = 10.0 * TILE_SIZE;

        // Capture current position from node layout
        vp.currentX = vp.node.getLayoutX();
        vp.currentY = vp.node.getLayoutY();

        orphans.add(vp);
    }

    private void updateOrphans(double deltaTime) {
        java.util.Iterator<VisualProjectile> it = orphans.iterator();
        while (it.hasNext()) {
            VisualProjectile vp = it.next();

            // Move towards target
            double dx = vp.targetX - vp.currentX;
            double dy = vp.targetY - vp.currentY;
            double dist = Math.sqrt(dx * dx + dy * dy);

            double move = vp.speedPixelsPerSec * deltaTime;

            if (move >= dist) {
                // Hit
                unitLayer.getChildren().remove(vp.node);
                it.remove();
            } else {
                // Normalize and move
                vp.currentX += (dx / dist) * move;
                vp.currentY += (dy / dist) * move;
                vp.node.setLayoutX(vp.currentX);
                vp.node.setLayoutY(vp.currentY);
            }
        }
    }

    private void updateActiveProjectile(ICombatant attacker, ICombatant target) {
        double duration = Math.max(0.15, attacker.getHitSpeed());
        double cooldown = attacker.getAttackCooldown();
        // phase goes 0.0 (start) -> 1.0 (hit)
        double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));

        // Get Positions
        com.kuroyale.model.entities.GridPosition startPos = attacker.getCenterPosition();
        double sx = (startPos.getX() + 0.5) * TILE_SIZE;
        double sy = (startPos.getY() + 0.5) * TILE_SIZE;

        // Target Center or Perimeter? The original used simplistic center/top-left
        // logic.
        // Let's stick to simple center logic for visuals to avoid jumping.
        com.kuroyale.model.entities.GridPosition targetGridPos = target.getPosition();
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
        VisualProjectile vp = activeProjectiles.get(attacker);
        if (vp == null) {
            Circle dot = new Circle(2.5);
            dot.setFill(attacker.isPlayerSide() ? Color.LIGHTSKYBLUE : Color.ORANGERED);
            dot.setStroke(Color.color(0, 0, 0, 0.35));
            dot.setStrokeWidth(0.8);

            unitLayer.getChildren().add(dot);
            vp = new VisualProjectile(dot, attacker);
            activeProjectiles.put(attacker, vp);
        }

        vp.node.setLayoutX(px);
        vp.node.setLayoutY(py);

        // Update state
        vp.targetX = tx;
        vp.targetY = ty;
        vp.lastPhase = phase;
    }
}
