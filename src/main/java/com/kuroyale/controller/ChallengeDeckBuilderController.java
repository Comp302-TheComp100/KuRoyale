package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.kuroyale.view.*;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Challenge;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.logic.DeckBuilderModel;
import com.kuroyale.util.ButtonFactory;
import com.kuroyale.util.SceneLoader;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.util.StyleHelper;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/*Controller for deck building with challenge-specific restrictions.
 * Filters available cards based on challenge rules and validates deck.*/
public class ChallengeDeckBuilderController {

    // Constants
    private static final int CARDS_PER_ROW = 4;
    private static final int DECK_SLOT_ROWS = 2;
    private static final int DECK_SLOT_COLS = 4;
    private static final int BUTTON_GAP = 5;
    private static final int CARD_BUTTON_OFFSET_Y = -13;
    private static final int CARD_CONTAINER_SPACING = 5;

    // Slot dimensions and layout constants
    private static final int SLOT_WIDTH = 90;
    private static final int SLOT_HEIGHT = 120;
    private static final int SLOT_GAP_X = 20;
    private static final int SLOT_GAP_Y = 20;
    private static final int SLOT_CONTAINER_WIDTH = 620;
    private static final int SLOT_CONTAINER_HEIGHT = 280;
    private static final int RECESSED_RECT_OFFSET_Y = 20;

    @FXML
    private AnchorPane deckSlotsBackground;
    @FXML
    private AnchorPane deckSlotsContainer;
    @FXML
    private AnchorPane cardsGridBackground;
    @FXML
    private GridPane cardsGrid;
    @FXML
    private ScrollPane cardsScrollPane;
    @FXML
    private StackPane rootPane;
    @FXML
    private Button backButton;
    @FXML
    private Button startChallengeButton;
    @FXML
    private Label averageElixirValue;
    @FXML
    private Label battleDeckTitle;
    @FXML private HBox averageElixirContainer;
    @FXML private VBox challengeBanner;
    @FXML
    private Label challengeNameLabel;
    @FXML
    private Label challengeRulesLabel;
    @FXML
    private HBox validationBox;
    @FXML
    private Label validationLabel;

    private final DeckBuilderModel model = new DeckBuilderModel();
    private final SceneLoader sceneLoader = new SceneLoader();

    private Challenge currentChallenge;
    private Set<String> allowedCardNames;
    private Deck deck;
    private List<DeckSlotView> deckSlots;
    private CardView selectedCardView;
    private HBox cardButtonsBox;
    private Map<Card, VBox> cardContainerMap;
    private DeckSlotView selectedDeckSlot;
    private HBox deckSlotButtonsBox;
    private boolean replaceMode;
    private Card cardToReplace;

    /*
     * Sets the challenge for this deck builder.
     * Must be called before the scene is shown.
     */
    public void setChallenge(Challenge challenge) {
        this.currentChallenge = challenge;

        // Get allowed cards for this challenge
        List<String> allowed = challenge.getAllowedCardNames();
        if (allowed != null) {
            this.allowedCardNames = new HashSet<>(allowed);
        } else {
            this.allowedCardNames = null; // All cards allowed
        }

        // Update UI
        updateChallengeBanner();
        loadAllCards();
        validateDeck();
    }

