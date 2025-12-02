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

// Renders the battle arena and placed units.
public class BattleArenaView extends javafx.scene.layout.BorderPane {
    private final GridPane grid;
    private final Pane unitLayer;
    private final Pane arenaPane;
    private final GameState gameState;
    private final javafx.scene.control.Label timerLabel;
    private final javafx.scene.control.Label scoreLabel;
    private final java.util.Map<Long, javafx.scene.Node> cellIndex = new java.util.HashMap<>();
    
    // Track hovered tile for highlighting
    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

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
        this.grid.setHgap(0);
        this.grid.setVgap(0);
        this.unitLayer = new Pane();

        // Make unit layer transparent to mouse events so clicks go to grid for placement
        unitLayer.setMouseTransparent(true);

        this.arenaPane = new Pane(grid, unitLayer);
        // Set preferred size to match grid size
        arenaPane.setPrefSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        // Bind unitLayer position to grid position to ensure perfect alignment
        unitLayer.layoutXProperty().bind(grid.layoutXProperty());
        unitLayer.layoutYProperty().bind(grid.layoutYProperty());

        // Handle clicks directly on the arena pane to get correct local coordinates
        arenaPane.setOnMouseClicked(e -> {
            if (onGridClick != null) {
                int[] coords = calculateTileCoordinates(e.getX(), e.getY());
                if (coords != null) {
                    onGridClick.accept(coords[0], coords[1]);
                }
            }
        });
        
        // Handle mouse movement for hover highlighting
        arenaPane.setOnMouseMoved(e -> {
            int[] coords = calculateTileCoordinates(e.getX(), e.getY());
            if (coords != null) {
                highlightHoveredTile(coords[0], coords[1]);
            } else {
                clearHoverHighlight();
            }
        });
        
        // Clear hover highlight when mouse leaves arena
        arenaPane.setOnMouseExited(e -> {clearHoverHighlight();});

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
        cellIndex.clear();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = gameState.getArena().getCell(x, y);
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                // Style based on type
                TileType type = cell.getTileType();

                // Treat towers as grass for the base tile so they look right when destroyed
                if (type == TileType.PRINCESS_TOWER_USER || type == TileType.PRINCESS_TOWER_COMPUTER || type == TileType.KING_TOWER_USER || type == TileType.KING_TOWER_COMPUTER) {
                    type = TileType.GRASS;
                }

                switch (type) {
                    case GRASS:
                        // Checkered pattern
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
                    default:
                        rect.setFill(Color.GRAY);
                        break;
                }
                // Remove stroke for seamless look
                rect.setStroke(Color.TRANSPARENT);
                rect.setStrokeWidth(0.0);

