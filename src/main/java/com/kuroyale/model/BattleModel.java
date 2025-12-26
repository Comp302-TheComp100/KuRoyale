package com.kuroyale.model;

import java.util.List;

import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.GameSaveService;
import com.kuroyale.util.ServiceFactory;

/*The Model component for the Battle screen.
 * Encapsulates business logic for game initialization, save/load operations.*/
public class BattleModel {
    
    private final AuthenticationService authService;
    private final ArenaService arenaService;
    private final GameSaveService gameSaveService;
    private final com.kuroyale.service.CardCatalog cardCatalog;
    
    public BattleModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.arenaService = factory.getArenaService();
        this.gameSaveService = factory.getGameSaveService();
        this.cardCatalog = factory.getCardCatalog();
    }
    
    //Gets the current logged-in user
    public User getCurrentUser() {
        return authService.getCurrentUser();
    }
    
    //Sets the current user in arena service to load their saved layout
    public void setCurrentUserInArenaService(User user) {
        arenaService.setCurrentUser(user);
    }
    
    //Loads the arena layout for the current user
    public ArenaLayout loadArenaLayout() {
        return arenaService.loadArenaLayout();
    }
    
    //Creates an arena from the given layout
    public Arena createArena(ArenaLayout layout) {
        return arenaService.createArena(layout);
    }
    
    //Creates a deck from a list of card names
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
    
    //Creates a bot deck (currently uses player's deck)
    public Deck createBotDeck(User playerUser) {
        return createDeckFromNames(playerUser.getDeck());
    }
    
    //Saves the current game state
    public SavedGameState saveGame(GameState gameState, User user, ArenaLayout layout) {
        return gameSaveService.saveGame(gameState, user, layout);
    }
    
    public void saveCurrentUser() throws java.io.IOException {
        authService.saveCurrentUser();
    }
    
    //Gets a card by name from the catalog
    public Card getCardByName(String cardName) {
        return cardCatalog.getCardByName(cardName);
    }
}

