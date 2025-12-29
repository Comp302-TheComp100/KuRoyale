package com.kuroyale.view.battle;

import com.kuroyale.model.logic.GameState;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.enums.UnitState;
import com.kuroyale.model.entities.CombatStats;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class TroopRenderer {
    private static final int TILE_SIZE = 18;

    private final Pane unitLayer;
    private final BiFunction<Integer, Integer, Node> gridCellProvider;

    // Tracking maps
    private final Map<Troop, Node> activeTroopVisuals = new HashMap<>();
    private final Map<Troop, HealthBarVisual> troopHealthBars = new HashMap<>(); // Pooled health bars
    private final Map<Troop, Node> activeProjectiles = new HashMap<>();
    private final Map<Troop, Point2D> lastTroopPositions = new HashMap<>();
    private final Map<Troop, String> lastTroopState = new HashMap<>();

    // Reusable set to avoid per-frame allocation
    private final Set<Troop> currentTroops = new HashSet<>();

    public TroopRenderer(Pane unitLayer, BiFunction<Integer, Integer, Node> gridCellProvider) {
        this.unitLayer = unitLayer;
        this.gridCellProvider = gridCellProvider;
    }

    public void render(GameState gameState) {
        List<Troop> troops = gameState.getActiveTroops();

        // Reuse the set instead of creating a new one every frame
        currentTroops.clear();
        currentTroops.addAll(troops);

        // Cleanup dead troops
        cleanupDeadTroops(currentTroops);

        // Cleanup stale projectiles
        cleanupStaleProjectiles(currentTroops);

        // Render active troops
        for (Troop troop : troops) {
            renderTroop(troop, gameState);
        }
    }

    /**
     * Render method for PvP mode (uses PvPGameState).
     */
    public void renderPvP(com.kuroyale.model.logic.PvPGameState pvpGameState) {
        List<Troop> troops = pvpGameState.getActiveTroops();

        currentTroops.clear();
        currentTroops.addAll(troops);

        cleanupDeadTroops(currentTroops);
        cleanupStaleProjectiles(currentTroops);

        for (Troop troop : troops) {
            renderTroopPvP(troop, pvpGameState);
        }
    }

    private void renderTroopPvP(Troop troop, com.kuroyale.model.logic.PvPGameState pvpGameState) {
        com.kuroyale.model.entities.Vector2 worldPos = troop.getWorldPosition();
        if (worldPos == null)
            return;

        double visualX = (worldPos.getX() - 0.5) * TILE_SIZE;
        double visualY = (worldPos.getY() - 0.5) * TILE_SIZE;

        GridPosition gridPos = troop.getPosition();
        Node cellNode = gridPos != null ? gridCellProvider.apply(gridPos.getX(), gridPos.getY()) : null;
        Bounds cellBounds = cellNode != null ? cellNode.getBoundsInParent()
                : new javafx.geometry.BoundingBox(visualX, visualY, TILE_SIZE, TILE_SIZE);

        lastTroopPositions.put(troop, new Point2D(visualX, visualY));

        Node unitNode = activeTroopVisuals.get(troop);
        String cardName = troop.getBaseCard().getName();
        boolean needsCreation = (unitNode == null);

        if (needsCreation) {
            unitNode = createTroopVisualPvP(troop, cardName);
            unitLayer.getChildren().add(unitNode);
            activeTroopVisuals.put(troop, unitNode);
        }

        unitNode.setLayoutX(visualX);
        unitNode.setLayoutY(visualY);

        renderHealthBar(troop, visualX, visualY, cellBounds);
        renderProjectilePvP(troop, visualX, visualY, cellBounds, pvpGameState);
    }

    private Node createTroopVisualPvP(Troop troop, String cardName) {
        try {
            String imgPath = troop.getBaseCard().getImagePath();
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream(imgPath));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(TILE_SIZE);
            iv.setFitHeight(TILE_SIZE);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            Circle fallback = new Circle(TILE_SIZE / 2.5);
            fallback.setFill(troop.isPlayerSide() ? (troop.isAirUnit() ? Color.DODGERBLUE : Color.BLUE)
                    : (troop.isAirUnit() ? Color.ORANGERED : Color.RED));
            return fallback;
        }
    }

    private void renderProjectilePvP(Troop troop, double visualX, double visualY, Bounds cellBounds,
            com.kuroyale.model.logic.PvPGameState pvpGameState) {
        double centerX = visualX + cellBounds.getWidth() / 2.0;
        double centerY = visualY + cellBounds.getHeight() / 2.0;

        if (troop.getUnitState() == UnitState.ATTACKING &&
                troop.getCombatStats() != null &&
                troop.getCombatStats().getAttackType() == CombatStats.AttackType.RANGED) {

            com.kuroyale.model.entities.ICombatant target = troop.getTarget();
            if (target != null && target.isAlive()) {
                GridPosition targetPos = target.getCenterPosition();
                Node targetNode = targetPos != null ? gridCellProvider.apply(targetPos.getX(), targetPos.getY()) : null;

                if (targetNode != null) {
                    Bounds tb = targetNode.getBoundsInParent();
                    double tx = tb.getMinX() + tb.getWidth() / 2.0;
                    double ty = tb.getMinY() + tb.getHeight() / 2.0;

                    double duration = Math.max(0.15, troop.getCombatStats().getHitSpeedSeconds());
                    double cooldown = troop.getAttackCooldown();
                    double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));

                    double px = centerX + (tx - centerX) * phase;
                    double py = centerY + (ty - centerY) * phase;

                    Node projNode = activeProjectiles.get(troop);
                    if (projNode == null) {
                        Circle dot = new Circle(2.5);
                        dot.setFill(troop.isPlayerSide() ? Color.YELLOW : Color.ORANGE);
                        dot.setStroke(Color.color(0, 0, 0, 0.35));
                        dot.setStrokeWidth(0.8);
                        projNode = dot;
                        unitLayer.getChildren().add(projNode);
                        activeProjectiles.put(troop, projNode);
                    }
                    projNode.setLayoutX(px);
                    projNode.setLayoutY(py);
                    if (!unitLayer.getChildren().contains(projNode)) {
                        unitLayer.getChildren().add(projNode);
                    }
                }
            }
        }
    }

    private void cleanupDeadTroops(Set<Troop> currentTroops) {
        Iterator<Map.Entry<Troop, Node>> troopIt = activeTroopVisuals.entrySet().iterator();
        while (troopIt.hasNext()) {
            Map.Entry<Troop, Node> entry = troopIt.next();
            Troop t = entry.getKey();
            if (!currentTroops.contains(t)) {
                // Remove visual
                Node node = entry.getValue();
                if (node instanceof AnimatedSprite) {
                    ((AnimatedSprite) node).stop();
                }
                unitLayer.getChildren().remove(node);

                // Remove pooled health bar
                HealthBarVisual hpBar = troopHealthBars.remove(t);
                if (hpBar != null) {
                    unitLayer.getChildren().remove(hpBar.background);
                    unitLayer.getChildren().remove(hpBar.foreground);
                }

                // Remove projectile
                if (activeProjectiles.containsKey(t)) {
                    unitLayer.getChildren().remove(activeProjectiles.get(t));
                    activeProjectiles.remove(t);
                }

                troopIt.remove();
                lastTroopPositions.remove(t);
                lastTroopState.remove(t);
            }
        }
    }

    private void cleanupStaleProjectiles(Set<Troop> currentTroops) {
        Iterator<Map.Entry<Troop, Node>> projIt = activeProjectiles.entrySet().iterator();
        while (projIt.hasNext()) {
            Map.Entry<Troop, Node> entry = projIt.next();
            Troop t = entry.getKey();
            if (!currentTroops.contains(t) || t.getUnitState() != UnitState.ATTACKING) {
                unitLayer.getChildren().remove(entry.getValue());
                projIt.remove();
            }
        }
    }

    private void renderTroop(Troop troop, GameState gameState) {
        com.kuroyale.model.entities.Vector2 worldPos = troop.getWorldPosition();
        if (worldPos == null)
            return;

        // Calculate visual position (subtract 0.5 to get top-left as Vector2 is
        // centered)
        double visualX = (worldPos.getX() - 0.5) * TILE_SIZE;
        double visualY = (worldPos.getY() - 0.5) * TILE_SIZE;

        // Get cell bounds or default bounds for HUD alignment
        GridPosition gridPos = troop.getPosition();
        Node cellNode = gridPos != null ? gridCellProvider.apply(gridPos.getX(), gridPos.getY()) : null;
        Bounds cellBounds = cellNode != null ? cellNode.getBoundsInParent()
                : new javafx.geometry.BoundingBox(visualX, visualY, TILE_SIZE, TILE_SIZE);

        lastTroopPositions.put(troop, new Point2D(visualX, visualY));

        // Create or update troop visual
        Node unitNode = activeTroopVisuals.get(troop);
        String cardName = troop.getBaseCard().getName();
        boolean needsCreation = (unitNode == null);

        if (needsCreation) {
            unitNode = createTroopVisual(troop, cardName, gameState);
            unitLayer.getChildren().add(unitNode);
            activeTroopVisuals.put(troop, unitNode);
        }

        unitNode.setLayoutX(visualX);
        unitNode.setLayoutY(visualY);

        renderHealthBar(troop, visualX, visualY, cellBounds);
        renderProjectile(troop, visualX, visualY, cellBounds, gameState);
    }

    private Node createTroopVisual(Troop troop, String cardName, GameState gameState) {

        try {
            String imgPath = troop.getBaseCard().getImagePath();
            javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream(imgPath));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setFitWidth(TILE_SIZE);
            iv.setFitHeight(TILE_SIZE);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        } catch (Exception e) {
            Circle fallback = new Circle(TILE_SIZE / 2.5);
            fallback.setFill(troop.isPlayerSide() ? (troop.isAirUnit() ? Color.DODGERBLUE : Color.BLUE)
                    : (troop.isAirUnit() ? Color.ORANGERED : Color.RED));
            return fallback;
        }
    }

    private void renderHealthBar(Troop troop, double visualX, double visualY, Bounds cellBounds) {
        double maxHp = troop.getBaseCard().getHp();
        double curHp = Math.max(0, troop.getCurrentHealth());
        double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;

        HealthBarVisual hpBar = troopHealthBars.get(troop);
        if (hpBar == null) {
            hpBar = new HealthBarVisual();
            troopHealthBars.put(troop, hpBar);
            unitLayer.getChildren().addAll(hpBar.background, hpBar.foreground);
        }
        hpBar.update(visualX, visualY, cellBounds.getWidth(), cellBounds.getHeight(), pct, troop.isPlayerSide());
    }

    private void renderProjectile(Troop troop, double visualX, double visualY, Bounds cellBounds, GameState gameState) {
        double centerX = visualX + cellBounds.getWidth() / 2.0;
        double centerY = visualY + cellBounds.getHeight() / 2.0;

        if (troop.getUnitState() == UnitState.ATTACKING &&
                troop.getCombatStats() != null &&
                troop.getCombatStats().getAttackType() == CombatStats.AttackType.RANGED) {

            com.kuroyale.model.entities.ICombatant target = troop.getTarget();
            if (target != null && target.isAlive()) {
                GridPosition targetPos = target.getCenterPosition();
                Node targetNode = targetPos != null ? gridCellProvider.apply(targetPos.getX(), targetPos.getY()) : null;

                if (targetNode != null) {
                    Bounds tb = targetNode.getBoundsInParent();
                    double tx = tb.getMinX() + tb.getWidth() / 2.0;
                    double ty = tb.getMinY() + tb.getHeight() / 2.0;

                    double duration = Math.max(0.15, troop.getCombatStats().getHitSpeedSeconds());
                    double cooldown = troop.getAttackCooldown();
                    double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));

                    double px = centerX + (tx - centerX) * phase;
                    double py = centerY + (ty - centerY) * phase;

                    Node projNode = activeProjectiles.get(troop);
                    if (projNode == null) {
                        Circle dot = new Circle(2.5);
                        dot.setFill(troop.isPlayerSide() ? Color.YELLOW : Color.ORANGE);
                        dot.setStroke(Color.color(0, 0, 0, 0.35));
                        dot.setStrokeWidth(0.8);
                        projNode = dot;
                        unitLayer.getChildren().add(projNode);
                        activeProjectiles.put(troop, projNode);
                    }
                    projNode.setLayoutX(px);
                    projNode.setLayoutY(py);
                    if (!unitLayer.getChildren().contains(projNode)) {
                        unitLayer.getChildren().add(projNode);
                    }
                }
            }
        }
    }

    // Inner class for pooled health bar visuals
    private static class HealthBarVisual {
        final Rectangle background;
        final Rectangle foreground;

        HealthBarVisual() {
            double barWidth = TILE_SIZE * 0.9;
            double barHeight = 4;

            background = new Rectangle(barWidth, barHeight);
            background.setFill(Color.color(0.2, 0.2, 0.2, 0.8));
            background.setStroke(Color.BLACK);
            background.setStrokeWidth(0.3);

            foreground = new Rectangle(barWidth, barHeight);
            foreground.setFill(Color.LIMEGREEN);
        }

        void update(double visualX, double visualY, double cellWidth, double cellHeight,
                double healthPct, boolean isPlayerSide) {
            double barWidth = TILE_SIZE * 0.9;
            double centerX = visualX + cellWidth / 2.0;
            double centerY = visualY + cellHeight / 2.0;

            background.setX(centerX - barWidth / 2.0);
            background.setY(centerY - (TILE_SIZE / 2.5) - 6);

            foreground.setWidth(barWidth * healthPct);
            foreground.setX(background.getX());
            foreground.setY(background.getY());

            if (isPlayerSide) {
                foreground.setFill(Color.ROYALBLUE);
            } else {
                foreground.setFill(Color.CRIMSON);
            }
        }
    }
}
