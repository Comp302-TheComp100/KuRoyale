package com.kuroyale.controller;

import com.kuroyale.model.entities.Arena;
import com.kuroyale.model.logic.ArenaDesignModel;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.util.GameConstants;
import com.kuroyale.model.enums.TileType;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.view.battle.renderers.ArenaRenderer;

import javafx.fxml.FXML;

import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.ImagePattern;

import java.io.IOException;
import java.util.List;

/* Controller for the Arena Design UI.
 * Refactored to Orchestrate Model and View (Renderer).*/
public class ArenaDesignController {

    @FXML
    private GridPane arenaGrid;
    @FXML
    private TextField arenaNameField;
    @FXML
    private javafx.scene.shape.Rectangle draggableBridge;
    @FXML
    private javafx.scene.shape.Rectangle draggablePrincessTower;
    @FXML
    private javafx.scene.shape.Rectangle draggableKingTower;

    private final ArenaDesignModel model = new ArenaDesignModel();
    private ArenaLayout currentLayout;
    private ArenaRenderer renderer;
    private final SceneLoader sceneLoader = new SceneLoader();

    public ArenaDesignController() {
    }

    @FXML
    public void initialize() {
        // Initialize Renderer
        renderer = new ArenaRenderer(arenaGrid);

        // Setup Palette Icons using images from renderer
        updatePaletteIcons();

        // Set current user in arena service (auth logic)
        if (model.isLoggedIn()) {
            model.setCurrentUserInArenaService(model.getCurrentUser());
        }

        // Load the saved layout
        currentLayout = model.loadArenaLayout();

        if (arenaNameField != null) {
            arenaNameField.setText(currentLayout.getName());
        }

        setupDragSource();
        renderArena();
    }

