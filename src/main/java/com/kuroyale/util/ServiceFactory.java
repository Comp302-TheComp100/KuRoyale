package com.kuroyale.util;

import com.kuroyale.repository.JsonUserRepository;
import com.kuroyale.repository.UserRepository;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.CardCatalog;
import com.kuroyale.service.DeckManagementService;
import com.kuroyale.service.ArenaService;
import com.kuroyale.service.GameSaveService;
import com.kuroyale.service.ChallengeService;

/*Service Factory for managing service instances and dependencies
 * Pure Fabrication - created to manage object creation and dependencies
 * Low Coupling - centralizes dependency management*/
public class ServiceFactory {

    private static ServiceFactory instance;

    // Service instances
    private final UserRepository userRepository;
    private final CardCatalog cardCatalog;
    private final AuthenticationService authenticationService;
    private final DeckManagementService deckManagementService;
    private final ArenaService arenaService;
    private final GameSaveService gameSaveService;
    private ChallengeService challengeService; // Lazy initialized

    /*
     * Private constructor to enforce singleton pattern
     * Creator pattern - ServiceFactory has initialization data for services
     */
    private ServiceFactory() {
        // Create repository
        this.userRepository = new JsonUserRepository();

        // Create card catalog
        this.cardCatalog = new CardCatalog();

        // Create services with dependencies
        this.authenticationService = new AuthenticationService(userRepository);
        this.deckManagementService = new DeckManagementService(userRepository, cardCatalog);
        this.arenaService = new ArenaService(userRepository);
        this.gameSaveService = new GameSaveService();
    }

    // Gets the singleton instance of ServiceFactory
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }

    // Initializes the ServiceFactory
    public static void initialize() {
        getInstance();
    }

    // Gets the UserRepository instance
    public UserRepository getUserRepository() {
        return userRepository;
    }

    // Gets the CardCatalog instance
    public CardCatalog getCardCatalog() {
        return cardCatalog;
    }

    // Gets the AuthenticationService instance
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    // Gets the DeckManagementService instance
    public DeckManagementService getDeckManagementService() {
        return deckManagementService;
    }

    // Gets the ArenaService instance
    public ArenaService getArenaService() {
        return arenaService;
    }

    // Gets the GameSaveService instance
    public GameSaveService getGameSaveService() {
        return gameSaveService;
    }

    // Gets the ChallengeService instance
    public ChallengeService getChallengeService() {
        if (challengeService == null) {
            challengeService = new ChallengeService();
        }
        return challengeService;
    }

    // Resets the singleton instance
    static void reset() {
        instance = null;
    }
}
