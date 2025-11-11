package com.kuroyale.view;

import com.kuroyale.model.Card;
import com.kuroyale.model.CardType;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Custom JavaFX component for displaying a card with swipeable pages
 */
public class CardView extends StackPane {
    private final Card card;
    private int currentPage = 0; // 0 = basic info, 1 = detailed stats
    private final StackPane contentPane;
    private final VBox page1;
    private final VBox page2;
    private double startX;
    private final HBox pageIndicator;
    
    public CardView(Card card) {
        this.card = card;
        
        // Set preferred size
        setPrefSize(160, 200);
        setMinSize(160, 200);
        setMaxSize(160, 200);
        
        // Apply style classes
        getStyleClass().add("card-view");
        getStyleClass().add("card-" + card.getType().toString().toLowerCase());
        
        // Create content pane for pages
        contentPane = new StackPane();
        
        // Create both pages
        page1 = createPage1();
        page2 = createPage2();
        
        contentPane.getChildren().addAll(page1, page2);
        page2.setVisible(false);
        
        // Create page indicator
        pageIndicator = createPageIndicator();
        
        // Add everything
        getChildren().addAll(contentPane, pageIndicator);
        StackPane.setAlignment(pageIndicator, Pos.BOTTOM_CENTER);
        StackPane.setMargin(pageIndicator, new Insets(0, 0, 5, 0));
        
        // Setup swipe gesture
        setupSwipeGesture();
    }
    
    private VBox createPage1() {
        VBox page = new VBox(8);
        page.setAlignment(Pos.TOP_CENTER);
        page.setPadding(new Insets(10));
        
        // Elixir cost at top right
        Label costLabel = new Label(String.valueOf(card.getCost()));
        costLabel.getStyleClass().add("card-cost");
        HBox costBox = new HBox(costLabel);
        costBox.setAlignment(Pos.TOP_RIGHT);
        
        // Card image placeholder
        ImageView imageView = createCardImage();
        
        // Card name
        Label nameLabel = new Label(card.getName());
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(140);
        
        // Type badge
        Label typeLabel = new Label(card.getType().toString());
        typeLabel.getStyleClass().addAll("card-type-badge", "badge-" + card.getType().toString().toLowerCase());
        
        page.getChildren().addAll(costBox, imageView, nameLabel, typeLabel);
        
        return page;
    }
    
    private VBox createPage2() {
        VBox page = new VBox(5);
        page.setAlignment(Pos.TOP_LEFT);
        page.setPadding(new Insets(10));
        
        // Card name header
        Label nameLabel = new Label(card.getName());
        nameLabel.getStyleClass().add("card-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(140);
        
        // Stats
        VBox statsBox = new VBox(3);
        statsBox.getStyleClass().add("card-stats");
        
        addStat(statsBox, "Cost", card.getCost() + " elixir");
        addStat(statsBox, "Type", card.getType().toString());
        
        if (card.getHp() > 0) {
            addStat(statsBox, "HP", String.valueOf(card.getHp()));
        }
        
        if (card.getDamage() > 0) {
            addStat(statsBox, "DMG", String.valueOf(card.getDamage()));
        }
        
        if (card.getHitSpeed() > 0) {
            addStat(statsBox, "Hit Speed", card.getHitSpeed() + "s");
        }
        
        if (card.getRange() > 0) {
            addStat(statsBox, "Range", card.getRange() + " tiles");
        }
        
        if (card.getSpeed() != null && card.getSpeed().toString() != "NONE") {
            addStat(statsBox, "Speed", formatSpeed(card.getSpeed().toString()));
        }
        
        addStat(statsBox, "Target", card.getTarget().toString());
        
        if (card.getCount() > 1) {
            addStat(statsBox, "Count", card.getCount() + "x");
        }
        
        if (card.getLifetime() > 0) {
            addStat(statsBox, "Lifetime", card.getLifetime() + "s");
        }
        
        // Description
        Label descLabel = new Label(card.getDescription());
        descLabel.getStyleClass().add("card-description");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(140);
        
        page.getChildren().addAll(nameLabel, statsBox, descLabel);
        
        return page;
    }
    
    private void addStat(VBox container, String label, String value) {
        Label statLabel = new Label(label + ": " + value);
        statLabel.setStyle("-fx-font-size: 10px;");
        container.getChildren().add(statLabel);
    }
    
    private String formatSpeed(String speed) {
        return speed.replace("_", " ").toLowerCase()
                .replaceFirst("^.", String.valueOf(speed.charAt(0)));
    }
    
    private ImageView createCardImage() {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setPreserveRatio(true);
        
        try {
            String imagePath = card.getImagePath();
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            imageView.setImage(image);
        } catch (Exception e) {
            // Create placeholder colored rectangle
            StackPane placeholder = new StackPane();
            placeholder.setPrefSize(80, 80);
            placeholder.setStyle(getPlaceholderStyle());
            
            Label placeholderLabel = new Label(card.getName().substring(0, Math.min(3, card.getName().length())).toUpperCase());
            placeholderLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
            placeholder.getChildren().add(placeholderLabel);
        }
        
        return imageView;
    }
    
    private String getPlaceholderStyle() {
        String color;
        switch (card.getType()) {
            case TROOP:
                color = "#f97316";
                break;
            case BUILDING:
                color = "#92400e";
                break;
            case SPELL:
                color = "#8b5cf6";
                break;
            default:
                color = "#64748b";
        }
        return "-fx-background-color: " + color + "; -fx-background-radius: 8;";
    }
    
    private HBox createPageIndicator() {
        HBox indicator = new HBox(5);
        indicator.setAlignment(Pos.CENTER);
        
        Circle dot1 = new Circle(4);
        Circle dot2 = new Circle(4);
        
        dot1.getStyleClass().addAll("page-indicator", "page-indicator-active");
        dot2.getStyleClass().add("page-indicator");
        
        indicator.getChildren().addAll(dot1, dot2);
        return indicator;
    }
    
    private void setupSwipeGesture() {
        setOnMousePressed(event -> {
            startX = event.getSceneX();
        });
        
        setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - startX;
            
            if (Math.abs(deltaX) > 5) {
                contentPane.setTranslateX(deltaX * 0.3); // Reduced movement for better feel
            }
        });
        
        setOnMouseReleased(event -> {
            double deltaX = event.getSceneX() - startX;
            
            // Swipe threshold
            if (Math.abs(deltaX) > 30) {
                if (deltaX > 0 && currentPage == 1) {
                    // Swipe right - go to page 1
                    switchToPage(0);
                } else if (deltaX < 0 && currentPage == 0) {
                    // Swipe left - go to page 2
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
        
        // Update visibility
        page1.setVisible(pageIndex == 0);
        page2.setVisible(pageIndex == 1);
        
        // Update page indicator
        Circle dot1 = (Circle) pageIndicator.getChildren().get(0);
        Circle dot2 = (Circle) pageIndicator.getChildren().get(1);
        
        if (pageIndex == 0) {
            dot1.getStyleClass().add("page-indicator-active");
            dot2.getStyleClass().remove("page-indicator-active");
        } else {
            dot1.getStyleClass().remove("page-indicator-active");
            dot2.getStyleClass().add("page-indicator-active");
        }
    }
    
    public Card getCard() {
        return card;
    }
}