                grid.add(rect, x, y);
                indexCellNode(x, y, rect);
            }
        }

        // Render Towers on top
        renderTowerImages();
    }

    private final java.util.Map<com.kuroyale.model.GridPosition, javafx.scene.Node> activeTowerVisuals = new java.util.HashMap<>();
    // Track last visual positions for interpolation
    private final java.util.Map<com.kuroyale.model.Troop, javafx.geometry.Point2D> lastTroopPositions = new java.util.HashMap<>();
    // Track active troop visuals to prevent recreation
    private final java.util.Map<com.kuroyale.model.Troop, javafx.scene.Node> activeTroopVisuals = new java.util.HashMap<>();
    // Track active building visuals to prevent duplication
    private final java.util.Map<com.kuroyale.model.Building, javafx.scene.Node> activeBuildingVisuals = new java.util.HashMap<>();
    // Track active projectiles to prevent trails
    private final java.util.Map<com.kuroyale.model.Troop, javafx.scene.Node> activeProjectiles = new java.util.HashMap<>();
    private final java.util.Map<com.kuroyale.model.Tower, javafx.scene.Node> activeTowerProjectiles = new java.util.HashMap<>();
    private final java.util.Map<com.kuroyale.model.Building, javafx.scene.Node> activeBuildingProjectiles = new java.util.HashMap<>();
    // Track active spell effect visuals for cleanup
    private final java.util.Map<com.kuroyale.model.GameState.SpellEffect, javafx.scene.Node> activeSpellVisuals = new java.util.HashMap<>();
    // Track last troop state to detect animation changes
    private final java.util.Map<com.kuroyale.model.Troop, String> lastTroopState = new java.util.HashMap<>();

    private static final double LERP_FACTOR = 0.3; // Interpolation smoothness (0-1, higher = smoother but more lag)

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
        fg.setId("towerHpFg_" + x + "_" + y);

        // Health Text: "1400" (Remaining only)
        javafx.scene.text.Text healthText = new javafx.scene.text.Text(String.format("%.0f", currentHealth));
        healthText.setId("towerHpText_" + x + "_" + y);
        healthText.setFont(javafx.scene.text.Font.font("Arial Black", javafx.scene.text.FontWeight.BOLD, 10));
        healthText.setFill(Color.WHITE);
        healthText.setStroke(Color.BLACK);
        healthText.setStrokeWidth(0.5);

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
        // Index each covered coordinate to the stack
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                indexCellNode(x + dx, y + dy, towerStack);
            }
        }

        // Track visual
        activeTowerVisuals.put(new com.kuroyale.model.GridPosition(x, y), towerStack);
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

        // Remove destroyed towers
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.GridPosition, javafx.scene.Node>> it = activeTowerVisuals
                .entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.GridPosition, javafx.scene.Node> entry = it.next();
            com.kuroyale.model.GridPosition pos = entry.getKey();
            if (gameState.getArena().getTowerAt(pos.getX(), pos.getY()) == null) {
                // Tower is gone, remove visual
                grid.getChildren().remove(entry.getValue());
                it.remove();
            }
        }

        // Update existing tower health bars (inside grid stack panes)
        updateExistingTowerHealthBars();

        // Render active moving troops
        java.util.List<com.kuroyale.model.Troop> troops = gameState.getActiveTroops();
        java.util.Set<com.kuroyale.model.Troop> currentTroops = new java.util.HashSet<>(troops);

        // Cleanup visuals for dead troops
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.Troop, javafx.scene.Node>> troopIt = activeTroopVisuals
                .entrySet().iterator();
        while (troopIt.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.Troop, javafx.scene.Node> entry = troopIt.next();
            com.kuroyale.model.Troop t = entry.getKey();
            if (!currentTroops.contains(t)) {
                // Troop is dead or gone
                javafx.scene.Node node = entry.getValue();
                if (node instanceof AnimatedSprite) {
                    ((AnimatedSprite) node).stop();
                }
                unitLayer.getChildren().remove(node);
                // Remove associated health bars
                String hpBarId = "hp_" + t.hashCode();
                unitLayer.getChildren().removeIf(n -> hpBarId.equals(n.getId()));

                // Remove associated projectile
                if (activeProjectiles.containsKey(t)) {
                    unitLayer.getChildren().remove(activeProjectiles.get(t));
                    activeProjectiles.remove(t);
                }

                troopIt.remove();
                lastTroopPositions.remove(t);
                lastTroopState.remove(t);
            }
        }

        // Cleanup projectiles for troops that are no longer attacking
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.Troop, javafx.scene.Node>> projIt = activeProjectiles
                .entrySet().iterator();
        while (projIt.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.Troop, javafx.scene.Node> entry = projIt.next();
            com.kuroyale.model.Troop t = entry.getKey();
            if (!currentTroops.contains(t) || t.getUnitState() != com.kuroyale.model.UnitState.ATTACKING) {
                unitLayer.getChildren().remove(entry.getValue());
                projIt.remove();
            }
        }

        for (com.kuroyale.model.Troop troop : troops) {
            com.kuroyale.model.GridPosition pos = troop.getPosition();
            javafx.scene.Node cellNode = getGridCell(pos.getX(), pos.getY());
            if (cellNode != null) {
                // Get cell bounds for positioning
                javafx.geometry.Bounds cellBounds = cellNode.getBoundsInParent();

                // Visual interpolation for smooth movement
                double visualX, visualY;

                if (troop.getPath() != null && !troop.getPath().isEmpty()
                        && troop.getUnitState() == com.kuroyale.model.UnitState.MOVING) {
                    com.kuroyale.model.GridPosition current = troop.getPosition();
                    com.kuroyale.model.GridPosition next = troop.getPath().peekFirst();

                    // Linear interpolation: start + (end - start) * progress
                    double progress = troop.getMoveProgress();
                    progress = Math.max(0.0, Math.min(1.0, progress));

                    // Use actual cell bounds instead of arithmetic to avoid cumulative offset
                    javafx.scene.Node currentCell = getGridCell(current.getX(), current.getY());
                    javafx.scene.Node nextCell = getGridCell(next.getX(), next.getY());

                    if (currentCell != null && nextCell != null) {
                        javafx.geometry.Bounds currentBounds = currentCell.getBoundsInParent();
                        javafx.geometry.Bounds nextBounds = nextCell.getBoundsInParent();

                        double startX = currentBounds.getMinX();
                        double startY = currentBounds.getMinY();
                        double endX = nextBounds.getMinX();
                        double endY = nextBounds.getMinY();

                        visualX = startX + (endX - startX) * progress;
                        visualY = startY + (endY - startY) * progress;
                    } else {
                        // Fallback to arithmetic if cells not found
                        double startX = current.getX() * TILE_SIZE;
                        double startY = current.getY() * TILE_SIZE;
                        double endX = next.getX() * TILE_SIZE;
                        double endY = next.getY() * TILE_SIZE;

                        visualX = startX + (endX - startX) * progress;
                        visualY = startY + (endY - startY) * progress;
                    }
                } else {
                    // Not moving or no path, snap to grid using actual cell bounds
                    javafx.scene.Node cell = getGridCell(troop.getPosition().getX(), troop.getPosition().getY());
                    if (cell != null) {
                        javafx.geometry.Bounds bounds = cell.getBoundsInParent();
                        visualX = bounds.getMinX();
                        visualY = bounds.getMinY();
                    } else {
                        // Fallback to arithmetic
                        visualX = troop.getPosition().getX() * TILE_SIZE;
                        visualY = troop.getPosition().getY() * TILE_SIZE;
                    }
                }

                // Update last known position for reference (optional now, but good for debug)
                lastTroopPositions.put(troop, new javafx.geometry.Point2D(visualX, visualY));

                // Check if we already have a visual for this troop
                javafx.scene.Node unitNode = activeTroopVisuals.get(troop);
                String cardName = troop.getBaseCard().getName();
                boolean needsCreation = (unitNode == null);

                // Check state changes for animated units
                if (AnimatedSprite.isAnimated(cardName) && !needsCreation) {
                    String currentState = troop.getUnitState() == com.kuroyale.model.UnitState.ATTACKING ? "fight"
                            : "walk";
                    boolean isRage = gameState.isDoubleElixir();
                    String stateKey = currentState + (isRage ? "-rage" : "");

                    String lastState = lastTroopState.get(troop);
                    if (!stateKey.equals(lastState)) {
                        // State changed, try to update existing sprite
                        if (unitNode instanceof AnimatedSprite) {
                            try {
                                String gifPath = AnimatedSprite.buildGifPath(cardName, currentState,
                                        troop.isPlayerSide(), isRage);

                                double speedMult = troop.getMoveSpeed();
                                if ("fight".equals(currentState)) {
                                    double hitSpeed = troop.getCombatStats().getHitSpeedSeconds();
                                    speedMult = 1.0 / Math.max(0.1, hitSpeed);
                                }

                                ((AnimatedSprite) unitNode).updateAnimation(gifPath, speedMult);
                                lastTroopState.put(troop, stateKey);
                                // System.out.println("Updated sprite for " + cardName + " to " + currentState);
                            } catch (Exception e) {
                                // Fallback to recreation if update fails
                                unitLayer.getChildren().remove(unitNode);
                                needsCreation = true;
                            }
                        } else {
                            unitLayer.getChildren().remove(unitNode);
                            needsCreation = true;
                        }
                    }
                }

                if (needsCreation) {
                    // Create new visual
                    if (AnimatedSprite.isAnimated(cardName)) {
                        try {
                            String state = troop.getUnitState() == com.kuroyale.model.UnitState.ATTACKING ? "fight"
                                    : "walk";
                            boolean isRage = gameState.isDoubleElixir();
                            String stateKey = state + (isRage ? "-rage" : "");

                            String gifPath = AnimatedSprite.buildGifPath(cardName, state, troop.isPlayerSide(), isRage);

                            // Adjust animation speed based on movement speed
                            // Base speed is ~1.0 (Slow). Fast troops (1.6) should animate faster.
                            double speedMult = troop.getMoveSpeed();
                            if ("fight".equals(state)) {
                                // Fight animation speed based on hit speed
                                double hitSpeed = troop.getCombatStats().getHitSpeedSeconds();
                                speedMult = 1.0 / Math.max(0.1, hitSpeed);
                            }

                            AnimatedSprite sprite = new AnimatedSprite(gifPath, TILE_SIZE, speedMult);
                            System.out.println("Created new sprite for " + cardName + " state=" + state);
                            unitNode = sprite;
                            lastTroopState.put(troop, stateKey);
                        } catch (Exception e) {
                            Circle fallback = new Circle(TILE_SIZE / 2.5);
                            fallback.setFill(troop.isPlayerSide() ? Color.BLUE : Color.RED);
                            unitNode = fallback;
                        }
                    } else {
                        // Static image for others
                        try {
                            String imgPath = troop.getBaseCard().getImagePath();
                            javafx.scene.image.Image img = new javafx.scene.image.Image(
                                    getClass().getResourceAsStream(imgPath));
                            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                            iv.setFitWidth(TILE_SIZE);
                            iv.setFitHeight(TILE_SIZE);
                            iv.setPreserveRatio(true);
                            iv.setSmooth(true);
                            unitNode = iv;
                        } catch (Exception e) {
                            Circle fallback = new Circle(TILE_SIZE / 2.5);
                            fallback.setFill(troop.isPlayerSide() ? (troop.isAirUnit() ? Color.DODGERBLUE : Color.BLUE)
                                    : (troop.isAirUnit() ? Color.ORANGERED : Color.RED));
                            unitNode = fallback;
                        }
                    }

                    // Add to scene and cache
                    unitLayer.getChildren().add(unitNode);
                    activeTroopVisuals.put(troop, unitNode);
                }

                // Update position
                unitNode.setLayoutX(visualX);
                unitNode.setLayoutY(visualY);

                String hpBarId = "hp_" + troop.hashCode();
                unitLayer.getChildren().removeIf(n -> hpBarId.equals(n.getId())); // Troop health bar above the unit
                double maxHp = troop.getBaseCard().getHp();
                double curHp = Math.max(0, troop.getCurrentHealth());
                double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;
                double barWidth = TILE_SIZE * 0.9;
                double barHeight = 4;

                javafx.scene.shape.Rectangle hpBg = new javafx.scene.shape.Rectangle(barWidth, barHeight);
                hpBg.setId(hpBarId); // Tag for removal
                hpBg.setFill(Color.color(0.2, 0.2, 0.2, 0.8));
                hpBg.setStroke(Color.BLACK);
                hpBg.setStrokeWidth(0.3);

                // Calculate center based on visual position
                double centerX = visualX + cellBounds.getWidth() / 2.0;
                double centerY = visualY + cellBounds.getHeight() / 2.0;

                hpBg.setX(centerX - barWidth / 2.0);
                hpBg.setY(centerY - (TILE_SIZE / 2.5) - 6);

                javafx.scene.shape.Rectangle hpFg = new javafx.scene.shape.Rectangle(barWidth * pct, barHeight);
                hpFg.setId(hpBarId); // Tag for removal
                if (troop.isPlayerSide()) {
                    hpFg.setFill(pct > 0.5 ? Color.LIMEGREEN : (pct > 0.2 ? Color.GOLD : Color.CRIMSON));
                } else {
                    hpFg.setFill(Color.CRIMSON);
                }
                hpFg.setX(hpBg.getX());
                hpFg.setY(hpBg.getY());

                unitLayer.getChildren().addAll(hpBg, hpFg);

                //  projectile for ranged units
                if (troop.getUnitState() == com.kuroyale.model.UnitState.ATTACKING) {
                    // Projectile for ranged attackers (moving dot)
                    if (troop.getCombatStats() != null &&
                            troop.getCombatStats()
                                    .getAttackType() == com.kuroyale.model.CombatStats.AttackType.RANGED) {

                        com.kuroyale.model.Troop target = findNearestEnemyTroopInRange(troop);
                        if (target != null) {
                            javafx.scene.Node targetNode = getGridCell(target.getPosition().getX(),
                                    target.getPosition().getY());
                            if (targetNode != null) {
                                javafx.geometry.Bounds tb = targetNode.getBoundsInParent();
                                double tx = tb.getMinX() + tb.getWidth() / 2.0;
                                double ty = tb.getMinY() + tb.getHeight() / 2.0;
                                // Animate dot using attack cooldown progress to ensure forward motion
                                double duration = Math.max(0.15, troop.getCombatStats().getHitSpeedSeconds());
                                double cooldown = troop.getAttackCooldown();
                                double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration)); // 0..1 from
                                                                                                        // attacker to
                                                                                                        // target
                                double px = centerX + (tx - centerX) * phase;
                                double py = centerY + (ty - centerY) * phase;

                                // Get or create projectile
                                javafx.scene.Node projNode = activeProjectiles.get(troop);
                                if (projNode == null) {
                                    Circle dot = new Circle(2.5);
                                    dot.setFill(troop.isPlayerSide() ? Color.YELLOW : Color.ORANGE);
                                    dot.setStroke(Color.color(0, 0, 0, 0.35));
                                    dot.setStrokeWidth(0.8);
                                    projNode = dot;
                                    unitLayer.getChildren().add(projNode);
                                    activeProjectiles.put(troop, projNode);
                                }

                                // Update position
                                projNode.setLayoutX(px);
                                projNode.setLayoutY(py);
                                // Ensure it's visible (might have been removed if state flickered)
                                if (!unitLayer.getChildren().contains(projNode)) {
                                    unitLayer.getChildren().add(projNode);
                                }
                            }
                        }
                    }
                }

            }
        }

        // Render active buildings with caching to prevent duplication
        java.util.List<com.kuroyale.model.Building> buildings = gameState.getActiveBuildings();
        java.util.Set<com.kuroyale.model.Building> currentBuildings = new java.util.HashSet<>(buildings);

        // Cleanup visuals for destroyed buildings
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.Building, javafx.scene.Node>> buildingIt = activeBuildingVisuals.entrySet().iterator();
        while (buildingIt.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.Building, javafx.scene.Node> entry = buildingIt.next();
            com.kuroyale.model.Building b = entry.getKey();
            if (!currentBuildings.contains(b) || !b.isAlive()) {
                // Building is destroyed or gone
                unitLayer.getChildren().remove(entry.getValue());
                buildingIt.remove();
                // Also remove associated projectile
                if (activeBuildingProjectiles.containsKey(b)) {
                    unitLayer.getChildren().remove(activeBuildingProjectiles.get(b));
                    activeBuildingProjectiles.remove(b);
                }
            }
        }

        for (com.kuroyale.model.Building b : buildings) {
            int x = b.getPosition().getX();
            int y = b.getPosition().getY();
            int w = Math.max(1, b.getWidth());
            int h = Math.max(1, b.getHeight());

            // Check if we already have a visual for this building
            javafx.scene.Node buildingNode = activeBuildingVisuals.get(b);
            if (buildingNode == null) {
                // Create new building visual
                StackPane buildingStack = new StackPane();
                buildingStack.setPrefSize(TILE_SIZE * w, TILE_SIZE * h);

                try {
                    String imgPath = b.getImagePath();
                    java.io.InputStream is = imgPath != null ? getClass().getResourceAsStream(imgPath) : null;
                    if (is != null) {
                        javafx.scene.image.Image img = new javafx.scene.image.Image(is);
                        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(img);
                        imageView.setFitWidth(TILE_SIZE * w);
                        imageView.setFitHeight(TILE_SIZE * h);
                        imageView.setPreserveRatio(false);
                        imageView.setSmooth(true);
                        buildingStack.getChildren().add(imageView);
                    } else {
                        javafx.scene.shape.Rectangle fallback = new javafx.scene.shape.Rectangle(TILE_SIZE * w,
                                TILE_SIZE * h);
                        fallback.setFill(
                                b.isPlayerSide() ? javafx.scene.paint.Color.DARKBLUE
                                        : javafx.scene.paint.Color.DARKRED);
                        fallback.setStroke(javafx.scene.paint.Color.BLACK);
                        fallback.setStrokeWidth(0.5);
                        buildingStack.getChildren().add(fallback);
                    }
                } catch (Exception e) {
                    javafx.scene.shape.Rectangle fallback = new javafx.scene.shape.Rectangle(TILE_SIZE * w,
                            TILE_SIZE * h);
                    fallback.setFill(
                            b.isPlayerSide() ? javafx.scene.paint.Color.DARKBLUE : javafx.scene.paint.Color.DARKRED);
                    fallback.setStroke(javafx.scene.paint.Color.BLACK);
                    fallback.setStrokeWidth(0.5);
                    buildingStack.getChildren().add(fallback);
                }

                // Health bar (centered at top) - will be updated each frame
                double maxHp = b.getMaxHealth();
                double curHp = Math.max(0, b.getCurrentHealth());
                double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;
                double hbWidth = Math.max(40, TILE_SIZE * w - 6);
                double hbHeight = 12;

                javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(hbWidth, hbHeight);
                bg.setFill(javafx.scene.paint.Color.DARKBLUE);
                bg.setStroke(javafx.scene.paint.Color.BLACK);
                bg.setStrokeWidth(0.5);
                bg.setId("buildingHpBg_" + b.hashCode());

                javafx.scene.shape.Rectangle fg = new javafx.scene.shape.Rectangle(hbWidth * pct, hbHeight);
                fg.setFill(b.isPlayerSide() ? javafx.scene.paint.Color.ROYALBLUE : javafx.scene.paint.Color.CRIMSON);
                fg.setId("buildingHpFg_" + b.hashCode());
                StackPane hbPane = new StackPane(bg, fg);
                hbPane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                StackPane.setAlignment(hbPane, javafx.geometry.Pos.TOP_CENTER);
                StackPane.setMargin(hbPane, new javafx.geometry.Insets(2, 0, 0, 0));
                buildingStack.getChildren().add(hbPane);

                // Position on unitLayer using the top-left cell's bounds
                javafx.scene.Node topLeftCell = getGridCell(x, y);
                if (topLeftCell != null) {
                    javafx.geometry.Bounds bnds = topLeftCell.getBoundsInParent();
                    buildingStack.setLayoutX(bnds.getMinX());
                    buildingStack.setLayoutY(bnds.getMinY());
                }
                unitLayer.getChildren().add(buildingStack);
                activeBuildingVisuals.put(b, buildingStack);
                buildingNode = buildingStack;
            } else {
                // Update existing building health bar
                double maxHp = b.getMaxHealth();
                double curHp = Math.max(0, b.getCurrentHealth());
                double pct = maxHp > 0 ? (curHp / maxHp) : 0.0;
                double hbWidth = Math.max(40, TILE_SIZE * w - 6);

                javafx.scene.Node fgNode = buildingNode.lookup("#buildingHpFg_" + b.hashCode());
                if (fgNode instanceof javafx.scene.shape.Rectangle) {
                    ((javafx.scene.shape.Rectangle) fgNode).setWidth(hbWidth * pct);
                }
            }
        }

        // Visual projectiles for towers and buildings (moving dots)
        renderTowerProjectiles();
        renderBuildingProjectiles();

        // Render transient spell AoE overlays and auto-remove after 1s
        renderSpellEffects();
    }

    /* Helper method to calculate tile coordinates from mouse position.
     * Finds which cell actually contains the mouse point to avoid offset issues.*/
    private int[] calculateTileCoordinates(double mouseX, double mouseY) {
        // Use arithmetic calculation (same approach that works when no card is selected)
        javafx.geometry.Bounds gridBounds = grid.getBoundsInParent();
        
        // Calculate relative position within grid
        double gridX = mouseX - gridBounds.getMinX();
        double gridY = mouseY - gridBounds.getMinY();
        
        // Calculate tile coordinates using arithmetic
        int tileX = (int) Math.floor(gridX / TILE_SIZE);
        int tileY = (int) Math.floor(gridY / TILE_SIZE);
        
        // Ensure coordinates are within valid bounds
        if (tileX >= 0 && tileX < Arena.WIDTH && tileY >= 0 && tileY < Arena.HEIGHT) {
            return new int[] { tileX, tileY };
        }
        return null;
    }
    
    //Helper method to get the grid cell node at the specified grid coordinates.
    private javafx.scene.Node getGridCell(int x, int y) {
        return cellIndex.getOrDefault(key(x, y), null);
    }

    private void indexCellNode(int x, int y, javafx.scene.Node node) {
        cellIndex.put(key(x, y), node);
    }

    private long key(int x, int y) {
        return (((long) x) << 32) | (y & 0xFFFFFFFFL);
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
                        // Use glow effect for visibility on colored tiles
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

    //Highlights the tile at the specified coordinates to show where the mouse is hovering.
    private void highlightHoveredTile(int tileX, int tileY) {
        // If hovering over the same tile, no need to update
        if (currentHoveredTileX == tileX && currentHoveredTileY == tileY) {
            return;
        }
        
        // Clear previous hover highlight
        clearHoverHighlight();
        
        // Get the cell node for the hovered tile to get its bounds
        javafx.scene.Node node = getGridCell(tileX, tileY);
        if (node == null) {
            return;
        }
        
        // Skip towers (StackPanes) for hover highlight
        if (node instanceof StackPane) {
            return;
        }

        javafx.geometry.Bounds localBounds = node.getBoundsInLocal();
        
        // Convert local bounds (0,0 to TILE_SIZE, TILE_SIZE) to parent (arenaPane) coordinates
        javafx.geometry.Point2D topLeft = node.localToParent(0, 0);
        javafx.geometry.Point2D bottomRight = node.localToParent(TILE_SIZE, TILE_SIZE);
        
        // Calculate actual cell dimensions and position without effects
        double cellX = topLeft.getX();
        double cellY = topLeft.getY();
        double cellWidth = bottomRight.getX() - topLeft.getX();
        double cellHeight = bottomRight.getY() - topLeft.getY();
        
        // Use TILE_SIZE for overlay dimensions (not bounds which include effects)
        Rectangle overlay = new Rectangle(TILE_SIZE, TILE_SIZE);
        
        // Clash Royale style: semi-transparent cyan fill with bright border
        overlay.setFill(Color.color(0.0, 0.8, 1.0, 0.25)); // Cyan with 25% opacity
        overlay.setStroke(Color.CYAN);
        overlay.setStrokeWidth(2.0);
        overlay.setStrokeType(javafx.scene.shape.StrokeType.INSIDE); // Stroke inside to avoid gaps
        
        // Position overlay using actual cell position (without effects)
        overlay.setLayoutX(cellX);
        overlay.setLayoutY(cellY);
        
        // Make overlay transparent to mouse events so clicks pass through
        overlay.setMouseTransparent(true);
        
        // Add overlay to unitLayer
        unitLayer.getChildren().add(overlay);
        
        // Track current hovered tile
        currentHoveredTileX = tileX;
        currentHoveredTileY = tileY;
        currentHoveredOverlay = overlay;
    }
    
    //Clears the hover highlight from the currently hovered tile.
    private void clearHoverHighlight() {
        if (currentHoveredOverlay != null) {
            unitLayer.getChildren().remove(currentHoveredOverlay);
            currentHoveredOverlay = null;
        }
        
        currentHoveredTileX = -1;
        currentHoveredTileY = -1;
    }

    public GridPane getGrid() {
        return grid;
    }
    public int getTileSize() {
        return TILE_SIZE;
    }

    //find nearest enemy troop within range
    private com.kuroyale.model.Troop findNearestEnemyTroopInRange(com.kuroyale.model.Troop self) {
        com.kuroyale.model.Troop best = null;
        double bestDist = Double.MAX_VALUE;
        for (com.kuroyale.model.Troop t : gameState.getActiveTroops()) {
            if (!t.isAlive())
                continue;
            if (t.isPlayerSide() == self.isPlayerSide())
                continue;
            double dx = self.getPosition().getX() - t.getPosition().getX();
            double dy = self.getPosition().getY() - t.getPosition().getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double range = self.getCombatStats() != null ? self.getCombatStats().getRangeTiles()
                    : (int) Math.round(self.getAttackRange());
            if (dist <= range && dist < bestDist) {
                bestDist = dist;
                best = t;
            }
        }
        return best;
    }

    // Visualize tower shots as moving dots towards target troops
    private void renderTowerProjectiles() {
        com.kuroyale.model.Arena arena = gameState.getArena();
        java.util.Set<com.kuroyale.model.Tower> currentAttackingTowers = new java.util.HashSet<>();

        java.util.Map<com.kuroyale.model.Tower, java.util.List<com.kuroyale.model.GridCell>> groups = new java.util.HashMap<>();
        for (com.kuroyale.model.GridCell cell : arena.getAllCells()) {
            com.kuroyale.model.TileType tt = cell.getTileType();
            boolean isTowerTile = tt == com.kuroyale.model.TileType.PRINCESS_TOWER_USER
                    || tt == com.kuroyale.model.TileType.PRINCESS_TOWER_COMPUTER
                    || tt == com.kuroyale.model.TileType.KING_TOWER_USER
                    || tt == com.kuroyale.model.TileType.KING_TOWER_COMPUTER;
            if (!isTowerTile)
                continue;
            com.kuroyale.model.Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null || tower.getCurrentHealth() <= 0)
                continue;
            groups.computeIfAbsent(tower, k -> new java.util.ArrayList<>()).add(cell);
        }

        for (java.util.Map.Entry<com.kuroyale.model.Tower, java.util.List<com.kuroyale.model.GridCell>> e : groups
                .entrySet()) {
            com.kuroyale.model.Tower tower = e.getKey();
            java.util.List<com.kuroyale.model.GridCell> cells = e.getValue();
            boolean isPlayerTower = false;
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            for (com.kuroyale.model.GridCell c : cells) {
                com.kuroyale.model.TileType tt = c.getTileType();
                if (tt == com.kuroyale.model.TileType.PRINCESS_TOWER_USER
                        || tt == com.kuroyale.model.TileType.KING_TOWER_USER)
                    isPlayerTower = true;
                int x = c.getPosition().getX();
                int y = c.getPosition().getY();
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
            int cx = (minX + maxX) / 2;
            int cy = (minY + maxY) / 2;
            javafx.scene.Node centerCell = getGridCell(cx, cy);
            if (centerCell == null)
                continue;
            javafx.geometry.Bounds cb = centerCell.getBoundsInParent();
            double sx = cb.getMinX() + cb.getWidth() / 2.0;
            double sy = cb.getMinY() + cb.getHeight() / 2.0;

            // Find nearest enemy troop within tower range
            com.kuroyale.model.Troop target = null;
            double bestDist = Double.MAX_VALUE;
            for (com.kuroyale.model.Troop t : gameState.getActiveTroops()) {
                if (!t.isAlive())
                    continue;
                if (t.isPlayerSide() == isPlayerTower)
                    continue;
                if (tower.getTargetType() == com.kuroyale.model.TargetType.GROUND && t.isAirUnit())
                    continue;
                javafx.scene.Node tn = getGridCell(t.getPosition().getX(), t.getPosition().getY());
                if (tn == null)
                    continue;
                javafx.geometry.Bounds tb = tn.getBoundsInParent();
                double tx = tb.getMinX() + tb.getWidth() / 2.0;
                double ty = tb.getMinY() + tb.getHeight() / 2.0;
                double dist = Math.hypot(tx - sx, ty - sy) / TILE_SIZE; // in tiles approx
                if (dist <= Math.round(tower.getRange()) && dist < bestDist) {
                    bestDist = dist;
                    target = t;
                }
            }

            if (target != null) {
                currentAttackingTowers.add(tower);

                double duration = Math.max(0.15, tower.getHitSpeed());
                double cooldown = tower.getAttackCooldown();
                double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));
                javafx.scene.Node tn = getGridCell(target.getPosition().getX(), target.getPosition().getY());
                if (tn != null) {
                    javafx.geometry.Bounds tb = tn.getBoundsInParent();
                    double tx = tb.getMinX() + tb.getWidth() / 2.0;
                    double ty = tb.getMinY() + tb.getHeight() / 2.0;

                    double px = sx + (tx - sx) * phase;
                    double py = sy + (ty - sy) * phase;

                    // Get or create projectile
                    javafx.scene.Node projNode = activeTowerProjectiles.get(tower);
                    if (projNode == null) {
                        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(2.5);
                        dot.setFill(isPlayerTower ? javafx.scene.paint.Color.LIGHTSKYBLUE
                                : javafx.scene.paint.Color.ORANGERED);
                        dot.setStroke(javafx.scene.paint.Color.color(0, 0, 0, 0.35));
                        dot.setStrokeWidth(0.8);
                        projNode = dot;
                        unitLayer.getChildren().add(projNode);
                        activeTowerProjectiles.put(tower, projNode);
                    }

                    // Update position
                    projNode.setLayoutX(px);
                    projNode.setLayoutY(py);
                    // Ensure visible
                    if (!unitLayer.getChildren().contains(projNode)) {
                        unitLayer.getChildren().add(projNode);
                    }
                }
            }
        }

        // Cleanup inactive tower projectiles
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.Tower, javafx.scene.Node>> it = activeTowerProjectiles
                .entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.Tower, javafx.scene.Node> entry = it.next();
            if (!currentAttackingTowers.contains(entry.getKey())) {
                unitLayer.getChildren().remove(entry.getValue());
                it.remove();
            }
        }
    }

    // Visualize building shots as moving dots towards target troops
    private void renderBuildingProjectiles() {
        java.util.Set<com.kuroyale.model.Building> currentAttackingBuildings = new java.util.HashSet<>();

        for (com.kuroyale.model.Building b : gameState.getActiveBuildings()) {
            if (!b.isAlive())
                continue;
            int cx = b.getPosition().getX() + Math.max(0, b.getWidth() - 1) / 2;
            int cy = b.getPosition().getY() + Math.max(0, b.getHeight() - 1) / 2;
            javafx.scene.Node centerCell = getGridCell(cx, cy);
            if (centerCell == null)
                continue;
            javafx.geometry.Bounds cb = centerCell.getBoundsInParent();
            double sx = cb.getMinX() + cb.getWidth() / 2.0;
            double sy = cb.getMinY() + cb.getHeight() / 2.0;

            // Find nearest enemy troop within building range
            com.kuroyale.model.Troop target = null;
            double bestDist = Double.MAX_VALUE;
            for (com.kuroyale.model.Troop t : gameState.getActiveTroops()) {
                if (!t.isAlive())
                    continue;
                if (t.isPlayerSide() == b.isPlayerSide())
                    continue;
                if (!b.canTargetTroop(t))
                    continue;
                javafx.scene.Node tn = getGridCell(t.getPosition().getX(), t.getPosition().getY());
                if (tn == null)
                    continue;
                javafx.geometry.Bounds tb = tn.getBoundsInParent();
                double tx = tb.getMinX() + tb.getWidth() / 2.0;
                double ty = tb.getMinY() + tb.getHeight() / 2.0;
                double dist = Math.hypot(tx - sx, ty - sy) / TILE_SIZE; // tiles approx
                if (dist <= b.getRangeTiles() && dist < bestDist) {
                    bestDist = dist;
                    target = t;
                }
            }

            if (target != null) {
                currentAttackingBuildings.add(b);

                double duration = Math.max(0.15, b.getHitSpeedSeconds());
                double cooldown = b.getAttackCooldown();
                double phase = 1.0 - Math.max(0.0, Math.min(1.0, cooldown / duration));
                javafx.scene.Node tn = getGridCell(target.getPosition().getX(), target.getPosition().getY());
                if (tn != null) {
                    javafx.geometry.Bounds tb = tn.getBoundsInParent();
                    double tx = tb.getMinX() + tb.getWidth() / 2.0;
                    double ty = tb.getMinY() + tb.getHeight() / 2.0;

                    double px = sx + (tx - sx) * phase;
                    double py = sy + (ty - sy) * phase;

                    // Get or create projectile
                    javafx.scene.Node projNode = activeBuildingProjectiles.get(b);
                    if (projNode == null) {
                        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(2.5);
                        dot.setFill(b.isPlayerSide() ? javafx.scene.paint.Color.LIGHTSKYBLUE
                                : javafx.scene.paint.Color.ORANGERED);
                        dot.setStroke(javafx.scene.paint.Color.color(0, 0, 0, 0.35));
                        dot.setStrokeWidth(0.8);
                        projNode = dot;
                        unitLayer.getChildren().add(projNode);
                        activeBuildingProjectiles.put(b, projNode);
                    }

                    // Update position
                    projNode.setLayoutX(px);
                    projNode.setLayoutY(py);
                    // Ensure visible
                    if (!unitLayer.getChildren().contains(projNode)) {
                        unitLayer.getChildren().add(projNode);
                    }
                }
            }
        }

        // Cleanup inactive building projectiles
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.Building, javafx.scene.Node>> it = activeBuildingProjectiles
                .entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.Building, javafx.scene.Node> entry = it.next();
            if (!currentAttackingBuildings.contains(entry.getKey())) {
                unitLayer.getChildren().remove(entry.getValue());
                it.remove();
            }
        }
    }

    private void renderSpellEffects() {
        java.util.List<com.kuroyale.model.GameState.SpellEffect> effects = gameState.getActiveSpellEffects();
        java.util.Set<com.kuroyale.model.GameState.SpellEffect> currentEffects = new java.util.HashSet<>();
        if (effects != null) {
            currentEffects.addAll(effects);
        }

        // Cleanup expired spell visuals
        java.util.Iterator<java.util.Map.Entry<com.kuroyale.model.GameState.SpellEffect, javafx.scene.Node>> spellIt = activeSpellVisuals
                .entrySet().iterator();
        while (spellIt.hasNext()) {
            java.util.Map.Entry<com.kuroyale.model.GameState.SpellEffect, javafx.scene.Node> entry = spellIt.next();
            if (!currentEffects.contains(entry.getKey())) {
                // Spell effect expired, remove visual
                unitLayer.getChildren().remove(entry.getValue());
                spellIt.remove();
            }
        }

        // Create visuals for new spell effects
        if (effects == null || effects.isEmpty())
            return;
        for (com.kuroyale.model.GameState.SpellEffect se : effects) {
            // Skip if we already have a visual for this effect
            if (activeSpellVisuals.containsKey(se))
                continue;

            com.kuroyale.model.GridPosition c = se.center;
            if (c == null)
                continue;
            javafx.scene.Node centerCell = getGridCell(c.getX(), c.getY());
            if (centerCell == null)
                continue;
            javafx.geometry.Bounds cb = centerCell.getBoundsInParent();
            double cx = cb.getMinX() + cb.getWidth() / 2.0;
            double cy = cb.getMinY() + cb.getHeight() / 2.0;
            double rPixels = se.radiusTiles * TILE_SIZE;
            javafx.scene.shape.Circle aoe = new javafx.scene.shape.Circle(cx, cy, rPixels);
            aoe.setFill(se.isPlayerSide ? javafx.scene.paint.Color.color(0.2, 0.6, 1.0, 0.18)
                    : javafx.scene.paint.Color.color(1.0, 0.3, 0.2, 0.18));
            aoe.setStroke(javafx.scene.paint.Color.color(1, 1, 1, 0.6));
            aoe.setStrokeWidth(1.2);
            unitLayer.getChildren().add(aoe);
            activeSpellVisuals.put(se, aoe);
        }
    }

    // Update the health bar rectangles/text created in renderTowerAt
    private void updateExistingTowerHealthBars() {
        com.kuroyale.model.Arena arena = gameState.getArena();
        if (arena.getLayout() == null)
            return;
        com.kuroyale.model.ArenaLayout layout = arena.getLayout();
        // Princess towers
        if (layout.getPrincessTowerPositions() != null) {
            for (com.kuroyale.model.GridPosition p : layout.getPrincessTowerPositions()) {
                updateSingleTowerBar(p.getX(), p.getY(), 3);
                updateSingleTowerBar(p.getX(), com.kuroyale.model.Arena.HEIGHT - 3 - p.getY(), 3);
            }
        }
        // King towers
        if (layout.getKingTowerPosition() != null) {
            com.kuroyale.model.GridPosition p = layout.getKingTowerPosition();
            updateSingleTowerBar(p.getX(), p.getY(), 4);
            updateSingleTowerBar(p.getX(), com.kuroyale.model.Arena.HEIGHT - 4 - p.getY(), 4);
        }
    }

    private void updateSingleTowerBar(int x, int y, int size) {
        com.kuroyale.model.Tower tower = gameState.getArena().getTowerAt(x, y);
        if (tower == null)
            return;
        double currentHealth = tower.getCurrentHealth();
        double maxHealth = tower.getMaxHealth();
        double width = size == 4 ? 50 : 40;
        double pct = maxHealth > 0 ? Math.max(0, currentHealth) / maxHealth : 0.0;
        javafx.scene.Node fgNode = grid.lookup("#towerHpFg_" + x + "_" + y);
        if (fgNode instanceof javafx.scene.shape.Rectangle) {
            ((javafx.scene.shape.Rectangle) fgNode).setWidth(width * pct);
        }
        javafx.scene.Node textNode = grid.lookup("#towerHpText_" + x + "_" + y);
        if (textNode instanceof javafx.scene.text.Text) {
            ((javafx.scene.text.Text) textNode).setText(String.format("%.0f", currentHealth));
        }
    }
}