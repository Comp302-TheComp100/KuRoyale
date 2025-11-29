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

        // Bind unitLayer position to grid position to ensure perfect alignment
        unitLayer.layoutXProperty().bind(grid.layoutXProperty());
        unitLayer.layoutYProperty().bind(grid.layoutYProperty());

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
        com.kuroyale.model.Tower tower = gameState.getArena().getTowerAt(x, y);
        double currentHealth = (tower != null) ? tower.getCurrentHealth() : 1.0;
        double maxHealth = (tower != null) ? tower.getMaxHealth() : 1.0;

        // Health bar dimensions
        double width = 40;
        if (size == 4)
            width = 50; // Wider for King Tower
        double height = 12; // Height for text visibility

        // Background (Dark Blue)
        javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(width, height);
        bg.setFill(Color.DARKBLUE);
        bg.setStroke(Color.BLACK);
        bg.setStrokeWidth(0.5);

        // Foreground (Royal Blue)
        double healthPercentage = currentHealth / maxHealth;
        javafx.scene.shape.Rectangle fg = new javafx.scene.shape.Rectangle(width * healthPercentage, height);
        fg.setFill(Color.ROYALBLUE);

        // Health Text: "1400" (Remaining only)
        javafx.scene.text.Text healthText = new javafx.scene.text.Text(String.format("%.0f", currentHealth));
        // Font like Clash Royale: Bold, Impact-like
        healthText.setFont(javafx.scene.text.Font.font("Arial Black", javafx.scene.text.FontWeight.BOLD, 10));
        healthText.setFill(Color.WHITE);
        healthText.setStroke(Color.BLACK);
        healthText.setStrokeWidth(0.5); // Thicker stroke for CR look

        StackPane healthBarContainer = new StackPane();
        // Align foreground to left
        StackPane.setAlignment(fg, javafx.geometry.Pos.CENTER_LEFT);

        healthBarContainer.getChildren().addAll(bg, fg, healthText);
        healthBarContainer.setAlignment(javafx.geometry.Pos.CENTER);

        // Position above the tower
        StackPane.setAlignment(healthBarContainer, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(healthBarContainer, new javafx.geometry.Insets(2, 0, 0, 0));

        towerStack.getChildren().add(healthBarContainer);

        // Add to grid, spanning 'size' columns and rows
        grid.add(towerStack, x, y, size, size);
        GridPane.setHalignment(towerStack, javafx.geometry.HPos.CENTER);
        GridPane.setValignment(towerStack, javafx.geometry.VPos.CENTER);
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

        // Render active moving troops
        java.util.List<com.kuroyale.model.Troop> troops = gameState.getActiveTroops();
        for (com.kuroyale.model.Troop troop : troops) {
            com.kuroyale.model.GridPosition pos = troop.getPosition();
            javafx.scene.Node cellNode = getGridCell(pos.getX(), pos.getY());
            if (cellNode != null) {
                javafx.geometry.Bounds cellBounds = cellNode.getBoundsInParent();
            // Try to render troop as image; fall back to circle if image missing
            javafx.scene.Node unitNode;
            try {
                String imgPath = troop.getBaseCard().getImagePath();
                javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream(imgPath));
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(TILE_SIZE);
                iv.setFitHeight(TILE_SIZE);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                iv.setLayoutX(cellBounds.getMinX());
                iv.setLayoutY(cellBounds.getMinY());
                unitNode = iv;
            } catch (Exception e) {
                Circle fallback = new Circle(TILE_SIZE / 2.5);
                fallback.setCenterX(cellBounds.getMinX() + cellBounds.getWidth() / 2.0);
                fallback.setCenterY(cellBounds.getMinY() + cellBounds.getHeight() / 2.0);
                fallback.setFill(troop.isPlayerSide() ? (troop.isAirUnit() ? Color.DODGERBLUE : Color.BLUE)
                    : (troop.isAirUnit() ? Color.ORANGERED : Color.RED));
                unitNode = fallback;
            }

            unitLayer.getChildren().add(unitNode);

            // Troop health bar above the unit
            double maxHp = troop.getBaseCard().getHp();
            double curHp = Math.max(0, troop.getCurrentHealth());
            double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;
            double barWidth = TILE_SIZE * 0.9;
            double barHeight = 4;

            javafx.scene.shape.Rectangle hpBg = new javafx.scene.shape.Rectangle(barWidth, barHeight);
            hpBg.setFill(Color.color(0.2, 0.2, 0.2, 0.8));
            hpBg.setStroke(Color.BLACK);
            hpBg.setStrokeWidth(0.3);
                double centerX = cellBounds.getMinX() + cellBounds.getWidth() / 2.0;
                double centerY = cellBounds.getMinY() + cellBounds.getHeight() / 2.0;
                hpBg.setX(centerX - barWidth / 2.0);
                hpBg.setY(centerY - (TILE_SIZE / 2.5) - 6);

                javafx.scene.shape.Rectangle hpFg = new javafx.scene.shape.Rectangle(barWidth * pct, barHeight);
                if (troop.isPlayerSide()) {
                    hpFg.setFill(pct > 0.5 ? Color.LIMEGREEN : (pct > 0.2 ? Color.GOLD : Color.CRIMSON));
                } else {
                    hpFg.setFill(Color.CRIMSON);
                }
            hpFg.setX(hpBg.getX());
            hpFg.setY(hpBg.getY());

            unitLayer.getChildren().addAll(hpBg, hpFg);

            // Attack feedback: projectile for ranged units (glow removed)
            if (troop.getUnitState() == com.kuroyale.model.UnitState.ATTACKING) {
                // Projectile for ranged attackers (moving dot)
                if (troop.getCombatStats() != null &&
                        troop.getCombatStats().getAttackType() == com.kuroyale.model.CombatStats.AttackType.RANGED) {
                    com.kuroyale.model.Troop target = findNearestEnemyTroopInRange(troop);
                    if (target != null) {
                        javafx.scene.Node targetNode = getGridCell(target.getPosition().getX(), target.getPosition().getY());
                        if (targetNode != null) {
                            javafx.geometry.Bounds tb = targetNode.getBoundsInParent();
                            double tx = tb.getMinX() + tb.getWidth() / 2.0;
                            double ty = tb.getMinY() + tb.getHeight() / 2.0;
                            // Animate dot using attack cooldown progress to ensure forward motion
                            double duration = Math.max(0.15, troop.getCombatStats().getHitSpeedSeconds());
                            double cooldown = troop.getAttackCooldown();
                            double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration)); // 0..1 from attacker to target
                            double px = centerX + (tx - centerX) * phase;
                            double py = centerY + (ty - centerY) * phase;
                            Circle dot = new Circle(px, py, 2.5);
                            dot.setFill(troop.isPlayerSide() ? Color.YELLOW : Color.ORANGE);
                            dot.setStroke(Color.color(0,0,0,0.35));
                            dot.setStrokeWidth(0.8);
                            unitLayer.getChildren().add(dot);
                        }
                    }
                }
            }

            }
        }

        // Remove all placed card markers; units/towers/spells are visualized elsewhere.
    }

    /**
     * Helper method to get the grid cell node at the specified grid coordinates.
     */
    private javafx.scene.Node getGridCell(int x, int y) {
        for (javafx.scene.Node node : grid.getChildren()) {
            Integer colIndex = GridPane.getColumnIndex(node);
            Integer rowIndex = GridPane.getRowIndex(node);

            // Handle null indices (default to 0)
            int col = (colIndex == null) ? 0 : colIndex;
            int row = (rowIndex == null) ? 0 : rowIndex;

            if (col == x && row == y) {
                return node;
            }
        }
        return null;
    }

    public void highlightValidCells(boolean show, boolean isSpell) {
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
                    boolean shouldHighlight = false;

                    if (isSpell) {
                        // Spells can be placed anywhere
                        shouldHighlight = true;
                    } else {
                        // Standard units: Player side (bottom half) AND walkable (Grass)
                        boolean isPlayerSide = y >= Arena.HEIGHT / 2;
                        GridCell cell = arena.getCell(x, y);
                        boolean isWalkable = cell.getTileType() == TileType.GRASS;

                        if (isPlayerSide && isWalkable) {
                            shouldHighlight = true;
                        }
                    }

                    if (shouldHighlight) {
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

    // Helper: find nearest enemy troop within range (view-side approximation)
    private com.kuroyale.model.Troop findNearestEnemyTroopInRange(com.kuroyale.model.Troop self) {
        com.kuroyale.model.Troop best = null;
        double bestDist = Double.MAX_VALUE;
        for (com.kuroyale.model.Troop t : gameState.getActiveTroops()) {
            if (!t.isAlive()) continue;
            if (t.isPlayerSide() == self.isPlayerSide()) continue;
            double dx = self.getPosition().getX() - t.getPosition().getX();
            double dy = self.getPosition().getY() - t.getPosition().getY();
            double dist = Math.sqrt(dx*dx + dy*dy);
            double range = self.getCombatStats() != null ? self.getCombatStats().getRangeTiles() : (int)Math.round(self.getAttackRange());
            if (dist <= range && dist < bestDist) { bestDist = dist; best = t; }
        }
        return best;
    }
}
