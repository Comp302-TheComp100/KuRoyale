package com.kuroyale.view.draft;

import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.view.card.CardView;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * View for Tower Defense Draft phase.
 * Shows attack wave at top, mega draft grid in center, player decks on sides.
 */
public class TowerDefenseDraftView extends BorderPane {

    private static final int GRID_COLUMNS = 7;
    private static final double CARD_SCALE = 0.65;
    private static final double SMALL_CARD_SCALE = 0.45;
    private static final double ATTACK_CARD_SCALE = 0.55;

    // UI Components
    private Label roundLabel;
    private Label scoreLabel;
    private Label turnIndicatorLabel;
    private Label attackElixirLabel;
    private final GridPane cardGrid;
    private final VBox player1Panel;
    private final VBox player2Panel;
    private final HBox attackWaveDisplay;
    private Button player1ReadyButton;
    private Button player2ReadyButton;

    // Card tracking
    private final Map<Card, CardView> cardViews = new HashMap<>();
    private final Map<Card, Boolean> pickedCards = new HashMap<>();

    // Callbacks
    private Consumer<Card> onCardSelected;
    private Consumer<Integer> onPlayerReady;
    private Runnable onBack;

    // State
    private int currentPlayer = 1;

    public TowerDefenseDraftView(List<Card> allCards, List<Card> attackWave, int round, int p1Score, int p2Score) {
        setPadding(new Insets(10));
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.92); -fx-background-radius: 10;");

        // === TOP SECTION - Attack Wave + Info ===
        VBox topSection = createTopSection(attackWave, round, p1Score, p2Score);
        setTop(topSection);
        BorderPane.setMargin(topSection, new Insets(0, 0, 10, 0));

        // === LEFT PANEL - Player 1 Deck ===
        player1Panel = createPlayerPanel("PLAYER 1", "#22c55e", 1);
        setLeft(player1Panel);
        BorderPane.setMargin(player1Panel, new Insets(0, 10, 0, 0));

        // === RIGHT PANEL - Player 2 Deck ===
        player2Panel = createPlayerPanel("PLAYER 2", "#3b82f6", 2);
        setRight(player2Panel);
        BorderPane.setMargin(player2Panel, new Insets(0, 0, 0, 10));

        // === CENTER - Card Grid ===
        cardGrid = createCardGrid(allCards);
        ScrollPane gridScroll = new ScrollPane(cardGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(gridScroll);

        // === BOTTOM - Back Button ===
        Button backButton = new Button("BACK");
        backButton.getStyleClass().add("back-button");
        backButton.setOnAction(e -> {
            if (onBack != null)
                onBack.run();
        });
        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        setBottom(bottomBar);

        // Create attack wave display
        attackWaveDisplay = new HBox(5);
        attackWaveDisplay.setAlignment(Pos.CENTER);
    }

    private VBox createTopSection(List<Card> attackWave, int round, int p1Score, int p2Score) {
        VBox topSection = new VBox(8);
        topSection.setAlignment(Pos.CENTER);
        topSection.setPadding(new Insets(10));
        topSection.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95); -fx-background-radius: 10;");

        // Round and score info
        HBox infoBar = new HBox(30);
        infoBar.setAlignment(Pos.CENTER);

        roundLabel = new Label("ROUND " + round);
        roundLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");

        scoreLabel = new Label("P1: " + p1Score + " - P2: " + p2Score);
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");

        turnIndicatorLabel = new Label("PLAYER 1's TURN");
        turnIndicatorLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");

        infoBar.getChildren().addAll(roundLabel, turnIndicatorLabel, scoreLabel);

        // Attack wave label
        int totalElixir = attackWave.stream().mapToInt(Card::getCost).sum();
        attackElixirLabel = new Label(
                "COMPUTER ATTACK WAVE (" + totalElixir + " Elixir, " + attackWave.size() + " troops)");
        attackElixirLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");

