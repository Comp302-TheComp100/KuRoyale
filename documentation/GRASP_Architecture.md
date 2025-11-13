# KU Royale - GRASP Architecture Documentation

## Overview

This document describes the architecture of KU Royale, which follows GRASP (General Responsibility Assignment Software Patterns) design principles from Craig Larman's "Applying UML and Patterns".

## Architecture Layers

```
┌─────────────────────────────────────────────────┐
│                 UI Layer                        │
│  (LoginController, DeckBuilderController, etc) │
│          Thin controllers - UI only             │
└─────────────────┬───────────────────────────────┘
                  │ delegates to
┌─────────────────▼───────────────────────────────┐
│              Service Layer                      │
│  AuthenticationService, DeckManagementService   │
│     Business logic & orchestration              │
└─────────────────┬───────────────────────────────┘
                  │ uses
┌─────────────────▼───────────────────────────────┐
│             Domain Layer                        │
│     User, Deck, Card (rich models)             │
│     Behavior + data together                    │
└─────────────────┬───────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────┐
│           Repository Layer                      │
│      UserRepository (interface)                 │
│      JsonUserRepository (implementation)        │
│           Persistence only                      │
└─────────────────────────────────────────────────┘
```

## GRASP Patterns Applied

### 1. Information Expert
**Principle:** Assign responsibility to the class that has the information needed to fulfill it.

**Implementation:**
- `User` validates its own password and manages its own deck data
- `Deck` knows if it's valid, handles card replacement, calculates average elixir cost
- `Card` calculates its own DPS (damage per second)
- `CardCatalog` knows all available cards and finds cards by name

**Example:**
```java
// User is the Information Expert for password validation
public boolean validatePassword(String password) {
    return PasswordUtil.verifyPassword(password, this.passwordHash);
}

// Deck is the Information Expert for validity
public boolean isValid() {
    return cards.size() == MAX_CARDS;
}
```

### 2. Creator
**Principle:** Assign class B the responsibility to create instance of class A if B aggregates, contains, closely uses, or has initializing data for A.

**Implementation:**
- `AuthenticationService` creates `User` objects (has initialization data: username, password hash)
- `CardFactory` creates `Card` objects (has card specifications)
- `DeckManagementService` creates `Deck` objects from user data
- `ServiceFactory` creates all service instances

**Example:**
```java
public User register(String username, String password) throws IOException {
    String passwordHash = PasswordUtil.hashPassword(password);
    User newUser = new User(username, passwordHash); // Creator pattern
    userRepository.save(newUser);
    return newUser;
}
```

### 3. Controller
**Principle:** Assign responsibility for handling system events to a class representing the overall system, device, or use case scenario.

**Implementation:**
- `LoginController` - thin controller for login UI, delegates to `AuthenticationService`
- `DeckBuilderController` - thin controller for deck building UI, delegates to `DeckManagementService`
- `MainMenuController` - thin controller for main menu UI
- Controllers focus ONLY on UI concerns and delegate all business logic to services

**Example:**
```java
@FXML
private void handleLogin() {
    // UI validation only
    if (username.isEmpty() || password.isEmpty()) {
        showError("Please enter both username and password");
        return;
    }
    
    // Delegate to service (Controller pattern)
    User user = authService.authenticate(username, password);
    if (user != null) {
        authService.setCurrentUser(user);
        navigateToMainMenu(); // UI concern
    } else {
        showError("Invalid username or password"); // UI concern
    }
}
```

### 4. Low Coupling
**Principle:** Assign responsibilities so that coupling remains low.

**Implementation:**
- Eliminated all static services
- Dependency injection via `ServiceFactory`
- Services depend on interfaces (`UserRepository`), not concrete implementations
- Controllers receive services via factory, not direct instantiation

**Example:**
```java
// Interface-based dependency (Protected Variations + Low Coupling)
public class AuthenticationService {
    private final UserRepository userRepository; // Interface, not concrete class
    
    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository; // Dependency injection
    }
}
```

### 5. High Cohesion
**Principle:** Keep objects focused, understandable, and manageable.

**Implementation:**
- `AuthenticationService` - ONLY handles authentication logic
- `DeckManagementService` - ONLY handles deck operations
- `UserRepository` - ONLY handles persistence
- `ValidationUtil` - ONLY validates business rules
- Each class has a single, clear responsibility

**Example:**
```java
// High Cohesion - ValidationUtil only does validation
public class ValidationUtil {
    public static boolean isValidPassword(String password) { ... }
    public static boolean isValidUsername(String username) { ... }
    public static String getPasswordRequirements() { ... }
    public static String getUsernameRequirements() { ... }
}
```