    private void updatePaletteIcons() {
        try {
            if (draggablePrincessTower != null) {
                draggablePrincessTower
                        .setFill(new ImagePattern(com.kuroyale.util.GameAssets.getInstance().getPrincessTowerUser()));
            }
            if (draggableKingTower != null) {
                draggableKingTower
                        .setFill(new ImagePattern(com.kuroyale.util.GameAssets.getInstance().getKingTowerUser()));
            }
            if (draggableBridge != null) {
                draggableBridge.setFill(javafx.scene.paint.Color.SADDLEBROWN);
            }
        } catch (Exception e) {
            System.err.println("Error setting up palette: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupDragSource() {
        if (draggableBridge != null) {
            draggableBridge.setOnDragDetected(event -> {
                Dragboard db = draggableBridge.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("BRIDGE");
                db.setContent(content);
                event.consume();
            });
        }

        if (draggablePrincessTower != null) {
            draggablePrincessTower.setOnDragDetected(event -> {
                Dragboard db = draggablePrincessTower.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("PRINCESS_TOWER");
                db.setContent(content);
                event.consume();
            });
        }

        if (draggableKingTower != null) {
            draggableKingTower.setOnDragDetected(event -> {
                Dragboard db = draggableKingTower.startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putString("KING_TOWER");
                db.setContent(content);
                event.consume();
            });
        }
    }

    private void renderArena() {
        // Create a temporary Arena from the layout for Grid state
        Arena arena = model.createArena(currentLayout);

        // Use View Helper to render
        renderer.renderArena(arena, currentLayout, new ArenaRenderer.ArenaInteractionListener() {
            @Override
            public void onDragOver(javafx.scene.input.DragEvent event, int x, int y) {
                if (event.getGestureSource() != null && event.getDragboard().hasString()) {
                    String dragType = event.getDragboard().getString();

                    // Bridges: river area only
                    if ("BRIDGE".equals(dragType)) {
                        if (y == GameConstants.RIVER_ROW_1 || y == GameConstants.RIVER_ROW_2) {
                            event.acceptTransferModes(TransferMode.COPY);
                            renderer.highlightRegion(x, y, 1, 1, true);
                        } else {
                            renderer.highlightRegion(x, y, 1, 1, false);
                        }
                    }
                    // Towers: user's bottom half only (y > 16)
                    else if ("PRINCESS_TOWER".equals(dragType)) {
                        int size = GameConstants.PRINCESS_TOWER_SIZE;
                        // Center the placement on the cursor if possible, or just use top-left.
                        int startX = x - 1;
                        int startY = y - 1;

                        boolean validBounds = startX >= 0 && startY >= 0 &&
                                startX + size <= Arena.WIDTH &&
                                startY + size <= Arena.HEIGHT;

                        boolean validZone = y > GameConstants.USER_SIDE_BOUNDARY_Y; // Rough check, real check is
                                                                                    // complex

                        if (validBounds && validZone) {
                            event.acceptTransferModes(TransferMode.COPY);
                            renderer.highlightRegion(startX, startY, size, size, true);
                        } else {
                            // Show invalid highlight at the attempted position
                            renderer.highlightRegion(startX, startY, size, size, false);
                        }
                    } else if ("KING_TOWER".equals(dragType)) {
                        int size = GameConstants.KING_TOWER_SIZE;
                        int startX = x - 2;
                        int startY = y - 2;

                        boolean validBounds = startX >= 0 && startY >= 0 &&
                                startX + size <= Arena.WIDTH &&
                                startY + size <= Arena.HEIGHT;

                        boolean validZone = y > GameConstants.USER_SIDE_BOUNDARY_Y;

                        if (validBounds && validZone) {
                            event.acceptTransferModes(TransferMode.COPY);
                            renderer.highlightRegion(startX, startY, size, size, true);
                        } else {
                            renderer.highlightRegion(startX, startY, size, size, false);
                        }
                    }
                }
                event.consume();
            }

            @Override
            public void onDragDropped(javafx.scene.input.DragEvent event, int x, int y) {
                renderer.clearHighlight();
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    String dragType = db.getString();
                    if ("BRIDGE".equals(dragType)) {
                        success = handleBridgeDrop(x, y);
                    } else if ("PRINCESS_TOWER".equals(dragType)) {
                        success = handlePrincessTowerDrop(x, y);
                    } else if ("KING_TOWER".equals(dragType)) {
                        success = handleKingTowerDrop(x, y);
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            }

            @Override
            public void onTileClicked(int x, int y, TileType type) {
                if (type == TileType.BRIDGE) {
                    handleBridgeRemoval(x, y);
                }
            }

            @Override
            public void onTowerClicked(int x, int y, boolean isKing) {
                handleTowerRemoval(x, y, isKing);
            }
        });
    }

    private boolean handleBridgeDrop(int x, int y) {
        StringBuilder errorMsg = new StringBuilder();
        if (!model.canPlaceBridge(currentLayout, x, y, errorMsg)) {
            if (errorMsg.length() > 0)
                showAlert("Cannot Place Bridge", errorMsg.toString());
            return false;
        }

        model.placeBridge(currentLayout, x);
        renderArena();
        return true;
    }

    private boolean handlePrincessTowerDrop(int x, int y) {
        StringBuilder errorMsg = new StringBuilder();
        if (!model.canPlacePrincessTower(currentLayout, x, y, errorMsg)) {
            if (errorMsg.length() > 0)
                showAlert("Cannot Place Tower", errorMsg.toString()); // Optional to suppress if just drag fail
            return false;
        }

        model.placePrincessTower(currentLayout, x, y);
        renderArena();
        return true;
    }

    private boolean handleKingTowerDrop(int x, int y) {
        StringBuilder errorMsg = new StringBuilder();
        if (!model.canPlaceKingTower(currentLayout, x, y, errorMsg)) {
            if (errorMsg.length() > 0)
                showAlert("Cannot Place Tower", errorMsg.toString());
            return false;
        }

        model.placeKingTower(currentLayout, x, y);
        renderArena();
        return true;
    }

    private void handleBridgeRemoval(int x, int y) {
        model.removeBridge(currentLayout, x);
        renderArena();
    }

    private void handleTowerRemoval(int x, int y, boolean isKing) {
        if (isKing) {
            model.removeKingTower(currentLayout);
        } else {
            model.removePrincessTower(currentLayout, x, y);
        }
        renderArena();
    }

    @FXML
    public void handleSave() {
        if (currentLayout != null) {
            String name = arenaNameField.getText();
            if (name != null && !name.isEmpty()) {
                currentLayout.setName(name);
            }

            List<String> validationErrors = model.validateLayout(currentLayout);
            if (!validationErrors.isEmpty()) {
                showAlert("Invalid Layout", validationErrors.get(0));
                return;
            }
            try {
                model.saveArenaLayout(currentLayout);
                com.kuroyale.util.ThemedAlertManager.show(arenaGrid.getScene().getWindow(), "Success",
                        "Arena layout saved successfully!",
                        this::handleBack);
            } catch (IOException e) {
                showAlert("Error", "Failed to save arena layout: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleBack() {
        try {
            sceneLoader.load(arenaNameField, "/fxml/main-menu.fxml", "KU Royale - Main Menu", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        com.kuroyale.util.ThemedAlertManager.show(arenaGrid.getScene().getWindow(), title, content, null);
    }
}
