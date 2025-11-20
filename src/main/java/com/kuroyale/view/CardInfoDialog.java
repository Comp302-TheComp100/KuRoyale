package com.kuroyale.view;

import com.kuroyale.model.Card;
import com.kuroyale.model.SpeedType;
import com.kuroyale.util.StyleHelper;

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

    public CardInfoDialog(Card card, Runnable onClose) {
        this.card = card;

        // Full screen overlay
        getStyleClass().add("overlay-background");

        // Main container - apply CSS class
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

        // Close button - apply CSS class
        Button closeButton = new Button("CLOSE");
        closeButton.getStyleClass().add("overlay-button");
        closeButton.setPrefWidth(150);
        closeButton.setPrefHeight(40);
        closeButton.setOnAction(e -> onClose.run());

        mainContainer.getChildren().addAll(contentPane, pageIndicator, closeButton);
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

        // Card name
        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; " +
                "-fx-font-family: '" + StyleHelper.FONT_FAMILY + "', Arial; " +
                "-fx-text-fill: " + StyleHelper.COLOR_DARK + ";");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(300);

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
        typeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " + "-fx-font-family: '" + StyleHelper.FONT_FAMILY +
                "', Arial; " + "-fx-text-fill: " + StyleHelper.COLOR_WHITE + "; " + "-fx-background-color: " + getTypeColor() +
                "; " + "-fx-padding: 5 15 5 15; -fx-background-radius: 5;");

        page.getChildren().addAll(imageView, nameLabel, costBox, typeLabel);
        return page;
    }

    private VBox createPage2() {
        VBox page = new VBox(8);
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(15));

        // Card name header
        Label nameLabel = new Label(card.getName());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(300);

        // Stats
        VBox statsBox = new VBox(5);
        addStat(statsBox, "Cost", card.getCost() + " elixir");
        addStat(statsBox, "Type", card.getType().toString());

        if (card.getHp() > 0) {addStat(statsBox, "HP", String.valueOf(card.getHp()));}
        if (card.getDamage() > 0) {addStat(statsBox, "DMG", String.valueOf(card.getDamage()));
            if (card.getHitSpeed() > 0) {addStat(statsBox, "DPS", String.format("%.1f", card.getDPS()));}}
        if (card.getHitSpeed() > 0) {addStat(statsBox, "Hit Speed", card.getHitSpeed() + "s");}
        if (card.getRange() > 0) {addStat(statsBox, "Range", card.getRange() + " tiles");}
        if (card.getSpeed() != SpeedType.NONE) {addStat(statsBox, "Speed", formatSpeed(card.getSpeed().toString()));}
        addStat(statsBox, "Target", card.getTarget().toString());
        if (card.isAreaEffect()) {addStat(statsBox, "Area Effect", "Yes");}
        if (card.getCount() > 1) {addStat(statsBox, "Count", card.getCount() + "x");}
        if (card.getLifetime() > 0) {addStat(statsBox, "Lifetime", card.getLifetime() + "s");}

        // Description
        Label descLabel = new Label(card.getDescription());
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-wrap-text: true;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(300);

        page.getChildren().addAll(nameLabel, statsBox, descLabel);
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
}