### 6. Polymorphism
**Principle:** Use polymorphic operations to handle alternatives based on type.

**Implementation:**
- `UserRepository` interface with `JsonUserRepository` implementation
- Allows for different persistence mechanisms without changing client code
- Could add `DatabaseUserRepository`, `CloudUserRepository`, etc.

**Example:**
```java
// Interface
public interface UserRepository {
    User findByUsername(String username) throws IOException;
    void save(User user) throws IOException;
    List<User> findAll() throws IOException;
}

// Implementation can be swapped without affecting clients
public class JsonUserRepository implements UserRepository {
    // JSON-specific implementation
}
```

### 7. Pure Fabrication
**Principle:** Assign responsibilities to a class that doesn't represent a domain concept to support high cohesion and low coupling.

**Implementation:**
- `AuthenticationService` - doesn't represent a domain concept, created for authentication logic
- `DeckManagementService` - created to handle deck operations coordination
- `CardCatalog` - created to manage card availability
- `UserRepository` - created to handle persistence concerns
- `ValidationUtil` - created for validation concerns
- `ServiceFactory` - created for dependency management

**Example:**
```java
// Pure Fabrication - doesn't represent a real-world concept
public class ServiceFactory {
    private final AuthenticationService authenticationService;
    private final DeckManagementService deckManagementService;
    
    private ServiceFactory() {
        // Creates and manages all services
    }
}
```

### 8. Indirection
**Principle:** Assign responsibility to an intermediate object to mediate between components.

**Implementation:**
- Services mediate between controllers and domain objects
- `CardCatalog` mediates between controllers and `CardFactory`
- `UserRepository` mediates between services and data storage
- `ServiceFactory` mediates between controllers and services

**Example:**
```java
// CardCatalog provides indirection between controllers and CardFactory
public class CardCatalog {
    private final CardFactory cardFactory;
    
    public List<Card> getAllCards() {
        return cardFactory.getAllCards(); // Indirection
    }
}
```

### 9. Protected Variations
**Principle:** Identify points of predicted variation and assign responsibilities to create a stable interface.

**Implementation:**
- `UserRepository` interface protects against data storage changes
- `ServiceFactory` protects against service creation/initialization changes
- Services protect controllers from domain model changes

**Example:**
```java
// Protected Variations - interface protects against implementation changes
public interface UserRepository {
    User findByUsername(String username) throws IOException;
    // Client code doesn't know if it's JSON, database, cloud, etc.
}
```

## Project Structure

```
src/main/java/com/kuroyale/
├── controller/                     # UI Layer (Thin Controllers)
│   ├── LoginController.java        # Handles login UI events
│   ├── DeckBuilderController.java  # Handles deck building UI events
│   └── MainMenuController.java     # Handles main menu UI events
│
├── service/                        # Service Layer (Business Logic)
│   ├── AuthenticationService.java  # Authentication business logic
│   ├── DeckManagementService.java  # Deck management business logic
│   └── CardCatalog.java           # Card catalog service
│
├── model/                         # Domain Layer (Rich Models)
│   ├── User.java                  # User with behavior methods
│   ├── Deck.java                  # Deck with behavior methods
│   ├── Card.java                  # Card with behavior methods
│   ├── CardFactory.java           # Creates Card instances
│   ├── CardType.java              # Card type enum
│   ├── SpeedType.java             # Speed type enum
│   └── TargetType.java            # Target type enum
│
├── repository/                    # Repository Layer (Persistence)
│   ├── UserRepository.java        # Repository interface
│   └── JsonUserRepository.java    # JSON implementation
│
├── util/                          # Utilities
│   ├── ServiceFactory.java        # Dependency injection container
│   ├── ValidationUtil.java        # Business rule validation
│   ├── PasswordUtil.java          # Password hashing/verification
│   ├── ButtonFactory.java         # UI button creation
│   └── StyleHelper.java           # UI styling utilities
│
└── view/                          # Custom UI Components
    ├── CardView.java              # Card display component
    ├── DeckSlotView.java          # Deck slot component
    └── CardInfoDialog.java        # Card info dialog
```

## Component Responsibilities

### UI Layer (Controllers)
**Responsibilities:**
- Handle user input events
- Update UI displays
- Navigate between screens
- Validate UI input (empty fields, etc.)
- Delegate business logic to services

**NOT Responsible For:**
- Business logic
- Data persistence
- Domain rules
- Object creation (except UI components)

### Service Layer
**Responsibilities:**
- Orchestrate business operations
- Coordinate between domain objects
- Handle complex workflows
- Manage transactions (if applicable)
- Create domain objects (Creator pattern)

**NOT Responsible For:**
- UI concerns
- Direct data access (uses repositories)

