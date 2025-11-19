package com.kuroyale.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.kuroyale.model.Card;
import com.kuroyale.model.Deck;
import com.kuroyale.model.User;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.CardCatalog;
import com.kuroyale.service.DeckManagementService;
import com.kuroyale.util.ButtonFactory;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.SoundEffectUtil;
import com.kuroyale.util.StyleHelper;
import com.kuroyale.view.CardInfoDialog;
import com.kuroyale.view.CardView;
import com.kuroyale.view.DeckSlotView;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

/**
 * Controller for the deck builder screen
 * Follows Controller GRASP pattern - thin controller that delegates to services
 * Follows Low Coupling - uses services via dependency injection
 * Follows High Cohesion - focused on UI presentation and user interactions
 */
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

    // Service dependencies (injected via ServiceFactory)
    private AuthenticationService authService;
    private DeckManagementService deckService;
    private CardCatalog cardCatalog;

    private Deck deck;
    private List<DeckSlotView> deckSlots;
    private CardView selectedCardView;
    private HBox cardButtonsBox;
    private Map<Card, VBox> cardContainerMap; // Track card containers for hiding/showing
    private DeckSlotView selectedDeckSlot;
    private HBox deckSlotButtonsBox;
    private boolean replaceMode; // Track if we're in replace mode
    private Card cardToReplace; // Card selected for replacement

    @FXML
    private void initialize() {
        // Get services from factory (dependency injection)
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.deckService = factory.getDeckManagementService();
        this.cardCatalog = factory.getCardCatalog();

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
    }

    /**
     * Apply styles to deck builder components
     */
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

    /**
     * Creates 8 recessed rectangles that serve as visual backgrounds for deck slots
     */
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
        // Delegate to CardCatalog service (Indirection pattern)
        List<Card> allCards = cardCatalog.getAllCards();

        // Store all cards for later reorganization
        for (Card card : allCards) {
            // Create container for card + buttons
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

    /**
     * Reorganizes the bottom card grid so visible cards fill rows of 4
     * Cards are always shown in the same consistent order (from CardCatalog)
     * Only cards NOT in the deck are displayed
     * UI concern - appropriate for controller
     */
    private void reorganizeCardGrid() {
        // Clear the grid
        cardsGrid.getChildren().clear();

        // Get all cards in consistent order: Troops, Buildings, Spells
        // Delegate to CardCatalog (Indirection pattern)
        List<Card> allCards = cardCatalog.getAllCards();
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

    /**
     * Gets the main AnchorPane from the root pane
     */
    private AnchorPane getMainAnchorPane() {
        return (AnchorPane) rootPane.getChildren().get(0);
    }

    /**
     * Removes deck slot buttons and resets selection state
     */
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
            return; // Do nothing if slot is empty
        }

        // If this slot is already selected, deselect it
        if (selectedDeckSlot == slot && deckSlotButtonsBox != null) {
            removeDeckSlotButtons();
            removeCardButtons(); // Also remove card buttons
            return;
        }

        // Remove buttons from previously selected slot AND card
        removeDeckSlotButtons();
        removeCardButtons(); // Ensure card buttons are also removed

        selectedDeckSlot = slot;
        Card card = slot.getCard();
        showDeckSlotButtons(slot, card);
    }

    /**
     * Handles deck slot click when in replace mode
     * The clicked slot's card will be swapped with the card selected from bottom
     * grid
     * The new card takes the exact position of the old card
     */
    private void handleReplaceModeClick(DeckSlotView slot) {
        if (!slot.isEmpty() && cardToReplace != null) {
            Card oldCard = slot.getCard(); // Card currently in the clicked slot
            Card newCard = cardToReplace; // Card selected from bottom to replace with
            replaceCardInDeck(oldCard, newCard);
            exitReplaceMode();
        } else if (slot.isEmpty()) {
            // Cancel replace mode if clicking empty slot
            exitReplaceMode();
        }
    }

    /**
     * Shows action buttons for a deck slot
     */
    private void showDeckSlotButtons(DeckSlotView slot, Card card) {
        HBox buttonsBox = createDeckSlotButtons(card);
        positionButtons(buttonsBox, slot, 0, -10); // 3 pixels below the card
        deckSlotButtonsBox = buttonsBox;
    }

    /**
     *
     * Creates action buttons for a deck slot
     */
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
            removeCardButtons(); // This stops the ghost buttons from the library
            removeCardFromDeck(card);
        });

        buttonsBox.getChildren().addAll(infoButton, removeButton);
        return buttonsBox;
    }

    /**
     * Removes card buttons from the overlay
     */
    private void removeCardButtons() {
        if (cardButtonsBox != null) {
            // Remove from AnchorPane overlay
            AnchorPane mainPane = getMainAnchorPane();
            mainPane.getChildren().remove(cardButtonsBox);
            cardButtonsBox = null;
            selectedCardView = null;
        }
    }

    /**
     * Helper method to position buttons below a node (card or deck slot)
     * 
     * @param buttonsBox The HBox containing the buttons
     * @param node       The node (CardView or DeckSlotView) to position buttons
     *                   below
     * @param xOffset    Additional X offset to center buttons (0 for deck slots, 0
     *                   for cards)
     * @param yOffset    Vertical offset from bottom of node
     */
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
            removeDeckSlotButtons(); // Also remove deck slot buttons
            return;
        }

        // Remove buttons from previously selected card AND deck slot
        removeCardButtons();
        removeDeckSlotButtons(); // Ensure deck slot buttons are also removed

        selectedCardView = cardView;
        showCardButtons(card, cardContainer);
    }

    /**
     * Shows action buttons for a card (overlaid, not affecting layout)
     */
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

    /**
     * Creates the appropriate action button (USE/REMOVE/REPLACE) for a card
     */
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
        CardInfoDialog infoDialog = new CardInfoDialog(card, () -> {
            rootPane.getChildren().remove(rootPane.getChildren().size() - 1);
        });

        rootPane.getChildren().add(infoDialog);
    }

    private void addCardToDeck(Card card) {
        if (deck.isFull()) {
            System.out.println("Deck is full!");
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
        }
    }

    /**
     * Enters replace mode - highlights deck slots and waits for user to select a
     * slot
     */
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

    /**
     * Exits replace mode - removes highlights
     */
    private void exitReplaceMode() {
        this.replaceMode = false;
        this.cardToReplace = null;

        // Remove highlights from all deck slots
        for (DeckSlotView slot : deckSlots) {
            slot.removeHighlight();
        }
    }

    /**
     * Replaces a card in the deck with a new card
     * The new card takes the exact position of the old card in the deck
     */
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
            // Delegate to service (Controller pattern)
            deckService.replaceCardInDeck(deck, oldCard, newCard);

            // Replace the card in the UI at the SAME position (UI concern)
            targetSlot.setCard(newCard);

            // Reorganize bottom grid to reflect changes
            // (oldCard will now appear in bottom, newCard will be hidden)
            reorganizeCardGrid();

            // Update button states
            updateCardButtons();

            // Update average elixir cost
            updateAverageElixirCost();

            // Auto-save deck
            saveDeck();
        }
    }

    /**
     * Updates button states for cards (USE vs REPLACE)
     * Removes current buttons to force refresh on next interaction
     */
    private void updateCardButtons() {
        // Simply remove current buttons - they'll be recreated with correct state on
        // next click
        removeCardButtons();
    }

    /**
     * Reorganizes deck slots so that all cards move up to fill empty slots
     */
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

    /**
     * Centers the average elixir cost display within the brown background area
     */
    private void centerAverageElixirInBrownArea() {
        if (averageElixirContainer != null && deckSlotsBackground != null) {
            // Use Platform.runLater to ensure layout is complete
            javafx.application.Platform.runLater(() -> {
                averageElixirContainer.applyCss();
                averageElixirContainer.layout();
                double containerWidth = averageElixirContainer.getWidth();
                if (containerWidth == 0) {
                    // Fallback: calculate preferred width
                    containerWidth = averageElixirContainer.prefWidth(-1);
                }
                // Center horizontally within 620px brown area
                double centerX = (620.0 - containerWidth) / 2;
                AnchorPane.setLeftAnchor(averageElixirContainer, centerX);
            });
        }
    }

    /**
     * Updates the average elixir cost display
     * UI concern - appropriate for controller
     */
    private void updateAverageElixirCost() {
        if (averageElixirValue != null) {
            // Delegate calculation to service (Information Expert)
            double avgCost = deckService.getDeckAverageElixirCost(deck);
            averageElixirValue.setText(String.format(Locale.ENGLISH, "%.1f", avgCost));
        }
    }

    /**
     * Loads the user's saved deck from their account
     * Preserves the exact slot positions of cards
     * Delegates to service (Controller pattern)
     */
    private void loadUserDeck() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return; // No user logged in
        }

        // Delegate to service (Controller pattern)
        Map<Integer, Card> deckMap = deckService.loadUserDeckWithPositions(currentUser);

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

    /**
     * Saves the current deck to the user's account
     * Saves cards in their exact slot positions (left to right, top to bottom)
     * Empty slots are saved as empty strings to preserve positions
     * Delegates to service (Controller pattern)
     */
    private void saveDeck() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return; // No user logged in
        }

        // Build map of slot positions to cards (UI data)
        Map<Integer, Card> slotCards = new HashMap<>();
        for (int i = 0; i < deckSlots.size(); i++) {
            DeckSlotView slot = deckSlots.get(i);
            if (!slot.isEmpty()) {
                slotCards.put(i, slot.getCard());
            }
        }

        // Delegate to service (Controller pattern)
        try {
            deckService.saveDeckWithPositions(currentUser, slotCards);
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

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            // Load stylesheet for new scene
            scene.getStylesheets().add(getClass().getResource("/styles/application.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("KU Royale");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
