package com.kuroyale.util;

import com.kuroyale.repository.JsonUserRepository;
import com.kuroyale.repository.UserRepository;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.service.CardCatalog;
import com.kuroyale.service.DeckManagementService;

/**
 * Service Factory for managing service instances and dependencies
 * Follows Pure Fabrication GRASP pattern - created to manage object creation and dependencies
 * Follows Low Coupling - centralizes dependency management
 * Implements Service Locator pattern for simple dependency injection
 */
public class ServiceFactory {
    
    private static ServiceFactory instance;
    
    // Service instances
    private final UserRepository userRepository;
    private final CardCatalog cardCatalog;
    private final AuthenticationService authenticationService;
    private final DeckManagementService deckManagementService;
    
    /**
     * Private constructor to enforce singleton pattern
     * Follows Creator pattern - ServiceFactory has initialization data for services
     */
    private ServiceFactory() {
        // Create repository
        this.userRepository = new JsonUserRepository();
        
        // Create card catalog
        this.cardCatalog = new CardCatalog();
        
        // Create services with dependencies
        this.authenticationService = new AuthenticationService(userRepository);
        this.deckManagementService = new DeckManagementService(userRepository, cardCatalog);
    }
    
    /**
     * Gets the singleton instance of ServiceFactory
     * @return The ServiceFactory instance
     */
    public static synchronized ServiceFactory getInstance() {
        if (instance == null) {
            instance = new ServiceFactory();
        }
        return instance;
    }
    
    /**
     * Initializes the ServiceFactory
     * Should be called once at application startup
     */
    public static void initialize() {
        getInstance();
    }
    
    /**
     * Gets the UserRepository instance
     * @return UserRepository
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }
    
    /**
     * Gets the CardCatalog instance
     * @return CardCatalog
     */
    public CardCatalog getCardCatalog() {
        return cardCatalog;
    }
    
    /**
     * Gets the AuthenticationService instance
     * @return AuthenticationService
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
    
    /**
     * Gets the DeckManagementService instance
     * @return DeckManagementService
     */
    public DeckManagementService getDeckManagementService() {
        return deckManagementService;
    }
    
    /**
     * Resets the singleton instance (useful for testing)
     * Package-private for testing purposes
     */
    static void reset() {
        instance = null;
    }
}



