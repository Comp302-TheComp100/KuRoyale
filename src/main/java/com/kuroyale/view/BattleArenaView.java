package com.kuroyale.view;

import com.kuroyale.model.Arena;
import com.kuroyale.model.GameState;
import com.kuroyale.model.GridCell;
import com.kuroyale.model.TileType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Renders the battle arena and placed units.
 */
public class BattleArenaView extends javafx.scene.layout.BorderPane {
    private final GridPane grid;
    private final Pane unitLayer;
    private final GameState gameState;
    private final javafx.scene.control.Label timerLabel;
    private final javafx.scene.control.Label scoreLabel;

    private static final int TILE_SIZE = 18; // Matches ArenaDesignController

    private javafx.scene.image.Image princessTowerUserImg;
    private javafx.scene.image.Image princessTowerComputerImg;
    private javafx.scene.image.Image kingTowerUserImg;
    private javafx.scene.image.Image kingTowerComputerImg;

    public BattleArenaView(GameState gameState) {
        this.gameState = gameState;

        // Load images
        try {
            princessTowerUserImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/tower_archer_blue.png"));
            princessTowerComputerImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/tower_archer_red.png"));
            kingTowerUserImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/Clash_Royale_icon_King_Tower_Blue.png"));
            kingTowerComputerImg = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/Clash_Royale_icon_King_Tower_Red.png"));
        } catch (Exception e) {
            System.err.println("Failed to load tower images: " + e.getMessage());
        }

        // Right Sidebar (Timer and Score)
        javafx.scene.layout.VBox sidebar = new javafx.scene.layout.VBox(20);
        sidebar.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        sidebar.setPadding(new javafx.geometry.Insets(20));
        sidebar.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        sidebar.setPrefWidth(200);

        javafx.scene.control.Label timerTitle = new javafx.scene.control.Label("TIME");
        timerTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        timerLabel = new javafx.scene.control.Label("03:00");
        timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        javafx.scene.control.Label scoreTitle = new javafx.scene.control.Label("SCORE");
        scoreTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        scoreLabel = new javafx.scene.control.Label("0 - 0");
        scoreLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        javafx.scene.control.Label opponentLabel = new javafx.scene.control.Label("OPPONENT");
        opponentLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 18px; -fx-font-weight: bold;");

        sidebar.getChildren().addAll(timerTitle, timerLabel, new javafx.scene.control.Separator(), scoreTitle,
                scoreLabel, new javafx.scene.control.Separator(), opponentLabel);
        this.setRight(sidebar);

        // Center Arena
        this.grid = new GridPane();
        this.unitLayer = new Pane();

        // Make unit layer transparent to mouse events so clicks go to grid for
        // placement
        unitLayer.setMouseTransparent(true);

        Pane arenaPane = new Pane(grid, unitLayer);
        // Set preferred size to match grid size
        arenaPane.setMaxSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        // Handle clicks directly on the arena pane to get correct local coordinates
        arenaPane.setOnMouseClicked(e -> {
            if (onGridClick != null) {
                int tileX = (int) (e.getX() / TILE_SIZE);
                int tileY = (int) (e.getY() / TILE_SIZE);
                onGridClick.accept(tileX, tileY);
            }
        });

        StackPane centerContainer = new StackPane(arenaPane);
        centerContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER); // Align to top as requested
        centerContainer.setPadding(new javafx.geometry.Insets(20, 0, 0, 0)); // Add some top padding

        this.setCenter(centerContainer);

