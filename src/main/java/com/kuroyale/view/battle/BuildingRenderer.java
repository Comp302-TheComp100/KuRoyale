package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Building;
import com.kuroyale.model.logic.GameState;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import com.kuroyale.util.GameConstants;
import com.kuroyale.util.GameColors;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BuildingRenderer {

    private static class BuildingVisual {
        final StackPane root;
        final HealthBarRenderer.HealthBarNodes hpNodes;
        final Rectangle elixirForeground;
        final javafx.scene.shape.Line laserBeam;

        BuildingVisual(StackPane root, HealthBarRenderer.HealthBarNodes hpNodes, Rectangle elixirForeground,
                javafx.scene.shape.Line laserBeam) {
            this.root = root;
            this.hpNodes = hpNodes;
            this.elixirForeground = elixirForeground;
            this.laserBeam = laserBeam;
        }
    }

    private final Pane unitLayer;
    private final int TILE_SIZE;
    private final Map<Building, BuildingVisual> activeBuildingVisuals = new HashMap<>();
    private final java.util.function.BiFunction<Integer, Integer, Node> gridCellProvider;

    public BuildingRenderer(Pane unitLayer, int tileSize,
            java.util.function.BiFunction<Integer, Integer, Node> gridCellProvider) {
        this.unitLayer = unitLayer;
        this.TILE_SIZE = tileSize;
        this.gridCellProvider = gridCellProvider;
    }

    public void render(GameState gameState) {
        java.util.List<Building> buildings = gameState.getActiveBuildings();
        Set<Building> currentBuildings = new HashSet<>(buildings);

        // Cleanup visuals for destroyed buildings
        Iterator<Map.Entry<Building, BuildingVisual>> buildingIt = activeBuildingVisuals.entrySet().iterator();
        while (buildingIt.hasNext()) {
            Map.Entry<Building, BuildingVisual> entry = buildingIt.next();
            Building b = entry.getKey();
            if (!currentBuildings.contains(b) || !b.isAlive()) {
                for (Node child : entry.getValue().root.getChildren()) {
                    if (child instanceof PngSequenceSprite) {
                        ((PngSequenceSprite) child).stop();
                    }
                }
                unitLayer.getChildren().remove(entry.getValue().root);
                if (entry.getValue().laserBeam != null) {
                    unitLayer.getChildren().remove(entry.getValue().laserBeam);
                }
                buildingIt.remove();
            }
        }

        // Render Active Buildings
        for (Building b : buildings) {
            renderBuilding(b);
        }
    }

    /**
     * Render method for PvP mode (uses PvPGameState).
     */
    public void renderPvP(com.kuroyale.model.logic.PvPGameState pvpGameState) {
        java.util.List<Building> buildings = pvpGameState.getActiveBuildings();
        Set<Building> currentBuildings = new HashSet<>(buildings);

        // Cleanup visuals for destroyed buildings
        Iterator<Map.Entry<Building, BuildingVisual>> buildingIt = activeBuildingVisuals.entrySet().iterator();
        while (buildingIt.hasNext()) {
            Map.Entry<Building, BuildingVisual> entry = buildingIt.next();
            Building b = entry.getKey();
            if (!currentBuildings.contains(b) || !b.isAlive()) {
                for (Node child : entry.getValue().root.getChildren()) {
                    if (child instanceof PngSequenceSprite) {
                        ((PngSequenceSprite) child).stop();
                    }
                }
                unitLayer.getChildren().remove(entry.getValue().root);
                if (entry.getValue().laserBeam != null) {
                    unitLayer.getChildren().remove(entry.getValue().laserBeam);
                }
                buildingIt.remove();
            }
        }

        // Render Active Buildings
        for (Building b : buildings) {
            renderBuilding(b);
        }
    }

    private void renderBuilding(Building b) {
        BuildingVisual visual = activeBuildingVisuals.get(b);
        int w = Math.max(1, b.getWidth());

        if (visual == null) {
            // Create new visual
            visual = createBuildingVisual(b);

            // Position
            int x = b.getPosition().getX();
            int y = b.getPosition().getY();
            Node topLeftCell = gridCellProvider.apply(x, y);

            if (topLeftCell != null) {
                javafx.geometry.Bounds bnds = topLeftCell.getBoundsInParent();
                visual.root.setLayoutX(bnds.getMinX());
                visual.root.setLayoutY(bnds.getMinY());
            }

            unitLayer.getChildren().add(visual.root);
            activeBuildingVisuals.put(b, visual);
            // Play spawn animation to avoid visual flash
            playSpawnAnimation(visual.root);
        } else {
            // Update existing bars
            updateBuildingBars(b, visual, w * TILE_SIZE);
        }
    }

    private BuildingVisual createBuildingVisual(Building b) {
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());

        StackPane buildingStack = new StackPane();
        buildingStack.setPrefSize(TILE_SIZE * w, TILE_SIZE * h);

        try {
            String side = b.isPlayerSide() ? "player" : "enemy";
            String cardName = b.getCardName();
            if (cardName == null && b.getBaseCard() != null) {
                cardName = b.getBaseCard().getName();
            }
            String cardKey = PngSequenceSprite.toCardKey(cardName);

            if (cardKey != null && !cardKey.isBlank()) {
                String baseFolder = "/images/animations/buildings/" + cardKey + "/" + side + "/idle";
                PngSequenceSprite sprite = new PngSequenceSprite(baseFolder, TILE_SIZE * w, TILE_SIZE * h,
                        b.getImagePath());
                sprite.setPreserveRatio(false);
                buildingStack.getChildren().add(sprite);
            } else {
                String imgPath = b.getImagePath();
                InputStream is = imgPath != null ? getClass().getResourceAsStream(imgPath) : null;
                if (is != null) {
                    Image img = new Image(is);
                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(TILE_SIZE * w);
                    imageView.setFitHeight(TILE_SIZE * h);
                    imageView.setPreserveRatio(false);
                    imageView.setSmooth(true);
                    buildingStack.getChildren().add(imageView);
                } else {
                    buildingStack.getChildren().add(createFallbackRect(b, w, h));
                }
            }
        } catch (Exception e) {
            buildingStack.getChildren().add(createFallbackRect(b, w, h));
        }

        // Add Health Bar
        HealthBarRenderer.HealthBarNodes hpNodes = HealthBarRenderer.createDetailedHealthBar(
                Math.max(40, TILE_SIZE * w - 6),
                GameConstants.HEALTH_BAR_HEIGHT_TEXT,
                b.getCurrentHealth());

        StackPane.setMargin(hpNodes.root, new javafx.geometry.Insets(-35, 0, 0, 0));
        buildingStack.getChildren().add(hpNodes.root);

        // Apply initial color and width
        HealthBarRenderer.updateHealthBar(hpNodes, b.getCurrentHealth(), b.getMaxHealth(), b.isPlayerSide());

        // Add Elixir Bar if applicable
        Rectangle elixirFg = null;
        if ("ELIXIR".equals(b.getProductionResource())) {
            elixirFg = createElixirBar(b, w, buildingStack);
        }

        // Create Laser Beam for Inferno Tower
        javafx.scene.shape.Line laser = null;
        if ("Inferno Tower".equals(b.getCardName())) {
            laser = new javafx.scene.shape.Line();
            laser.setStroke(Color.ORANGE);
            laser.setStrokeWidth(2.0);
            laser.setVisible(false);
            // Add to unitLayer so it can span across the arena
            unitLayer.getChildren().add(laser);
            // Ensure laser is behind the building itself? Or on top? Usually on top.
            laser.toBack();
        }

        return new BuildingVisual(buildingStack, hpNodes, elixirFg, laser);
    }

    private Rectangle createFallbackRect(Building b, int w, int h) {
        Rectangle fallback = new Rectangle(TILE_SIZE * w, TILE_SIZE * h);
        fallback.setFill(b.isPlayerSide() ? GameColors.PLAYER_TEAM : GameColors.ENEMY_TEAM); // shades maybe
        fallback.setStroke(Color.BLACK);
        fallback.setStrokeWidth(0.5);
        return fallback;
    }

    // Previous createHealthBar removed as it is replaced by HealthBarRenderer

    private Rectangle createElixirBar(Building b, int wTiles, StackPane container) {
        double interval = b.getProductionInterval();
        double timer = b.getProductionTimer();
        double pct = interval > 0 ? (1.0 - (timer / interval)) : 0.0;

        double hbWidth = Math.max(40, TILE_SIZE * wTiles - 6);
        double hbHeight = GameConstants.HEALTH_BAR_HEIGHT;

        Rectangle bg = new Rectangle(hbWidth, hbHeight);
        bg.setFill(Color.BLACK); // Elixir bar bg
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);

        Rectangle fg = new Rectangle(hbWidth * pct, hbHeight);
        fg.setFill(GameColors.ELIXIR_BAR);

        StackPane elPane = new StackPane(bg, fg);
        elPane.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(elPane, Pos.TOP_CENTER);

        // Position BELOW the building image
        // Top margin = height of building + small gap
        int h = Math.max(1, b.getHeight());
        double topMargin = (h * TILE_SIZE) + 2;
        StackPane.setMargin(elPane, new javafx.geometry.Insets(topMargin, 0, 0, 0));

        container.getChildren().add(elPane);

        return fg;
    }

    private void updateBuildingBars(Building b, BuildingVisual visual, double totalWidthPixels) {
        // Update Health Bar
        HealthBarRenderer.updateHealthBar(visual.hpNodes, b.getCurrentHealth(), b.getMaxHealth(), b.isPlayerSide());

        // Update Elixir Bar
        if (visual.elixirForeground != null && "ELIXIR".equals(b.getProductionResource())) {
            double interval = b.getProductionInterval();
            double timer = b.getProductionTimer();
            // Timer counts down from interval to 0
            double elPct = interval > 0 ? (1.0 - (timer / interval)) : 0.0;
            // Clamp between 0 and 1 just in case
            elPct = Math.max(0.0, Math.min(1.0, elPct));

            double barWidth = Math.max(40, totalWidthPixels - 6);

            visual.elixirForeground.setWidth(barWidth * elPct);
        }

        // Update Laser Beam
        if (visual.laserBeam != null) {
            com.kuroyale.model.entities.ICombatant target = b.getTarget();
            if (target != null && target.isAlive()) {
                com.kuroyale.model.entities.GridPosition myPos = b.getCenterPosition();
                com.kuroyale.model.entities.GridPosition targetPos = target.getCenterPosition();

                if (myPos != null && targetPos != null) {
                    double sx = myPos.getX() * TILE_SIZE + TILE_SIZE / 2.0;
                    double sy = myPos.getY() * TILE_SIZE + TILE_SIZE / 2.0;
                    double ex = targetPos.getX() * TILE_SIZE + TILE_SIZE / 2.0;
                    double ey = targetPos.getY() * TILE_SIZE + TILE_SIZE / 2.0;

                    visual.laserBeam.setStartX(sx);
                    visual.laserBeam.setStartY(sy);
                    visual.laserBeam.setEndX(ex);
                    visual.laserBeam.setEndY(ey);
                    visual.laserBeam.setVisible(true);

                    // Scale width and color based on damage
                    // 20 to 400
                    int damage = b.getDamage();
                    double intensity = (damage - 20) / 380.0; // 0.0 to 1.0
                    visual.laserBeam.setStrokeWidth(2.0 + (intensity * 6.0)); // 2.0 to 8.0

                    // Color from Orange to Red/Magenta
                    visual.laserBeam.setStroke(Color.ORANGE.interpolate(Color.MAGENTA, intensity));
                } else {
                    visual.laserBeam.setVisible(false);
                }
            } else {
                visual.laserBeam.setVisible(false);
            }
        }
    }

    /**
     * Plays a spawn animation on the given node to prevent visual flash.
     * The node starts invisible and small, then fades in with a scale-up effect.
     */
    private void playSpawnAnimation(Node node) {
        // Start invisible and slightly scaled down
        node.setOpacity(0.0);
        node.setScaleX(0.5);
        node.setScaleY(0.5);

        // Fade in animation
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(150), node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        // Scale up animation
        javafx.animation.ScaleTransition scaleUp = new javafx.animation.ScaleTransition(
                javafx.util.Duration.millis(150), node);
        scaleUp.setFromX(0.5);
        scaleUp.setFromY(0.5);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);

        // Play both animations together
        javafx.animation.ParallelTransition spawnAnim = new javafx.animation.ParallelTransition(fadeIn, scaleUp);
        spawnAnim.play();
    }
}
