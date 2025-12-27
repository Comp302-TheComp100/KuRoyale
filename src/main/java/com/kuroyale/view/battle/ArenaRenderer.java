package com.kuroyale.view.battle;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.util.GameConstants;
import com.kuroyale.model.GridCell;
import com.kuroyale.model.GridPosition;
import com.kuroyale.model.TileType;

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
                if (isTower(cell.getTileType())) {
                    // Use checkered pattern for grass under towers
                    if ((x + y) % 2 == 0) {
                        rect.setFill(Color.rgb(124, 252, 0)); // LawnGreen
                    } else {
                        rect.setFill(Color.rgb(50, 205, 50)); // LimeGreen
                    }
                } else {
                    updateTileStyle(rect, cell.getTileType(), x, y);
                }

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
            addTowerImage(p.getX(), p.getY(), com.kuroyale.util.GameAssets.getInstance().getPrincessTowerUser(), true,
                    false, listener);
            // Mirror Computer Princess
            addTowerImage(p.getX(), Arena.HEIGHT - 3 - p.getY(),
                    com.kuroyale.util.GameAssets.getInstance().getPrincessTowerComputer(), false,
                    false, null);
        }

        // Render User King Tower
        if (layout.getKingTowerPosition() != null) {
            GridPosition p = layout.getKingTowerPosition();
            addTowerImage(p.getX(), p.getY(), com.kuroyale.util.GameAssets.getInstance().getKingTowerUser(), true, true,
                    listener);
            // Mirror Computer King (4x4)
            addTowerImage(p.getX(), Arena.HEIGHT - 4 - p.getY(),
                    com.kuroyale.util.GameAssets.getInstance().getKingTowerComputer(), false, true, null);
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
        double width = 40;
        if (colSpan == 4)
            width = 50; // Wider for King Tower
        double height = 12; // Height for text visibility

        // Background (Dark Blue)
        Rectangle bg = new Rectangle(width, height);
        bg.setFill(Color.DARKBLUE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);

        // Foreground (Royal Blue)
        double healthPercentage = currentHealth / maxHealth;
        Rectangle fg = new Rectangle(width * healthPercentage, height);
        fg.setFill(Color.ROYALBLUE);

        // Health Text: "1400"
        Text healthText = new Text(String.format("%.0f", currentHealth));
        // Font like Clash Royale
        healthText.setFont(Font.font("Arial Black", FontWeight.BOLD, 10));
        healthText.setFill(Color.WHITE);
        healthText.setStroke(Color.BLACK);
        healthText.setStrokeWidth(0.5);

        StackPane healthBarContainer = new StackPane();
        // Align foreground to left
        StackPane.setAlignment(fg, Pos.CENTER_LEFT);

        healthBarContainer.getChildren().addAll(bg, fg, healthText);
        healthBarContainer.setAlignment(Pos.CENTER);
        healthBarContainer.setTranslateY(-10);

        // Add to grid, spanning colSpan cols, 1 row
        arenaGrid.add(healthBarContainer, x, y, colSpan, 1);
    }

    private void updateTileStyle(Rectangle rect, TileType type, int x, int y) {
        switch (type) {
            case GRASS:
                if ((x + y) % 2 == 0) {
                    rect.setFill(Color.rgb(124, 252, 0)); // LawnGreen
                } else {
                    rect.setFill(Color.rgb(50, 205, 50)); // LimeGreen
                }
                break;
            case WATER:
                rect.setFill(Color.LIGHTBLUE);
                break;
            case BRIDGE:
                rect.setFill(Color.SADDLEBROWN);
                break;
            case ROAD:
                rect.setFill(Color.SANDYBROWN);
                break;
            case PRINCESS_TOWER_USER:
                rect.setFill(Color.HOTPINK);
                break;
            case PRINCESS_TOWER_COMPUTER:
                rect.setFill(Color.DEEPPINK);
                break;
            case KING_TOWER_USER:
                rect.setFill(Color.GOLD);
                break;
            case KING_TOWER_COMPUTER:
                rect.setFill(Color.ORANGE);
                break;
        }
    }

    private boolean isTower(TileType type) {
        return type == TileType.PRINCESS_TOWER_USER ||
                type == TileType.PRINCESS_TOWER_COMPUTER ||
                type == TileType.KING_TOWER_USER ||
                type == TileType.KING_TOWER_COMPUTER;
    }
}
