package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.GridPosition;
import com.kuroyale.model.entities.Tower;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

public class TowerRenderer {
    private final GridPane grid;
    private final int TILE_SIZE = com.kuroyale.util.GameConstants.TILE_SIZE;
    private final Map<String, HealthBarRenderer.HealthBarNodes> towerHpNodes = new HashMap<>();
    private final Map<GridPosition, Node> activeTowerVisuals = new HashMap<>();

    public interface CellIndexer {
        void index(int x, int y, Node node);
    }

    private final CellIndexer cellIndexer;

    public TowerRenderer(GridPane grid, int tileSize, CellIndexer cellIndexer) {
        this.grid = grid;
        // this.TILE_SIZE = tileSize; // Ignored, using constant
        this.cellIndexer = cellIndexer;
    }

    public void renderTowers(Arena arena) {
        if (arena.getLayout() != null) {
            ArenaLayout layout = arena.getLayout();
            com.kuroyale.util.GameAssets assets = com.kuroyale.util.GameAssets.getInstance();

            // Princess Towers
            if (layout.getPrincessTowerPositions() != null) {
                for (GridPosition p : layout.getPrincessTowerPositions()) {
                    renderTowerAt(arena, p.getX(), p.getY(), assets.getPrincessTowerUser(), 3);
                    renderTowerAt(arena, p.getX(), Arena.HEIGHT - 3 - p.getY(), assets.getPrincessTowerComputer(), 3);
                }
            }

            // King Tower
            if (layout.getKingTowerPosition() != null) {
                GridPosition p = layout.getKingTowerPosition();
                renderTowerAt(arena, p.getX(), p.getY(), assets.getKingTowerUser(), 4);
                renderTowerAt(arena, p.getX(), Arena.HEIGHT - 4 - p.getY(), assets.getKingTowerComputer(), 4);
            }
        }
    }

    public void cleanupDestroyedTowers(Arena arena) {
        activeTowerVisuals.entrySet().removeIf(entry -> {
            GridPosition pos = entry.getKey();
            if (arena.getTowerAt(pos.getX(), pos.getY()) == null) {
                // Tower is gone, remove visual
                grid.getChildren().remove(entry.getValue());
                return true;
            }
            return false;
        });
    }

    public void updateHealthBars(Arena arena) {
        if (arena.getLayout() == null)
            return;
        ArenaLayout layout = arena.getLayout();

        if (layout.getPrincessTowerPositions() != null) {
            for (GridPosition p : layout.getPrincessTowerPositions()) {
                updateSingleTowerBar(arena, p.getX(), p.getY(), 3);
                updateSingleTowerBar(arena, p.getX(), Arena.HEIGHT - 3 - p.getY(), 3);
            }
        }
        if (layout.getKingTowerPosition() != null) {
            GridPosition p = layout.getKingTowerPosition();
            updateSingleTowerBar(arena, p.getX(), p.getY(), 4);
            updateSingleTowerBar(arena, p.getX(), Arena.HEIGHT - 4 - p.getY(), 4);
        }
    }

    private void renderTowerAt(Arena arena, int x, int y, Image img, int size) {
        if (activeTowerVisuals.containsKey(new GridPosition(x, y)))
            return;

        StackPane towerStack = new StackPane();
        towerStack.setPrefSize(TILE_SIZE * size, TILE_SIZE * size);

        if (img != null) {
            ImageView imageView = new ImageView(img);
            imageView.setFitWidth(TILE_SIZE * size);
            imageView.setFitHeight(TILE_SIZE * size);
            towerStack.getChildren().add(imageView);
        } else {
            Rectangle rect = new Rectangle(TILE_SIZE * size, TILE_SIZE * size);
            rect.setFill(com.kuroyale.util.GameColors.DEFAULT);
            towerStack.getChildren().add(rect);
        }

        // Health Bar
        Tower tower = arena.getTowerAt(x, y);
        double currentHealth = (tower != null) ? tower.getCurrentHealth() : 1.0;

        double width = (size == 4) ? com.kuroyale.util.GameConstants.HEALTH_BAR_WIDTH_LARGE
                : com.kuroyale.util.GameConstants.HEALTH_BAR_WIDTH_STANDARD;

        HealthBarRenderer.HealthBarNodes hpNodes = HealthBarRenderer.createDetailedHealthBar(
                width, com.kuroyale.util.GameConstants.HEALTH_BAR_HEIGHT_TEXT, currentHealth);

        // Store nodes for updates
        towerHpNodes.put(x + "_" + y, hpNodes);

        // Set initial color and width
        HealthBarRenderer.updateHealthBar(hpNodes, currentHealth, (tower != null) ? tower.getMaxHealth() : 1.0,
                (tower != null) && tower.isPlayerSide());

        if (tower != null && tower.isPlayerSide()) {
            // Player Tower: Bottom
            StackPane.setAlignment(hpNodes.root, javafx.geometry.Pos.BOTTOM_CENTER);
            StackPane.setMargin(hpNodes.root, new javafx.geometry.Insets(0, 0, -15, 0));
        } else {
            // Computer (Component) Tower: Top
            StackPane.setAlignment(hpNodes.root, javafx.geometry.Pos.TOP_CENTER);
            StackPane.setMargin(hpNodes.root, new javafx.geometry.Insets(-75, 0, 0, 0));
        }

        towerStack.getChildren().add(hpNodes.root);

        grid.add(towerStack, x, y, size, size);
        GridPane.setHalignment(towerStack, javafx.geometry.HPos.CENTER);
        GridPane.setValignment(towerStack, javafx.geometry.VPos.CENTER);

        // Index cells
        indexTowerCells(x, y, size, towerStack);

        activeTowerVisuals.put(new GridPosition(x, y), towerStack);
    }

    private void indexTowerCells(int x, int y, int size, Node node) {
        if (cellIndexer == null)
            return;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                cellIndexer.index(x + dx, y + dy, node);
            }
        }
    }

    private void updateSingleTowerBar(Arena arena, int x, int y, int size) {
        Tower tower = arena.getTowerAt(x, y);
        if (tower == null)
            return;

        String towerKey = x + "_" + y;
        HealthBarRenderer.HealthBarNodes nodes = towerHpNodes.get(towerKey);
        if (nodes != null) {
            HealthBarRenderer.updateHealthBar(nodes, tower.getCurrentHealth(), tower.getMaxHealth(),
                    tower.isPlayerSide());
        }
    }

    public Node getTowerVisual(GridPosition pos) {
        return activeTowerVisuals.get(pos);
    }
}
