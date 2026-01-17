package com.kuroyale.view.battle;

import com.kuroyale.model.logic.GameState;
import com.kuroyale.model.entities.Troop;
import com.kuroyale.model.enums.UnitState;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TroopRenderer {
    private static final int TILE_SIZE = com.kuroyale.util.GameConstants.TILE_SIZE;

    private final Pane unitLayer;

    // Tracking maps
    private final Map<Troop, Node> activeTroopVisuals = new HashMap<>();
    private final Map<Troop, HealthBarVisual> troopHealthBars = new HashMap<>(); // Pooled health bars
    private final Map<Troop, Point2D> lastTroopPositions = new HashMap<>();
    private final Map<Troop, String> lastTroopState = new HashMap<>();

    // Reusable set to avoid per-frame allocation
    private final Set<Troop> currentTroops = new HashSet<>();

    public TroopRenderer(Pane unitLayer) {
        this.unitLayer = unitLayer;
    }

    private static double getVisualScale(Troop troop) {
        if (troop == null || troop.getBaseCard() == null) {
            return 1.0;
        }
        String cardKey = PngSequenceSprite.toCardKey(troop.getBaseCard().getName());
        if ("skeletons".equals(cardKey) || "barbarians".equals(cardKey)) {
            return 2.75*1.25;
        }else if("archers".equals(cardKey) ){
            return 1.5*1.25;
        }else if("bomber".equals(cardKey) || "mini_pekka".equals(cardKey) || "hog_rider".equals(cardKey) || "giant".equals(cardKey)){
            return 2.0*1.25;
        }else if("goblins".equals(cardKey) || "knight".equals(cardKey)){
            return 2.5*1.25;
        }
        return 1.0;
    }

    private static double getVisualWidthTiles(Troop troop) {
        if (troop == null || troop.getBaseCard() == null) {
            return 1.0;
        }
        double base = troop.getBaseCard().getWidth();
        if (base <= 0) {
            base = 1.0;
        }
        return base * getVisualScale(troop);
    }

    private static double getVisualHeightTiles(Troop troop) {
        if (troop == null || troop.getBaseCard() == null) {
            return 1.0;
        }
        double base = troop.getBaseCard().getHeight();
        if (base <= 0) {
            base = 1.0;
        }
        return base * getVisualScale(troop);
    }

    private static String getSideFolder(boolean isPlayerSide) {
        return isPlayerSide ? "player" : "enemy";
    }

    private static String getTroopActionFolder(UnitState state) {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case MOVING -> "walk";
            case ATTACKING -> "attack";
            default -> null;
        };
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public void render(GameState gameState) {
        List<Troop> troops = gameState.getActiveTroops();

        // Reuse the set instead of creating a new one every frame
        currentTroops.clear();
        currentTroops.addAll(troops);

        // Cleanup dead troops
        cleanupDeadTroops(currentTroops);

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

        for (Troop troop : troops) {
            renderTroopPvP(troop, pvpGameState);
        }
    }

    private void renderTroopPvP(Troop troop, com.kuroyale.model.logic.PvPGameState pvpGameState) {
        com.kuroyale.model.entities.Vector2 worldPos = troop.getWorldPosition();
        if (worldPos == null)
            return;

        // Get sprite size from card
        double spriteW = getVisualWidthTiles(troop) * TILE_SIZE;
        double spriteH = getVisualHeightTiles(troop) * TILE_SIZE;

        // Center sprite on world position
        double visualX = worldPos.getX() * TILE_SIZE - spriteW / 2;
        double visualY = worldPos.getY() * TILE_SIZE - spriteH / 2;

        lastTroopPositions.put(troop, new Point2D(visualX, visualY));

        Node unitNode = activeTroopVisuals.get(troop);
        String cardName = troop.getBaseCard().getName();
        boolean needsCreation = (unitNode == null);

        if (needsCreation) {
            unitNode = createTroopVisualPvP(troop, cardName);
            unitLayer.getChildren().add(unitNode);
            activeTroopVisuals.put(troop, unitNode);
        }

        updateTroopAnimationIfNeeded(troop, unitNode);

        unitNode.setLayoutX(visualX);
        unitNode.setLayoutY(visualY);

        renderHealthBar(troop, visualX, visualY, spriteW, spriteH);
    }

    private Node createTroopVisualPvP(Troop troop, String cardName) {
        // Get sprite size from card
        double spriteW = getVisualWidthTiles(troop) * TILE_SIZE;
        double spriteH = getVisualHeightTiles(troop) * TILE_SIZE;

        String fallbackImagePath = troop.getBaseCard().getImagePath();
        PngSequenceSprite sprite = new PngSequenceSprite(null, spriteW, spriteH, fallbackImagePath);
        lastTroopState.put(troop, null);
        return sprite;
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
                if (node instanceof PngSequenceSprite) {
                    ((PngSequenceSprite) node).stop();
                }
                unitLayer.getChildren().remove(node);

                // Remove pooled health bar
                HealthBarVisual hpBar = troopHealthBars.remove(t);
                if (hpBar != null) {
                    unitLayer.getChildren().remove(hpBar.getRoot());
                }

                troopIt.remove();
                lastTroopPositions.remove(t);
                lastTroopState.remove(t);
            }
        }
    }

    private void renderTroop(Troop troop, GameState gameState) {
        com.kuroyale.model.entities.Vector2 worldPos = troop.getWorldPosition();
        if (worldPos == null)
            return;

        // Get sprite size from card
        double spriteW = getVisualWidthTiles(troop) * TILE_SIZE;
        double spriteH = getVisualHeightTiles(troop) * TILE_SIZE;

        // Center sprite on world position
        double visualX = worldPos.getX() * TILE_SIZE - spriteW / 2;
        double visualY = worldPos.getY() * TILE_SIZE - spriteH / 2;

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

        updateTroopAnimationIfNeeded(troop, unitNode);

        unitNode.setLayoutX(visualX);
        unitNode.setLayoutY(visualY);

        renderHealthBar(troop, visualX, visualY, spriteW, spriteH);
    }

    private Node createTroopVisual(Troop troop, String cardName, GameState gameState) {
        // Get sprite size from card
        double spriteW = getVisualWidthTiles(troop) * TILE_SIZE;
        double spriteH = getVisualHeightTiles(troop) * TILE_SIZE;

        String fallbackImagePath = troop.getBaseCard().getImagePath();
        PngSequenceSprite sprite = new PngSequenceSprite(null, spriteW, spriteH, fallbackImagePath);
        lastTroopState.put(troop, null);
        return sprite;
    }

    private void updateTroopAnimationIfNeeded(Troop troop, Node unitNode) {
        if (!(unitNode instanceof PngSequenceSprite sprite)) {
            return;
        }

        UnitState state = troop.getUnitState();
        String action = getTroopActionFolder(state);
        String side = getSideFolder(troop.isPlayerSide());

        String newStateKey;
        if (action == null) {
            newStateKey = "static";
        } else {
            String cardKey = PngSequenceSprite.toCardKey(troop.getBaseCard().getName());
            newStateKey = cardKey + ":" + side + ":" + action;
        }

        String lastKey = lastTroopState.get(troop);
        if (newStateKey.equals(lastKey)) {
            return;
        }
        lastTroopState.put(troop, newStateKey);

        sprite.setFallbackImagePath(troop.getBaseCard().getImagePath());

        if (action == null) {
            sprite.updateSequence(null);
            return;
        }

        String cardKey = PngSequenceSprite.toCardKey(troop.getBaseCard().getName());
        String baseFolder = "/images/animations/troops/" + cardKey + "/" + side + "/" + action;

        sprite.setLoop(true);

        if ("attack".equals(action)) {
            double hitSpeed = troop.getHitSpeed();
            sprite.setLoopDurationSeconds(clamp(hitSpeed, 0.25, 3.5));
        } else {
            double move = troop.getMoveSpeed();
            double walkLoop = 1.6 / Math.max(0.1, move);
            sprite.setLoopDurationSeconds(clamp(walkLoop, 0.6, 1.8));
        }

        sprite.updateSequence(baseFolder);
    }

    private void renderHealthBar(Troop troop, double visualX, double visualY, double spriteW, double spriteH) {
        double maxHp = troop.getBaseCard().getHp();
        double curHp = Math.max(0, troop.getCurrentHealth());
        double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;

        HealthBarVisual hpBar = troopHealthBars.get(troop);
        if (hpBar == null) {
            hpBar = new HealthBarVisual();
            troopHealthBars.put(troop, hpBar);
            unitLayer.getChildren().add(hpBar.getRoot());
        }
        hpBar.update(visualX, visualY, spriteW, spriteH, pct, troop.isPlayerSide());
    }

    // Inner class for pooled health bar visuals
    private static class HealthBarVisual {
        final HealthBarRenderer.HealthBarNodes nodes;

        HealthBarVisual() {
            double barWidth = com.kuroyale.util.GameConstants.TILE_SIZE * 0.9;
            double barHeight = com.kuroyale.util.GameConstants.HEALTH_BAR_HEIGHT;

            this.nodes = HealthBarRenderer.createSimpleHealthBar(barWidth, barHeight);
        }

        // Expose underlying nodes for addition/removal
        public Node getRoot() {
            return nodes.root;
        }

        void update(double visualX, double visualY, double cellWidth, double cellHeight,
                double healthPct, boolean isPlayerSide) {

            double barWidth = com.kuroyale.util.GameConstants.TILE_SIZE * 0.9;
            double centerX = visualX + cellWidth / 2.0;
            double centerY = visualY + cellHeight / 2.0;

            nodes.root.setLayoutX(centerX - barWidth / 2.0);
            nodes.root.setLayoutY(centerY - (com.kuroyale.util.GameConstants.TILE_SIZE / 2.5) - 6);

            HealthBarRenderer.updateHealthBar(nodes, healthPct, 1.0, isPlayerSide);
        }
    }
}
