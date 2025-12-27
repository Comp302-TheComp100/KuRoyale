package com.kuroyale.view;

import com.kuroyale.model.Building;
import com.kuroyale.model.GameState;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BuildingRenderer {
    private final Pane unitLayer;
    private final int TILE_SIZE;
    private final Map<Building, Node> activeBuildingVisuals = new HashMap<>();
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
        Iterator<Map.Entry<Building, Node>> buildingIt = activeBuildingVisuals.entrySet().iterator();
        while (buildingIt.hasNext()) {
            Map.Entry<Building, Node> entry = buildingIt.next();
            Building b = entry.getKey();
            if (!currentBuildings.contains(b) || !b.isAlive()) {
                unitLayer.getChildren().remove(entry.getValue());
                buildingIt.remove();
            }
        }

        // Render Active Buildings
        for (Building b : buildings) {
            renderBuilding(b);
        }
    }

    private void renderBuilding(Building b) {
        Node buildingNode = activeBuildingVisuals.get(b);
        int w = Math.max(1, b.getWidth());

        if (buildingNode == null) {
            // Create new visual
            StackPane buildingStack = createBuildingVisual(b);

            // Position
            int x = b.getPosition().getX();
            int y = b.getPosition().getY();
            Node topLeftCell = gridCellProvider.apply(x, y);

            if (topLeftCell != null) {
                javafx.geometry.Bounds bnds = topLeftCell.getBoundsInParent();
                buildingStack.setLayoutX(bnds.getMinX());
                buildingStack.setLayoutY(bnds.getMinY());
            }

            unitLayer.getChildren().add(buildingStack);
            activeBuildingVisuals.put(b, buildingStack);
            buildingNode = buildingStack;
        } else {
            // Update existing health bar
            updateBuildingHealthBar(b, buildingNode, w * TILE_SIZE);
        }
    }

    private StackPane createBuildingVisual(Building b) {
        int w = Math.max(1, b.getWidth());
        int h = Math.max(1, b.getHeight());

        StackPane buildingStack = new StackPane();
        buildingStack.setPrefSize(TILE_SIZE * w, TILE_SIZE * h);

        try {
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
        } catch (Exception e) {
            buildingStack.getChildren().add(createFallbackRect(b, w, h));
        }

        // Add Health Bar
        buildingStack.getChildren().add(createHealthBar(b, w));

        return buildingStack;
    }

    private Rectangle createFallbackRect(Building b, int w, int h) {
        Rectangle fallback = new Rectangle(TILE_SIZE * w, TILE_SIZE * h);
        fallback.setFill(b.isPlayerSide() ? Color.DARKBLUE : Color.DARKRED);
        fallback.setStroke(Color.BLACK);
        fallback.setStrokeWidth(0.5);
        return fallback;
    }

    private StackPane createHealthBar(Building b, int wPixelsTiles) {
        double maxHp = b.getMaxHealth();
        double curHp = Math.max(0, b.getCurrentHealth());
        double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;

        double hbWidth = Math.max(40, TILE_SIZE * wPixelsTiles - 6);
        double hbHeight = 12;

        Rectangle bg = new Rectangle(hbWidth, hbHeight);
        bg.setFill(Color.DARKBLUE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);
        bg.setId("buildingHpBg_" + b.hashCode());

        Rectangle fg = new Rectangle(hbWidth * pct, hbHeight);
        fg.setFill(b.isPlayerSide() ? Color.ROYALBLUE : Color.CRIMSON);
        fg.setId("buildingHpFg_" + b.hashCode());

        StackPane hbPane = new StackPane(bg, fg);
        hbPane.setAlignment(Pos.CENTER_LEFT);
        StackPane.setAlignment(hbPane, Pos.TOP_CENTER);
        StackPane.setMargin(hbPane, new javafx.geometry.Insets(2, 0, 0, 0));
        return hbPane;
    }

    private void updateBuildingHealthBar(Building b, Node buildingNode, double totalWidthPixels) {
        double maxHp = b.getMaxHealth();
        double curHp = Math.max(0, b.getCurrentHealth());
        double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;
        double hbWidth = Math.max(40, totalWidthPixels - 6);

        Node fgNode = buildingNode.lookup("#buildingHpFg_" + b.hashCode());
        if (fgNode instanceof Rectangle) {
            ((Rectangle) fgNode).setWidth(hbWidth * pct);
        }
    }
}
