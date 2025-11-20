package com.kuroyale.controller;

import com.kuroyale.model.ArenaLayout;
import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.ServiceFactory;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/* Controller for the Arena Design UI.
 * GRASP Pattern: Controller - Delegates business logic to ArenaService.*/
public class ArenaDesignController {

    @FXML
    private GridPane arenaGrid;

    @FXML
    private TextField arenaNameField;

    private final ArenaService arenaService;
    private final AuthenticationService authService;
    private ArenaLayout currentLayout;

    public ArenaDesignController() {
        // Get services from factory (Dependency Injection / Service Locator)
        ServiceFactory factory = ServiceFactory.getInstance();
        this.arenaService = factory.getArenaService();
        this.authService = factory.getAuthenticationService();
    }

    @FXML
    private javafx.scene.shape.Rectangle draggableBridge;

    @FXML
    public void initialize() {
        // Set current user in arena service so it can save to user's profile
        if (authService.isLoggedIn()) {
            arenaService.setCurrentUser(authService.getCurrentUser());
        }
        // Load the saved layout
        currentLayout = arenaService.loadArenaLayout();

        if (arenaNameField != null) {
            arenaNameField.setText(currentLayout.getName());
        }

        setupDragSource();
        renderArena();
    }

    private void setupDragSource() {
        if (draggableBridge != null) {
            draggableBridge.setOnDragDetected(event -> {
                javafx.scene.input.Dragboard db = draggableBridge
                        .startDragAndDrop(javafx.scene.input.TransferMode.COPY);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString("BRIDGE");
                db.setContent(content);
                event.consume();
            });
        }
    }

    private void renderArena() {
        arenaGrid.getChildren().clear();
        arenaGrid.getColumnConstraints().clear();
        arenaGrid.getRowConstraints().clear();

        // Create a temporary Arena from the layout to get the full grid state
        com.kuroyale.model.Arena arena = arenaService.createArena(currentLayout);

        for (int x = 0; x < com.kuroyale.model.Arena.WIDTH; x++) {
            for (int y = 0; y < com.kuroyale.model.Arena.HEIGHT; y++) {
                com.kuroyale.model.Tile tile = arena.getTile(x, y);
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(18, 18);

                // Style based on type
                updateTileStyle(rect, tile.getType());

                rect.setStroke(javafx.scene.paint.Color.BLACK);
                rect.setStrokeWidth(0.5);

                // Add interaction
                final int finalX = x;
                final int finalY = y;

                // Drag over logic
                rect.setOnDragOver(event -> {
                    if (event.getGestureSource() != rect && event.getDragboard().hasString()) {
                        // Check if valid placement (River area: y=15 or y=16)
                        if (finalY == 15 || finalY == 16) {
                            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                        }
                    }
                    event.consume();
                });

                // Drag dropped logic
                rect.setOnDragDropped(event -> {
                    javafx.scene.input.Dragboard db = event.getDragboard();
                    boolean success = false;
                    if (db.hasString() && "BRIDGE".equals(db.getString())) {
                        handleBridgeDrop(finalX, finalY);
                        success = true;
                    }
                    event.setDropCompleted(success);
                    event.consume();
                });

                // Keep click for removing bridges
                rect.setOnMouseClicked(e -> {
                    if (tile.getType() == com.kuroyale.model.TileType.BRIDGE) {
                        // Remove the 2x2 bridge.
                        // Algorithm: Find the start of the contiguous bridge block to the left.
                        // Then decompose into 2-wide segments.

                        int blockStart = finalX;
                        while (blockStart > 0) {
                            final int checkX = blockStart - 1;
                            boolean isBridgeLeft = currentLayout.getBridgePositions().stream()
                                    .anyMatch(p -> p.x == checkX && (p.y == 15 || p.y == 16));
                            if (isBridgeLeft) {
                                blockStart--;
                            } else {
                                break;
                            }
                        }

                        // Calculate which segment the clicked tile belongs to
                        int offset = finalX - blockStart;
                        int bridgeStartX = blockStart + (offset / 2) * 2;

                        currentLayout.getBridgePositions().removeIf(
                                p -> (p.x == bridgeStartX || p.x == bridgeStartX + 1) && (p.y == 15 || p.y == 16));
                        renderArena();
                    }
                });

                arenaGrid.add(rect, x, y);
            }
        }
    }

    private void updateTileStyle(javafx.scene.shape.Rectangle rect, com.kuroyale.model.TileType type) {
        switch (type) {
            case GRASS:
                rect.setFill(javafx.scene.paint.Color.LIGHTGREEN);
                break;
            case WATER:
                rect.setFill(javafx.scene.paint.Color.LIGHTBLUE);
                break;
            case BRIDGE:
                rect.setFill(javafx.scene.paint.Color.SADDLEBROWN);
                break;
            case ROAD:
                rect.setFill(javafx.scene.paint.Color.SANDYBROWN);
                break;
        }
    }

    private void handleBridgeDrop(int x, int y) {
        // Check max bridges (3 bridges * 4 tiles = 12 tiles)
        if (currentLayout.getBridgePositions().size() >= 12) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Limit Reached");
            alert.setHeaderText(null);
            alert.setContentText("You can only place a maximum of 3 bridges.");
            alert.showAndWait();
            return;
        }

        // Logic to place a 2x2 bridge starting at (x, y) or adjusting to fit
        // River is y=15, 16.
        int startY = 15;

        // Align X. If X is last column, shift left.
        int startX = x;
        if (startX >= com.kuroyale.model.Arena.WIDTH - 1) {
            startX = com.kuroyale.model.Arena.WIDTH - 2;
        }

        // Check if space is already occupied
        boolean occupied = false;
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                final int tx = startX + dx;
                final int ty = startY + dy;
                if (currentLayout.getBridgePositions().stream().anyMatch(p -> p.x == tx && p.y == ty)) {
                    occupied = true;
                    break;
                }
            }
        }

        if (occupied) {
            //overlap
            return;
        }

        // Place 2x2 bridge
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 2; dy++) {
                currentLayout.addBridgePosition(startX + dx, startY + dy);
            }
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
            try {
                arenaService.saveArenaLayout(currentLayout);

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Arena layout saved successfully!");
                alert.showAndWait();
            } catch (java.io.IOException e) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to save arena layout: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) arenaNameField.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Main Menu");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}