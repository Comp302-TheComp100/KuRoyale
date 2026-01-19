package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kuroyale.view.card.CardView;
import com.kuroyale.view.card.DeckSlotView;
import com.kuroyale.view.dialog.CardInfoDialog;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Deck;
import com.kuroyale.model.logic.DeckBuilderModel;
import com.kuroyale.model.entities.User;
import com.kuroyale.model.enums.ComboType;
import com.kuroyale.service.DeckComboAnalyzer;
import com.kuroyale.util.audio.SoundEffectUtil;
import com.kuroyale.util.ui.ButtonFactory;
import com.kuroyale.util.ui.SceneLoader;
import com.kuroyale.util.ui.StyleHelper;
import com.kuroyale.model.state.PvPDeckBuilderSession;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import javafx.scene.shape.Rectangle;

/* Controller for the deck builder screen
 *  Controller -  delegates to services
 *  Low Coupling - uses services via dependency injection
 *  High Cohesion - focused on UI presentation and user interactions*/
public class DeckBuilderController {

    // Constants
    private static final int CARDS_PER_ROW = 4;
    private static final int DECK_SLOT_ROWS = 2;
    private static final int DECK_SLOT_COLS = 4;
    private static final int BUTTON_GAP = 5;
    private static final int CARD_BUTTON_OFFSET_Y = -13; // Vertical offset for bottom grid card buttons (negative =
                                                         // higher/closer)
    private static final int CARD_CONTAINER_SPACING = 5;

    // Slot dimensions and layout constants
    private static final int SLOT_WIDTH = 90;
    private static final int SLOT_HEIGHT = 120;
    private static final int SLOT_GAP_X = 20;
    private static final int SLOT_GAP_Y = 20;
    private static final int SLOT_CONTAINER_WIDTH = 620;
    private static final int SLOT_CONTAINER_HEIGHT = 280;
    private static final int RECESSED_RECT_OFFSET_Y = 20; // Deck slots are 20px lower than brown background

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
    private Label averageElixirValue;
    @FXML
    private Label battleDeckTitle;
    @FXML
    private HBox averageElixirContainer;
    @FXML
    private VBox comboPanel;
    @FXML
    private VBox comboList;
    @FXML
    private ScrollPane comboScrollPane;

    private final DeckBuilderModel model = new DeckBuilderModel();
    private final SceneLoader sceneLoader = new SceneLoader();
    private final DeckComboAnalyzer comboAnalyzer = new DeckComboAnalyzer();

    private Deck deck;
    private List<DeckSlotView> deckSlots;
    private CardView selectedCardView;
    private HBox cardButtonsBox;
    private Map<Card, VBox> cardContainerMap; // Track card containers for hiding/showing
    private DeckSlotView selectedDeckSlot;
    private HBox deckSlotButtonsBox;
    private boolean replaceMode; // Track if in replace mode
    private Card cardToReplace; // Card selected for replacement
    private boolean pvpMode = false; // Track if in PvP deck building mode

    /**
     * Set PvP mode - when true, deck is saved to session and navigates back to PvP
     * selection.
     */
    public void setPvPMode(boolean pvpMode) {
        this.pvpMode = pvpMode;
    }

