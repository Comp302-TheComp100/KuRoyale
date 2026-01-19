package com.kuroyale.view.draft;

import com.kuroyale.model.entities.Card;
import com.kuroyale.view.card.CardView;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * View for Mega Draft mode - displays all cards in a grid with pick tracking
 * panels.
 * Shows player picks on left, all cards in center, bot picks on right.
 */
public class MegaDraftView extends BorderPane {

    private static final int TIMER_SECONDS = 15;
    private static final int GRID_COLUMNS = 7;
    private static final double CARD_SCALE = 0.70;
    private static final double SMALL_CARD_SCALE = 0.45;

    // UI Components
    private Label timerLabel;
    private Label turnIndicatorLabel;
    private Label firstPickerLabel;
    private final GridPane cardGrid;
    private final VBox playerPicksContent;
    private final VBox botPicksContent;

    // Card tracking
    private final Map<Card, CardView> cardViews = new HashMap<>();
    private final Map<Card, Boolean> pickedCards = new HashMap<>();

    // Timer
    private Timeline countdownTimer;
    private int remainingSeconds;

    // Callbacks
    private Consumer<Card> onCardSelected;
    private Runnable onTimeout;

    // State
    private boolean isPlayerTurn = false;
    private boolean inputEnabled = false;

    public MegaDraftView(List<Card> allCards) {
        setPadding(new Insets(10));
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10;");
        setMaxHeight(680);

        // === TOP SECTION - Fixed height ===
        HBox topBar = createTopBar();
        topBar.setMinHeight(70);
        topBar.setMaxHeight(70);
        setTop(topBar);
        BorderPane.setMargin(topBar, new Insets(0, 0, 10, 0));

        // === LEFT PANEL - Player Picks with ScrollPane ===
        VBox leftPanel = createPicksPanelWithScroll("YOUR DECK", "#22c55e");
        playerPicksContent = (VBox) ((ScrollPane) leftPanel.getChildren().get(1)).getContent();
        setLeft(leftPanel);
        BorderPane.setMargin(leftPanel, new Insets(0, 10, 0, 0));

        // === RIGHT PANEL - Bot Picks with ScrollPane ===
        VBox rightPanel = createPicksPanelWithScroll("OPPONENT", "#ef4444");
        botPicksContent = (VBox) ((ScrollPane) rightPanel.getChildren().get(1)).getContent();
        setRight(rightPanel);
        BorderPane.setMargin(rightPanel, new Insets(0, 0, 0, 10));

        // === CENTER - Card Grid ===
        cardGrid = createCardGrid(allCards);
        StackPane centerWrapper = new StackPane(cardGrid);
        centerWrapper.setAlignment(Pos.CENTER);
        setCenter(centerWrapper);
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(40);
        topBar.setAlignment(Pos.CENTER);
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95); -fx-background-radius: 10;");

