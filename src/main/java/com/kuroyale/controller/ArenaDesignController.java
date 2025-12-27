package com.kuroyale.controller;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaDesignModel;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.util.GameConstants;
import com.kuroyale.model.TileType;
import com.kuroyale.view.ArenaRenderer;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.util.List;

/* Controller for the Arena Design UI.
 * Refactored to Orchestrate Model and View (Renderer).
 */
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
                draggablePrincessTower.setFill(new ImagePattern(renderer.getPrincessTowerUserImg()));
            }
            if (draggableKingTower != null) {
                draggableKingTower.setFill(new ImagePattern(renderer.getKingTowerUserImg()));
            }
            if (draggableBridge != null) {
                draggableBridge.setFill(javafx.scene.paint.Color.SADDLEBROWN);
            }
        } catch (Exception e) {
            System.err.println("Error setting up palette: " + e.getMessage());
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
                    if ("BRIDGE".equals(dragType)
                            && (y == GameConstants.RIVER_ROW_1 || y == GameConstants.RIVER_ROW_2)) {
                        event.acceptTransferModes(TransferMode.COPY);
                    }
                    // Towers: user's bottom half only (y > 16)
                    else if ("PRINCESS_TOWER".equals(dragType) && y > GameConstants.USER_SIDE_BOUNDARY_Y) {
                        int startX = x - 1;
                        int startY = y - 1;
                        if (startX >= 0 && startY >= 0 &&
                                startX + GameConstants.PRINCESS_TOWER_SIZE < Arena.WIDTH &&
                                startY + GameConstants.PRINCESS_TOWER_SIZE < Arena.HEIGHT) {
                            event.acceptTransferModes(TransferMode.COPY);
                        }
                    } else if ("KING_TOWER".equals(dragType) && y > GameConstants.USER_SIDE_BOUNDARY_Y) {
                        int startX = x - 2;
                        int startY = y - 2;
                        if (startX >= 0 && startY >= 0 &&
                                startX + GameConstants.KING_TOWER_SIZE < Arena.WIDTH &&
                                startY + GameConstants.KING_TOWER_SIZE < Arena.HEIGHT) {
                            event.acceptTransferModes(TransferMode.COPY);
                        }
                    }
                }
                event.consume();
            }

            @Override
            public void onDragDropped(javafx.scene.input.DragEvent event, int x, int y) {
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

        // Apply Logic
        int startX = x;
        if (startX >= Arena.WIDTH - 1) {
            startX = Arena.WIDTH - 2;
        }
        int startY = GameConstants.RIVER_ROW_1; // Force Y to river

        for (int dx = 0; dx < GameConstants.BRIDGE_WIDTH; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                currentLayout.addBridgePosition(startX + dx, startY + dy);
            }
        }
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

        int startX = x - 1;
        int startY = y - 1;
        currentLayout.addPrincessTowerPosition(startX, startY);
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

        int startX = x - 2;
        int startY = y - 2;
        currentLayout.setKingTowerPosition(startX, startY);
        renderArena();
        return true;
    }

    private void handleBridgeRemoval(int x, int y) {
        // Logic to remove the 2x2 bridge
        // We need to find the top-left of the bridge block this tile belongs to
        // Bridges are always at y=15,16.
        int blockStart = x;

        // Search left for the start of this bridge block
        while (blockStart > 0) {
            final int checkX = blockStart - 1;
            boolean isBridgeLeft = currentLayout.getBridgePositions().stream()
                    .anyMatch(p -> p.getX() == checkX
                            && (p.getY() == GameConstants.RIVER_ROW_1 || p.getY() == GameConstants.RIVER_ROW_2));
            if (isBridgeLeft) {
                blockStart--;
            } else {
                break;
            }
        }

        // NOTE: The bridge removal logic in original controller was slightly complex
        // due to fused bridges.
        // Assuming bridges are 2x2 blocks aligned. If they merge, clicking one tile
        // removes its 2x2 origin block?
        // Original logic:
        /*
         * int offset = finalX - blockStart;
         * int bridgeStartX = blockStart + (offset / 2) * 2;
         */
        // Let's try to replicate:
        int offset = x - blockStart;
        int bridgeStartX = blockStart + (offset / 2) * 2;

        currentLayout.getBridgePositions().removeIf(
                p -> (p.getX() == bridgeStartX || p.getX() == bridgeStartX + 1)
                        && (p.getY() == GameConstants.RIVER_ROW_1 || p.getY() == GameConstants.RIVER_ROW_2));
        renderArena();
    }

    private void handleTowerRemoval(int x, int y, boolean isKing) {
        if (isKing) {
            currentLayout.setKingTowerPosition(null);
        } else {
            currentLayout.getPrincessTowerPositions().removeIf(
                    p -> p.getX() == x && p.getY() == y);
        }
        renderArena();
    }

    @FXML
    public void handleSave() {
        if (currentLayout != null) {
            List<String> validationErrors = model.validateLayout(currentLayout);
            if (!validationErrors.isEmpty()) {
                showAlert("Invalid Layout", validationErrors.get(0));
                return;
            }

            String name = arenaNameField.getText();
            if (name != null && !name.isEmpty()) {
                currentLayout.setName(name);
            }
            try {
                model.saveArenaLayout(currentLayout);
                showAlert("Success", "Arena layout saved successfully!");
            } catch (IOException e) {
                showAlert("Error", "Failed to save arena layout: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) arenaNameField.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Main Menu");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        if (title.contains("Error") || title.contains("Invalid") || title.contains("Limit")
                || title.contains("Cannot")) {
            alert.setAlertType(Alert.AlertType.WARNING);
        }
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}