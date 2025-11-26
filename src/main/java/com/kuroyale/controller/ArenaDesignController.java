package com.kuroyale.controller;

import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.GridPosition;
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

    private javafx.scene.image.Image princessTowerUserImg;
    private javafx.scene.image.Image princessTowerComputerImg;
    private javafx.scene.image.Image kingTowerUserImg;
    private javafx.scene.image.Image kingTowerComputerImg;
    // Bridge image removed as requested

    public ArenaDesignController() {
        // Get services from factory (Dependency Injection / Service Locator)
        ServiceFactory factory = ServiceFactory.getInstance();
        this.arenaService = factory.getArenaService();
        this.authService = factory.getAuthenticationService();
    }

    @FXML
    private javafx.scene.shape.Rectangle draggableBridge;

    @FXML
    private javafx.scene.shape.Rectangle draggablePrincessTower;

    @FXML
    private javafx.scene.shape.Rectangle draggableKingTower;

    @FXML
    public void initialize() {
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

            // Update palette icons
            if (draggablePrincessTower != null) {
                draggablePrincessTower.setFill(new javafx.scene.paint.ImagePattern(princessTowerUserImg));
            }
            if (draggableKingTower != null) {
                draggableKingTower.setFill(new javafx.scene.paint.ImagePattern(kingTowerUserImg));
            }
            // Bridge palette icon remains default or we can set it to color
            if (draggableBridge != null) {
                draggableBridge.setFill(javafx.scene.paint.Color.SADDLEBROWN);
            }
        } catch (Exception e) {
            System.err.println("Failed to load images: " + e.getMessage());
            e.printStackTrace();
        }

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

        if (draggablePrincessTower != null) {
            draggablePrincessTower.setOnDragDetected(event -> {
                javafx.scene.input.Dragboard db = draggablePrincessTower
                        .startDragAndDrop(javafx.scene.input.TransferMode.COPY);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString("PRINCESS_TOWER");
                db.setContent(content);
                event.consume();
            });
        }

        if (draggableKingTower != null) {
            draggableKingTower.setOnDragDetected(event -> {
                javafx.scene.input.Dragboard db = draggableKingTower
                        .startDragAndDrop(javafx.scene.input.TransferMode.COPY);
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString("KING_TOWER");
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

        // Pass 1: Render Grid (Ground)
        for (int x = 0; x < com.kuroyale.model.Arena.WIDTH; x++) {
            for (int y = 0; y < com.kuroyale.model.Arena.HEIGHT; y++) {
                com.kuroyale.model.Tile tile = arena.getTile(x, y);
                javafx.scene.shape.Rectangle rect = new javafx.scene.shape.Rectangle(18, 18);

                // For towers, render underlying terrain (grass)
                // Bridges are rendered as SADDLEBROWN tiles here
                if (isTower(tile.getType())) {
                    rect.setFill(javafx.scene.paint.Color.LIGHTGREEN);
                } else {
                    updateTileStyle(rect, tile.getType());
                }

                rect.setStroke(javafx.scene.paint.Color.BLACK);
                rect.setStrokeWidth(0.5);

                // Add interaction
                final int finalX = x;
                final int finalY = y;

                // Drag over logic
                rect.setOnDragOver(event -> {
                    if (event.getGestureSource() != rect && event.getDragboard().hasString()) {
                        String dragType = event.getDragboard().getString();

                        // Bridges: river area only (y=15 or y=16)
                        if ("BRIDGE".equals(dragType) && (finalY == 15 || finalY == 16)) {
                            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                        }
                        // Towers: user's bottom half only (y > 16)
                        // Also check bounds for 3x3 placement
                        else if (("PRINCESS_TOWER".equals(dragType) || "KING_TOWER".equals(dragType))
                                && finalY > 16 && finalX + 2 < com.kuroyale.model.Arena.WIDTH
                                && finalY + 2 < com.kuroyale.model.Arena.HEIGHT) {
                            event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
                        }
                    }
                    event.consume();
                });

                // Drag dropped logic
                rect.setOnDragDropped(event -> {
                    javafx.scene.input.Dragboard db = event.getDragboard();
                    boolean success = false;
                    if (db.hasString()) {
                        String dragType = db.getString();
                        if ("BRIDGE".equals(dragType)) {
                            handleBridgeDrop(finalX, finalY);
                            success = true;
                        } else if ("PRINCESS_TOWER".equals(dragType)) {
                            handlePrincessTowerDrop(finalX, finalY);
                            success = true;
                        } else if ("KING_TOWER".equals(dragType)) {
                            handleKingTowerDrop(finalX, finalY);
                            success = true;
                        }
                    }
                    event.setDropCompleted(success);
                    event.consume();
                });

                // Click to remove for bridges (towers handled in Pass 2)
                rect.setOnMouseClicked(e -> {
                    com.kuroyale.model.TileType tileType = tile.getType();

                    if (tileType == com.kuroyale.model.TileType.BRIDGE) {
                        // Remove the 2x2 bridge
                        int blockStart = finalX;
                        while (blockStart > 0) {
                            final int checkX = blockStart - 1;
                            boolean isBridgeLeft = currentLayout.getBridgePositions().stream()
                                    .anyMatch(p -> p.getX() == checkX && (p.getY() == 15 || p.getY() == 16));
                            if (isBridgeLeft) {
                                blockStart--;
                            } else {
                                break;
                            }
                        }

                        int offset = finalX - blockStart;
                        int bridgeStartX = blockStart + (offset / 2) * 2;

                        currentLayout.getBridgePositions().removeIf(
                                p -> (p.getX() == bridgeStartX || p.getX() == bridgeStartX + 1)
                                        && (p.getY() == 15 || p.getY() == 16));
                        renderArena();
                    }
                });

                arenaGrid.add(rect, x, y);
            }
        }

        // Pass 2: Render Towers (Images)
        renderStructureImages();
    }

    private boolean isTower(com.kuroyale.model.TileType type) {
        return type == com.kuroyale.model.TileType.PRINCESS_TOWER_USER ||
                type == com.kuroyale.model.TileType.PRINCESS_TOWER_COMPUTER ||
                type == com.kuroyale.model.TileType.KING_TOWER_USER ||
                type == com.kuroyale.model.TileType.KING_TOWER_COMPUTER;
    }

    private void renderStructureImages() {
        // Bridges are rendered as tiles in Pass 1, no image overlay needed as per
        // request

        // Render User Princess Towers
        for (GridPosition p : currentLayout.getPrincessTowerPositions()) {
            addTowerImage(p.getX(), p.getY(), princessTowerUserImg, true, false);
            // Mirror Computer Princess
            addTowerImage(p.getX(), com.kuroyale.model.Arena.HEIGHT - 3 - p.getY(), princessTowerComputerImg, false,
                    false);
        }

        // Render User King Tower
        if (currentLayout.getKingTowerPosition() != null) {
            GridPosition p = currentLayout.getKingTowerPosition();
            addTowerImage(p.getX(), p.getY(), kingTowerUserImg, true, true);
            // Mirror Computer King
            addTowerImage(p.getX(), com.kuroyale.model.Arena.HEIGHT - 3 - p.getY(), kingTowerComputerImg, false, true);
        }
    }

    private void addTowerImage(int x, int y, javafx.scene.image.Image img, boolean isUser, boolean isKing) {
        if (img == null)
            return;

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(img);
        imageView.setFitWidth(54); // 3 * 18
        imageView.setFitHeight(54); // 3 * 18

        // Add click to remove for user towers
        if (isUser) {
            imageView.setOnMouseClicked(e -> {
                if (isKing) {
                    currentLayout.setKingTowerPosition(null);
                } else {
                    currentLayout.getPrincessTowerPositions().removeIf(
                            p -> p.getX() == x && p.getY() == y);
                }
                renderArena();
            });
        }

        arenaGrid.add(imageView, x, y, 3, 3);

        // Add Health Bar
        addHealthBar(x, y);
    }

    private void addHealthBar(int x, int y) {
        // Health bar dimensions
        double width = 40;
        double height = 5;

        // Background (Red)
        javafx.scene.shape.Rectangle bg = new javafx.scene.shape.Rectangle(width, height);
        bg.setFill(javafx.scene.paint.Color.RED);
        bg.setStroke(javafx.scene.paint.Color.BLACK);
        bg.setStrokeWidth(0.5);

        // Foreground (Green) - Full health for design view
        javafx.scene.shape.Rectangle fg = new javafx.scene.shape.Rectangle(width, height);
        fg.setFill(javafx.scene.paint.Color.LIMEGREEN);

        // Center the health bar above the tower (3x3 = 54px wide)
        // We need to add it to the grid, but spanning columns?
        // Grid pane cells are 18x18.
        // We can add it to the top-left cell but translate it?
        // Or add it to a StackPane if the cells were StackPanes.
        // Since we are adding to GridPane directly, we can use translation.

        // Better: Wrap the ImageView and HealthBar in a VBox or StackPane and add THAT
        // to the grid.
        // But the grid is 18x18 cells. The tower spans 3x3 cells.
        // If we add a VBox to (x,y) spanning 3x3, it will fill the 54x54 area.
        // We can put the health bar at the top of that VBox.

        // Let's try adding the health bar as a separate node spanning 3 columns,
        // but we need it to be *above* the image visually.
        // The ImageView is already added.
        // Let's add the health bar to the same (x,y) spanning 3 cols, but set
        // alignment/margin.

        javafx.scene.layout.StackPane healthBarContainer = new javafx.scene.layout.StackPane();
        healthBarContainer.getChildren().addAll(bg, fg);
        healthBarContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        healthBarContainer.setTranslateY(-5); // Move up slightly

        // Add to grid, spanning 3 cols, 1 row (the top row of the tower)
        arenaGrid.add(healthBarContainer, x, y, 3, 1);
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
            case PRINCESS_TOWER_USER:
                rect.setFill(javafx.scene.paint.Color.HOTPINK);
                break;
            case PRINCESS_TOWER_COMPUTER:
                rect.setFill(javafx.scene.paint.Color.DEEPPINK);
                break;
            case KING_TOWER_USER:
                rect.setFill(javafx.scene.paint.Color.GOLD);
                break;
            case KING_TOWER_COMPUTER:
                rect.setFill(javafx.scene.paint.Color.ORANGE);
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
                if (currentLayout.getBridgePositions().stream().anyMatch(p -> p.getX() == tx && p.getY() == ty)) {
                    occupied = true;
                    break;
                }
            }
        }

        if (occupied) {
            // overlap
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

    private void handlePrincessTowerDrop(int x, int y) {
        // Check max 2 Princess towers
        if (currentLayout.getPrincessTowerPositions().size() >= 2) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Limit Reached");
            alert.setHeaderText(null);
            alert.setContentText("You can only place a maximum of 2 Princess towers.");
            alert.showAndWait();
            return;
        }

        // Only allow placement in user's bottom half (y > 16)
        if (y <= 16) {
            return;
        }

        // Check bounds for 3x3 tower
        if (x + 2 >= com.kuroyale.model.Arena.WIDTH || y + 2 >= com.kuroyale.model.Arena.HEIGHT) {
            return;
        }

        // Check if position is already occupied by a tower (3x3 overlap check)
        // We check if any cell in the new 3x3 area overlaps with any existing tower's
        // 3x3 area
        boolean occupied = false;

        // Check against existing Princess towers
        for (GridPosition p : currentLayout.getPrincessTowerPositions()) {
            if (isOverlap(x, y, 3, 3, p.getX(), p.getY(), 3, 3)) {
                occupied = true;
                break;
            }
        }

        // Check against King tower
        if (!occupied && currentLayout.getKingTowerPosition() != null) {
            GridPosition k = currentLayout.getKingTowerPosition();
            if (isOverlap(x, y, 3, 3, k.getX(), k.getY(), 3, 3)) {
                occupied = true;
            }
        }

        if (occupied) {
            return;
        }

        // Place Princess tower
        currentLayout.addPrincessTowerPosition(x, y);
        renderArena();
    }

    private void handleKingTowerDrop(int x, int y) {
        // Check if King tower already placed
        if (currentLayout.getKingTowerPosition() != null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING);
            alert.setTitle("Limit Reached");
            alert.setHeaderText(null);
            alert.setContentText("You can only place 1 King tower.");
            alert.showAndWait();
            return;
        }

        // Only allow placement in user's bottom half (y > 16)
        if (y <= 16) {
            return;
        }

        // Check bounds for 3x3 tower
        if (x + 2 >= com.kuroyale.model.Arena.WIDTH || y + 2 >= com.kuroyale.model.Arena.HEIGHT) {
            return;
        }

        // Check if position is already occupied by a Princess tower (3x3 overlap check)
        boolean occupied = false;
        for (GridPosition p : currentLayout.getPrincessTowerPositions()) {
            if (isOverlap(x, y, 3, 3, p.getX(), p.getY(), 3, 3)) {
                occupied = true;
                break;
            }
        }

        if (occupied) {
            return;
        }

        // Place King tower
        currentLayout.setKingTowerPosition(x, y);
        renderArena();
    }

    // Helper to check if two rectangles overlap
    private boolean isOverlap(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
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