        // Timer
        timerLabel = new Label("15");
        timerLabel.setMinWidth(80);
        timerLabel.setAlignment(Pos.CENTER);
        timerLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #22c55e; " +
                "-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 8 20; -fx-background-radius: 8;");

        // Turn indicator
        turnIndicatorLabel = new Label("WAITING...");
        turnIndicatorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");

        // First picker indicator
        firstPickerLabel = new Label("");
        firstPickerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8;");

        VBox turnBox = new VBox(3, turnIndicatorLabel, firstPickerLabel);
        turnBox.setAlignment(Pos.CENTER);

        topBar.getChildren().addAll(timerLabel, turnBox);
        return topBar;
    }

    private VBox createPicksPanelWithScroll(String title, String accentColor) {
        VBox panel = new VBox(5);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setMinWidth(85);
        panel.setMaxWidth(85);
        panel.setStyle("-fx-background-color: rgba(30, 30, 40, 0.95); -fx-background-radius: 10; " +
                "-fx-border-color: " + accentColor + "; -fx-border-width: 2; -fx-border-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");
        titleLabel.setPadding(new Insets(8, 5, 5, 5));

        // Content VBox for cards
        VBox content = new VBox(3);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(2));

        // ScrollPane for cards
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setMaxHeight(480);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        panel.getChildren().addAll(titleLabel, scrollPane);
        return panel;
    }

    private GridPane createCardGrid(List<Card> allCards) {
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(5));

        int col = 0;
        int row = 0;

        for (Card card : allCards) {
            StackPane cardContainer = createCardContainer(card);
            grid.add(cardContainer, col, row);

            col++;
            if (col >= GRID_COLUMNS) {
                col = 0;
                row++;
            }
        }

        return grid;
    }

    private StackPane createCardContainer(Card card) {
        CardView cardView = new CardView(card);
        cardView.setHoverScalingEnabled(false); // Disable internal hover scaling
        cardView.setScaleX(CARD_SCALE);
        cardView.setScaleY(CARD_SCALE);
        cardViews.put(card, cardView);
        pickedCards.put(card, false);

        // Fixed size container
        double containerWidth = 90 * CARD_SCALE;
        double containerHeight = 120 * CARD_SCALE;

        StackPane container = new StackPane(cardView);
        container.setAlignment(Pos.CENTER);
        container.setMinSize(containerWidth, containerHeight);
        container.setMaxSize(containerWidth, containerHeight);
        container.setPrefSize(containerWidth, containerHeight);

        // Hover effects - only glow, no size change
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(12);
        glow.setSpread(0.5);

        container.setOnMouseEntered(e -> {
            if (inputEnabled && isPlayerTurn && !pickedCards.get(card)) {
                cardView.setEffect(glow);
                container.setStyle("-fx-cursor: hand;");
            }
        });

        container.setOnMouseExited(e -> {
            if (!pickedCards.get(card)) {
                cardView.setEffect(null);
            }
            container.setStyle("");
        });

        // Click handler
        container.setOnMouseClicked(e -> {
            if (inputEnabled && isPlayerTurn && !pickedCards.get(card)) {
                stopCountdown();
                if (onCardSelected != null) {
                    onCardSelected.accept(card);
                }
            }
        });

        return container;
    }

    /**
     * Sets up the view for a new draft session.
     */
    public void setupDraft(boolean playerIsFirstPicker) {
        String firstPickerText = playerIsFirstPicker ? "You pick first!" : "Opponent picks first!";
        firstPickerLabel.setText(firstPickerText);
    }

    /**
     * Updates the turn indicator and enables/disables input.
     */
    public void setTurn(boolean isPlayer, int picksRemaining) {
        this.isPlayerTurn = isPlayer;
        this.inputEnabled = isPlayer;

        if (isPlayer) {
            String pickText = picksRemaining == 1 ? "Pick 1 card" : "Pick " + picksRemaining + " cards";
            turnIndicatorLabel.setText("🎯 YOUR TURN - " + pickText);
            turnIndicatorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
            startCountdown();
        } else {
            turnIndicatorLabel.setText("⏳ OPPONENT PICKING...");
            turnIndicatorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
            stopCountdown();
        }
    }

    /**
     * Marks a card as picked by desaturating it.
     */
    public void markCardAsPicked(Card card, boolean isPlayerPick) {
        // Mark as picked
        pickedCards.put(card, true);

        // Apply desaturation effect to the grid card
        CardView gridCardView = cardViews.get(card);
        if (gridCardView != null) {
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-0.7);
            desaturate.setBrightness(-0.3);
            gridCardView.setEffect(desaturate);
            gridCardView.setOpacity(0.5);
        }

        // Add small card to the appropriate panel
        CardView smallCard = new CardView(card);
        smallCard.setHoverScalingEnabled(false); // Disable hover scaling for deck cards too
        smallCard.setScaleX(SMALL_CARD_SCALE);
        smallCard.setScaleY(SMALL_CARD_SCALE);

        // Fixed size wrapper
        double wrapperWidth = 90 * SMALL_CARD_SCALE;
        double wrapperHeight = 120 * SMALL_CARD_SCALE;

        StackPane cardWrapper = new StackPane(smallCard);
        cardWrapper.setMinSize(wrapperWidth, wrapperHeight);
        cardWrapper.setMaxSize(wrapperWidth, wrapperHeight);
        cardWrapper.setPrefSize(wrapperWidth, wrapperHeight);

        if (isPlayerPick) {
            playerPicksContent.getChildren().add(cardWrapper);
        } else {
            botPicksContent.getChildren().add(cardWrapper);
        }
    }

    /**
     * Shows draft completion message.
     */
    public void showDraftComplete() {
        stopCountdown();
        inputEnabled = false;
        turnIndicatorLabel.setText("✅ DRAFT COMPLETE!");
        turnIndicatorLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        timerLabel.setVisible(false);
    }

    private void startCountdown() {
        remainingSeconds = TIMER_SECONDS;
        updateTimerDisplay();

        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            updateTimerDisplay();

            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                if (onTimeout != null) {
                    onTimeout.run();
                }
            }
        }));
        countdownTimer.setCycleCount(TIMER_SECONDS);
        countdownTimer.play();
    }

    private void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    private void updateTimerDisplay() {
        timerLabel.setText(String.valueOf(remainingSeconds));

        if (remainingSeconds <= 5) {
            timerLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #ef4444; " +
                    "-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 8 20; -fx-background-radius: 8;");
        } else if (remainingSeconds <= 10) {
            timerLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #f59e0b; " +
                    "-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 8 20; -fx-background-radius: 8;");
        } else {
            timerLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #22c55e; " +
                    "-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 8 20; -fx-background-radius: 8;");
        }
    }

    public void setOnCardSelected(Consumer<Card> callback) {
        this.onCardSelected = callback;
    }

    public void setOnTimeout(Runnable callback) {
        this.onTimeout = callback;
    }

    public void cleanup() {
        stopCountdown();
    }
}
