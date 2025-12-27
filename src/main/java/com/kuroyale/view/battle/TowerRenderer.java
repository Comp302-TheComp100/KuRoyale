package com.kuroyale.view.battle;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.GridPosition;
import com.kuroyale.model.Tower;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Map;

public class TowerRenderer {
    private final GridPane grid;
    private final int TILE_SIZE;
    private final Map<String, Rectangle> towerHpForegrounds = new HashMap<>();
    private final Map<String, Text> towerHpTexts = new HashMap<>();
    private final Map<GridPosition, Node> activeTowerVisuals = new HashMap<>();

    public interface CellIndexer {
        void index(int x, int y, Node node);
    }

    private final CellIndexer cellIndexer;

    public TowerRenderer(GridPane grid, int tileSize, CellIndexer cellIndexer) {
        this.grid = grid;
        this.TILE_SIZE = tileSize;
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
            rect.setFill(Color.MAGENTA);
            towerStack.getChildren().add(rect);
        }

        // Health Bar
        Tower tower = arena.getTowerAt(x, y);
        double currentHealth = (tower != null) ? tower.getCurrentHealth() : 1.0;
        double maxHealth = (tower != null) ? tower.getMaxHealth() : 1.0;

        double width = (size == 4) ? 50 : 40;
        double height = 12;

        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.DARKBLUE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);

        double healthPercentage = currentHealth / maxHealth;
        Rectangle fg = new Rectangle(width * healthPercentage, height);
        fg.setFill(Color.ROYALBLUE);

        String towerKey = x + "_" + y;
        towerHpForegrounds.put(towerKey, fg);

        Text healthText = new Text(String.format("%.0f", currentHealth));
        towerHpTexts.put(towerKey, healthText);
        healthText.setFont(Font.font("Arial Black", FontWeight.BOLD, 10));
        healthText.setFill(Color.WHITE);
        healthText.setStroke(Color.BLACK);
        healthText.setStrokeWidth(0.5);

        StackPane healthBarContainer = new StackPane(bg, fg, healthText);
        StackPane.setAlignment(fg, javafx.geometry.Pos.CENTER_LEFT);
        StackPane.setAlignment(healthBarContainer, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(healthBarContainer, new javafx.geometry.Insets(2, 0, 0, 0));

        towerStack.getChildren().add(healthBarContainer);

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

        double currentHealth = tower.getCurrentHealth();
        double maxHealth = tower.getMaxHealth();
        double width = size == 4 ? 50 : 40;
        double pct = maxHealth > 0 ? Math.max(0, currentHealth) / maxHealth : 0.0;

        String towerKey = x + "_" + y;
        Rectangle fgNode = towerHpForegrounds.get(towerKey);
        if (fgNode != null) {
            fgNode.setWidth(width * pct);
        }
        Text textNode = towerHpTexts.get(towerKey);
        if (textNode != null) {
            textNode.setText(String.format("%.0f", currentHealth));
        }
    }
}