    @FXML
    private void initialize() {
        deck = new Deck();
        deckSlots = new ArrayList<>();
        cardContainerMap = new HashMap<>();
        replaceMode = false;
        cardToReplace = null;

        // Apply styles
        applyStyles();

        // Create recessed rectangles for deck slot backgrounds
        createRecessedRectangles();

        // Create 8 deck slots
        createDeckSlots();

        // Center average elixir cost within brown area
        centerAverageElixirInBrownArea();

        // Load all 28 cards
        loadAllCards();

        // Load user's saved deck if exists
        loadUserDeck();

        // Initialize average elixir cost display
        updateAverageElixirCost();

        // Add scroll listener to update button positions when scrolling
        cardsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            removeCardButtons();
        });

        // Initialize combo panel display
        updateComboPanel();
    }

    // Apply styles to deck builder components
    private void applyStyles() {
        // Apply CSS classes
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

        // Apply scroll pane style (deferred to ensure viewport is available)
        javafx.application.Platform.runLater(() -> {
            cardsScrollPane.getStyleClass().add("scroll-pane-transparent");
            // Hide scrollbars completely
            cardsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            cardsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        });

        cardsGrid.getStyleClass().add("cards-grid");

        // Apply average elixir label styles
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
    }

    // Creates 8 recessed rectangles that serve as visual backgrounds for deck slot
    private void createRecessedRectangles() {
        // Calculate center offset
        final int TOTAL_WIDTH = (DECK_SLOT_COLS * SLOT_WIDTH) + ((DECK_SLOT_COLS - 1) * SLOT_GAP_X);
        final int TOTAL_HEIGHT = (DECK_SLOT_ROWS * SLOT_HEIGHT) + ((DECK_SLOT_ROWS - 1) * SLOT_GAP_Y);
        final int OFFSET_X = (SLOT_CONTAINER_WIDTH - TOTAL_WIDTH) / 2;
        final int OFFSET_Y = (SLOT_CONTAINER_HEIGHT - TOTAL_HEIGHT) / 2;

        for (int row = 0; row < DECK_SLOT_ROWS; row++) {
            for (int col = 0; col < DECK_SLOT_COLS; col++) {
                // Create rectangle for this slot position
                Rectangle recess = new Rectangle(SLOT_WIDTH, SLOT_HEIGHT);

                // Apply recessed rectangle style
                recess.setStyle(
                        "-fx-fill: " + StyleHelper.COLOR_BROWN_LIGHT + ";" +
                                "-fx-arc-width: 12;" +
                                "-fx-arc-height: 12;");
                recess.setEffect(StyleHelper.getInnerShadowEffect());

                // Calculate position
                double x = OFFSET_X + (col * (SLOT_WIDTH + SLOT_GAP_X));
                double y = RECESSED_RECT_OFFSET_Y + OFFSET_Y + (row * (SLOT_HEIGHT + SLOT_GAP_Y));

                // Add to background AnchorPane
                deckSlotsBackground.getChildren().add(recess);
                AnchorPane.setLeftAnchor(recess, x);
                AnchorPane.setTopAnchor(recess, y);
            }
        }
    }

    private void createDeckSlots() {
        // Calculate center offset to center the 4x2 grid
        final int TOTAL_WIDTH = (DECK_SLOT_COLS * SLOT_WIDTH) + ((DECK_SLOT_COLS - 1) * SLOT_GAP_X);
        final int TOTAL_HEIGHT = (DECK_SLOT_ROWS * SLOT_HEIGHT) + ((DECK_SLOT_ROWS - 1) * SLOT_GAP_Y);
        final int OFFSET_X = (SLOT_CONTAINER_WIDTH - TOTAL_WIDTH) / 2; // Center horizontally
        final int OFFSET_Y = (SLOT_CONTAINER_HEIGHT - TOTAL_HEIGHT) / 2; // Center vertically

        for (int row = 0; row < DECK_SLOT_ROWS; row++) {
            for (int col = 0; col < DECK_SLOT_COLS; col++) {
                DeckSlotView slot = new DeckSlotView();
                slot.setOnMouseClicked(event -> handleDeckSlotClick(slot));
                deckSlots.add(slot);

                // Calculate position for this slot
                double x = OFFSET_X + (col * (SLOT_WIDTH + SLOT_GAP_X));
                double y = OFFSET_Y + (row * (SLOT_HEIGHT + SLOT_GAP_Y));

                // Add to AnchorPane with constraints
                deckSlotsContainer.getChildren().add(slot);
                AnchorPane.setLeftAnchor(slot, x);
                AnchorPane.setTopAnchor(slot, y);
            }
        }
    }

    private void loadAllCards() {
        List<Card> allCards = model.getAllCards();

        // Store all cards for later reorganization
        for (Card card : allCards) {
            VBox cardContainer = new VBox(CARD_CONTAINER_SPACING);
            cardContainer.setAlignment(Pos.TOP_CENTER);

            CardView cardView = new CardView(card);
            cardContainer.getChildren().add(cardView);

            cardView.setOnMouseClicked(event -> {
                handleCardClick(cardView, cardContainer);
                event.consume();
            });

            // Store the container in the map
            cardContainerMap.put(card, cardContainer);
        }

        // Initial layout
        reorganizeCardGrid();
    }

    /*
     * Reorganizes the bottom card grid so visible cards fill rows of 4
     * Cards are always shown in the same consistent order
     * Only cards NOT in the deck are displayed
     */
    private void reorganizeCardGrid() {
        // Clear the grid
        cardsGrid.getChildren().clear();

        // Get all cards in consistent order: Troops, Buildings, Spells
        List<Card> allCards = model.getAllCards();
        int column = 0;
        int row = 0;

        for (Card card : allCards) {
            VBox cardContainer = cardContainerMap.get(card);
            if (cardContainer == null)
                continue;

            // Only add visible cards (not in deck)
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

    // Gets the main AnchorPane from the root pane
    private AnchorPane getMainAnchorPane() {
        return (AnchorPane) rootPane.getChildren().get(0);
    }

    // Removes deck slot buttons and resets selection state
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
        // If in replace mode, handle replacement or cancellation
        if (replaceMode) {
            handleReplaceModeClick(slot);
            return;
        }

        if (slot.isEmpty()) {
            removeDeckSlotButtons();
            removeCardButtons();
            return;
        }

        // If this slot is already selected, deselect it
        if (selectedDeckSlot == slot && deckSlotButtonsBox != null) {
            removeDeckSlotButtons();
            removeCardButtons();
            return;
        }

        // Remove buttons from previously selected slot AND card
        removeDeckSlotButtons();
        removeCardButtons();

        selectedDeckSlot = slot;
        Card card = slot.getCard();
        showDeckSlotButtons(slot, card);
    }

    /*
     * Handles deck slot click when in replace mode
     * The clicked slot's card will be swapped with the card selected from bottom
     * grid
     * The new card takes the exact position of the old card
     */
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

    // Shows action buttons for a deck slot
    private void showDeckSlotButtons(DeckSlotView slot, Card card) {
        HBox buttonsBox = createDeckSlotButtons(card);
        positionButtons(buttonsBox, slot, 0, -10); // 3 pixels below the card
        deckSlotButtonsBox = buttonsBox;
    }

    // Creates action buttons for a deck slot
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

    // Removes card buttons from the overlay
    private void removeCardButtons() {
        if (cardButtonsBox != null) {
            // Remove from AnchorPane overlay
            AnchorPane mainPane = getMainAnchorPane();
            mainPane.getChildren().remove(cardButtonsBox);
            cardButtonsBox = null;
            selectedCardView = null;
        }
    }

    // Helper method to position buttons below a node (card or deck slot)
    private void positionButtons(HBox buttonsBox, javafx.scene.Node node, double xOffset, double yOffset) {
        AnchorPane mainPane = getMainAnchorPane();

        // Add buttons to AnchorPane first so we can measure their actual width
        mainPane.getChildren().add(buttonsBox);

        // Force layout to get actual button width
        buttonsBox.applyCss();
        buttonsBox.layout();

        // Calculate button position relative to node
        Bounds nodeBoundsInScene = node.localToScene(node.getBoundsInLocal());
        Point2D nodePointInAnchorPane = mainPane.sceneToLocal(
                nodeBoundsInScene.getMinX(),
                nodeBoundsInScene.getMinY());

        double nodeWidth = node.getBoundsInLocal().getWidth();
        double nodeHeight = node.getBoundsInLocal().getHeight();
        double buttonsWidth = buttonsBox.getBoundsInLocal().getWidth();

        // Center buttons horizontally below the node
        double buttonX = nodePointInAnchorPane.getX() + (nodeWidth / 2) - (buttonsWidth / 2) + xOffset;
        double buttonY = nodePointInAnchorPane.getY() + nodeHeight + yOffset;

        AnchorPane.setLeftAnchor(buttonsBox, buttonX);
        AnchorPane.setTopAnchor(buttonsBox, buttonY);
    }

    private void handleCardClick(CardView cardView, VBox cardContainer) {
        SoundEffectUtil.playButtonClick();
        Card card = cardView.getCard();

        // If in replace mode, exit it first
        if (replaceMode) {
            exitReplaceMode();
        }

        // If this card is already selected, deselect it
        if (selectedCardView == cardView && cardButtonsBox != null) {
            removeCardButtons();
            removeDeckSlotButtons();
            return;
        }

        // Remove buttons from previously selected card AND deck slot
        removeCardButtons();
        removeDeckSlotButtons();

        selectedCardView = cardView;
        showCardButtons(card, cardContainer);
    }

    // Shows action buttons for a card (overlaid, not affecting layout)
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

        // Get the CardView from the container (first child)
        CardView cardView = (CardView) cardContainer.getChildren().get(0);

        // Position buttons using helper method - centered below card with custom Y
        // offset
        positionButtons(buttonsBox, cardView, 0, CARD_BUTTON_OFFSET_Y);

        cardButtonsBox = buttonsBox;
    }

    // Creates the appropriate action button (USE/REMOVE/REPLACE) for a card
    private Button createCardActionButton(Card card) {
        Button actionButton;

        if (deck.contains(card)) {
            // Card is in deck - create REMOVE button
            actionButton = ButtonFactory.createRemoveButton();
            actionButton.setOnAction(e -> {
                SoundEffectUtil.playButtonClick();
                removeCardFromDeck(card);
                removeCardButtons();
            });
        } else if (deck.isFull()) {
            // Deck is full - create REPLACE button
            actionButton = ButtonFactory.createReplaceButton();
            actionButton.setOnAction(e -> {
                SoundEffectUtil.playButtonClick();
                enterReplaceMode(card);
                removeCardButtons();
            });
        } else {
            // Deck has space - create USE button
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
        CardInfoDialog infoDialog = new CardInfoDialog(
                card,
                () -> {
                    // On close
                    rootPane.getChildren().remove(rootPane.getChildren().size() - 1);
                },
                () -> {
                    String upgradedCardName = card.getName();
                    User currentUser = model.getCurrentUser();

                    // Refresh deck slots
                    for (DeckSlotView slot : deckSlots) {
                        if (!slot.isEmpty() && slot.getCard().getName().equals(upgradedCardName)) {
                            slot.refreshCardDisplay();
                        }
                    }

                    // Refresh card views in bottom grid
                    // First, update the Card instance's level from User's saved levels
                    if (currentUser != null) {
                        int savedLevel = currentUser.getCardLevel(upgradedCardName);
                        for (Map.Entry<Card, VBox> entry : cardContainerMap.entrySet()) {
                            Card mapCard = entry.getKey();
                            if (mapCard.getName().equals(upgradedCardName)) {
                                // Update the Card instance's level before refreshing
                                mapCard.setLevel(savedLevel);
                                VBox container = entry.getValue();
                                if (container.getChildren().size() > 0
                                        && container.getChildren().get(0) instanceof CardView) {
                                    CardView cardView = (CardView) container.getChildren().get(0);
                                    cardView.refreshLevelIndicator();
                                }
                            }
                        }
                    }
                });

        rootPane.getChildren().add(infoDialog);
    }

    private void addCardToDeck(Card card) {
        if (deck.isFull()) {
            return;
        }

        if (deck.addCard(card)) {
            // Find first empty slot and add card
            for (DeckSlotView slot : deckSlots) {
                if (slot.isEmpty()) {
                    slot.setCard(card);
                    break;
                }
            }

            // Reorganize bottom grid to remove this card and shift others
            reorganizeCardGrid();

            // Update average elixir cost
            updateAverageElixirCost();

            // Auto-save deck
            saveDeck();

            // Update combo panel
            updateComboPanel();
        }
    }

    private void removeCardFromDeck(Card card) {
        if (deck.removeCard(card)) {
            // Find and clear the slot with this card
            for (DeckSlotView slot : deckSlots) {
                if (!slot.isEmpty() && slot.getCard().equals(card)) {
                    slot.clear();
                    break;
                }
            }

            // Reorganize remaining cards to fill gaps
            reorganizeDeckSlots();

            // Reorganize bottom grid to add this card back and shift others
            reorganizeCardGrid();

            // Update button states for cards that might now show USE instead of REPLACE
            updateCardButtons();

            // Update average elixir cost
            updateAverageElixirCost();

            // Auto-save deck
            saveDeck();

            // Update combo panel
            updateComboPanel();
        }
    }

    // Enters replace mode - highlights deck slots and waits for user to select
    // aslot
    private void enterReplaceMode(Card cardToReplace) {
        this.replaceMode = true;
        this.cardToReplace = cardToReplace;

        // Highlight all filled deck slots
        for (DeckSlotView slot : deckSlots) {
            if (!slot.isEmpty()) {
                slot.highlightForReplace();
            }
        }
    }

    // Exits replace mode
    private void exitReplaceMode() {
        this.replaceMode = false;
        this.cardToReplace = null;

        // Remove highlights from all deck slots
        for (DeckSlotView slot : deckSlots) {
            slot.removeHighlight();
        }
    }

    // Replaces a card in the deck with a new card
    private void replaceCardInDeck(Card oldCard, Card newCard) {
        // Find the slot with the old card FIRST (before modifying deck)
        DeckSlotView targetSlot = null;
        for (DeckSlotView slot : deckSlots) {
            if (!slot.isEmpty() && slot.getCard().equals(oldCard)) {
                targetSlot = slot;
                break;
            }
        }

        // If we found the slot, perform the replacement
        if (targetSlot != null) {
            model.replaceCardInDeck(deck, oldCard, newCard);

            // Replace the card in the UI at the SAME position (UI concern)
            targetSlot.setCard(newCard);

            // Reorganize bottom grid to reflect changes
            reorganizeCardGrid();

            // Update button states
            updateCardButtons();

            // Update average elixir cost
            updateAverageElixirCost();

            // Auto-save deck
            saveDeck();

            // Update combo panel
            updateComboPanel();
        }
    }

    // Removes current buttons to force refresh on next interaction
    private void updateCardButtons() {
        removeCardButtons();
    }

    // Reorganizes deck slots so that all cards move up to fill empty slots
    private void reorganizeDeckSlots() {
        List<Card> currentCards = new ArrayList<>();

        // Collect all cards currently in deck
        for (DeckSlotView slot : deckSlots) {
            if (!slot.isEmpty()) {
                currentCards.add(slot.getCard());
            }
        }

        // Clear all slots
        for (DeckSlotView slot : deckSlots) {
            slot.clear();
        }

        // Re-add cards starting from first slot
        for (int i = 0; i < currentCards.size(); i++) {
            deckSlots.get(i).setCard(currentCards.get(i));
        }
    }

    // Centers the average elixir cost display within the brown background area
    private void centerAverageElixirInBrownArea() {
        if (averageElixirContainer != null && deckSlotsBackground != null) {
            // Use Platform.runLater to ensure layout is complete
            javafx.application.Platform.runLater(() -> {
                averageElixirContainer.applyCss();
                averageElixirContainer.layout();
                double containerWidth = averageElixirContainer.getWidth();
                if (containerWidth == 0) {
                    containerWidth = averageElixirContainer.prefWidth(-1);
                }
                // Center horizontally within 620px brown area
                double centerX = (620.0 - containerWidth) / 2;
                AnchorPane.setLeftAnchor(averageElixirContainer, centerX);
            });
        }
    }

    // Updates the average elixir cost display
    private void updateAverageElixirCost() {
        if (averageElixirValue != null) {
            double avgCost = model.getDeckAverageElixirCost(deck);
            averageElixirValue.setText(String.format(Locale.ENGLISH, "%.1f", avgCost));
        }
    }

    // Loads the user's saved deck from their account with exact slot positions of
    // cards
    private void loadUserDeck() {
        Map<Integer, Card> deckMap = model.loadUserDeckWithPositions();

        // Update UI with loaded cards
        for (Map.Entry<Integer, Card> entry : deckMap.entrySet()) {
            int slotIndex = entry.getKey();
            Card card = entry.getValue();

            if (slotIndex < deckSlots.size()) {
                deck.addCard(card);
                deckSlots.get(slotIndex).setCard(card);
            }
        }

        // Reorganize grid to hide loaded cards (UI concern)
        reorganizeCardGrid();
    }

    // Saves the current deck to the user's account in their exact slot positions
    private void saveDeck() {
        // Build map of slot positions to cards (UI data)
        Map<Integer, Card> slotCards = new HashMap<>();
        for (int i = 0; i < deckSlots.size(); i++) {
            DeckSlotView slot = deckSlots.get(i);
            if (!slot.isEmpty()) {
                slotCards.put(i, slot.getCard());
            }
        }

        try {
            model.saveDeckWithPositions(slotCards);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save deck: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        SoundEffectUtil.playButtonClick();
        // Exit replace mode if active
        if (replaceMode) {
            exitReplaceMode();
            return;
        }

        if (pvpMode) {
            // Save deck to PvP session and return to PvP selection
            PvPDeckBuilderSession session = PvPDeckBuilderSession.getInstance();
            session.saveBuiltDeck(deck);
            try {
                sceneLoader.load(backButton, "/fxml/pvp-deck-selection.fxml", "KU Royale - PvP Deck Selection", null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                sceneLoader.load(backButton, "/fxml/main-menu.fxml", "KU Royale", null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Updates the combo panel to show available combos based on current deck
    private void updateComboPanel() {
        if (comboList == null)
            return;

        comboList.getChildren().clear();

        // Get available combos from deck
        List<ComboType> availableCombos = comboAnalyzer.getAvailableCombos(deck);

        // Get near-complete combos (missing 1 card)
        List<DeckComboAnalyzer.NearComboInfo> nearComplete = comboAnalyzer.getNearCompleteCombos(deck, availableCombos);

        // Show achieved combos at top (green styling)
        for (ComboType combo : availableCombos) {
            VBox comboEntry = createAchievedComboEntry(combo);
            comboList.getChildren().add(comboEntry);
        }

        // Add separator if there are both achieved and near-complete combos
        if (!availableCombos.isEmpty() && !nearComplete.isEmpty()) {
            javafx.scene.control.Separator separator = new javafx.scene.control.Separator();
            separator.setStyle("-fx-background-color: #555; -fx-padding: 5 0 5 0;");
            comboList.getChildren().add(separator);

            Label nearLabel = new Label("Almost Complete");
            nearLabel.setStyle(
                    "-fx-text-fill: #999; -fx-font-size: 11px; -fx-font-style: italic; -fx-padding: 0 0 5 0;");
            comboList.getChildren().add(nearLabel);
        }

        // Show near-complete combos (grey styling with what's needed)
        for (DeckComboAnalyzer.NearComboInfo info : nearComplete) {
            VBox comboEntry = createNearComboEntry(info);
            comboList.getChildren().add(comboEntry);
        }

        // Style the scroll pane
        if (comboScrollPane != null) {
            comboScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            comboScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        }
    }

    // Creates a visual entry for an achieved combo (green styling)
    private VBox createAchievedComboEntry(ComboType combo) {
        VBox entry = new VBox(4);
        entry.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(40, 80, 40, 0.9), rgba(30, 60, 30, 0.9)); " +
                        "-fx-background-radius: 10; -fx-padding: 10 12; " +
                        "-fx-border-color: #4CAF50; -fx-border-width: 1.5; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.5), 8, 0.4, 0, 0);");

        // Header row with name and checkmark
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label checkMark = new Label("✓");
        checkMark.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label nameLabel = new Label(combo.getDisplayName());
        nameLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 14px; -fx-font-weight: bold;");

        headerRow.getChildren().addAll(checkMark, nameLabel);

        // Effect description
        Label effectLabel = new Label("⚡ " + combo.getEffectDescription());
        effectLabel.setStyle("-fx-text-fill: #90EE90; -fx-font-size: 12px;");

        entry.getChildren().addAll(headerRow, effectLabel);
        return entry;
    }

    // Creates a visual entry for a near-complete combo (grey styling)
    private VBox createNearComboEntry(DeckComboAnalyzer.NearComboInfo info) {
        VBox entry = new VBox(4);
        entry.setStyle(
                "-fx-background-color: linear-gradient(to right, rgba(50, 50, 50, 0.8), rgba(40, 40, 40, 0.8)); " +
                        "-fx-background-radius: 10; -fx-padding: 10 12; " +
                        "-fx-border-color: #666; -fx-border-width: 1; -fx-border-radius: 10;");

        // Header row with name
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(info.combo.getDisplayName());
        nameLabel.setStyle("-fx-text-fill: #bbb; -fx-font-size: 13px; -fx-font-weight: bold;");

        headerRow.getChildren().add(nameLabel);

        // Effect description
        Label effectLabel = new Label("⚡ " + info.combo.getEffectDescription());
        effectLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");

        // What's needed
        Label needLabel = new Label("➕ " + info.missingCard);
        needLabel.setStyle("-fx-text-fill: #f7b32b; -fx-font-size: 11px; -fx-font-weight: bold;");
        needLabel.setWrapText(true);
        needLabel.setMaxWidth(250);

        entry.getChildren().addAll(headerRow, effectLabel, needLabel);
        return entry;
    }
}
