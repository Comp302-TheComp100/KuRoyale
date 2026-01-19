package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.GridCell;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.enums.TileType;
import com.kuroyale.model.logic.GameState;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

// Renders the battle arena and placed units.
public class BattleArenaView extends javafx.scene.layout.BorderPane implements com.kuroyale.event.GameEventListener {
    private final GridPane grid;
    private final Pane unitLayer;
    private final Pane effectLayer;
    private final Canvas highlightLayer;
    private final Canvas debugLayer;
    private final Pane arenaPane;
    private final GameState gameState;
    private final com.kuroyale.model.logic.PvPGameState pvpGameState; // For PvP mode
    private final java.util.Map<Long, javafx.scene.Node> cellIndex = new java.util.HashMap<>();

    // Track hovered tile for highlighting
    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

    private static final int TILE_SIZE = com.kuroyale.util.GameConstants.TILE_SIZE;

    // Renderers
    private TroopRenderer troopRenderer;
    private ProjectileRenderer projectileRenderer;
    private TowerRenderer towerRenderer;
    private BuildingRenderer buildingRenderer;

    private final java.util.List<ActiveSpellVisual> activeSpellVisuals = new java.util.ArrayList<>();

    private static class ActiveSpellVisual {
        final javafx.scene.Node node;
        double timeRemaining;

        ActiveSpellVisual(javafx.scene.Node node, double timeRemaining) {
            this.node = node;
            this.timeRemaining = timeRemaining;
        }
    }

    private java.util.function.BiConsumer<Integer, Integer> onGridClick;

    public void setOnGridClicked(java.util.function.BiConsumer<Integer, Integer> handler) {
        this.onGridClick = handler;
    }

