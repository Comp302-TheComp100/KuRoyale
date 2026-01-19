package com.kuroyale.view.battle;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.entities.GridCell;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.enums.TileType;
import com.kuroyale.model.logic.GameState;
import com.kuroyale.view.battle.renderers.BuildingRenderer;
import com.kuroyale.view.battle.renderers.ProjectileRenderer;
import com.kuroyale.view.battle.renderers.TowerRenderer;
import com.kuroyale.view.battle.renderers.TroopRenderer;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Refactored BattleArenaView with improved cohesion.
 * 
 * This class now acts as a coordinator, delegating responsibilities to:
 * - ArenaInputHandler: Mouse input and highlighting
 * - ArenaEffectManager: Visual effects and animations
 * - Specialized renderers: TroopRenderer, TowerRenderer, etc.
 */
public class BattleArenaView extends javafx.scene.layout.BorderPane implements com.kuroyale.event.GameEventListener {
    private final GridPane grid;
    private final Pane unitLayer;
    private final Pane effectLayer;
    private final Canvas highlightLayer;
    private final Canvas debugLayer;
    private final Pane arenaPane;
    private final GameState gameState;
    private final com.kuroyale.model.logic.PvPGameState pvpGameState;
    private final java.util.Map<Long, javafx.scene.Node> cellIndex = new java.util.HashMap<>();

    private static final int TILE_SIZE = com.kuroyale.util.config.GameConstants.TILE_SIZE;

    // Delegated components
    private ArenaInputHandler inputHandler;
    private ArenaEffectManager effectManager;

    // Renderers
    private TroopRenderer troopRenderer;
    private ProjectileRenderer projectileRenderer;
    private TowerRenderer towerRenderer;
    private BuildingRenderer buildingRenderer;

    // moji System
    private EmojiButton emojiButton;
    private EmojiPanel emojiPanel;

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

