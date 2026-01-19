package com.kuroyale.view.battle.renderers;

import com.kuroyale.model.core.entities.Arena;
import com.kuroyale.model.core.entities.ArenaLayout;
import com.kuroyale.model.core.entities.GridCell;
import com.kuroyale.model.core.entities.GridPosition;
import com.kuroyale.model.core.enums.TileType;
import com.kuroyale.util.config.GameConstants;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ArenaRenderer {

    private final GridPane arenaGrid;

    public interface ArenaInteractionListener {
        void onDragOver(javafx.scene.input.DragEvent event, int x, int y);

        void onDragDropped(javafx.scene.input.DragEvent event, int x, int y);

        void onTileClicked(int x, int y, TileType type);

        void onTowerClicked(int x, int y, boolean isKing);
    }

    public ArenaRenderer(GridPane arenaGrid) {
        this.arenaGrid = arenaGrid;
    }

    public void renderArena(Arena arena, ArenaLayout layout, ArenaInteractionListener listener) {
        arenaGrid.getChildren().clear();
        arenaGrid.getColumnConstraints().clear();
        arenaGrid.getRowConstraints().clear();

        // Pass 1: Render Grid (Ground)
        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = arena.getCell(x, y);
                Rectangle rect = new Rectangle(GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);

                // For towers, render underlying terrain (grass)
                // Use ViewUtils for consistent coloring
                rect.setFill(com.kuroyale.view.battle.ViewUtils.getTileColor(cell.getTileType(), x, y));

                // Remove borders for seamless appearance
                rect.setStroke(Color.TRANSPARENT);
                rect.setStrokeWidth(0.0);

                // Add interaction
                final int finalX = x;
                final int finalY = y;
                final TileType type = cell.getTileType();

                if (listener != null) {
                    rect.setOnDragOver(event -> listener.onDragOver(event, finalX, finalY));
                    rect.setOnDragDropped(event -> listener.onDragDropped(event, finalX, finalY));
                    rect.setOnMouseClicked(event -> listener.onTileClicked(finalX, finalY, type));
                }

                arenaGrid.add(rect, x, y);
            }
        }

        // Pass 2: Render Towers (Images)
        renderStructureImages(layout, listener);
    }

    private void renderStructureImages(ArenaLayout layout, ArenaInteractionListener listener) {
        // Render User Princess Towers
        for (GridPosition p : layout.getPrincessTowerPositions()) {
            addTowerImage(p.getX(), p.getY(), com.kuroyale.util.ui.GameAssets.getInstance().getPrincessTowerUser(),
                    true,
                    false, listener);
            // Mirror Computer Princess
            addTowerImage(p.getX(), Arena.HEIGHT - 3 - p.getY(),
                    com.kuroyale.util.ui.GameAssets.getInstance().getPrincessTowerComputer(), false,
                    false, null);
        }

        // Render User King Tower
        if (layout.getKingTowerPosition() != null) {
            GridPosition p = layout.getKingTowerPosition();
            addTowerImage(p.getX(), p.getY(), com.kuroyale.util.ui.GameAssets.getInstance().getKingTowerUser(), true,
                    true,
                    listener);
            // Mirror Computer King (4x4)
            addTowerImage(p.getX(), Arena.HEIGHT - 4 - p.getY(),
                    com.kuroyale.util.ui.GameAssets.getInstance().getKingTowerComputer(), false, true, null);
        }
    }

    private void addTowerImage(int x, int y, Image img, boolean isUser, boolean isKing,
            ArenaInteractionListener listener) {
        if (img == null)
            return;

        int size = isKing ? GameConstants.KING_TOWER_SIZE : GameConstants.PRINCESS_TOWER_SIZE;
        int pixelSize = size * GameConstants.TILE_SIZE;

        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(pixelSize);
        imageView.setFitHeight(pixelSize);

        // Add click to remove for user towers
        if (isUser && listener != null) {
            imageView.setOnMouseClicked(e -> {
                listener.onTowerClicked(x, y, isKing);
            });
        }

        arenaGrid.add(imageView, x, y, size, size);

        // Add Health Bar
        double maxHealth = isKing ? GameConstants.KING_TOWER_HEALTH : GameConstants.PRINCESS_TOWER_HEALTH;
        // In design mode, current health is always max
        addHealthBar(x, y, maxHealth, maxHealth, size);
    }

    private void addHealthBar(int x, int y, double currentHealth, double maxHealth, int colSpan) {
        // Health bar dimensions
        double width = (colSpan == 4) ? GameConstants.HEALTH_BAR_WIDTH_LARGE : GameConstants.HEALTH_BAR_WIDTH_STANDARD;
        double height = GameConstants.HEALTH_BAR_HEIGHT_TEXT;

        Rectangle bg = new Rectangle(width, height);
        bg.setFill(com.kuroyale.util.config.GameColors.HEALTH_BAR_BG);
        bg.setStroke(com.kuroyale.util.config.GameColors.HEALTH_BAR_STROKE);
        bg.setStrokeWidth(0.5);

        double healthPercentage = currentHealth / maxHealth;
        Rectangle fg = new Rectangle(width * healthPercentage, height);
        fg.setFill(com.kuroyale.util.config.GameColors.PLAYER_TEAM);

        Text healthText = new Text(String.format("%.0f", currentHealth));
        healthText.setFont(Font.font("Arial Black", FontWeight.BOLD, 10));
        healthText.setFill(com.kuroyale.util.config.GameColors.TEXT_FILL);
        healthText.setStroke(com.kuroyale.util.config.GameColors.TEXT_STROKE);
        healthText.setStrokeWidth(0.5);

        StackPane healthBarContainer = new StackPane();
        StackPane.setAlignment(fg, Pos.CENTER_LEFT);

        healthBarContainer.getChildren().addAll(bg, fg, healthText);
        healthBarContainer.setAlignment(Pos.CENTER);
        healthBarContainer.setTranslateY(-10);

        arenaGrid.add(healthBarContainer, x, y, colSpan, 1);
    }

    private java.util.List<javafx.scene.Node> highlightNodes = new java.util.ArrayList<>();

    public void highlightRegion(int x, int y, int width, int height, boolean isValid) {
        clearHighlight();

        // Create a semi-transparent rectangle for the highlight
        Rectangle highlight = new Rectangle(width * GameConstants.TILE_SIZE, height * GameConstants.TILE_SIZE);
        highlight.setFill(isValid ? Color.rgb(0, 255, 0, 0.3) : Color.rgb(255, 0, 0, 0.3));
        highlight.setStroke(isValid ? Color.GREEN : Color.RED);
        highlight.setStrokeWidth(2.0);

        // Disable mouse events so it doesn't interfere with drops
        highlight.setMouseTransparent(true);

        arenaGrid.add(highlight, x, y, width, height);
        highlightNodes.add(highlight);
    }

    public void clearHighlight() {
        arenaGrid.getChildren().removeAll(highlightNodes);
        highlightNodes.clear();
    }
}