    /**
     * Constructor for PvP mode.
     */
    public BattleArenaView(com.kuroyale.model.logic.PvPGameState pvpGameState) {
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.gameState = null;
        this.pvpGameState = pvpGameState;

        Arena arena = pvpGameState.getArena();

        // Initialize with common setup
        this.grid = new GridPane();
        this.grid.setHgap(0);
        this.grid.setVgap(0);
        this.unitLayer = new Pane();
        this.unitLayer.setMouseTransparent(true);
        this.effectLayer = new Pane();
        this.effectLayer.setMouseTransparent(true);
        this.highlightLayer = new Canvas(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        this.highlightLayer.setMouseTransparent(true);
        this.debugLayer = new Canvas(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        this.debugLayer.setMouseTransparent(true);
        this.arenaPane = new Pane(grid, highlightLayer, unitLayer, effectLayer, debugLayer);
        arenaPane.setPrefSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        bindLayers();
        setupInteractions();

        // PvP mode: No sidebar (timer/score handled in controller)
        StackPane centerContainer = new StackPane(arenaPane);
        centerContainer.setAlignment(javafx.geometry.Pos.CENTER);
        centerContainer.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
        this.setCenter(centerContainer);

        initRenderers();

        com.kuroyale.event.GameEventBus.getInstance().subscribe(this);

        renderArenaPvP(arena);
    }

    public BattleArenaView(GameState gameState) {
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.gameState = gameState;
        this.pvpGameState = null;

        // Center Arena
        this.grid = new GridPane();
        this.grid.setHgap(0);
        this.grid.setVgap(0);
        this.unitLayer = new Pane();
        this.unitLayer.setMouseTransparent(true);
        this.effectLayer = new Pane();
        this.effectLayer.setMouseTransparent(true);
        this.highlightLayer = new Canvas(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        this.highlightLayer.setMouseTransparent(true);
        this.debugLayer = new Canvas(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        this.debugLayer.setMouseTransparent(true);
        this.arenaPane = new Pane(grid, highlightLayer, unitLayer, effectLayer, debugLayer);
        arenaPane.setPrefSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);

        bindLayers();
        setupInteractions();

        StackPane centerContainer = new StackPane(arenaPane);
        // Explicitly center the arenaPane within the StackPane
        StackPane.setAlignment(arenaPane, javafx.geometry.Pos.CENTER);
        // Constrain arenaPane to its preferred size so it doesn't stretch
        arenaPane.setMaxSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        arenaPane.setMinSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        // Shift arena 20px to the left
        arenaPane.setTranslateX(-10);

        this.setCenter(centerContainer);

        initRenderers();

        com.kuroyale.event.GameEventBus.getInstance().subscribe(this);

        renderArena();
    }

    private void bindLayers() {
        unitLayer.layoutXProperty().bind(grid.layoutXProperty());
        unitLayer.layoutYProperty().bind(grid.layoutYProperty());
        effectLayer.layoutXProperty().bind(grid.layoutXProperty());
        effectLayer.layoutYProperty().bind(grid.layoutYProperty());
        highlightLayer.layoutXProperty().bind(grid.layoutXProperty());
        highlightLayer.layoutYProperty().bind(grid.layoutYProperty());
        debugLayer.layoutXProperty().bind(grid.layoutXProperty());
        debugLayer.layoutYProperty().bind(grid.layoutYProperty());
    }

    private void setupInteractions() {
        arenaPane.setOnMouseClicked(e -> {
            if (onGridClick != null) {
                int[] coords = calculateTileCoordinates(e.getX(), e.getY());
                if (coords != null) {
                    onGridClick.accept(coords[0], coords[1]);
                }
            }
        });

        arenaPane.setOnMouseMoved(e -> {
            int[] coords = calculateTileCoordinates(e.getX(), e.getY());
            if (coords != null) {
                highlightHoveredTile(coords[0], coords[1]);
            } else {
                clearHoverHighlight();
            }
        });

        arenaPane.setOnMouseExited(e -> clearHoverHighlight());
    }

    private void initRenderers() {
        this.troopRenderer = new TroopRenderer(unitLayer);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode);
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);
    }

    // ==========================================
    // Visual Effects
    // ==========================================

    public void showComboText(String text) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(text + "!");
        label.setStyle(
                "-fx-font-size: 32px; -fx-text-fill: gold; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);");

        // Center relative to arena size using layoutX/Y binding (but strictly centered)
        // We use subtract(200) to move it higher as requested
        label.layoutXProperty().bind(arenaPane.widthProperty().subtract(label.widthProperty()).divide(2));
        label.layoutYProperty()
                .bind(arenaPane.heightProperty().subtract(label.heightProperty()).divide(2).subtract(200));

        // Add to effectLayer so it's on top and not cleared
        arenaPane.getChildren().add(label);

        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(2.0),
                label);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setOnFinished(e -> arenaPane.getChildren().remove(label));

        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                javafx.util.Duration.seconds(2.0), label);
        tt.setByY(-50);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(ft, tt);
        pt.play();
    }

    public void playDamageEffect(com.kuroyale.model.entities.Tower tower) {
        javafx.scene.Node towerNode = getTowerNode(tower);
        if (towerNode != null && towerNode instanceof StackPane) {
            StackPane stack = (StackPane) towerNode;

            // 1. Red Overlay Flash
            Rectangle overlay = new Rectangle(stack.getWidth(), stack.getHeight());
            overlay.setFill(Color.RED);
            overlay.setOpacity(0.0);
            overlay.setMouseTransparent(true);

            stack.getChildren().add(overlay);

            javafx.animation.FadeTransition flash = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(100), overlay);
            flash.setFromValue(0.0);
            flash.setToValue(0.3);
            flash.setCycleCount(2);
            flash.setAutoReverse(true);
            flash.setOnFinished(e -> stack.getChildren().remove(overlay));
            flash.play();

            // 2. Shake
            javafx.animation.TranslateTransition shake = new javafx.animation.TranslateTransition(
                    javafx.util.Duration.millis(50), towerNode);
            shake.setByX(2);
            shake.setCycleCount(4);
            shake.setAutoReverse(true);
            shake.play();
        }
    }

    public void playTowerDeathEffect(com.kuroyale.model.entities.Tower tower) {
        // Use robust position calculation
        javafx.geometry.Point2D center = getTowerCenterPosition(tower);
        if (center == null)
            return;

        double startX = center.getX();
        double startY = center.getY();

        // 1. Procedural Explosion
        javafx.scene.shape.Circle explosionCore = new javafx.scene.shape.Circle(10, Color.ORANGE);
        explosionCore.setStroke(Color.RED);
        explosionCore.setStrokeWidth(2);
        explosionCore.setTranslateX(startX);
        explosionCore.setTranslateY(startY);

        javafx.scene.shape.Circle explosionRing = new javafx.scene.shape.Circle(10, Color.TRANSPARENT);
        explosionRing.setStroke(Color.YELLOW);
        explosionRing.setStrokeWidth(4);
        explosionRing.setTranslateX(startX);
        explosionRing.setTranslateY(startY);

        // Add to effectLayer
        effectLayer.getChildren().addAll(explosionCore, explosionRing);

        javafx.animation.Timeline explodeAnim = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(explosionCore.radiusProperty(), 10),
                        new javafx.animation.KeyValue(explosionCore.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(400),
                        new javafx.animation.KeyValue(explosionCore.radiusProperty(), 60),
                        new javafx.animation.KeyValue(explosionCore.opacityProperty(), 0.0)),
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO,
                        new javafx.animation.KeyValue(explosionRing.radiusProperty(), 10),
                        new javafx.animation.KeyValue(explosionRing.opacityProperty(), 1.0),
                        new javafx.animation.KeyValue(explosionRing.strokeWidthProperty(), 4)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(600),
                        new javafx.animation.KeyValue(explosionRing.radiusProperty(), 80),
                        new javafx.animation.KeyValue(explosionRing.opacityProperty(), 0.0),
                        new javafx.animation.KeyValue(explosionRing.strokeWidthProperty(), 0)));

        explodeAnim.setOnFinished(e -> effectLayer.getChildren().removeAll(explosionCore, explosionRing));
        explodeAnim.play();
    }

    /**
     * Calculates the center position of a tower in the arena pane coordinates.
     * Robust against the visual node being removed.
     */
    public javafx.geometry.Point2D getTowerCenterPosition(com.kuroyale.model.entities.Tower tower) {
        if (tower == null)
            return null;

        // Try getting visual node first (most accurate for scene graph)
        javafx.scene.Node node = getTowerNode(tower);
        if (node != null && node.getParent() != null) {
            javafx.geometry.Bounds bounds = node.getBoundsInParent();
            return new javafx.geometry.Point2D(bounds.getCenterX(), bounds.getCenterY());
        }

        // Fallback: Calculate from grid position and type
        com.kuroyale.model.entities.GridPosition pos = tower.getPosition();
        if (pos == null)
            return null;

        int size = (tower.getType() == com.kuroyale.model.entities.Tower.TowerType.KING) ? 4 : 3;

        // Grid is at (0,0) in arenaPane usually, but let's double check alignment using
        // TILE_SIZE
        double x = (pos.getX() + size / 2.0) * TILE_SIZE;
        double y = (pos.getY() + size / 2.0) * TILE_SIZE;

        return new javafx.geometry.Point2D(x, y);
    }

    // ==========================================
    // Logic Methods
    // ==========================================

    private void renderArena() {
        grid.getChildren().clear();
        cellIndex.clear();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = gameState.getArena().getCell(x, y);
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                TileType type = cell.getTileType();
                rect.setFill(com.kuroyale.view.battle.ViewUtils.getTileColor(type, x, y));
                rect.setStroke(Color.TRANSPARENT);
                rect.setStrokeWidth(0.0);
                grid.add(rect, x, y);
                indexCellNode(x, y, rect);
            }
        }
        towerRenderer.renderTowers(gameState.getArena());
    }

    private void renderArenaPvP(Arena arena) {
        grid.getChildren().clear();
        cellIndex.clear();

        for (int x = 0; x < Arena.WIDTH; x++) {
            for (int y = 0; y < Arena.HEIGHT; y++) {
                GridCell cell = arena.getCell(x, y);
                Rectangle rect = new Rectangle(TILE_SIZE, TILE_SIZE);
                TileType type = cell.getTileType();
                rect.setFill(com.kuroyale.view.battle.ViewUtils.getTileColor(type, x, y));
                rect.setStroke(Color.TRANSPARENT);
                rect.setStrokeWidth(0.0);
                grid.add(rect, x, y);
                indexCellNode(x, y, rect);
            }
        }
        towerRenderer.renderTowers(arena);
    }

    private Arena getArena() {
        if (gameState != null) {
            return gameState.getArena();
        } else if (pvpGameState != null) {
            return pvpGameState.getArena();
        }
        return null;
    }

    public void update(double deltaTime) {
        towerRenderer.cleanupDestroyedTowers(gameState.getArena());
        towerRenderer.updateHealthBars(gameState.getArena());
        troopRenderer.render(gameState);
        buildingRenderer.render(gameState);
        projectileRenderer.render(gameState.getProjectiles(), deltaTime);
        updateSpellEffects(deltaTime);
        renderDebug();
    }

    public void updatePvP(double deltaTime) {
        if (pvpGameState == null)
            return;
        Arena arena = pvpGameState.getArena();
        towerRenderer.cleanupDestroyedTowers(arena);
        towerRenderer.updateHealthBars(arena);
        troopRenderer.renderPvP(pvpGameState);
        buildingRenderer.renderPvP(pvpGameState);
        projectileRenderer.render(pvpGameState.getProjectiles(), deltaTime);
        updateSpellEffects(deltaTime);
        renderDebug();
    }

    private void renderDebug() {
        // Debug rendering disabled - uncomment to re-enable
        /*
         * GraphicsContext gc = debugLayer.getGraphicsContext2D();
         * gc.clearRect(0, 0, debugLayer.getWidth(), debugLayer.getHeight());
         * 
         * Arena arena = getArena();
         * if (arena == null)
         * return;
         * 
         * // 1. Grid Lines
         * gc.setStroke(Color.rgb(200, 200, 200, 0.15));
         * gc.setLineWidth(1.0);
         * for (int x = 0; x < Arena.WIDTH; x++) {
         * for (int y = 0; y < Arena.HEIGHT; y++) {
         * gc.strokeRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
         * }
         * }
         * 
         * // 2. Highlight Bridge Walkable Areas
         * gc.setFill(Color.rgb(0, 0, 255, 0.3)); // Blue transparent
         * gc.setStroke(Color.BLUE);
         * gc.setLineWidth(2.0);
         * 
         * for (int x = 0; x < Arena.WIDTH; x++) {
         * for (int y = 0; y < Arena.HEIGHT; y++) {
         * GridCell cell = arena.getCell(x, y);
         * if (cell.getTileType() == TileType.BRIDGE) {
         * gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
         * gc.strokeRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
         * }
         * }
         * }
         * 
         * // 3. Highlight Tower Areas
         * gc.setFill(Color.rgb(0, 255, 0, 0.3)); // Green transparent
         * gc.setStroke(Color.GREEN);
         * gc.setLineWidth(2.0);
         * 
         * for (com.kuroyale.model.entities.Tower tower : arena.getAllTowers()) {
         * com.kuroyale.model.entities.GridPosition pos = tower.getPosition();
         * if (pos == null)
         * continue;
         * 
         * int size = (tower.getType() ==
         * com.kuroyale.model.entities.Tower.TowerType.KING) ? 4 : 3;
         * 
         * // Tower position is top-left
         * gc.fillRect(pos.getX() * TILE_SIZE, pos.getY() * TILE_SIZE, size * TILE_SIZE,
         * size * TILE_SIZE);
         * gc.strokeRect(pos.getX() * TILE_SIZE, pos.getY() * TILE_SIZE, size *
         * TILE_SIZE, size * TILE_SIZE);
         * }
         * 
         * // 4. Highlight Troop Collision Areas
         * java.util.List<com.kuroyale.model.entities.Troop> troops = null;
         * if (gameState != null) {
         * troops = gameState.getActiveTroops();
         * } else if (pvpGameState != null) {
         * troops = pvpGameState.getActiveTroops();
         * }
         * 
         * if (troops != null) {
         * gc.setStroke(Color.RED);
         * gc.setLineWidth(1.0);
         * gc.setFill(Color.rgb(255, 0, 0, 0.2));
         * 
         * for (com.kuroyale.model.entities.Troop troop : troops) {
         * com.kuroyale.model.entities.Vector2 pos = troop.getWorldPosition();
         * if (pos == null)
         * continue;
         * 
         * double r = troop.getCollisionRadius();
         * double dPixels = r * 2.0 * TILE_SIZE;
         * 
         * double cx = pos.getX() * TILE_SIZE;
         * double cy = pos.getY() * TILE_SIZE;
         * 
         * double tlX = cx - dPixels / 2.0;
         * double tlY = cy - dPixels / 2.0;
         * 
         * gc.fillOval(tlX, tlY, dPixels, dPixels);
         * gc.strokeOval(tlX, tlY, dPixels, dPixels);
         * }
         * }
         */
    }

    private int[] calculateTileCoordinates(double mouseX, double mouseY) {
        double gridOffsetX = grid.getLayoutX();
        double gridOffsetY = grid.getLayoutY();
        double gridX = mouseX - gridOffsetX;
        double gridY = mouseY - gridOffsetY;
        int tileX = (int) Math.floor(gridX / TILE_SIZE);
        int tileY = (int) Math.floor(gridY / TILE_SIZE);
        if (tileX >= 0 && tileX < Arena.WIDTH && tileY >= 0 && tileY < Arena.HEIGHT) {
            return new int[] { tileX, tileY };
        }
        return null;
    }

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
        GraphicsContext gc = highlightLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());

        if (!show)
            return;

        gc.setFill(com.kuroyale.util.GameColors.HIGHLIGHT_VALID);

        if (isSpell) {
            gc.fillRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());
        } else {
            Arena arena = getArena();
            if (arena == null)
                return;
            for (int x = 0; x < Arena.WIDTH; x++) {
                for (int y = Arena.HEIGHT / 2; y < Arena.HEIGHT; y++) {
                    GridCell cell = arena.getCell(x, y);
                    if (cell.getTileType() == TileType.GRASS) {
                        gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    public void highlightPlayer2ValidCells(boolean show, boolean isSpell) {
        GraphicsContext gc = highlightLayer.getGraphicsContext2D();
        gc.clearRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());

        if (!show)
            return;

        gc.setFill(com.kuroyale.util.GameColors.HIGHLIGHT_VALID_P2);

        if (isSpell) {
            gc.fillRect(0, 0, highlightLayer.getWidth(), highlightLayer.getHeight());
        } else {
            Arena arena = getArena();
            if (arena == null)
                return;
            for (int x = 0; x < Arena.WIDTH; x++) {
                for (int y = 0; y < Arena.HEIGHT / 2; y++) {
                    GridCell cell = arena.getCell(x, y);
                    if (cell.getTileType() == TileType.GRASS) {
                        gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    private void highlightHoveredTile(int tileX, int tileY) {
        if (currentHoveredTileX == tileX && currentHoveredTileY == tileY) {
            return;
        }

        clearHoverHighlight();

        javafx.scene.Node node = getGridCell(tileX, tileY);
        if (node == null) {
            return;
        }

        if (node instanceof StackPane) {
            return;
        }

        javafx.geometry.Point2D topLeft = node.localToParent(0, 0);

        double cellX = topLeft.getX();
        double cellY = topLeft.getY();

        Rectangle overlay = new Rectangle(TILE_SIZE, TILE_SIZE);

        overlay.setFill(com.kuroyale.util.GameColors.HOVER_FILL);
        overlay.setStroke(com.kuroyale.util.GameColors.HOVER_STROKE);
        overlay.setStrokeWidth(2.0);
        overlay.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);

        overlay.setLayoutX(cellX + TILE_SIZE / 2.0 - TILE_SIZE / 2.0);
        overlay.setLayoutY(cellY);

        overlay.setMouseTransparent(true);

        unitLayer.getChildren().add(overlay);

        currentHoveredTileX = tileX;
        currentHoveredTileY = tileY;
        currentHoveredOverlay = overlay;
    }

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

    @Override
    public void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.Vector2 center, double radius,
            double duration, String effectType) {
        javafx.application.Platform.runLater(() -> {
            boolean isFireball = "Fireball".equalsIgnoreCase(effectType);
            boolean isRocket = "Rocket".equalsIgnoreCase(effectType);
            boolean isZap = "Zap".equalsIgnoreCase(effectType);
            boolean isArrow = "Arrows".equalsIgnoreCase(effectType);
            double cx = center.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = center.getY() * TILE_SIZE + (TILE_SIZE / 2.0);
            double rPixels = radius * TILE_SIZE;

            if (isFireball) {
                // Use the procedural explosion effect
                com.kuroyale.view.util.FireballFactory.playExplosionEffect(unitLayer, cx, cy, rPixels);
            } else if (isRocket) {
                // Use the rocket explosion effect (bigger and with smoke)
                com.kuroyale.view.util.RocketFactory.playExplosionEffect(unitLayer, cx, cy, rPixels);
            } else if (isZap) {
                // 1. ZEMİN EFEKTİ (Perspektifli Elips)
                // 2D oyunlarda derinlik hissi için Y ekseni X'in yarısı veya 0.6'sı kadar
                // olmalıdır.
                double radiusX = rPixels;
                double radiusY = rPixels * 0.9; // Basıklık katsayısı (0.6 iyi bir orandır)

                javafx.scene.shape.Ellipse areaEllipse = new javafx.scene.shape.Ellipse(cx, cy, radiusX, radiusY);

                areaEllipse.setFill(javafx.scene.paint.Color.rgb(255, 255, 0, 0.3)); // Biraz daha belirgin sarı
                areaEllipse.setStroke(javafx.scene.paint.Color.WHITE);
                areaEllipse.setStrokeWidth(3.0);

                // Glow efekti
                javafx.scene.effect.DropShadow zapGlow = new javafx.scene.effect.DropShadow();
                zapGlow.setColor(javafx.scene.paint.Color.WHITE); // Merkezi sarı, parlaması turuncu daha hoş durur
                zapGlow.setRadius(25);
                zapGlow.setSpread(0.6);
                areaEllipse.setEffect(zapGlow);
                areaEllipse.setMouseTransparent(true);

                unitLayer.getChildren().add(areaEllipse);
                activeSpellVisuals.add(new ActiveSpellVisual(areaEllipse, duration));

                // 2. YILDIRIM GIF'İ (Tepeden Düşen)
                javafx.scene.image.Image gifImage = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/gifs/zap.gif"));
                javafx.scene.image.ImageView gifView = new javafx.scene.image.ImageView(gifImage);

                double spellWidth = rPixels * 2.0;

                double visualOffset = 100.0; // Ekranın üstünden de yukarıda başlasın
                double lightningHeight = cy + visualOffset;

                gifView.setFitWidth(spellWidth);
                gifView.setFitHeight(lightningHeight);
                gifView.setPreserveRatio(false); // Uzunlamasına sündürmek için oranı bozuyoruz
                gifView.setLayoutX(cx - (spellWidth / 2.0));
                gifView.setLayoutY(cy - lightningHeight + (radiusY / 2));

                gifView.setMouseTransparent(true);
                unitLayer.getChildren().add(gifView);
                activeSpellVisuals.add(new ActiveSpellVisual(gifView, duration));
            } else if (isArrow) {
                // Arrows büyüsü - birden fazla ok gökten yağar
                int arrowCount = 8;
                double spreadRadius = rPixels * 0.8;

                // 1. ÖNCE Alan göstergesi (daire) görünsün
                javafx.scene.shape.Circle areaIndicator = new javafx.scene.shape.Circle(cx, cy, rPixels);
                areaIndicator.setFill(javafx.scene.paint.Color.rgb(255, 100, 0, 0.15));
                areaIndicator.setStroke(javafx.scene.paint.Color.rgb(255, 150, 0, 0.5));
                areaIndicator.setStrokeWidth(2.0);
                areaIndicator.setMouseTransparent(true);
                unitLayer.getChildren().add(areaIndicator);
                activeSpellVisuals.add(new ActiveSpellVisual(areaIndicator, duration));

                // 2. SONRA Oklar gecikmeyle başlasın
                double startY = isPlayerSource ? (cy + 200) : (cy - 200);

                for (int i = 0; i < arrowCount; i++) {
                    double offsetX = (Math.random() - 0.5) * spreadRadius * 2;
                    double offsetY = (Math.random() - 0.5) * spreadRadius * 2;
                    double targetX = cx + offsetX;
                    double targetY = cy + offsetY;
                    double startX = targetX + (Math.random() - 0.5) * 80;

                    javafx.scene.shape.Rectangle arrowVisual = new javafx.scene.shape.Rectangle(25, 3);
                    arrowVisual.setArcWidth(3);
                    arrowVisual.setArcHeight(3);
                    arrowVisual.setFill(javafx.scene.paint.Color.rgb(139, 90, 43));

                    javafx.scene.effect.DropShadow trailGlow = new javafx.scene.effect.DropShadow();
                    trailGlow.setColor(javafx.scene.paint.Color.ORANGE);
                    trailGlow.setRadius(8);
                    trailGlow.setSpread(0.3);
                    arrowVisual.setEffect(trailGlow);
                    arrowVisual.setMouseTransparent(true);
                    arrowVisual.setVisible(false); // Başlangıçta gizli
                    unitLayer.getChildren().add(arrowVisual);

                    javafx.scene.shape.QuadCurve arrowPath = new javafx.scene.shape.QuadCurve();
                    arrowPath.setStartX(startX);
                    arrowPath.setStartY(startY);
                    arrowPath.setEndX(targetX);
                    arrowPath.setEndY(targetY);

                    double midX = (startX + targetX) / 2.0;
                    double midY = (startY + targetY) / 2.0;
                    double arcAmount = isPlayerSource ? 60.0 : -60.0;
                    arrowPath.setControlX(midX);
                    arrowPath.setControlY(midY + arcAmount);
                    arrowPath.setVisible(false);
                    unitLayer.getChildren().add(arrowPath);

                    javafx.animation.PathTransition pathTransition = new javafx.animation.PathTransition();
                    // Her ok için gecikme: daire sonrası + sıralı gecikme
                    double delayMs = (i * 40) + Math.random() * 60;
                    pathTransition.setDuration(javafx.util.Duration.millis(300 + Math.random() * 100));
                    pathTransition.setPath(arrowPath);
                    pathTransition.setNode(arrowVisual);
                    pathTransition
                            .setOrientation(javafx.animation.PathTransition.OrientationType.ORTHOGONAL_TO_TANGENT);
                    pathTransition.setCycleCount(1);

                    final javafx.scene.shape.Rectangle fArrow = arrowVisual;
                    final javafx.scene.shape.QuadCurve fPath = arrowPath;
                    final double fTargetX = targetX, fTargetY = targetY;

                    pathTransition.setOnFinished(e -> {
                        unitLayer.getChildren().remove(fArrow);
                        unitLayer.getChildren().remove(fPath);
                        createImpactEffect(fTargetX, fTargetY, unitLayer);
                    });

                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                            javafx.util.Duration.millis(delayMs));
                    delay.setOnFinished(e -> {
                        fArrow.setVisible(true); // Ok görünür olsun
                        pathTransition.play();
                    });
                    delay.play();
                }
            }
        });
    }

    private void createImpactEffect(double x, double y, javafx.scene.layout.Pane layer) {
        javafx.scene.shape.Circle impact = new javafx.scene.shape.Circle(x, y, 0); // Başlangıç yarıçapı 0
        impact.setFill(javafx.scene.paint.Color.rgb(255, 200, 0, 0.8)); // Sarı-turuncu yarı saydam
        impact.setStroke(javafx.scene.paint.Color.WHITE);
        impact.setStrokeWidth(2.0);
        impact.setMouseTransparent(true);

        layer.getChildren().add(impact);

        // İki animasyonu paralel oynat: Büyüme ve Sönme
        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300),
                impact);
        scale.setFromX(0.1);
        scale.setFromY(0.1);
        scale.setToX(1.5);
        scale.setToY(1.5); // 1.5 katına büyüsün

        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300),
                impact);
        fade.setFromValue(1.0);
        fade.setToValue(0.0); // Görünmez olsun

        javafx.animation.ParallelTransition impactAnim = new javafx.animation.ParallelTransition(scale, fade);
        impactAnim.setOnFinished(e -> layer.getChildren().remove(impact)); // Bitince kaldır
        impactAnim.play();

    }

    private void updateSpellEffects(double deltaTime) {
        java.util.Iterator<ActiveSpellVisual> it = activeSpellVisuals.iterator();
        while (it.hasNext()) {
            ActiveSpellVisual visual = it.next();
            visual.timeRemaining -= deltaTime;
            if (visual.timeRemaining <= 0) {
                if (visual.node instanceof PngSequenceSprite) {
                    ((PngSequenceSprite) visual.node).stop();
                }
                unitLayer.getChildren().remove(visual.node);
                it.remove();
            }
        }
    }

    @Override
    public void onSpellCast(boolean isPlayer, com.kuroyale.model.entities.Card spell,
            com.kuroyale.model.entities.GridPosition center) {
        if (spell == null || center == null) {
            return;
        }
        if (spell.getType() != CardType.SPELL) {
            return;
        }

        javafx.application.Platform.runLater(() -> {
            String cardKey = PngSequenceSprite.toCardKey(spell.getName());
            String side = isPlayer ? "player" : "enemy";
            String folder = "/images/animations/spells/" + cardKey + "/" + side + "/attack";

            double rPixels = Math.max(1.0, spell.getRange()) * TILE_SIZE;
            double size = rPixels * 2.0;

            double totalDurationSeconds = 0.7;

            PngSequenceSprite sprite = new PngSequenceSprite(folder, size, size);
            sprite.setLoop(false);
            sprite.setLoopDurationSeconds(totalDurationSeconds);
            sprite.setMouseTransparent(true);

            double cx = center.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = center.getY() * TILE_SIZE + (TILE_SIZE / 2.0);
            sprite.setLayoutX(cx - size / 2.0);
            sprite.setLayoutY(cy - size / 2.0);

            unitLayer.getChildren().add(sprite);

            final ActiveSpellVisual[] visualRef = new ActiveSpellVisual[1];
            ActiveSpellVisual visual = new ActiveSpellVisual(sprite, totalDurationSeconds + 0.2);
            visualRef[0] = visual;
            activeSpellVisuals.add(visual);

            sprite.setOnFinished(() -> {
                if (visualRef[0] != null) {
                    visualRef[0].timeRemaining = 0.0;
                }
            });
        });
    }

    public void showComboEffect(com.kuroyale.model.enums.ComboType combo,
            java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {

        double duration = 2.0;

        switch (combo) {
            case TANK_SUPPORT:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addVisualEffect(unit, duration, node -> {
                            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                            glow.setColor(Color.GOLD);
                            glow.setRadius(20);
                            glow.setSpread(0.5);
                            node.setEffect(glow);
                        });
                    }
                }
                break;

            case SPELL_SYNERGY:
                javafx.application.Platform.runLater(() -> {
                    double cx = com.kuroyale.model.entities.Arena.WIDTH * TILE_SIZE / 2.0;
                    double cy = com.kuroyale.model.entities.Arena.HEIGHT * TILE_SIZE / 2.0;

                    for (int i = 0; i < 10; i++) {
                        javafx.scene.shape.Circle sparkle = new javafx.scene.shape.Circle(cx, cy, 5, Color.CYAN);
                        sparkle.setEffect(new javafx.scene.effect.Glow(1.0));
                        unitLayer.getChildren().add(sparkle);

                        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
                                javafx.util.Duration.seconds(1.0), sparkle);
                        tt.setByX((Math.random() - 0.5) * 100);
                        tt.setByY((Math.random() - 0.5) * 100);

                        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                                javafx.util.Duration.seconds(1.0), sparkle);
                        ft.setToValue(0);

                        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(tt, ft);
                        pt.setOnFinished(e -> unitLayer.getChildren().remove(sparkle));
                        pt.play();
                    }
                });
                break;

            case SWARM_ATTACK:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, y + 15, x - 25, y + 15);
                            line.setStroke(Color.WHITE);
                            line.setStrokeWidth(1.5);
                            line.getStrokeDashArray().addAll(4d, 6d);
                            return line;
                        });
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Line line = new javafx.scene.shape.Line(x, y - 15, x - 25, y - 15);
                            line.setStroke(Color.WHITE);
                            line.setStrokeWidth(1.5);
                            line.getStrokeDashArray().addAll(4d, 6d);
                            return line;
                        });
                    }
                }
                break;

            case BUILDING_DEFENSE:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.SVGPath shield = new javafx.scene.shape.SVGPath();
                            shield.setContent("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z");
                            shield.setFill(Color.LIGHTBLUE); // Use a visible color
                            shield.setStroke(Color.BLUE);
                            shield.setScaleX(1.5);
                            shield.setScaleY(1.5);
                            shield.setLayoutX(x - 12); // Center somewhat (path is ~24x24)
                            shield.setLayoutY(y - 12);
                            return shield;
                        });
                    }
                }
                break;

            case AIR_ASSAULT:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Polyline lightning = new javafx.scene.shape.Polyline();
                            lightning.getPoints().addAll(
                                    x, y - 20.0,
                                    x + 5.0, y - 10.0,
                                    x - 5.0, y,
                                    x + 5.0, y + 10.0,
                                    x, y + 20.0);
                            lightning.setStroke(Color.YELLOW);
                            lightning.setStrokeWidth(2);
                            lightning.setEffect(new javafx.scene.effect.Glow(0.8));
                            return lightning;
                        });
                    }
                }
                break;

            case ROYAL_COMBO:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.image.ImageView crown = new javafx.scene.image.ImageView(
                                    new javafx.scene.image.Image(getClass().getResourceAsStream("/images/crown.png")));
                            crown.setFitWidth(24);
                            crown.setFitHeight(24);
                            crown.setLayoutX(x - 12);
                            crown.setLayoutY(y - 30); // Above unit
                            return crown;
                        });
                    }
                }
                break;

            case SIEGE_MODE:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        addAttachedVisual(unit, duration, (u, x, y) -> {
                            javafx.scene.shape.Circle rangeCircle = new javafx.scene.shape.Circle(x, y, TILE_SIZE * 2);
                            rangeCircle.setFill(null);
                            rangeCircle.setStroke(Color.ORANGE);
                            rangeCircle.setStrokeWidth(2);
                            rangeCircle.getStrokeDashArray().addAll(10d, 10d);

                            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                                    javafx.util.Duration.seconds(1.0), rangeCircle);
                            st.setFromX(1.0);
                            st.setFromY(1.0);
                            st.setToX(1.5); // Expand to show increased range
                            st.setToY(1.5);
                            st.setCycleCount(2);
                            st.setAutoReverse(true);
                            st.play();

                            return rangeCircle;
                        });
                    }
                }
                break;

            case RUSH_ATTACK:
                if (affectedUnits != null) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        final javafx.animation.Timeline dust = new javafx.animation.Timeline(
                                new javafx.animation.KeyFrame(javafx.util.Duration.millis(200), e -> {
                                    com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
                                    if (pos == null)
                                        return;
                                    double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                                    double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                                    javafx.scene.shape.Circle dustParticle = new javafx.scene.shape.Circle(cx, cy, 5,
                                            Color.GRAY);
                                    dustParticle.setOpacity(0.6);
                                    unitLayer.getChildren().add(dustParticle);

                                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                                            javafx.util.Duration.seconds(0.5), dustParticle);
                                    ft.setToValue(0);
                                    ft.setOnFinished(ev -> unitLayer.getChildren().remove(dustParticle));
                                    ft.play();
                                }));
                        dust.setCycleCount(10); // Run for 2 seconds
                        dust.play();
                    }
                }
                break;

            default:
                if (affectedUnits != null && !affectedUnits.isEmpty()) {
                    for (com.kuroyale.model.entities.ICombatant unit : affectedUnits) {
                        com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
                        if (pos == null)
                            continue;

                        double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                        double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                        javafx.scene.shape.Circle ring = new javafx.scene.shape.Circle(cx, cy, TILE_SIZE * 0.8);
                        ring.setFill(null);
                        ring.setStroke(Color.GOLD);
                        ring.setStrokeWidth(3.0);
                        ring.setEffect(new javafx.scene.effect.Glow(0.8));

                        unitLayer.getChildren().add(ring);
                        activeSpellVisuals.add(new ActiveSpellVisual(ring, duration));

                        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                                javafx.util.Duration.seconds(0.5), ring);
                        st.setFromX(0.5);
                        st.setFromY(0.5);
                        st.setToX(1.2);
                        st.setToY(1.2);
                        st.setAutoReverse(true);
                        st.setCycleCount(2);
                        st.play();
                    }
                }
                break;
        }
    }

    private void addVisualEffect(com.kuroyale.model.entities.ICombatant unit, double duration,
            java.util.function.Consumer<javafx.scene.Node> effectApplier) {
        if (unit instanceof com.kuroyale.model.entities.Troop) {
            com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
            if (pos != null) {
                double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
                double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

                javafx.scene.shape.Circle highlight = new javafx.scene.shape.Circle(cx, cy, TILE_SIZE / 2);
                highlight.setFill(null);
                highlight.setStroke(Color.GOLD);
                highlight.setStrokeWidth(3);

                effectApplier.accept(highlight);

                unitLayer.getChildren().add(highlight);
                activeSpellVisuals.add(new ActiveSpellVisual(highlight, duration));
            }
        }
    }

    private void addAttachedVisual(com.kuroyale.model.entities.ICombatant unit, double duration,
            AttachedVisualCreator creator) {
        com.kuroyale.model.entities.GridPosition pos = unit.getCenterPosition();
        if (pos != null) {
            double cx = pos.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = pos.getY() * TILE_SIZE + (TILE_SIZE / 2.0);

            javafx.scene.Node visual = creator.create(unit, cx, cy);
            unitLayer.getChildren().add(visual);
            activeSpellVisuals.add(new ActiveSpellVisual(visual, duration));
        }
    }

    @FunctionalInterface
    private interface AttachedVisualCreator {
        javafx.scene.Node create(com.kuroyale.model.entities.ICombatant unit, double x, double y);
    }

    public javafx.scene.Node getTowerNode(com.kuroyale.model.entities.Tower tower) {
        if (tower == null || tower.getPosition() == null || towerRenderer == null) {
            return null;
        }
        return towerRenderer.getTowerVisual(tower.getPosition());
    }
}
