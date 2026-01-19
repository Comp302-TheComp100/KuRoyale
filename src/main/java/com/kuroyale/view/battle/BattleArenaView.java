package com.kuroyale.view.battle;

import com.kuroyale.model.entities.GridCell;
import com.kuroyale.model.entities.Arena;
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
import javafx.scene.control.Button;
import java.util.function.BiConsumer;

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

    private EmojiButton emojiButton;
    private EmojiPanel emojiPanel;
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

    private int currentHoveredTileX = -1;
    private int currentHoveredTileY = -1;
    private javafx.scene.Node currentHoveredOverlay = null;

    /**
     * Constructor for PvP mode.
     */

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
            //
            sequence.setOnFinished(e -> effectLayer.getChildren().remove(emojiView));

        } catch (Exception e) {
            System.err.println("[BattleArenaView] Failed to play emoji: " + emojiName);
            // e.printStackTrace(); // Suppress full stack trace to avoid spam if files
            // missing
        }
    }

    private void initializeComponents() {
        this.inputHandler = new ArenaInputHandler(arenaPane, grid, unitLayer, highlightLayer, cellIndex);
        this.effectManager = new ArenaEffectManager(arenaPane, unitLayer, effectLayer);
    }

    private void initRenderers() {
        this.troopRenderer = new TroopRenderer(unitLayer);
        this.projectileRenderer = new ProjectileRenderer(unitLayer, TILE_SIZE);
        this.towerRenderer = new TowerRenderer(grid, TILE_SIZE, this::indexCellNode);
        this.buildingRenderer = new BuildingRenderer(unitLayer, TILE_SIZE, this::getGridCell);
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

        overlay.setFill(com.kuroyale.util.config.GameColors.HOVER_FILL);
        overlay.setStroke(com.kuroyale.util.config.GameColors.HOVER_STROKE);
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
}