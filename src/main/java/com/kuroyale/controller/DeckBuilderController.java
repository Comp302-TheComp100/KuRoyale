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
    private static final int BUTTONS_WIDTH = 121; // Two buttons + gap (58 + 5 + 58)
    private static final int BUTTON_OFFSET_Y = 5; // Pixels below card
    private static final int CARD_CONTAINER_SPACING = 5;

    @FXML
    private AnchorPane deckSlotsBackground;

    @FXML
    private AnchorPane deckSlotsContainer;

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
    private VBox currentCardContainer; // Track the current card container for button repositioning

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
            updateCardButtonPositions();
        });
    }

    /**
     * Apply styles to deck builder components
     */
    private void applyStyles() {
        // Apply background to main anchor pane
        AnchorPane mainBackground = getMainAnchorPane();
        if (mainBackground != null) {
            StyleHelper.applyDeckBuilderBackground(mainBackground);
        }

        // Apply deck slots background style
        StyleHelper.applyDeckSlotsBackground(deckSlotsBackground);

        // Apply title style
        if (battleDeckTitle != null) {
            StyleHelper.applyBattleDeckTitleStyle(battleDeckTitle);
        }

        // Apply back button style
        StyleHelper.applyBackButtonStyle(backButton);

        // Apply scroll pane style (deferred to ensure viewport is available)
        javafx.application.Platform.runLater(() -> {
            StyleHelper.applyScrollPaneStyle(cardsScrollPane);
        });

        // Apply cards grid style
        StyleHelper.applyCardsGridStyle(cardsGrid);

        // Apply average elixir label styles
        if (averageElixirContainer != null) {
            for (javafx.scene.Node node : averageElixirContainer.getChildren()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    if (label.getText().startsWith("Average")) {
                        StyleHelper.applyAverageElixirLabelStyle(label);
                    } else if (label == averageElixirValue) {
                        StyleHelper.applyAverageElixirValueStyle(label);
                    }
                }
            }
        }
    }

    /**
     * Creates 8 recessed rectangles that serve as visual backgrounds for deck slots
     */
    private void createRecessedRectangles() {
        // Slot dimensions (must match createDeckSlots exactly)
        final int SLOT_WIDTH = 90;
        final int SLOT_HEIGHT = 120;
        final int SLOT_GAP_X = 20;
        final int SLOT_GAP_Y = 20;
        final int START_X = 0;
        final int SLOT_OFFSET_Y = 20; // Deck slots are 20px lower than brown background

        // Calculate center offset (must match createDeckSlots exactly)
        final int TOTAL_WIDTH = (DECK_SLOT_COLS * SLOT_WIDTH) + ((DECK_SLOT_COLS - 1) * SLOT_GAP_X);
        final int TOTAL_HEIGHT = (DECK_SLOT_ROWS * SLOT_HEIGHT) + ((DECK_SLOT_ROWS - 1) * SLOT_GAP_Y);
        final int OFFSET_X = (660 - TOTAL_WIDTH) / 2;
        final int OFFSET_Y = (280 - TOTAL_HEIGHT) / 2; // Use 280px to match deck slots container height

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

                // Calculate position (must match createDeckSlots exactly, with 20px offset)
                double x = START_X + OFFSET_X + (col * (SLOT_WIDTH + SLOT_GAP_X));
                double y = SLOT_OFFSET_Y + OFFSET_Y + (row * (SLOT_HEIGHT + SLOT_GAP_Y));

                // Add to background AnchorPane
                deckSlotsBackground.getChildren().add(recess);
                AnchorPane.setLeftAnchor(recess, x);
                AnchorPane.setTopAnchor(recess, y);
            }
        }
    }

    private void createDeckSlots() {
        // Slot dimensions
        final int SLOT_WIDTH = 90;
        final int SLOT_HEIGHT = 120;
        final int SLOT_GAP_X = 20; // Horizontal gap between slots
        final int SLOT_GAP_Y = 20; // Vertical gap between slots
        final int START_X = 0; // Starting X position within container
        final int START_Y = 0; // Starting Y position within container

        // Calculate center offset to center the 4x2 grid
        // Note: Container height is 280px (adjusted for smaller cards)
        final int TOTAL_WIDTH = (DECK_SLOT_COLS * SLOT_WIDTH) + ((DECK_SLOT_COLS - 1) * SLOT_GAP_X);
        final int TOTAL_HEIGHT = (DECK_SLOT_ROWS * SLOT_HEIGHT) + ((DECK_SLOT_ROWS - 1) * SLOT_GAP_Y);
        final int OFFSET_X = (660 - TOTAL_WIDTH) / 2; // Center horizontally in 660px container
        final int OFFSET_Y = (280 - TOTAL_HEIGHT) / 2; // Center vertically in 280px container

        for (int row = 0; row < DECK_SLOT_ROWS; row++) {
            for (int col = 0; col < DECK_SLOT_COLS; col++) {
                DeckSlotView slot = new DeckSlotView();
                slot.setOnMouseClicked(event -> handleDeckSlotClick(slot));
                deckSlots.add(slot);

                // Calculate position for this slot
                double x = START_X + OFFSET_X + (col * (SLOT_WIDTH + SLOT_GAP_X));
                double y = START_Y + OFFSET_Y + (row * (SLOT_HEIGHT + SLOT_GAP_Y));

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
        // If in replace mode, handle replacement or cancellation
        if (replaceMode) {
            handleReplaceModeClick(slot);
            return;
        }

        if (slot.isEmpty()) {
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
        AnchorPane mainPane = getMainAnchorPane();

        // Calculate button position
        Bounds slotBoundsInScene = slot.localToScene(slot.getBoundsInLocal());
        Point2D slotPointInAnchorPane = mainPane.sceneToLocal(
                slotBoundsInScene.getMinX(),
                slotBoundsInScene.getMinY());

        double cardWidth = slot.getWidth();
        double cardHeight = slot.getHeight();
        double buttonX = slotPointInAnchorPane.getX() + (cardWidth / 2) - (BUTTONS_WIDTH / 2);
        double buttonY = slotPointInAnchorPane.getY() + cardHeight + BUTTON_OFFSET_Y;

        // Add buttons to AnchorPane
        mainPane.getChildren().add(buttonsBox);
        AnchorPane.setLeftAnchor(buttonsBox, buttonX);
        AnchorPane.setTopAnchor(buttonsBox, buttonY);

        deckSlotButtonsBox = buttonsBox;
    }

    /**
     * Creates action buttons for a deck slot
     */
    private HBox createDeckSlotButtons(Card card) {
        HBox buttonsBox = new HBox(BUTTON_GAP);
        buttonsBox.setAlignment(Pos.CENTER);

        Button infoButton = ButtonFactory.createInfoButton();
        infoButton.setOnAction(e -> showCardInfo(card));

        Button removeButton = ButtonFactory.createRemoveButton();
        removeButton.setOnAction(e -> {
            removeCardFromDeck(card);
            removeDeckSlotButtons();
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
            currentCardContainer = null;
        }
    }

    /**
     * Updates card button positions when scrolling occurs
     */
    private void updateCardButtonPositions() {
        if (cardButtonsBox != null && selectedCardView != null && currentCardContainer != null) {
            AnchorPane mainPane = getMainAnchorPane();

            // Recalculate button position relative to card
            Bounds cardBoundsInScene = selectedCardView.localToScene(selectedCardView.getBoundsInLocal());
            Point2D cardPointInAnchorPane = mainPane.sceneToLocal(
                    cardBoundsInScene.getMinX(),
                    cardBoundsInScene.getMinY());

            double cardWidth = selectedCardView.getWidth();
            double cardHeight = selectedCardView.getHeight();
            double buttonX = cardPointInAnchorPane.getX() + (cardWidth / 2) - (BUTTONS_WIDTH / 2) + 18;
            double buttonY = cardPointInAnchorPane.getY() + cardHeight + BUTTON_OFFSET_Y;

            // Update button position
            AnchorPane.setLeftAnchor(cardButtonsBox, buttonX);
            AnchorPane.setTopAnchor(cardButtonsBox, buttonY);
        }
    }

    private void handleCardClick(CardView cardView, VBox cardContainer) {
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
        infoButton.setOnAction(e -> showCardInfo(card));

        Button actionButton = createCardActionButton(card, cardContainer);
        buttonsBox.getChildren().addAll(infoButton, actionButton);

        // Position buttons as overlay on AnchorPane instead of in container
        AnchorPane mainPane = getMainAnchorPane();

        // Get the CardView from the container (first child)
        CardView cardView = (CardView) cardContainer.getChildren().get(0);

        // Calculate button position relative to card
        Bounds cardBoundsInScene = cardView.localToScene(cardView.getBoundsInLocal());
        Point2D cardPointInAnchorPane = mainPane.sceneToLocal(
                cardBoundsInScene.getMinX(),
                cardBoundsInScene.getMinY());

        double cardWidth = cardView.getWidth();
        double cardHeight = cardView.getHeight();
        double buttonX = cardPointInAnchorPane.getX() + (cardWidth / 2) - (BUTTONS_WIDTH / 2) + 18;
        double buttonY = cardPointInAnchorPane.getY() + cardHeight + BUTTON_OFFSET_Y;

        // Add buttons to AnchorPane as overlay
        mainPane.getChildren().add(buttonsBox);
        AnchorPane.setLeftAnchor(buttonsBox, buttonX);
        AnchorPane.setTopAnchor(buttonsBox, buttonY);

        cardButtonsBox = buttonsBox;
        currentCardContainer = cardContainer;
    }

    /**
     * Creates the appropriate action button (USE/REMOVE/REPLACE) for a card
     */
    private Button createCardActionButton(Card card, VBox cardContainer) {
        if (deck.contains(card)) {
            return createRemoveButton(card, cardContainer);
        } else if (deck.isFull()) {
            return createReplaceButton(card, cardContainer);
        } else {
            return createUseButton(card, cardContainer);
        }
    }

    /**
     * Creates a REMOVE button for a card already in deck
     */
    private Button createRemoveButton(Card card, VBox cardContainer) {
        Button removeButton = ButtonFactory.createRemoveButton();
        removeButton.setOnAction(e -> {
            removeCardFromDeck(card);
            removeCardButtons();
        });
        return removeButton;
    }

    /**
     * Creates a REPLACE button when deck is full
     */
    private Button createReplaceButton(Card card, VBox cardContainer) {
        Button replaceButton = ButtonFactory.createReplaceButton();
        replaceButton.setOnAction(e -> {
            enterReplaceMode(card);
            removeCardButtons();
        });
        return replaceButton;
    }

    /**
     * Creates a USE button to add card to deck
     */
    private Button createUseButton(Card card, VBox cardContainer) {
        Button useButton = ButtonFactory.createUseButton();
        useButton.setOnAction(e -> {
            addCardToDeck(card);
            removeCardButtons();
        });
        return useButton;
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
     */
    private void updateCardButtons() {
        // If a card has buttons showing, update them
        if (selectedCardView != null && cardButtonsBox != null) {
            CardView cardViewToUpdate = selectedCardView;
            VBox cardContainer = (VBox) cardViewToUpdate.getParent();

            // Remove old buttons
            cardContainer.getChildren().remove(cardButtonsBox);
            cardButtonsBox = null;
            selectedCardView = null;

            // Re-trigger click to show updated buttons
            handleCardClick(cardViewToUpdate, cardContainer);
        }
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
                // Center horizontally within 800px brown area
                double centerX = (800.0 - containerWidth) / 2;
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
        // Exit replace mode if active
        if (replaceMode) {
            exitReplaceMode();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-menu.fxml"));
            Parent root = loader.load();

            // Get controller and initialize styles
            MainMenuController controller = loader.getController();
            controller.initializeStyles();

            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, 1280, 720);
            stage.setScene(scene);
            stage.setTitle("KU Royale");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
