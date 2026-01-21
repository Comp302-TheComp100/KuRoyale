package com.kuroyale.util.common;

import com.kuroyale.model.entities.CardCatalog;
import com.kuroyale.repository.JsonUserRepository;
import com.kuroyale.repository.UserRepository;
import com.kuroyale.service.auth.AuthenticationService;
import com.kuroyale.service.game.AchievementService;
import com.kuroyale.service.game.ChallengeService;
import com.kuroyale.service.game.GameSaveService;
import com.kuroyale.service.game.QuestService;
import com.kuroyale.service.game.PlayerStatsService;
import com.kuroyale.service.management.ArenaManagementService;
import com.kuroyale.service.management.DeckManagementService;

/**
 * Central Dependency Injection container implementing the **Singleton** and
 * **Factory** patterns.
 * <p>
 * <b>GRASP Pattern: Pure Fabrication</b><br>
 * This class does not represent a domain concept but was created to manage
 * object creation and dependencies,
 * promoting **Low Coupling** by centralizing dependency management.
 * </p>
 * <p>
 * <b>GRASP Pattern: Creator</b><br>
 * It creates and manages the lifecycle of all service instances, ensuring they
 * are initialized with
 * their required dependencies.
 * </p>
 */
public class ServiceFactory {

    private static ServiceFactory instance;

    // Service instances
    private final UserRepository userRepository;
    private final CardCatalog cardCatalog;
    private final AuthenticationService authenticationService;
    private final DeckManagementService deckManagementService;
    private final ArenaManagementService arenaService;
    private final GameSaveService gameSaveService;
    private ChallengeService challengeService; // Lazy initialized
    private QuestService questService; // Lazy initialized
    private AchievementService achievementService; // Lazy initialized
    private PlayerStatsService playerStatsService; // Lazy initialized

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
        this.authenticationService = new AuthenticationService(userRepository, cardCatalog);
        this.deckManagementService = new DeckManagementService(userRepository, cardCatalog);
        this.arenaService = new ArenaManagementService(userRepository);
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
        ServiceFactory factory = getInstance();
        // Force initialization of lazy services to ensure event listeners are
        // registered
        factory.getAchievementService();
        factory.getQuestService();
        factory.getPlayerStatsService();
        factory.getChallengeService();
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
    public ArenaManagementService getArenaService() {
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

    // Gets the QuestService instance
    public QuestService getQuestService() {
        if (questService == null) {
            questService = new QuestService();
        }
        return questService;
    }

    // Gets the AchievementService instance
    public AchievementService getAchievementService() {
        if (achievementService == null) {
            achievementService = new AchievementService();
        }
        return achievementService;
    }

    // Gets the PlayerStatsService instance
    public PlayerStatsService getPlayerStatsService() {
        if (playerStatsService == null) {
            playerStatsService = new PlayerStatsService();
        }
        return playerStatsService;
    }

    // Resets the singleton instance
    static void reset() {
        instance = null;
    }
}
