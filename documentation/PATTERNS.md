# Design Patterns in KuRoyale

This document provides comprehensive documentation of the Gang of Four (GoF) and GRASP design patterns implemented throughout the KuRoyale project. Each pattern is explained with its purpose, implementation locations, and code examples.

---

## Table of Contents

1. [Architectural Patterns](#architectural-patterns)
   - [Model-View-Controller (MVC)](#model-view-controller-mvc)
2. [Creational Patterns](#creational-patterns)
   - [Singleton](#singleton)
   - [Factory Method](#factory-method--simple-factory)
   - [Abstract Factory](#abstract-factory)
3. [Behavioral Patterns](#behavioral-patterns)
   - [Strategy](#strategy)
   - [Observer](#observer)
   - [Template Method](#template-method)
   - [State](#state)
   - [Command](#command)
4. [Structural Patterns](#structural-patterns)
   - [Facade](#facade)
   - [Flyweight](#flyweight)
   - [Composite](#composite)
   - [Adapter](#adapter)
5. [Data Patterns](#data-patterns)
   - [Repository](#repository)
6. [Cross-Reference: GRASP to GoF](#cross-reference-grasp-to-gof)

---

## Architectural Patterns

### Model-View-Controller (MVC)

**Intent:** Separates the application into three interconnected components to decouple internal business logic from UI presentation.

**Problem Solved:** Without MVC, UI code becomes tangled with business logic, making the codebase difficult to maintain and test.

**Implementation in KuRoyale:**

| Layer | Components | Responsibility |
|-------|-----------|----------------|
| **Model** | `GameState`, `PvPGameState`, `Deck`, `User`, `Card`, `Troop`, `Building`, `Tower` | Business logic, domain entities, game rules |
| **View** | `BattleArenaView`, `HandView`, `CardView`, FXML files | UI rendering, visual presentation |
| **Controller** | `BattleController`, `MainMenuController`, `DeckBuilderController` | Input handling, delegation to models/services |

**Key Files:**
```
Model Layer:
├── com.kuroyale.model.state.GameState          - Central game state
├── com.kuroyale.model.state.PvPGameState       - PvP-specific state
├── com.kuroyale.model.entities.*               - Domain entities
└── com.kuroyale.model.logic.BattleModel        - Battle orchestration

View Layer:
├── com.kuroyale.view.battle.BattleArenaView    - Arena rendering
├── com.kuroyale.view.card.CardView             - Card components
└── src/main/resources/fxml/*                    - FXML layouts

Controller Layer:
├── com.kuroyale.controller.BattleController    - Battle screen
├── com.kuroyale.controller.LoginController     - Authentication
└── com.kuroyale.controller.MainMenuController  - Navigation
```

**Example Flow:**
```
User clicks "Play Card"
     │
     ▼
┌─────────────────────────┐
│  BattleController       │  ◄── Controller: Handles click event
│  handleCardPlay()       │
└──────────┬──────────────┘
           │ delegates
           ▼
┌─────────────────────────┐
│  GameState              │  ◄── Model: Validates & executes
│  playCard(card, x, y)   │
└──────────┬──────────────┘
           │ notifies
           ▼
┌─────────────────────────┐
│  BattleArenaView        │  ◄── View: Updates display
│  renderTroop(troop)     │
└─────────────────────────┘
```

---

## Creational Patterns

### Singleton

**Intent:** Ensures a class has only one instance and provides a global point of access.

**Problem Solved:** Prevents multiple instances of managers/services that should be unique (e.g., audio, events, assets).

**Implementation:**

| Class | Purpose | Thread Safety |
|-------|---------|---------------|
| `GameEventBus` | Centralized event publishing | `synchronized` |
| `GameAssets` | Asset loading and caching | Lazy initialization |
| `AudioManager` | Music and SFX playback | `synchronized` |
| `ServiceFactory` | Service dependency injection | Eager initialization |

**GameEventBus Example:**
```java
// file: com/kuroyale/event/GameEventBus.java

public class GameEventBus {
    private static GameEventBus instance;  // Single instance
    
    private GameEventBus() { }  // Private constructor prevents external instantiation
    
    public static synchronized GameEventBus getInstance() {
        if (instance == null) {
            instance = new GameEventBus();
        }
        return instance;
    }
}
```

**Usage:**
```java
// Anywhere in the application
GameEventBus.getInstance().subscribe(questService);
GameEventBus.getInstance().publishCardPlayed(true, card, units);
```

**Why Singleton Here?**
- **GameEventBus:** One central hub ensures all events reach all subscribers
- **AudioManager:** Prevents audio conflicts from multiple instances
- **GameAssets:** Avoids redundant loading of the same resources

---

### Factory Method / Simple Factory

**Intent:** Defines an interface for creating objects, letting subclasses or the factory decide which class to instantiate.

**Problem Solved:** Decouples object creation from usage, enabling flexible instantiation based on data or configuration.

**Implementation:**

| Factory Class | Creates | Source of Data |
|---------------|---------|----------------|
| `CardFactory` | `Card` instances | Hardcoded game specs |
| `ChallengeFactory` | `Challenge` scenarios | Predefined configurations |
| `BattleStrategyFactory` | `BattleStrategy` variants | Battle mode selection |
| `ButtonFactory` | Styled JavaFX buttons | UI consistency |

**CardFactory Example:**
```java
// file: com/kuroyale/model/factory/CardFactory.java

/**
 * Implements the **Factory Method** pattern.
 * GRASP: Creator - has the initialization data to create Cards.
 */
public class CardFactory {
    
    public static Card createKnight() {
        return new Card("Knight", 3, CardType.TROOP, 1, 666, 75, 
                        1.1, 1.35, SpeedType.MEDIUM, TargetType.GROUND, 1);
    }
    
    public static Card createMusketeer() {
        return new Card("Musketeer", 4, CardType.TROOP, 1, 340, 103,
                        1.1, 6.0, SpeedType.MEDIUM, TargetType.AIR_GROUND, 1);
    }
    
    public static List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();
        cards.add(createKnight());
        cards.add(createMusketeer());
        // ... 26 more cards
        return cards;
    }
}
```

**BattleStrategyFactory Example:**
```java
// file: com/kuroyale/model/factory/BattleStrategyFactory.java

public class BattleStrategyFactory {
    public static BattleStrategy create(BattleType type) {
        return switch (type) {
            case LOCAL_BOT -> new LocalBotBattleStrategy();
            case LOCAL_PVP -> new LocalPvPBattleStrategy();
            case NETWORK_PVP -> new NetworkPvPBattleStrategy();
        };
    }
}
```

---

### Abstract Factory

**Intent:** Provides an interface for creating families of related objects without specifying concrete classes.

**Problem Solved:** When multiple related objects must be created together with varying implementations.

**Implementation:**

The `ServiceFactory` acts as an abstract factory, creating and providing access to related service families:

```java
// file: com/kuroyale/util/ServiceFactory.java

public class ServiceFactory {
    private static ServiceFactory instance;
    
    // Related service families
    private final AuthenticationService authService;
    private final DeckManagementService deckService;
    private final CardManagementService cardService;
    private final QuestService questService;
    private final AchievementService achievementService;
    
    private ServiceFactory() {
        // Creates family of services with shared dependencies
        UserRepository repo = new JsonUserRepository();
        CardCatalog catalog = new CardCatalog();
        
        this.authService = new AuthenticationService(repo);
        this.deckService = new DeckManagementService(repo, catalog);
        this.cardService = new CardManagementService(catalog);
        this.questService = new QuestService();
        this.achievementService = new AchievementService();
    }
    
    public static ServiceFactory getInstance() { ... }
    
    public AuthenticationService getAuthService() { return authService; }
    public DeckManagementService getDeckService() { return deckService; }
    // ... other getters
}
```

---

## Behavioral Patterns

### Strategy

**Intent:** Defines a family of interchangeable algorithms, encapsulating each one to make them substitutable at runtime.

**Problem Solved:** Avoids conditional logic sprawl when multiple algorithm variants exist.

**Implementation:**

KuRoyale uses Strategy pattern extensively in three domains:

#### 1. Pathfinding Strategy

Handles different movement behaviors for ground vs. air units.

```
┌─────────────────────────────┐
│   PathfindingStrategy       │  ◄── Interface
│   + computePath()           │
└──────────────┬──────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌──────────────┐ ┌──────────────────────┐
│ Ground...    │ │ AirDirect...         │
│ (A* on grid) │ │ (straight line)      │
└──────────────┘ └──────────────────────┘
```

```java
// file: com/kuroyale/model/strategy/pathfinding/PathfindingStrategy.java
public interface PathfindingStrategy {
    Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition destination);
}

// file: com/kuroyale/model/strategy/pathfinding/GroundPathfindingStrategy.java
public class GroundPathfindingStrategy implements PathfindingStrategy {
    @Override
    public Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition dest) {
        // A* algorithm considering obstacles and terrain
    }
}

// file: com/kuroyale/model/strategy/pathfinding/AirDirectPathfindingStrategy.java
public class AirDirectPathfindingStrategy implements PathfindingStrategy {
    @Override
    public Deque<GridPosition> computePath(Arena arena, Troop troop, GridPosition dest) {
        // Direct line - air units ignore terrain
    }
}
```

#### 2. Battle Strategy

Initializes different battle modes.

```java
// file: com/kuroyale/model/strategy/battle/BattleStrategy.java
public interface BattleStrategy {
    void initialize(BattleController controller);
    boolean isImplemented();
    String getNotImplementedMessage();
}

// Implementations:
// - LocalBotBattleStrategy    → vs. AI opponent
// - LocalPvPBattleStrategy    → local 2-player
// - NetworkPvPBattleStrategy  → online multiplayer
```

#### 3. Deck Building Strategy

Different ways to construct a deck for battle.

```java
// file: com/kuroyale/model/strategy/deck/DeckBuildingStrategy.java
public interface DeckBuildingStrategy {
    Deck buildDeck();
    String getDisplayName();
    boolean requiresUserInput();
}

// Implementations:
// - CurrentDeckStrategy  → Use saved deck ("My Deck")
// - RandomDeckStrategy   → Generate random 8 cards
// - CustomDeckStrategy   → Interactive card selection
```

**Usage in Code:**
```java
// Context selects strategy at runtime
DeckBuildingStrategy strategy = switch (selection) {
    case "Current Deck" -> new CurrentDeckStrategy(user);
    case "Random Deck"  -> new RandomDeckStrategy(catalog);
    case "Build Custom" -> new CustomDeckStrategy();
};

Deck deck = strategy.buildDeck();
```

---

### Observer

**Intent:** Defines a one-to-many dependency so that when one object changes state, all dependents are notified automatically.

**Problem Solved:** Decouples the game engine from secondary systems (quests, achievements, analytics) that react to events.

**Implementation:**

```
                    ┌─────────────────────────────┐
                    │      GameEventBus           │  ◄── Subject (Singleton)
                    │  - listeners: List          │
                    │  + subscribe(listener)      │
                    │  + publishCardPlayed()      │
                    │  + publishTowerDestroyed()  │
                    └──────────────┬──────────────┘
                                   │ notifies
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
           ▼                       ▼                       ▼
┌──────────────────┐   ┌──────────────────┐   ┌──────────────────┐
│  QuestService    │   │AchievementService│   │  AnalyticsService│
│  (Observer)      │   │  (Observer)      │   │  (Observer)      │
│  onCardPlayed()  │   │  onCardPlayed()  │   │  onCardPlayed()  │
└──────────────────┘   └──────────────────┘   └──────────────────┘
```

**GameEventListener Interface:**
```java
// file: com/kuroyale/event/GameEventListener.java

public interface GameEventListener {
    default void onCardPlayed(boolean isPlayer, Card card, List<ICombatant> spawnedUnits) { }
    default void onSpellCast(boolean isPlayer, Card spell, GridPosition center) { }
    default void onTowerDestroyed(boolean isPlayerTower, Tower tower) { }
    default void onElixirSpent(boolean isPlayer, int amount) { }
    default void onMatchStart() { }
    default void onMatchEnd(boolean playerWon) { }
    default void onUnitDied(ICombatant victim, ICombatant killer) { }
    // ... more event hooks
}
```

**GameEventBus Implementation:**
```java
// file: com/kuroyale/event/GameEventBus.java

public class GameEventBus {
    private final List<GameEventListener> listeners = new ArrayList<>();
    
    public void subscribe(GameEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    public void unsubscribe(GameEventListener listener) {
        listeners.remove(listener);
    }
    
    public void publishCardPlayed(boolean isPlayer, Card card, List<ICombatant> units) {
        // Copy to avoid ConcurrentModificationException
        new ArrayList<>(listeners).forEach(l -> l.onCardPlayed(isPlayer, card, units));
    }
    
    public void publishTowerDestroyed(boolean isPlayerTower, Tower tower) {
        new ArrayList<>(listeners).forEach(l -> l.onTowerDestroyed(isPlayerTower, tower));
    }
    // ... more publish methods
}
```

**Observer Implementation (QuestService):**
```java
// file: com/kuroyale/service/game/QuestService.java

public class QuestService implements GameEventListener {
    
    public QuestService() {
        GameEventBus.getInstance().subscribe(this);  // Subscribe on creation
    }
    
    @Override
    public void onCardPlayed(boolean isPlayer, Card card, List<ICombatant> units) {
        if (isPlayer) {
            progressQuest("play_cards", 1);
            if (card.getCardType() == CardType.SPELL) {
                progressQuest("cast_spells", 1);
            }
        }
    }
    
    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        if (!isPlayerTower) {
            progressQuest("destroy_towers", 1);
        }
    }
}
```

---

### Template Method

**Intent:** Defines the skeleton of an algorithm in a base class, letting subclasses override specific steps without changing the overall structure.

**Problem Solved:** Shares common behavior while allowing variations in specific steps.

**Implementation:**

`AbstractGameState` defines common game loop steps; subclasses customize behavior:

```
┌─────────────────────────────────────────┐
│          AbstractGameState              │  ◄── Template (abstract)
├─────────────────────────────────────────┤
│ # updateEntities(deltaTime)  ─────────┐ │
│ # handleCombat(deltaTime)             │ │  Shared algorithm
│ # cleanupEntities()          ─────────┘ │
│ # updateTiebreakerMode(deltaTime)       │
├─────────────────────────────────────────┤
│ ○ checkAndScoreDestroyedTowers()        │  ◄── Abstract: subclass implements
│ ○ checkTiebreakerWinCondition()         │
└─────────────────┬───────────────────────┘
                  │ extends
        ┌─────────┴─────────┐
        │                   │
        ▼                   ▼
┌───────────────┐   ┌───────────────┐
│   GameState   │   │ PvPGameState  │
│  (Solo mode)  │   │  (2-player)   │
└───────────────┘   └───────────────┘
```

```java
// file: com/kuroyale/model/state/AbstractGameState.java

public abstract class AbstractGameState {
    
    // Template method: called by game loop
    public void update(double deltaTime) {
        updateEntities(deltaTime);      // Common step
        handleCombat(deltaTime);        // Common step
        checkAndScoreDestroyedTowers(); // Abstract: subclasses customize
        cleanupEntities();              // Common step
        
        if (isTiebreakerMode()) {
            updateTiebreakerMode(deltaTime);
            checkTiebreakerWinCondition(); // Abstract: subclasses customize
        }
    }
    
    // Common implementations
    protected void updateEntities(double deltaTime) {
        for (Troop t : activeTroops) t.update(deltaTime);
        for (Building b : activeBuildings) b.update(deltaTime);
    }
    
    protected void handleCombat(double deltaTime) { /* shared logic */ }
    protected void cleanupEntities() { /* shared logic */ }
    
    // Abstract steps - must be customized
    protected abstract void checkAndScoreDestroyedTowers();
    protected abstract void checkTiebreakerWinCondition();
}
```

---

### State

**Intent:** Allows an object to alter its behavior when its internal state changes. The object appears to change its class.

**Problem Solved:** Manages complex state transitions without monolithic conditionals.

**Implementation:**

The `IBattleState` interface and deck builder states manage UI state transitions:

```java
// file: com/kuroyale/model/state/IBattleState.java

public interface IBattleState {
    void enter();
    void exit();  
    void update(double deltaTime);
}

// file: com/kuroyale/model/state/DeckBuilderState.java

public class DeckBuilderState {
    private DeckBuilderMode currentMode = DeckBuilderMode.VIEW;
    
    public enum DeckBuilderMode {
        VIEW,           // Browsing cards
        SELECTING,      // Picking cards for deck
        EDITING,        // Modifying existing deck
        CONFIRMING      // Confirming changes
    }
    
    public void transitionTo(DeckBuilderMode newMode) {
        exitCurrentState();
        this.currentMode = newMode;
        enterNewState();
    }
}
```

---

### Command

**Intent:** Encapsulates a request as an object, allowing parameterization, queueing, and undo functionality.

**Problem Solved:** Decouples action invocation from execution, enables undo/redo.

**Implementation:**

Card plays and game actions are encapsulated for network synchronization:

```java
// Conceptual representation in network code

public class PlayCardCommand {
    private final String cardName;
    private final int gridX;
    private final int gridY;
    private final long timestamp;
    
    public void execute(GameState state) {
        Card card = state.getCardByName(cardName);
        state.spawnUnit(true, card, gridX, gridY);
    }
}
```

---

## Structural Patterns

### Facade

**Intent:** Provides a simplified interface to a complex subsystem.

**Problem Solved:** Hides complexity of multiple services behind a single API.

**Implementation:**

| Facade Class | Subsystems Hidden |
|--------------|-------------------|
| `BattleModel` | Arena, Auth, Save, CardCatalog, State |
| `GameAssets` | Image loading, Font loading, Caching |

```java
// file: com/kuroyale/model/logic/BattleModel.java

/**
 * **Facade Pattern**: Simplifies controller interaction with game services.
 */
public class BattleModel {
    private final CardCatalog cardCatalog;
    private final AuthenticationService authService;
    private final SaveService saveService;
    private final ArenaManagementService arenaService;
    
    // Simple API for controllers - hides complex orchestration
    public void startNewGame(Deck playerDeck) {
        User currentUser = authService.getCurrentUser();
        Arena arena = arenaService.loadUserArena(currentUser);
        // ... complex initialization hidden
    }
    
    public void saveGame(GameState state) {
        saveService.save(state, authService.getCurrentUser());
    }
    
    public Card getCard(String name) {
        return cardCatalog.getCardByName(name);
    }
}
```

**Controller Usage:**
```java
// Controller code is clean and simple
public class BattleController {
    private BattleModel model;
    
    public void startGame() {
        model.startNewGame(selectedDeck);  // One call instead of many
    }
}
```

---

### Flyweight

**Intent:** Shares common state among many objects to reduce memory usage.

**Problem Solved:** Optimizes memory when many objects share identical intrinsic data.

**Implementation:**

| Flyweight Class | Shared Data | Unique Data |
|-----------------|-------------|-------------|
| `Card` (via `CardCatalog`) | Stats, images | None (immutable) |
| `TileType` (enum) | Terrain properties | Grid position |

```java
// file: com/kuroyale/service/game/CardCatalog.java

public class CardCatalog {
    private final Map<String, Card> cardCache = new HashMap<>();
    
    public CardCatalog() {
        // Load each card ONCE into cache
        for (Card card : CardFactory.getAllCards()) {
            cardCache.put(card.getName().toLowerCase(), card);
        }
    }
    
    public Card getCardByName(String name) {
        return cardCache.get(name.toLowerCase());  // Returns shared instance
    }
}
```

**Memory Benefit:**
```
Without Flyweight:              With Flyweight:
─────────────────────           ─────────────────────
100 Knight troops               100 Knight troops
  × Card data (100 bytes)         × Reference to shared Card
  = 10,000 bytes                  = 800 bytes (100 × 8-byte ref)
                                  + 100 bytes (1 shared Card)
                                  = 900 bytes
                                  
                                  Memory saved: ~91%
```

---

### Composite

**Intent:** Composes objects into tree structures to represent part-whole hierarchies.

**Problem Solved:** Treats individual objects and compositions uniformly.

**Implementation:**

Troops with sub-units (Skeletons, Goblins, Barbarians):

```java
// Card specifies spawn count
Card skeletons = new Card("Skeletons", 1, CardType.TROOP, ...count: 3...);

// When deployed, spawns multiple troops as a group
public List<Troop> spawnTroopGroup(Card card, int x, int y) {
    List<Troop> group = new ArrayList<>();
    for (int i = 0; i < card.getCount(); i++) {
        group.add(new Troop(card, x + offset[i], y));
    }
    return group;  // Composite result
}
```

---

### Adapter

**Intent:** Converts the interface of a class into another interface clients expect.

**Problem Solved:** Enables compatibility between incompatible interfaces.

**Implementation:**

Network message handling adapts different message formats:

```java
// Network layer receives raw JSON
public void onMessage(String json) {
    NetworkMessage msg = parseMessage(json);
    
    // Adapt network format to game commands
    switch (msg.getType()) {
        case "PLAY_CARD" -> {
            PlayCardData data = adaptToPlayCard(msg);
            gameState.processRemoteCardPlay(data);
        }
        // ...
    }
}
```

---

## Data Patterns

### Repository

**Intent:** Encapsulates data access logic, providing a collection-like interface for domain objects.

**Problem Solved:** Isolates persistence mechanism from business logic, enabling storage swaps.

**Implementation:**

```
                    ┌─────────────────────────┐
                    │    UserRepository       │  ◄── Interface
                    │  + findByUsername()     │
                    │  + save(user)           │
                    │  + findAll()            │
                    └───────────┬─────────────┘
                                │ implements
                                ▼
                    ┌─────────────────────────┐
                    │   JsonUserRepository    │  ◄── Concrete (JSON files)
                    │  - filePath: String     │
                    │  + findByUsername()     │
                    │  + save(user)           │
                    └─────────────────────────┘
                    
                    Future implementations:
                    ├── DatabaseUserRepository (SQL)
                    └── CloudUserRepository (Firebase)
```

```java
// file: com/kuroyale/repository/UserRepository.java

/**
 * GRASP: Protected Variations - isolates storage changes
 */
public interface UserRepository {
    User findByUsername(String username) throws IOException;
    void save(User user) throws IOException;
    List<User> findAll() throws IOException;
    boolean existsByUsername(String username) throws IOException;
}

// file: com/kuroyale/repository/JsonUserRepository.java

public class JsonUserRepository implements UserRepository {
    private static final String USERS_FILE = "data/users.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    @Override
    public User findByUsername(String username) throws IOException {
        List<User> users = loadAllUsers();
        return users.stream()
            .filter(u -> u.getUsername().equals(username))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public void save(User user) throws IOException {
        List<User> users = loadAllUsers();
        // Update or add user
        users.removeIf(u -> u.getUsername().equals(user.getUsername()));
        users.add(user);
        saveAllUsers(users);
    }
}
```

**Service Usage:**
```java
// Service doesn't know about JSON - only the interface
public class AuthenticationService {
    private final UserRepository userRepository;  // Interface type
    
    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;  // Dependency injection
    }
    
    public User authenticate(String username, String password) throws IOException {
        User user = userRepository.findByUsername(username);
        if (user != null && user.validatePassword(password)) {
            return user;
        }
        return null;
    }
}
```

---

## Cross-Reference: GRASP to GoF

This section maps GRASP principles to their GoF pattern implementations:

| GRASP Principle | Related GoF Pattern | KuRoyale Example |
|-----------------|---------------------|------------------|
| **Information Expert** | - | `User.validatePassword()`, `Deck.isValid()` |
| **Creator** | Factory Method | `CardFactory`, `ChallengeFactory` |
| **Controller** | MVC | `BattleController`, `DeckBuilderController` |
| **Low Coupling** | Strategy, Observer | Interface-based dependencies everywhere |
| **High Cohesion** | Single Responsibility | Each service has one purpose |
| **Polymorphism** | Strategy | `PathfindingStrategy` variants |
| **Pure Fabrication** | Facade, Service | `ServiceFactory`, `CardCatalog` |
| **Indirection** | Facade, Repository | `BattleModel`, `UserRepository` |
| **Protected Variations** | Repository, Strategy | Interface-based persistence/algorithms |

---

## Pattern Interaction Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              KuRoyale Architecture                           │
└──────────────────────────────────────────────────────────────────────────────┘

                        ┌─────────────────┐
                        │  Controllers    │  ◄── MVC: Handle input
                        │  (Thin Layer)   │
                        └────────┬────────┘
                                 │ delegates
                    ┌────────────┼────────────┐
                    ▼            ▼            ▼
            ┌─────────────┐ ┌─────────┐ ┌─────────────┐
            │ BattleModel │ │Services │ │ServiceFactory│
            │  (Facade)   │ │ Layer   │ │ (Singleton) │
            └──────┬──────┘ └────┬────┘ └─────────────┘
                   │             │
          ┌────────┴─────────────┴────────┐
          ▼                               ▼
    ┌──────────────┐              ┌────────────────────┐
    │  GameState   │              │  GameEventBus      │
    │(TemplateMethod)│ publishes  │ (Observer+Singleton)│
    └──────┬───────┘  ─────────▶  └──────────┬─────────┘
           │                                  │
           │                        ┌─────────┼─────────┐
           ▼                        ▼         ▼         ▼
    ┌─────────────┐          ┌─────────┐ ┌─────────┐ ┌─────────┐
    │ Domain      │          │ Quest   │ │Achieve- │ │Analytics│
    │ Entities    │          │ Service │ │ment Svc │ │ Service │
    │(Card,Troop, │          └─────────┘ └─────────┘ └─────────┘
    │ User,Deck)  │                  (Observers)
    └─────────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
┌─────────┐  ┌──────────┐
│Strategy │  │Repository│
│(Path,   │  │(User     │
│ Battle, │  │ persist) │
│ Deck)   │  └──────────┘
└─────────┘

Factory Pattern creates: Cards, Challenges, Strategies, Services
Flyweight Pattern shares: Card instances via CardCatalog
```

---

## Summary

KuRoyale implements 14+ design patterns across all layers:

| Category | Patterns Used |
|----------|---------------|
| **Architectural** | MVC |
| **Creational** | Singleton, Factory Method, Abstract Factory |
| **Behavioral** | Strategy, Observer, Template Method, State, Command |
| **Structural** | Facade, Flyweight, Composite, Adapter |
| **Data** | Repository |

Each pattern addresses specific design challenges, promoting:
- **Maintainability** through separation of concerns
- **Extensibility** via interfaces and polymorphism
- **Testability** by decoupling dependencies
- **Performance** with efficient memory usage (Flyweight)

---

**Document Version:** 2.0  
**Last Updated:** January 2026  
**Related Documentation:** [GRASP_Architecture.md](GRASP_Architecture.md)