        // Attack wave cards display
        HBox attackCards = new HBox(4);
        attackCards.setAlignment(Pos.CENTER);
        attackCards.setPadding(new Insets(5));

        for (Card card : attackWave) {
            CardView cardView = new CardView(card);
            cardView.setHoverScalingEnabled(false);
            cardView.setScaleX(ATTACK_CARD_SCALE);
            cardView.setScaleY(ATTACK_CARD_SCALE);

            StackPane wrapper = new StackPane(cardView);
            wrapper.setMinSize(90 * ATTACK_CARD_SCALE, 120 * ATTACK_CARD_SCALE);
            wrapper.setMaxSize(90 * ATTACK_CARD_SCALE, 120 * ATTACK_CARD_SCALE);
            attackCards.getChildren().add(wrapper);
        }

        ScrollPane attackScroll = new ScrollPane(attackCards);
        attackScroll.setFitToHeight(true);
        attackScroll.setMaxHeight(90);
        attackScroll.setStyle(
                "-fx-background: transparent; -fx-background-color: rgba(60, 20, 20, 0.5); -fx-background-radius: 8;");
        attackScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        attackScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        topSection.getChildren().addAll(infoBar, attackElixirLabel, attackScroll);
        return topSection;
    }

    private VBox createPlayerPanel(String title, String accentColor, int playerNum) {
        VBox panel = new VBox(5);
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setMinWidth(100);
        panel.setMaxWidth(100);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: rgba(30, 30, 40, 0.95); -fx-background-radius: 10; " +
                "-fx-border-color: " + accentColor + "; -fx-border-width: 2; -fx-border-radius: 10;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        Label deckCount = new Label("0 cards");
        deckCount.setId("deckCount" + playerNum);
        deckCount.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        // Deck cards container
        VBox deckCards = new VBox(2);
        deckCards.setId("deckCards" + playerNum);
        deckCards.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(deckCards);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        // Hide scrollbar completely
        scrollPane.getStyleClass().add("hidden-scrollbar");
        scrollPane.setMaxHeight(350);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Ready button (always enabled - player decides when to confirm)
        Button readyButton = new Button("CONFIRM");
        readyButton.setDisable(false);
        readyButton.setStyle("-fx-font-size: 11px; -fx-background-color: #22c55e; -fx-text-fill: white; " +
                "-fx-background-radius: 5; -fx-padding: 6 12; -fx-cursor: hand;");
        final int pNum = playerNum; // For lambda capture
        readyButton.setOnAction(e -> {
            System.out.println("[TOWER DEFENSE] CONFIRM button clicked for Player " + pNum);
            if (onPlayerReady != null) {
                onPlayerReady.accept(pNum);
            }
        });

        if (playerNum == 1) {
            player1ReadyButton = readyButton;
        } else {
            player2ReadyButton = readyButton;
        }

        panel.getChildren().addAll(titleLabel, deckCount, scrollPane, readyButton);
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
        cardView.setHoverScalingEnabled(false);
        cardView.setScaleX(CARD_SCALE);
        cardView.setScaleY(CARD_SCALE);
        cardViews.put(card, cardView);
        pickedCards.put(card, false);

        double containerWidth = 90 * CARD_SCALE;
        double containerHeight = 120 * CARD_SCALE;

        StackPane container = new StackPane(cardView);
        container.setAlignment(Pos.CENTER);
        container.setMinSize(containerWidth, containerHeight);
        container.setMaxSize(containerWidth, containerHeight);
        container.setPrefSize(containerWidth, containerHeight);

        // Hover effect
        DropShadow glow = new DropShadow();
        glow.setColor(Color.GOLD);
        glow.setRadius(12);
        glow.setSpread(0.5);

        container.setOnMouseEntered(e -> {
            if (!pickedCards.get(card)) {
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

        container.setOnMouseClicked(e -> {
            if (!pickedCards.get(card)) {
                if (onCardSelected != null) {
                    onCardSelected.accept(card);
                }
            }
        });

        return container;
    }

    public void setCurrentPlayer(int player) {
        this.currentPlayer = player;
        if (player == 1) {
            turnIndicatorLabel.setText("PLAYER 1's TURN");
            turnIndicatorLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
        } else {
            turnIndicatorLabel.setText("PLAYER 2's TURN");
            turnIndicatorLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #3b82f6;");
        }
    }

    public void markCardAsPicked(Card card, boolean isPlayer1) {
        pickedCards.put(card, true);

        CardView gridCardView = cardViews.get(card);
        if (gridCardView != null) {
            ColorAdjust desaturate = new ColorAdjust();
            desaturate.setSaturation(-0.7);
            desaturate.setBrightness(-0.3);
            gridCardView.setEffect(desaturate);
            gridCardView.setOpacity(0.5);
        }
    }

    public void updatePlayerDeck(int player, Deck deck) {
        VBox panel = player == 1 ? player1Panel : player2Panel;

        // Find deck cards container and count label
        VBox deckCards = (VBox) ((ScrollPane) panel.getChildren().get(2)).getContent();
        Label deckCount = (Label) panel.getChildren().get(1);

        deckCards.getChildren().clear();
        for (Card card : deck.getCards()) {
            CardView smallCard = new CardView(card);
            smallCard.setHoverScalingEnabled(false);
            smallCard.setScaleX(SMALL_CARD_SCALE);
            smallCard.setScaleY(SMALL_CARD_SCALE);

            StackPane wrapper = new StackPane(smallCard);
            wrapper.setMinSize(90 * SMALL_CARD_SCALE, 120 * SMALL_CARD_SCALE);
            wrapper.setMaxSize(90 * SMALL_CARD_SCALE, 120 * SMALL_CARD_SCALE);
            deckCards.getChildren().add(wrapper);
        }

        deckCount.setText(deck.size() + " cards");
    }

    /**
     * Updates the player's card display using a List (no 8-card limit).
     */
    public void updatePlayerCards(int player, List<Card> cards) {
        VBox panel = player == 1 ? player1Panel : player2Panel;

        // Find deck cards container and count label
        VBox deckCards = (VBox) ((ScrollPane) panel.getChildren().get(2)).getContent();
        Label deckCount = (Label) panel.getChildren().get(1);

        deckCards.getChildren().clear();
        for (Card card : cards) {
            CardView smallCard = new CardView(card);
            smallCard.setHoverScalingEnabled(false);
            smallCard.setScaleX(SMALL_CARD_SCALE);
            smallCard.setScaleY(SMALL_CARD_SCALE);

            StackPane wrapper = new StackPane(smallCard);
            wrapper.setMinSize(90 * SMALL_CARD_SCALE, 120 * SMALL_CARD_SCALE);
            wrapper.setMaxSize(90 * SMALL_CARD_SCALE, 120 * SMALL_CARD_SCALE);
            deckCards.getChildren().add(wrapper);
        }

        deckCount.setText(cards.size() + " cards");
    }

    public void enableReadyButton(int player) {
        Button button = player == 1 ? player1ReadyButton : player2ReadyButton;
        if (button != null) {
            button.setDisable(false);
            button.setStyle("-fx-font-size: 11px; -fx-background-color: #22c55e; -fx-text-fill: white; " +
                    "-fx-background-radius: 5; -fx-padding: 6 12; -fx-cursor: hand;");
            System.out.println("[TOWER DEFENSE] CONFIRM button enabled for Player " + player);
        }
    }

    public void showBattleStarting() {
        turnIndicatorLabel.setText("⚔️ BATTLE STARTING...");
        turnIndicatorLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
    }

    public void setOnCardSelected(Consumer<Card> callback) {
        this.onCardSelected = callback;
    }

    public void setOnPlayerReady(Consumer<Integer> callback) {
        this.onPlayerReady = callback;
    }

    public void setOnBack(Runnable callback) {
        this.onBack = callback;
    }
}
