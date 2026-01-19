package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Projectile;
import com.kuroyale.model.entities.Vector2;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProjectileRenderer {
    private final Pane unitLayer;
    // Map logical projectile to visual node
    private final Map<Projectile, Node> visualMap = new HashMap<>();
    private final int TILE_SIZE;

    public ProjectileRenderer(Pane unitLayer, int tileSize) {
        this.unitLayer = unitLayer;
        this.TILE_SIZE = tileSize;
    }

    public void render(List<Projectile> projectiles, double deltaTime) {
        // Mark checked visuals
        Map<Projectile, Boolean> checked = new HashMap<>();

        for (Projectile p : projectiles) {
            checked.put(p, true);

            Node node = visualMap.get(p);
            if (node == null) {
                // Create new visual
                if (isFireball(p)) {
                    node = com.kuroyale.view.battle.effects.FireballEffect.createProceduralFireball(6.0);
                } else if (isRocket(p)) {
                    node = com.kuroyale.view.battle.effects.RocketEffect.createProceduralRocket(8.0);
                } else {
                    Circle dot = new Circle(3.0);
                    dot.setFill(p.isPlayerSide() ? Color.LIGHTSKYBLUE : Color.ORANGERED);
                    dot.setStroke(Color.color(0, 0, 0, 0.45));
                    dot.setStrokeWidth(1.0);
                    node = dot;
                }

                unitLayer.getChildren().add(node);
                visualMap.put(p, node);
            }

            // Sync Position
            Vector2 pos = p.getPosition();
            if (pos != null) {
                node.setLayoutX(pos.getX() * TILE_SIZE);
                node.setLayoutY(pos.getY() * TILE_SIZE);
            }

            // Rotate rocket based on direction (enemy rockets face down towards player)
            if (isRocket(p)) {
                // Player rockets go up (0 degrees), enemy rockets go down (180 degrees)
                node.setRotate(p.isPlayerSide() ? 0 : 180);
            }

            // Ensure visibility
            if (!unitLayer.getChildren().contains(node)) {
                unitLayer.getChildren().add(node);
            }
        }

        // Cleanup removed projectiles
        Iterator<Map.Entry<Projectile, Node>> it = visualMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, Node> entry = it.next();
            if (!checked.containsKey(entry.getKey())) {
                unitLayer.getChildren().remove(entry.getValue());
                it.remove();
            }
        }
    }

    private boolean isFireball(Projectile p) {
        if (p.getSourceCard() != null) {
            String name = p.getSourceCard().getName();
            return "Fireball".equalsIgnoreCase(name) || "Baby Dragon".equalsIgnoreCase(name)
                    || "Wizard".equalsIgnoreCase(name) || "Witch".equalsIgnoreCase(name);
        }
        if (p.getOwner() instanceof com.kuroyale.model.entities.Troop) {
            com.kuroyale.model.entities.Troop t = (com.kuroyale.model.entities.Troop) p.getOwner();
            String name = t.getBaseCard().getName();
            return "Wizard".equalsIgnoreCase(name) || "Fireball".equalsIgnoreCase(name)
                    || "Baby Dragon".equalsIgnoreCase(name) || "Witch".equalsIgnoreCase(name);
        }
        return false;
    }

    private boolean isRocket(Projectile p) {
        if (p.getSourceCard() != null) {
            String name = p.getSourceCard().getName();
            return "Rocket".equalsIgnoreCase(name);
        }
        return false;
    }

}