        renderArena();
    }

    private java.util.function.BiConsumer<Integer, Integer> onGridClick;

    public void setOnGridClicked(java.util.function.BiConsumer<Integer, Integer> handler) {
        this.onGridClick = handler;
    }

    private void renderArena() {
        grid.getChildren().clear();
        Arena arena = gameState.getArena();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = arena.getCell(x, y);

                // Check if this is a tower tile
                boolean isTower = isTowerTile(cell.getTileType());

                if (isTower) {
                    // Render underlying tile (Grass usually)
                    Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                    rect.setFill(Color.LIGHTGREEN);
                    rect.setStroke(Color.BLACK);
                    rect.setStrokeWidth(0.2);
                    grid.add(rect, x, y);
                } else {
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

        // Render Towers on top
        renderTowerImages();
    }

    private void renderTowerImages() {
        if (gameState.getArena().getLayout() != null) {
            com.kuroyale.model.ArenaLayout layout = gameState.getArena().getLayout();

            // Princess Towers
            if (layout.getPrincessTowerPositions() != null) {
                for (com.kuroyale.model.GridPosition p : layout.getPrincessTowerPositions()) {
                    renderTowerAt(p.getX(), p.getY(), princessTowerUserImg, 3);
                    // Mirror
                    renderTowerAt(p.getX(), Arena.HEIGHT - 3 - p.getY(), princessTowerComputerImg, 3);
                }
            }

            // King Tower
            if (layout.getKingTowerPosition() != null) {
                com.kuroyale.model.GridPosition p = layout.getKingTowerPosition();
                renderTowerAt(p.getX(), p.getY(), kingTowerUserImg, 4);
                // Mirror
                renderTowerAt(p.getX(), Arena.HEIGHT - 4 - p.getY(), kingTowerComputerImg, 4);
            }
        }
    }

    private void renderTowerAt(int x, int y, javafx.scene.image.Image img, int size) {
        StackPane towerStack = new StackPane();
        // Spanning 'size' tiles
        towerStack.setPrefSize(TILE_SIZE * size, TILE_SIZE * size);

        if (img != null) {
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(img);
            imageView.setFitWidth(TILE_SIZE * size);
            imageView.setFitHeight(TILE_SIZE * size);
            towerStack.getChildren().add(imageView);
        } else {
            // Fallback
            Rectangle rect = new Rectangle(TILE_SIZE * size, TILE_SIZE * size);
            rect.setFill(Color.MAGENTA);
            towerStack.getChildren().add(rect);
        }

        // Health Bar with actual health from tower
        double healthPercentage = getTowerHealthAt(x, y);
        javafx.scene.control.ProgressBar healthBar = new javafx.scene.control.ProgressBar(healthPercentage);
        healthBar.setPrefWidth(TILE_SIZE * size * 0.8);
        healthBar.setPrefHeight(8);

        // Dynamic color based on health percentage
        String barColor;
        if (healthPercentage > 0.6) {
            barColor = "limegreen";
        } else if (healthPercentage > 0.3) {
            barColor = "orange";
        } else {
            barColor = "red";
        }

        healthBar.setStyle(String.format(
                "-fx-accent: %s; -fx-control-inner-background: rgba(0,0,0,0.5); -fx-background-radius: 3;",
                barColor));

        StackPane.setAlignment(healthBar, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(healthBar, new javafx.geometry.Insets(2, 0, 0, 0));

        towerStack.getChildren().add(healthBar);

        // Add to grid, spanning 'size' columns and rows
        grid.add(towerStack, x, y, size, size);
    }

    private double getTowerHealthAt(int x, int y) {
        // TODO: Implement tower health tracking in Arena
        // For now, return full health for all towers
        return 1.0;
    }

    private boolean isTowerTile(TileType type) {
        return type == TileType.PRINCESS_TOWER_USER ||
                type == TileType.PRINCESS_TOWER_COMPUTER ||
                type == TileType.KING_TOWER_USER ||
                type == TileType.KING_TOWER_COMPUTER;
    }

    public void update() {
        // Update Timer
        int totalSeconds = (int) Math.ceil(gameState.getGameTime());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));

        // Change timer color during double elixir (last 60 seconds)
        if (gameState.isDoubleElixir()) {
            timerLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 24px; -fx-font-weight: bold;");
        } else {
            timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        }

        // Update Score
        scoreLabel.setText(String.format("%d - %d", gameState.getPlayerScore(), gameState.getBotScore()));

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

    public void highlightValidCells(boolean show) {
        Arena arena = gameState.getArena();
        for (javafx.scene.Node node : grid.getChildren()) {
            Rectangle rect = null;

            if (node instanceof Rectangle) {
                rect = (Rectangle) node;
            } else if (node instanceof StackPane) {
                // Towers are StackPanes - skip them
                continue;
            }

            if (rect != null) {
                Integer x = GridPane.getColumnIndex(node);
                Integer y = GridPane.getRowIndex(node);

                // Handle null indices
                if (x == null || y == null)
                    continue;

                if (show) {
                    // Check if valid: Player side (bottom half) AND walkable (Grass/Bridge)
                    boolean isPlayerSide = y >= Arena.HEIGHT / 2;
                    GridCell cell = arena.getCell(x, y);
                    boolean isWalkable = cell.getTileType() == TileType.GRASS || cell.getTileType() == TileType.BRIDGE;

                    if (isPlayerSide && isWalkable) {
                        // Use glow effect instead of changing stroke width to prevent layout shift
                        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                        glow.setColor(Color.YELLOW);
                        glow.setRadius(8);
                        glow.setSpread(0.6);
                        rect.setEffect(glow);
                    } else {
                        rect.setEffect(null);
                    }
                } else {
                    // Reset - remove effects
                    rect.setEffect(null);
                }
            }
        }
    }

    public GridPane getGrid() {
        return grid;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }
}
