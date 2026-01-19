package com.kuroyale.view.dialog;

import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.User;
import com.kuroyale.model.core.enums.SpeedType;
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.util.common.ServiceFactory;
import com.kuroyale.util.ui.StyleHelper;

import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

//Dialog showing detailed card information with swipeable pages
public class CardInfoDialog extends StackPane {
    private final Card card;
    private int currentPage = 0; // 0 = basic info, 1 = detailed stats
    private final StackPane contentPane;
    private final VBox page1;
    private final VBox page2;
    private double startX;
    private final HBox pageIndicator;
    private final AuthenticationService authService;
    @SuppressWarnings("unused")
    private final Runnable onClose;
    private final Runnable onCardUpgraded;

    public CardInfoDialog(Card card, Runnable onClose) {
        this(card, onClose, null);
    }

    public CardInfoDialog(Card card, Runnable onClose, Runnable onCardUpgraded) {
        this.card = card;
        this.onClose = onClose;
        this.onCardUpgraded = onCardUpgraded;
        this.authService = ServiceFactory.getInstance().getAuthenticationService();

        // Full screen overlay
        getStyleClass().add("overlay-background");

        // Main container
        VBox mainContainer = new VBox(15);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(350);
        mainContainer.setMaxHeight(550);
        mainContainer.getStyleClass().add("overlay-card-container");

        // Content pane for swipeable pages
        contentPane = new StackPane();
        contentPane.setPrefHeight(400);

        // Create both pages
        page1 = createPage1();
        page2 = createPage2();

        contentPane.getChildren().addAll(page1, page2);
        page2.setVisible(false);

        // Create page indicator
        pageIndicator = createPageIndicator();

        // Create buttons container with Upgrade and Close buttons
        HBox buttonsContainer = new HBox(10);
        buttonsContainer.setAlignment(Pos.CENTER);

        // Upgrade button
        Button upgradeButton = new Button("UPGRADE");
        upgradeButton.getStyleClass().add("overlay-button");
        upgradeButton.setPrefWidth(180);
        upgradeButton.setPrefHeight(40);
        String normalStyle = "-fx-background-color: " + StyleHelper.COLOR_YELLOW_DARK + "; " +
                "-fx-text-fill: white; -fx-font-size: 14px;";
        String hoverStyle = "-fx-background-color: " + StyleHelper.COLOR_YELLOW + "; " +
                "-fx-text-fill: white; -fx-font-size: 14px;";

        upgradeButton.setStyle(normalStyle);
        upgradeButton.setOnMouseEntered(e -> {
            if (!upgradeButton.isDisabled())
                upgradeButton.setStyle(hoverStyle);
        });
        upgradeButton.setOnMouseExited(e -> {
            if (!upgradeButton.isDisabled())
                upgradeButton.setStyle(normalStyle);
        });

        upgradeButton.setOnAction(e -> handleUpgrade());

        // Update button state
        updateUpgradeButtonState(upgradeButton);

        // Close button
        Button closeButton = new Button("CLOSE");
        closeButton.getStyleClass().add("overlay-button");
        closeButton.setPrefWidth(150);
        closeButton.setPrefHeight(40);
        closeButton.setOnAction(e -> onClose.run());

        buttonsContainer.getChildren().addAll(upgradeButton, closeButton);

        mainContainer.getChildren().addAll(contentPane, pageIndicator, buttonsContainer);
        getChildren().add(mainContainer);

        // Setup swipe gesture
        setupSwipeGesture();

        // Click outside to close
        setOnMouseClicked(event -> {
            if (event.getTarget() == this) {
                onClose.run();
            }
        });
    }

    private VBox createPage1() {
        VBox page = new VBox(10);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(10));

        // Card image
        ImageView imageView = createLargeCardImage();

