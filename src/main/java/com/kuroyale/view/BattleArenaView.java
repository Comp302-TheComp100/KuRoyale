package com.kuroyale.view;

import com.kuroyale.model.Arena;
import com.kuroyale.model.GameState;
import com.kuroyale.model.GridCell;
import com.kuroyale.model.TileType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Renders the battle arena and placed units.
 */
public class BattleArenaView extends Pane {
    private final GridPane grid;
    private final Pane unitLayer;
    private final GameState gameState;

    private static final int TILE_SIZE = 18; // Matches ArenaDesignController

    public BattleArenaView(GameState gameState) {
        this.gameState = gameState;

        this.grid = new GridPane();
        this.unitLayer = new Pane();

        // Make unit layer transparent to mouse events so clicks go to grid for
        // placement
        unitLayer.setMouseTransparent(true);

        this.getChildren().addAll(grid, unitLayer);

        renderArena();
    }

    private void renderArena() {
        grid.getChildren().clear();
        Arena arena = gameState.getArena();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = arena.getCell(x, y);
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);

                // Style based on type
                switch (cell.getTileType()) {
                    case GRASS:
                        rect.setFill(Color.LIGHTGREEN);
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
                    case KING_TOWER_USER:
                    case KING_TOWER_COMPUTER:
                        rect.setFill(Color.GOLD);
                        break;
                    case PRINCESS_TOWER_USER:
                    case PRINCESS_TOWER_COMPUTER:
                        rect.setFill(Color.ORANGE);
                        break;
                    default:
                        rect.setFill(Color.GRAY);
                        break;
                }

                rect.setStroke(Color.BLACK);
                rect.setStrokeWidth(0.2);

                grid.add(rect, x, y);
            }
        }
    }

    public void update() {
        unitLayer.getChildren().clear();

        // Render placed cards
        for (GameState.PlacedCard pc : gameState.getPlacedCards()) {
            Circle unit = new Circle(TILE_SIZE / 2.0);
            unit.setCenterX(pc.x * TILE_SIZE + TILE_SIZE / 2.0);
            unit.setCenterY(pc.y * TILE_SIZE + TILE_SIZE / 2.0);

            if (pc.isPlayer) {
                unit.setFill(Color.BLUE);
            } else {
                unit.setFill(Color.RED);
            }

            unitLayer.getChildren().add(unit);
        }
    }

    public GridPane getGrid() {
        return grid;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }
}