    @FXML
    private void initialize() {
        deck = new Deck();
        deckSlots = new ArrayList<>();
        cardContainerMap = new HashMap<>();
        replaceMode = false;
        cardToReplace = null;

        applyStyles();
        createRecessedRectangles();
        createDeckSlots();
        centerAverageElixirInBrownArea();
        updateAverageElixirCost();

        cardsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            removeCardButtons();
        });
    }

    private void updateChallengeBanner() {
        if (currentChallenge != null && challengeNameLabel != null) {
            challengeNameLabel.setText("🏆 " + currentChallenge.getName());
            challengeNameLabel.setStyle(
                    "-fx-font-size: 20px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-font-family: 'Clash', Arial;" +
                            "-fx-text-fill: #fbbf24;");

            // Show all rules (with newlines for multi-line)
            String rules = currentChallenge.getRules();
            challengeRulesLabel.setText(rules);
            challengeRulesLabel.setWrapText(true);
            challengeRulesLabel.setStyle(
                    "-fx-font-size: 12px;" +
                            "-fx-font-family: 'Clash', Arial;" +
                            "-fx-text-fill: rgba(255, 255, 255, 0.7);");
        }
    }

    private void applyStyles() {
        AnchorPane mainBackground = getMainAnchorPane();
        if (mainBackground != null) {
            mainBackground.getStyleClass().add("deck-builder-background");
        }

        deckSlotsBackground.getStyleClass().add("deck-slots-background");

        if (cardsGridBackground != null) {
            cardsGridBackground.getStyleClass().add("cards-grid-background");
        }

        if (battleDeckTitle != null) {
            battleDeckTitle.getStyleClass().add("battle-deck-title-label");
        }

        backButton.getStyleClass().add("back-button");
        startChallengeButton.getStyleClass().add("challenge-start-button");

        javafx.application.Platform.runLater(() -> {
            cardsScrollPane.getStyleClass().add("scroll-pane-transparent");
            cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        });

        cardsGrid.getStyleClass().add("cards-grid");

        if (averageElixirContainer != null) {
            for (javafx.scene.Node node : averageElixirContainer.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    if (label.getText().startsWith("Average")) {
                        label.getStyleClass().add("average-elixir-label");
                    } else if (label == averageElixirValue) {
                        label.getStyleClass().add("average-elixir-value");
                    }
                }
            }
        }

        // Style validation box
        if (validationBox != null) {
            validationBox.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8; -fx-padding: 8;");
        }
    }

    private void createRecessedRectangles() {
        final int TOTAL_WIDTH = (DECK_SLOT_COLS * SLOT_WIDTH) + ((DECK_SLOT_COLS - 1) * SLOT_GAP_X);
        final int TOTAL_HEIGHT = (DECK_SLOT_ROWS * SLOT_HEIGHT) + ((DECK_SLOT_ROWS - 1) * SLOT_GAP_Y);
        final int OFFSET_X = (SLOT_CONTAINER_WIDTH - TOTAL_WIDTH) / 2;
        final int OFFSET_Y = (SLOT_CONTAINER_HEIGHT - TOTAL_HEIGHT) / 2;

        for (int row = 0; row < DECK_SLOT_ROWS; row++) {
            for (int col = 0; col < DECK_SLOT_COLS; col++) {
                Rectangle recess = new Rectangle(SLOT_WIDTH, SLOT_HEIGHT);
                recess.setStyle(
                        "-fx-fill: " + StyleHelper.COLOR_BROWN_LIGHT + ";" +
                                "-fx-arc-width: 12;" +
                                "-fx-arc-height: 12;");
                recess.setEffect(StyleHelper.getInnerShadowEffect());

                double x = OFFSET_X + (col * (SLOT_WIDTH + SLOT_GAP_X));
                double y = RECESSED_RECT_OFFSET_Y + OFFSET_Y + (row * (SLOT_HEIGHT + SLOT_GAP_Y));

                deckSlotsBackground.getChildren().add(recess);
                AnchorPane.setLeftAnchor(recess, x);
                AnchorPane.setTopAnchor(recess, y);
            }
        }
    }

    private void createDeckSlots() {
        final int TOTAL_WIDTH = (DECK_SLOT_COLS * SLOT_WIDTH) + ((DECK_SLOT_COLS - 1) * SLOT_GAP_X);
        final int TOTAL_HEIGHT = (DECK_SLOT_ROWS * SLOT_HEIGHT) + ((DECK_SLOT_ROWS - 1) * SLOT_GAP_Y);
        final int OFFSET_X = (SLOT_CONTAINER_WIDTH - TOTAL_WIDTH) / 2;
        final int OFFSET_Y = (SLOT_CONTAINER_HEIGHT - TOTAL_HEIGHT) / 2;

        for (int row = 0; row < DECK_SLOT_ROWS; row++) {
            for (int col = 0; col < DECK_SLOT_COLS; col++) {
                DeckSlotView slot = new DeckSlotView();
                slot.setOnMouseClicked(event -> handleDeckSlotClick(slot));
                deckSlots.add(slot);

                double x = OFFSET_X + (col * (SLOT_WIDTH + SLOT_GAP_X));
                double y = OFFSET_Y + (row * (SLOT_HEIGHT + SLOT_GAP_Y));

                deckSlotsContainer.getChildren().add(slot);
                AnchorPane.setLeftAnchor(slot, x);
                AnchorPane.setTopAnchor(slot, y);
            }
        }
    }

    private void loadAllCards() {
        cardContainerMap.clear();
        List<Card> allCards = model.getAllCards();

        for (Card card : allCards) {
            // Check if card is allowed for this challenge
            boolean isAllowed = isCardAllowed(card);

            if (isAllowed) {
                VBox cardContainer = new VBox(CARD_CONTAINER_SPACING);
                cardContainer.setAlignment(Pos.TOP_CENTER);

                CardView cardView = new CardView(card);
                cardContainer.getChildren().add(cardView);

                cardView.setOnMouseClicked(event -> {
                    handleCardClick(cardView, cardContainer);
                    event.consume();
                });

                cardContainerMap.put(card, cardContainer);
            }
        }

        reorganizeCardGrid();
    }

    // Checks if a card is allowed for the current challenge.
    private boolean isCardAllowed(Card card) {
        if (allowedCardNames == null) {
            return true;
        }
        return allowedCardNames.contains(card.getName());
    }

    private void reorganizeCardGrid() {
        cardsGrid.getChildren().clear();

        List<Card> allCards = model.getAllCards();
        int column = 0;
        int row = 0;

        for (Card card : allCards) {
            VBox cardContainer = cardContainerMap.get(card);
            if (cardContainer == null)
                continue;

            if (!deck.contains(card)) {
                cardsGrid.add(cardContainer, column, row);
                column++;
                if (column >= CARDS_PER_ROW) {
                    column = 0;
                    row++;
                }
            }
        }
    }

    // Validates the current deck against challenge requirements.
    private void validateDeck() {
        if (currentChallenge == null)
            return;

        List<Card> deckCards = deck.getCards();
        List<String> errors = currentChallenge.validateDeck(deckCards);
        boolean isValid = errors.isEmpty() && deck.isFull();

        // Update validation UI
        if (validationLabel != null) {
            if (deck.getCards().isEmpty()) {
                validationLabel.setText("Select cards to build your deck");
                validationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.7);");
            } else if (isValid) {
                validationLabel.setText("Deck is valid! Ready to start challenge.");
                validationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
            } else {
                // Show error message (e.g., "Must have at least 5 swarm cards (you have 0)")
                String message;
                if (!errors.isEmpty()) {
                    message = errors.get(0);
                } else {
                    message = "Need " + (8 - deckCards.size()) + " more cards";
                }

                validationLabel.setText(message);
                validationLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444; -fx-font-weight: bold;");
            }
        }

        // Enable/disable start button
        if (startChallengeButton != null) {
            startChallengeButton.setDisable(!isValid);
            if (isValid) {
                startChallengeButton.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #10b981 0%, #059669 100%);" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-family: 'Clash', Arial;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-radius: 10;" +
                                "-fx-border-color: #047857;" +
                                "-fx-border-width: 2;" +
                                "-fx-padding: 12 30 12 30;" +
                                "-fx-cursor: hand;");
            } else {
                startChallengeButton.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, #6b7280 0%, #4b5563 100%);" +
                                "-fx-text-fill: rgba(255,255,255,0.5);" +
                                "-fx-font-size: 16px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-font-family: 'Clash', Arial;" +
                                "-fx-background-radius: 10;" +
                                "-fx-border-radius: 10;" +
                                "-fx-border-color: #374151;" +
                                "-fx-border-width: 2;" +
                                "-fx-padding: 12 30 12 30;");
            }
        }
    }

    private AnchorPane getMainAnchorPane() {
        return (AnchorPane) rootPane.getChildren().get(0);
    }

    private void removeDeckSlotButtons() {
        if (deckSlotButtonsBox != null) {
            AnchorPane mainPane = getMainAnchorPane();
            mainPane.getChildren().remove(deckSlotButtonsBox);
            deckSlotButtonsBox = null;
            selectedDeckSlot = null;
        }
    }

    private void handleDeckSlotClick(DeckSlotView slot) {
        SoundEffectUtil.playButtonClick();
        if (replaceMode) {
            handleReplaceModeClick(slot);
            return;
        }

        if (slot.isEmpty()) {
            removeDeckSlotButtons();
            removeCardButtons();
            return;
        }

        if (selectedDeckSlot == slot && deckSlotButtonsBox != null) {
            removeDeckSlotButtons();
            removeCardButtons();
            return;
        }

        removeDeckSlotButtons();
        removeCardButtons();

        selectedDeckSlot = slot;
        Card card = slot.getCard();
        showDeckSlotButtons(slot, card);
    }

    private void handleReplaceModeClick(DeckSlotView slot) {
        if (!slot.isEmpty() && cardToReplace != null) {
            Card oldCard = slot.getCard();
            Card newCard = cardToReplace;
            replaceCardInDeck(oldCard, newCard);
            exitReplaceMode();
        } else if (slot.isEmpty()) {
            exitReplaceMode();
        }
    }

    private void showDeckSlotButtons(DeckSlotView slot, Card card) {
        HBox buttonsBox = createDeckSlotButtons(card);
        positionButtons(buttonsBox, slot, 0, -10);
        deckSlotButtonsBox = buttonsBox;
    }

    private HBox createDeckSlotButtons(Card card) {
        HBox buttonsBox = new HBox(BUTTON_GAP);
        buttonsBox.setAlignment(Pos.CENTER);

        Button infoButton = ButtonFactory.createInfoButton();
        infoButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            showCardInfo(card);
        });

        Button removeButton = ButtonFactory.createRemoveButton();
        removeButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            removeDeckSlotButtons();
            removeCardButtons();
            removeCardFromDeck(card);
        });

        buttonsBox.getChildren().addAll(infoButton, removeButton);
        return buttonsBox;
    }

    private void removeCardButtons() {
        if (cardButtonsBox != null) {
            AnchorPane mainPane = getMainAnchorPane();
            mainPane.getChildren().remove(cardButtonsBox);
            cardButtonsBox = null;
            selectedCardView = null;
        }
    }

    private void positionButtons(HBox buttonsBox, javafx.scene.Node node, double xOffset, double yOffset) {
        AnchorPane mainPane = getMainAnchorPane();
        mainPane.getChildren().add(buttonsBox);
        buttonsBox.applyCss();
        buttonsBox.layout();

        javafx.geometry.Bounds nodeBoundsInScene = node.localToScene(node.getBoundsInLocal());
        javafx.geometry.Point2D nodePointInAnchorPane = mainPane.sceneToLocal(
                nodeBoundsInScene.getMinX(),
                nodeBoundsInScene.getMinY());

        double nodeWidth = node.getBoundsInLocal().getWidth();
        double nodeHeight = node.getBoundsInLocal().getHeight();
        double buttonsWidth = buttonsBox.getBoundsInLocal().getWidth();

        double buttonX = nodePointInAnchorPane.getX() + (nodeWidth / 2) - (buttonsWidth / 2) + xOffset;
        double buttonY = nodePointInAnchorPane.getY() + nodeHeight + yOffset;

        AnchorPane.setLeftAnchor(buttonsBox, buttonX);
        AnchorPane.setTopAnchor(buttonsBox, buttonY);
    }

    private void handleCardClick(CardView cardView, VBox cardContainer) {
        SoundEffectUtil.playButtonClick();
        Card card = cardView.getCard();

        if (replaceMode) {
            exitReplaceMode();
        }

        if (selectedCardView == cardView && cardButtonsBox != null) {
            removeCardButtons();
            removeDeckSlotButtons();
            return;
        }

        removeCardButtons();
        removeDeckSlotButtons();

        selectedCardView = cardView;
        showCardButtons(card, cardContainer);
    }

    private void showCardButtons(Card card, VBox cardContainer) {
        HBox buttonsBox = new HBox(BUTTON_GAP);
        buttonsBox.setAlignment(Pos.CENTER);

        Button infoButton = ButtonFactory.createInfoButton();
        infoButton.setOnAction(e -> {
            SoundEffectUtil.playButtonClick();
            showCardInfo(card);
        });

        Button actionButton = createCardActionButton(card);
        buttonsBox.getChildren().addAll(infoButton, actionButton);

        CardView cardView = (CardView) cardContainer.getChildren().get(0);
        positionButtons(buttonsBox, cardView, 0, CARD_BUTTON_OFFSET_Y);

        cardButtonsBox = buttonsBox;
    }

    private Button createCardActionButton(Card card) {
        Button actionButton;

        if (deck.contains(card)) {
            actionButton = ButtonFactory.createRemoveButton();
            actionButton.setOnAction(e -> {
                SoundEffectUtil.playButtonClick();
                removeCardFromDeck(card);
                removeCardButtons();
            });
        } else if (deck.isFull()) {
            actionButton = ButtonFactory.createReplaceButton();
            actionButton.setOnAction(e -> {
                SoundEffectUtil.playButtonClick();
                enterReplaceMode(card);
                removeCardButtons();
            });
        } else {
            actionButton = ButtonFactory.createUseButton();
            actionButton.setOnAction(e -> {
                SoundEffectUtil.playButtonClick();
                addCardToDeck(card);
                removeCardButtons();
            });
        }

        return actionButton;
    }

    private void showCardInfo(Card card) {
        CardInfoDialog infoDialog = new CardInfoDialog(card, () -> {
            rootPane.getChildren().remove(rootPane.getChildren().size() - 1);
        });
        rootPane.getChildren().add(infoDialog);
    }

    private void addCardToDeck(Card card) {
        if (deck.isFull())
            return;

        if (deck.addCard(card)) {
            for (DeckSlotView slot : deckSlots) {
                if (slot.isEmpty()) {
                    slot.setCard(card);
                    break;
                }
            }
            reorganizeCardGrid();
            updateAverageElixirCost();
            validateDeck();
        }
    }

    private void removeCardFromDeck(Card card) {
        if (deck.removeCard(card)) {
            for (DeckSlotView slot : deckSlots) {
                if (!slot.isEmpty() && slot.getCard().equals(card)) {
                    slot.clear();
                    break;
                }
            }
            reorganizeDeckSlots();
            reorganizeCardGrid();
            updateCardButtons();
            updateAverageElixirCost();
            validateDeck();
        }
    }

    private void enterReplaceMode(Card cardToReplace) {
        this.replaceMode = true;
        this.cardToReplace = cardToReplace;

        for (DeckSlotView slot : deckSlots) {
            if (!slot.isEmpty()) {
                slot.highlightForReplace();
            }
        }
    }

    private void exitReplaceMode() {
        this.replaceMode = false;
        this.cardToReplace = null;

        for (DeckSlotView slot : deckSlots) {
            slot.removeHighlight();
        }
    }

    private void replaceCardInDeck(Card oldCard, Card newCard) {
        DeckSlotView targetSlot = null;
        for (DeckSlotView slot : deckSlots) {
            if (!slot.isEmpty() && slot.getCard().equals(oldCard)) {
                targetSlot = slot;
                break;
            }
        }

        if (targetSlot != null) {
            model.replaceCardInDeck(deck, oldCard, newCard);
            targetSlot.setCard(newCard);
            reorganizeCardGrid();
            updateCardButtons();
            updateAverageElixirCost();
            validateDeck();
        }
    }

    private void updateCardButtons() {
        removeCardButtons();
    }

    private void reorganizeDeckSlots() {
        List<Card> currentCards = new ArrayList<>();

        for (DeckSlotView slot : deckSlots) {
            if (!slot.isEmpty()) {
                currentCards.add(slot.getCard());
            }
        }

        for (DeckSlotView slot : deckSlots) {
            slot.clear();
        }

        for (int i = 0; i < currentCards.size(); i++) {
            deckSlots.get(i).setCard(currentCards.get(i));
        }
    }

    private void centerAverageElixirInBrownArea() {
        if (averageElixirContainer != null && deckSlotsBackground != null) {
            javafx.application.Platform.runLater(() -> {
                averageElixirContainer.applyCss();
                averageElixirContainer.layout();
                double containerWidth = averageElixirContainer.getWidth();
                if (containerWidth == 0) {
                    containerWidth = averageElixirContainer.prefWidth(-1);
                }
                double centerX = (620.0 - containerWidth) / 2;
                AnchorPane.setLeftAnchor(averageElixirContainer, centerX);
            });
        }
    }

    private void updateAverageElixirCost() {
        if (averageElixirValue != null) {
            double avgCost = model.getDeckAverageElixirCost(deck);
            averageElixirValue.setText(String.format(Locale.ENGLISH, "%.1f", avgCost));
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        if (replaceMode) {
            exitReplaceMode();
            return;
        }

        try {
            sceneLoader.load(backButton, "/fxml/challenge-selection.fxml", "KU Royale - Challenges", null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleStartChallenge() {
        SoundEffectUtil.playButtonClick();

        // Final validation
        if (currentChallenge == null)
            return;

        List<String> errors = currentChallenge.validateDeck(deck.getCards());
        if (!errors.isEmpty() || !deck.isFull()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid Deck");
            alert.setHeaderText("Your deck doesn't meet the challenge requirements");
            alert.setContentText(errors.isEmpty() ? "You need 8 cards in your deck." : errors.get(0));
            alert.showAndWait();
            return;
        }

        // Start Battle
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/battle.fxml"));
            javafx.scene.Parent root = loader.load();

            BattleController controller = loader.getController();
            // Start challenge match with validated deck
            controller.startChallengeGame(currentChallenge, deck);

            // Switch scene
            javafx.stage.Stage stage = (javafx.stage.Stage) startChallengeButton.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1280, 720);
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale - Challenge: " + currentChallenge.getName());

            // Start game logic (handled by controller.startChallengeGame -> startGame)
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to start challenge");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