        // Card name - wrapped in HBox for centering
        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                "-fx-font-family: '" + StyleHelper.FONT_FAMILY + "', Arial; " +
                "-fx-text-fill: " + StyleHelper.COLOR_DARK + ";");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(300);

        HBox nameContainer = new HBox();
        nameContainer.setAlignment(Pos.CENTER);
        nameContainer.getChildren().add(nameLabel);

        // Elixir cost
        HBox costBox = new HBox(8);
        costBox.setAlignment(Pos.CENTER);

        try {
            ImageView elixirIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/cost.png")));
            elixirIcon.setFitWidth(30);
            elixirIcon.setFitHeight(30);

            Label costLabel = new Label(String.valueOf(card.getCost()));
            costLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            costBox.getChildren().addAll(elixirIcon, costLabel);
        } catch (Exception e) {
            Label costLabel = new Label("Cost: " + card.getCost());
            costLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
            costBox.getChildren().add(costLabel);
        }

        // Type badge
        Label typeLabel = new Label(card.getType().toString());
        typeLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; " + "-fx-font-family: '" + StyleHelper.FONT_FAMILY +
                        "', Arial; " + "-fx-text-fill: " + StyleHelper.COLOR_WHITE + "; " + "-fx-background-color: "
                        + getTypeColor() +
                        "; " + "-fx-padding: 5 15 5 15; -fx-background-radius: 5;");

        // Rarity badge
        Label rarityLabel = new Label(card.getRarity().toString());
        rarityLabel.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; " + "-fx-font-family: '" + StyleHelper.FONT_FAMILY +
                        "', Arial; " + "-fx-text-fill: " + StyleHelper.COLOR_WHITE + "; " + "-fx-background-color: "
                        + getRarityColor() +
                        "; " + "-fx-padding: 5 15 5 15; -fx-background-radius: 5;");

        int level = card.getLevel();
        String stars = "";
        for (int i = 0; i < level; i++) {
            stars += "★";
        }
        Label levelLabel = new Label(stars);
        levelLabel.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold; " + "-fx-font-family: '" + StyleHelper.FONT_FAMILY +
                        "', Arial; " + "-fx-text-fill: #fbbf24;");

        page.getChildren().addAll(imageView, nameContainer, costBox, typeLabel, rarityLabel, levelLabel);
        return page;
    }

    private VBox createPage2() {
        VBox page = new VBox(8);
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(15));

        // Card name header - wrapped in HBox for centering
        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(300);

        HBox nameContainer = new HBox();
        nameContainer.setAlignment(Pos.CENTER);
        nameContainer.getChildren().add(nameLabel);

        // Stats
        VBox statsBox = new VBox(5);
        addStat(statsBox, "Cost", card.getCost() + " elixir");
        addStat(statsBox, "Type", card.getType().toString());
        addStat(statsBox, "Rarity", card.getRarity().toString());
        addStat(statsBox, "Level", String.valueOf(card.getLevel()));

        if (card.getHp() > 0) {
            addStat(statsBox, "HP", String.valueOf(card.getHp()));
        }
        if (card.getDamage() > 0) {
            addStat(statsBox, "DMG", String.valueOf(card.getDamage()));
            if (card.getHitSpeed() > 0) {
                addStat(statsBox, "DPS", String.format("%.1f", card.getDPS()));
            }
        }
        if (card.getHitSpeed() > 0) {
            addStat(statsBox, "Hit Speed", card.getHitSpeed() + "s");
        }
        if (card.getRange() > 0) {
            addStat(statsBox, "Range", card.getRange() + " tiles");
        }
        if (card.getSpeed() != SpeedType.NONE) {
            addStat(statsBox, "Speed", formatSpeed(card.getSpeed().toString()));
        }
        addStat(statsBox, "Target", card.getTarget().toString());
        if (card.getType() == com.kuroyale.model.core.enums.CardType.TROOP) {
            addStat(statsBox, "Unit Type", card.isAirUnit() ? "Air" : "Ground");
        }
        if (card.isAreaEffect()) {
            addStat(statsBox, "Area Effect", "Yes");
        }
        if (card.getCount() > 1) {
            addStat(statsBox, "Count", card.getCount() + "x");
        }
        if (card.getLifetime() > 0) {
            addStat(statsBox, "Lifetime", card.getLifetime() + "s");
        }

        // Description
        Label descLabel = new Label(card.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-wrap-text: true;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(300);

        page.getChildren().addAll(nameContainer, statsBox, descLabel);
        return page;
    }

    private void addStat(VBox container, String label, String value) {
        HBox statRow = new HBox(5);

        Label labelText = new Label(label + ":");
        labelText.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        labelText.setMinWidth(100);

        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");

        statRow.getChildren().addAll(labelText, valueText);
        container.getChildren().add(statRow);
    }

    private String formatSpeed(String speed) {
        String formatted = speed.replace("_", " ").toLowerCase(java.util.Locale.ENGLISH);
        return formatted.substring(0, 1).toUpperCase(java.util.Locale.ENGLISH) + formatted.substring(1);
    }

    private String getTypeColor() {
        switch (card.getType()) {
            case TROOP:
                return StyleHelper.COLOR_PRIMARY;
            case BUILDING:
                return "#92400e";
            case SPELL:
                return StyleHelper.COLOR_PURPLE;
            default:
                return StyleHelper.COLOR_SECONDARY;
        }
    }

    private String getRarityColor() {
        switch (card.getRarity()) {
            case COMMON:
                return "#9ca3af"; // Gray
            case RARE:
                return "#3b82f6"; // Blue
            case EPIC:
                return StyleHelper.COLOR_PURPLE; // Purple
            case LEGENDARY:
                return "#f59e0b"; // Orange/Gold
            default:
                return StyleHelper.COLOR_GRAY;
        }
    }

    private ImageView createLargeCardImage() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        try {
            String imagePath = card.getImagePath();
            Image image = new Image(getClass().getResourceAsStream(imagePath), 200, 200, true, true);
            imageView.setImage(image);
        } catch (Exception e) {
            // Placeholder - show error message
            System.err.println("Failed to load image for card: " + card.getName() + " at path: " + card.getImagePath());
            e.printStackTrace();
            imageView.setFitWidth(150);
            imageView.setFitHeight(150);
        }

        return imageView;
    }

    private HBox createPageIndicator() {
        HBox indicator = new HBox(8);
        indicator.setAlignment(Pos.CENTER);

        Circle dot1 = new Circle(5);
        Circle dot2 = new Circle(5);

        // Apply page indicator styles
        dot1.setStyle("-fx-fill: " + StyleHelper.COLOR_BLUE + ";"); // Active style
        dot2.setStyle("-fx-fill: " + StyleHelper.COLOR_GRAY + ";"); // Inactive style

        indicator.getChildren().addAll(dot1, dot2);
        return indicator;
    }

    private void setupSwipeGesture() {
        contentPane.setOnMousePressed(event -> {
            startX = event.getSceneX();
        });
        contentPane.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - startX;

            if (Math.abs(deltaX) > 5) {
                contentPane.setTranslateX(deltaX * 0.3);
            }
        });

        contentPane.setOnMouseReleased(event -> {
            double deltaX = event.getSceneX() - startX;

            // Swipe threshold
            if (Math.abs(deltaX) > 50) {
                if (deltaX > 0 && currentPage == 1) {
                    switchToPage(0);
                } else if (deltaX < 0 && currentPage == 0) {
                    switchToPage(1);
                }
            }
            // Reset position
            TranslateTransition transition = new TranslateTransition(Duration.millis(200), contentPane);
            transition.setToX(0);
            transition.play();
        });
    }

    private void switchToPage(int pageIndex) {
        currentPage = pageIndex;
        page1.setVisible(pageIndex == 0);
        page2.setVisible(pageIndex == 1);

        // Update indicators
        Circle dot1 = (Circle) pageIndicator.getChildren().get(0);
        Circle dot2 = (Circle) pageIndicator.getChildren().get(1);

        if (pageIndex == 0) {
            dot1.setStyle("-fx-fill: " + StyleHelper.COLOR_BLUE + ";"); // Active
            dot2.setStyle("-fx-fill: " + StyleHelper.COLOR_GRAY + ";"); // Inactive
        } else {
            dot1.setStyle("-fx-fill: " + StyleHelper.COLOR_GRAY + ";"); // Inactive
            dot2.setStyle("-fx-fill: " + StyleHelper.COLOR_BLUE + ";"); // Active
        }
    }

    private void updateUpgradeButtonState(Button upgradeButton) {
        User currentUser = authService.getCurrentUser();

        if (currentUser == null) {
            upgradeButton.setDisable(true);
            return;
        }

        boolean atMaxLevel = card.getLevel() >= Card.MAX_LEVEL;
        boolean hasEnoughGold = currentUser.getGold() >= card.calculateUpgradeCost();

        upgradeButton.setDisable(atMaxLevel || !hasEnoughGold);

        // Update button text to show reason if disabled
        if (atMaxLevel) {
            upgradeButton.setText("MAX LEVEL");
        } else if (!hasEnoughGold) {
            upgradeButton.setText("UPGRADE");
        } else {
            upgradeButton.setText("UPGRADE");
        }
    }

    private void handleUpgrade() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Open upgrade dialog
        UpgradeDialog upgradeDialog = new UpgradeDialog(
                card,
                currentUser,
                () -> {
                    // On upgrade success, refresh this dialog
                    refreshDialog();
                },
                () -> {
                    // On cancel, just close the upgrade dialog
                    getChildren().remove(getChildren().size() - 1);
                });

        getChildren().add(upgradeDialog);
    }

    private void refreshDialog() {
        // Remove upgrade dialog
        if (getChildren().size() > 1) {
            getChildren().remove(getChildren().size() - 1);
        }

        // Recreate pages with updated card info
        contentPane.getChildren().clear();
        VBox newPage1 = createPage1();
        VBox newPage2 = createPage2();
        contentPane.getChildren().addAll(newPage1, newPage2);
        newPage2.setVisible(currentPage == 1);
        newPage1.setVisible(currentPage == 0);

        // Update upgrade button state
        VBox mainContainer = (VBox) getChildren().get(0);
        HBox buttonsContainer = (HBox) mainContainer.getChildren().get(2);
        Button upgradeButton = (Button) buttonsContainer.getChildren().get(0);
        updateUpgradeButtonState(upgradeButton);

        if (onCardUpgraded != null) {
            onCardUpgraded.run();
        }
    }
}