### Domain Layer
**Responsibilities:**
- Encapsulate business entities
- Implement business rules on their own data
- Provide behavior related to their data (Information Expert)
- Validate their own state

**NOT Responsible For:**
- Persistence
- UI display
- Service orchestration

### Repository Layer
**Responsibilities:**
- Load and save domain objects
- Abstract data storage mechanism
- Handle data queries

**NOT Responsible For:**
- Business logic
- Creating domain objects (just loads/saves them)
- UI concerns

## Dependency Flow

```
Main.java
    └─> ServiceFactory (initializes)
            ├─> AuthenticationService
            │       └─> UserRepository
            │
            ├─> DeckManagementService
            │       ├─> UserRepository
            │       └─> CardCatalog
            │
            └─> CardCatalog
                    └─> CardFactory

Controllers get services from ServiceFactory
    └─> Services coordinate domain objects
            └─> Domain objects contain business logic
                    └─> Repositories handle persistence
```

## Key Design Decisions

### Why Instance-Based Instead of Static?
- **Low Coupling:** Static dependencies create tight coupling
- **Testability:** Instance-based allows mocking/testing
- **Flexibility:** Can create different configurations
- **Lifecycle:** Proper initialization and cleanup

### Why Services Layer?
- **High Cohesion:** Separates orchestration from domain logic
- **Controller Pattern:** Controllers stay thin and focused on UI
- **Reusability:** Business logic can be reused across different UIs
- **Testability:** Can test business logic without UI

### Why Repository Pattern?
- **Separation of Concerns:** Isolates persistence from business logic
- **Protected Variations:** Can change storage mechanism without affecting business logic
- **Single Responsibility:** Repositories only handle data access

### Why Rich Domain Models?
- **Information Expert:** Objects know their own data and can operate on it
- **Encapsulation:** Business rules are with the data they affect
- **Maintainability:** Related logic is together

## Benefits of This Architecture

1. **Testability**
   - Services can be unit tested independently
   - Domain objects can be tested without persistence
   - Controllers can be tested with mock services

2. **Maintainability**
   - Clear separation of concerns
   - Changes in one layer don't affect others
   - Easy to locate where to make changes

3. **Flexibility**
   - Can swap implementations (e.g., database instead of JSON)
   - Can add new features without modifying existing code
   - Can reuse services in different contexts

4. **Scalability**
   - Architecture supports growth
   - New features follow established patterns
   - Clear places to add functionality

5. **Code Quality**
   - High cohesion within classes
   - Low coupling between classes
   - Clear responsibilities
   - Self-documenting structure

## Example Workflows

### User Registration Flow
```
1. User enters username/password in LoginController
2. LoginController validates UI input (not empty)
3. LoginController delegates to AuthenticationService.register()
4. AuthenticationService validates business rules (via ValidationUtil)
5. AuthenticationService creates User object (Creator pattern)
6. AuthenticationService saves via UserRepository
7. JsonUserRepository persists to JSON file
8. AuthenticationService sets current user
9. LoginController navigates to main menu (UI concern)
```

### Deck Building Flow
```
1. User clicks "Use" button in DeckBuilderController
2. DeckBuilderController delegates to DeckManagementService.addCardToDeck()
3. DeckManagementService delegates to Deck.addCard() (Information Expert)
4. Deck validates its own rules (max 8 cards, no duplicates)
5. DeckBuilderController updates UI (reorganize grid, update elixir cost)
6. DeckBuilderController calls DeckManagementService.saveDeck()
7. DeckManagementService gets card names from Deck
8. DeckManagementService updates User's deck
9. DeckManagementService saves via UserRepository
10. JsonUserRepository persists to JSON file
```

## Future Enhancements

This architecture supports future enhancements such as:

- **Database Integration:** Create `DatabaseUserRepository` implementing `UserRepository`
- **Cloud Storage:** Create `CloudUserRepository` implementing `UserRepository`
- **Multiplayer:** Add `MatchmakingService`, `GameService`
- **Analytics:** Add `AnalyticsService` that observes game events
- **Achievements:** Add `AchievementService` tracking user progress
- **Social Features:** Add `FriendService`, `ChatService`

All can be added without modifying existing code, following Open/Closed Principle.

## References

- Craig Larman, "Applying UML and Patterns: An Introduction to Object-Oriented Analysis and Design and Iterative Development" (3rd Edition), Chapter 17: GRASP Design Patterns
- GRASP Patterns: Information Expert, Creator, Controller, Low Coupling, High Cohesion, Polymorphism, Pure Fabrication, Indirection, Protected Variations

---

**Document Version:** 1.0  
**Last Updated:** November 13, 2025  
**Architecture Status:** Implemented and Verified