    public BattleArenaView(com.kuroyale.model.logic.PvPGameState pvpGameState) {
        this.getStylesheets().add(getClass().getResource("/com/kuroyale/view/battle.css").toExternalForm());
        this.gameState = null;
        this.pvpGameState = pvpGameState;

        Arena arena = pvpGameState.getArena();

        // Initialize layers
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
        initializeComponents();
        setupInteractions();
        setupEmojiSystem();

        // PvP mode: No sidebar
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

        // Initialize layers
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
        initializeComponents();
        setupInteractions();
        setupEmojiSystem();

        StackPane centerContainer = new StackPane(arenaPane);
        StackPane.setAlignment(arenaPane, javafx.geometry.Pos.CENTER);
        arenaPane.setMaxSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
        arenaPane.setMinSize(Arena.WIDTH * TILE_SIZE, Arena.HEIGHT * TILE_SIZE);
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
        highlightLayer.layoutXProperty().bind(grid.layoutXProperty().add(TILE_SIZE / 2.0));
        highlightLayer.layoutYProperty().bind(grid.layoutYProperty().add(TILE_SIZE / 2.0));
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

    private void setupEmojiSystem() {
        // Create emoji button
        emojiButton = new EmojiButton();
        emojiButton.setLayoutX(10);
        emojiButton.setLayoutY(10);

        emojiPanel = new EmojiPanel();
        emojiPanel.setLayoutX(60); // To the right of the button
        emojiPanel.setLayoutY(10);

        emojiButton.setOnAction(e -> emojiPanel.toggle());

        emojiPanel.setOnEmojiSelected(emojiName -> {
            playEmoji(true, emojiName);
            com.kuroyale.event.GameEventBus.getInstance().publishEmojiPlayed(true, emojiName);
        });

        arenaPane.getChildren().addAll(emojiButton, emojiPanel);
    }

    private void initRenderers() {
        this.troopRenderer = new TroopRenderer(unitLayer);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode);
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);
    }

    // ==========================================
    // Public API
    // ==========================================

    public void setOnGridClicked(java.util.function.BiConsumer<Integer, Integer> handler) {
        inputHandler.setOnGridClicked(handler);
    }

    public void highlightValidCells(boolean show, boolean isSpell) {
        inputHandler.highlightValidCells(show, isSpell, getArena());
    }

    public void highlightPlayer2ValidCells(boolean show, boolean isSpell) {
        inputHandler.highlightPlayer2ValidCells(show, isSpell, getArena());
    }

    public void showComboText(String text) {
        effectManager.showComboText(text);
    }

    public void playDamageEffect(Tower tower) {
        javafx.scene.Node towerNode = getTowerNode(tower);
        effectManager.playTowerDamageEffect(tower, towerNode);
    }

    public void playTowerDeathEffect(Tower tower) {
        javafx.geometry.Point2D center = getTowerCenterPosition(tower);
        effectManager.playTowerDeathEffect(center);
    }

    public void showComboEffect(com.kuroyale.model.enums.ComboType combo,
            java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
        effectManager.showComboEffect(combo, affectedUnits);
    }

    // ==========================================
    // Rendering
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
        effectManager.update(deltaTime);
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
        effectManager.update(deltaTime);
    }

    // ==========================================
    // Event Handlers (GameEventListener)
    // ==========================================

    @Override
    public void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.Vector2 center, double radius,
            double duration, String effectType) {
        effectManager.playAreaEffect(isPlayerSource, center, radius, duration, effectType);
    }

    @Override
    public void onSpellCast(boolean isPlayer, com.kuroyale.model.entities.Card spell,
            com.kuroyale.model.entities.GridPosition center) {
        effectManager.playSpellCast(isPlayer, spell, center);
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Calculates the center position of a tower in the arena pane coordinates.
     * Robust against the visual node being removed.
     */
    public javafx.geometry.Point2D getTowerCenterPosition(Tower tower) {
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

        int size = (tower.getType() == Tower.TowerType.KING) ? 4 : 3;

        double x = (pos.getX() + size / 2.0) * TILE_SIZE;
        double y = (pos.getY() + size / 2.0) * TILE_SIZE;

        return new javafx.geometry.Point2D(x, y);
    }

    public javafx.scene.Node getTowerNode(Tower tower) {
        if (tower == null || tower.getPosition() == null || towerRenderer == null) {
            return null;
        }
        return towerRenderer.getTowerVisual(tower.getPosition());
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

    public GridPane getGrid() {
        return grid;
    }

    public int getTileSize() {
        return TILE_SIZE;
    }

    @Override
    public void onAreaEffect(boolean isPlayerSource, com.kuroyale.model.entities.GridPosition center, double radius,
            double duration) {
        javafx.application.Platform.runLater(() -> {
            double cx = center.getX() * TILE_SIZE + (TILE_SIZE / 2.0);
            double cy = center.getY() * TILE_SIZE + (TILE_SIZE / 2.0);
            double rPixels = radius * TILE_SIZE;
            javafx.scene.shape.Circle aoe = new javafx.scene.shape.Circle(cx, cy, rPixels);
            aoe.setFill(isPlayerSource ? com.kuroyale.util.GameColors.AOE_PLAYER
                    : com.kuroyale.util.GameColors.AOE_ENEMY);
            aoe.setStroke(com.kuroyale.util.GameColors.AOE_STROKE);
            aoe.setStrokeWidth(1.2);
            unitLayer.getChildren().add(aoe);
            activeSpellVisuals.add(new ActiveSpellVisual(aoe, duration));
        });
    }

    private void updateSpellEffects(double deltaTime) {
        java.util.Iterator<ActiveSpellVisual> it = activeSpellVisuals.iterator();
        while (it.hasNext()) {
            ActiveSpellVisual visual = it.next();
            visual.timeRemaining -= deltaTime;
            if (visual.timeRemaining <= 0) {
                unitLayer.getChildren().remove(visual.node);
                it.remove();
            }
        }
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

    /**
     * Plays an emoji animation on the arena.
     * 
     * @param isPlayer  true if player emoji, false if opponent emoji
     * @param emojiName name of the emoji to play (e.g., "emoji_1")
     */
    private void playEmoji(boolean isPlayer, String emojiName) { 
        try {
            // Load emoji GIF
            String emojiPath = "/gifs/emojis/" + emojiName + ".gif";
            javafx.scene.image.Image emojiImage = new javafx.scene.image.Image(
                    getClass().getResourceAsStream(emojiPath));

            javafx.scene.image.ImageView emojiView = new javafx.scene.image.ImageView(emojiImage);
            emojiView.setFitWidth(80);
            emojiView.setFitHeight(80);
            emojiView.setPreserveRatio(true);

            // Position emoji - center horizontally, bottom for player, top for opponent
            double centerX = (Arena.WIDTH * TILE_SIZE) / 2.0 - 40; // -40 to center the 80px image
            double posY = isPlayer ? (Arena.HEIGHT * TILE_SIZE) - 120 : 40; // Player bottom, opponent top

            emojiView.setLayoutX(centerX);
            emojiView.setLayoutY(posY);
            emojiView.setOpacity(0.0);

            // Add to effect layer
            effectLayer.getChildren().add(emojiView);

            // Animation: Fade in, stay, fade out
            javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(300), emojiView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            javafx.animation.PauseTransition stay = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2.5));

            javafx.animation.FadeTransition fadeOut = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(500), emojiView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            javafx.animation.SequentialTransition sequence = new javafx.animation.SequentialTransition(
                    fadeIn, stay, fadeOut);
            sequence.setOnFinished(e -> effectLayer.getChildren().remove(emojiView));
            sequence.play();

        } catch (Exception e) {
            System.err.println("Error playing emoji: " + e.getMessage());
         }
    }
}
