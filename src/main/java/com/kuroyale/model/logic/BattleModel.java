package com.kuroyale.model.logic;

import com.kuroyale.model.dto.*;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;

import java.util.List;

import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.service.game.ChallengeService;
import com.kuroyale.service.game.GameSaveService;
import com.kuroyale.service.management.ArenaManagementService;
import com.kuroyale.util.common.ServiceFactory;

/*The Model component for the Battle screen.
 * Encapsulates business logic for game initialization, save/load operations.*/
public class BattleModel {

    private static final int VICTORY_GOLD = 150;
    private static final int DRAW_GOLD = 75;
    private static final int DEFEAT_GOLD = 50;

    private final AuthenticationService authService;
    private final ArenaManagementService arenaService;
    private final GameSaveService gameSaveService;
    private final ChallengeService challengeService;
    private final com.kuroyale.model.entities.CardCatalog cardCatalog;

    public BattleModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.arenaService = factory.getArenaService();
        this.gameSaveService = factory.getGameSaveService();
        this.challengeService = factory.getChallengeService();
        this.cardCatalog = factory.getCardCatalog();
    }

    // Gets the current logged-in user
    public User getCurrentUser() {
        return authService.getCurrentUser();
    }

    // Sets the current user in arena service to load their saved layout
    public void setCurrentUserInArenaService(User user) {
        arenaService.setCurrentUser(user);
    }

    // Loads the arena layout for the current user
    public ArenaLayout loadArenaLayout() {
        return arenaService.loadArenaLayout();
    }

    // Creates an arena from the given layout
    public Arena createArena(ArenaLayout layout) {
        return arenaService.createArena(layout);
    }

    // Creates a deck from a list of card names
    public Deck createDeckFromNames(List<String> cardNames) {
        Deck deck = new Deck();
        if (cardNames != null) {
            User currentUser = authService.getCurrentUser();
            for (String name : cardNames) {
                int level = 1;
                if (currentUser != null) {
                    level = currentUser.getCardLevel(name);
                }
                Card card = cardCatalog.createCardWithLevel(name, level);
                if (card != null) {
                    deck.addCard(card);
                }
            }
        }
        return deck;
    }

    // Creates a list of cards from a list of card names (for restoring hand/draw
    // pile)
    private List<Card> createCardsFromNames(List<String> cardNames) {
        List<Card> cards = new java.util.ArrayList<>();
        if (cardNames != null) {
            User currentUser = authService.getCurrentUser();
            for (String name : cardNames) {
                int level = 1;
                if (currentUser != null) {
                    level = currentUser.getCardLevel(name);
                }
                Card card = cardCatalog.createCardWithLevel(name, level);
                if (card != null) {
                    cards.add(card);
                }
            }
        }
        return cards;
    }

    // Creates a bot deck (currently uses player's deck)
    public Deck createBotDeck(User playerUser) {
        return createDeckFromNames(playerUser.getDeck());
    }

    // Restores a full game state from a saved game object
    public GameState loadGame(SavedGameState savedGame) {
        // Create decks from saved card names
        Deck playerDeck = createDeckFromNames(savedGame.getPlayerDeckCards());
        Deck botDeck = createDeckFromNames(savedGame.getBotDeckCards());

        // Load arena layout from saved game
        ArenaLayout layout = savedGame.getArenaLayout();
        Arena arena = createArena(layout);

        // Create game state
        GameState gameState = new GameState(playerDeck, botDeck, arena);
        gameState.setCardCatalog(name -> getCardByName(name));

        // Restore saved state (time, elixir, scores)
        gameState.restoreFromSaved(
                savedGame.getGameTime(),
                savedGame.isDoubleElixir(),
                savedGame.getPlayerScore(),
                savedGame.getBotScore(),
                savedGame.getPlayerElixir(),
                savedGame.getBotElixir());

        // Restore player's hand and draw pile from saved data
        List<Card> restoredHandCards = createCardsFromNames(savedGame.getPlayerHandCards());
        List<Card> restoredDrawPileCards = createCardsFromNames(savedGame.getPlayerDrawPileCards());
        gameState.restorePlayerHand(restoredHandCards, restoredDrawPileCards);

        // Restore tower health
        for (SavedGameState.SavedTower savedTower : savedGame.getTowers()) {
            gameState.restoreTowerHealth(savedTower);
        }

        // Restore active troops
        for (SavedGameState.SavedTroop savedTroop : savedGame.getActiveTroops()) {
            Card card = getCardByName(savedTroop.getCardName());
            if (card != null) {
                GridPosition pos = GridPosition.tryCreate(savedTroop.getGridX(), savedTroop.getGridY());
                if (pos != null) {
                    Troop troop = new Troop(card, pos, savedTroop.isPlayerSide());
                    // Set health to saved value
                    int healthLoss = card.getHp() - savedTroop.getCurrentHealth();
                    if (healthLoss > 0) {
                        troop.takeDamage(healthLoss);
                    }
                    // Set state
                    try {
                        troop.setUnitState(UnitState.valueOf(savedTroop.getState()));
                    } catch (IllegalArgumentException e) {
                        troop.setUnitState(UnitState.IDLE);
                    }
                    gameState.getActiveTroops().add(troop);
                }
            }
        }

        // Restore active buildings
        for (SavedGameState.SavedBuilding savedBuilding : savedGame.getActiveBuildings()) {
            Card card = getCardByName(savedBuilding.getCardName());
            if (card != null) {
                GridPosition pos = GridPosition.tryCreate(savedBuilding.getGridX(), savedBuilding.getGridY());
                if (pos != null) {
                    Building building = new Building(
                            pos,
                            savedBuilding.getWidth(),
                            savedBuilding.getHeight(),
                            savedBuilding.isPlayerSide(),
                            card.getHp(),
                            card.getImagePath(),
                            card.getLifetime());
                    building.configureCombatFromCard(card);

                    // Set health to saved value
                    int healthLoss = card.getHp() - savedBuilding.getCurrentHealth();
                    if (healthLoss > 0) {
                        building.takeDamage(healthLoss);
                    }

                    // Occupy footprint
                    for (int dx = 0; dx < building.getWidth(); dx++) {
                        for (int dy = 0; dy < building.getHeight(); dy++) {
                            int gx = pos.getX() + dx;
                            int gy = pos.getY() + dy;
                            GridPosition cellPos = GridPosition.tryCreate(gx, gy);
                            if (cellPos != null) {
                                GridCell cell = arena.getCell(cellPos);
                                if (cell != null) {
                                    try {
                                        cell.setOccupant(building);
                                    } catch (IllegalStateException e) {
                                        // Ignore if invalid
                                    }
                                }
                            }
                        }
                    }
                    gameState.getActiveBuildings().add(building);
                }
            }
        }

        return gameState;
    }

    // Saves the current game state
    public SavedGameState saveGame(GameState gameState, User user, ArenaLayout layout, int comboCount) {
        return gameSaveService.saveGame(gameState, user, layout, comboCount);
    }

    public void saveCurrentUser() throws java.io.IOException {
        authService.saveCurrentUser();
    }

    public void processMatchResult(int playerScore, int botScore, int comboBonus, boolean playerWon, boolean isDraw)
            throws java.io.IOException {
        int bonus = 0;

        if (isDraw) {
            // True draw (equal scores AND equal lowest tower HP)
            bonus = DRAW_GOLD;
        } else if (playerWon) {
            // Player won (by score or tiebreaker)
            bonus = VICTORY_GOLD;
        } else {
            // Player lost (by score or tiebreaker)
            bonus = DEFEAT_GOLD;
        }

        // Add Combo Bonus (10 gold per unique combo)
        int comboGold = comboBonus * 10;
        bonus += comboGold;

        if (bonus > 0) {
            authService.awardGoldToCurrentUser(bonus);

            // Track GOLD_HOARDER achievement
            ServiceFactory.getInstance().getAchievementService()
                    .updateProgress(com.kuroyale.model.enums.AchievementType.GOLD_HOARDER, bonus);
        }
    }

    public void recordChallengeAttempt(int challengeId, boolean won, int timeSeconds, int damageTaken) {
        challengeService.recordAttempt(challengeId, won, timeSeconds, damageTaken);
    }

    // Gets a card by name from the catalog
    public Card getCardByName(String cardName) {
        return cardCatalog.getCardByName(cardName);
    }
}
