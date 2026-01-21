# Design Patterns in KuRoyale

This document lists the design patterns implemented in the KuRoyale project and their specific locations.

## Architectural Patterns

### Model-View-Controller (MVC)
*Separates the application into three main components: Model, View, and Controller to decouple internal representations of information from the ways that information is presented to or accepted from the user.*

- **Models**: Business logic and data state.
  - `com.kuroyale.model.logic.GameState`: Central game state manager.
  - `com.kuroyale.model.logic.BattleModel`: Facade for battle-related logic.
  - `com.kuroyale.model.entities.*`: Domain entities like `Troop`, `Building`, `Arena`.
- **Views**: UI components and layouts.
  - `com.kuroyale.view.*`: Custom UI components (e.g., `BattleArenaView`, `HandView`).
  - FXML files in `src/main/resources/fxml/`: Declarative UI structure.
- **Controllers**: Glue between Model and View, handling user input.
  - `com.kuroyale.controller.BattleController`: Manages the battle screen.
  - `com.kuroyale.controller.MainMenuController`: Manages main navigation.

---

## Creational Patterns

### Singleton
*Ensures a class has only one instance and provides a global point of access to it.*

- `com.kuroyale.event.GameEventBus`: Manages game-level events and subscribers.
- `com.kuroyale.util.GameAssets`: Centralizes asset loading (images, fonts) to ensure they are only loaded once.
- `com.kuroyale.util.ServiceFactory`: Provides centralized, single-instance access to all service layer objects.
- `com.kuroyale.util.AudioManager`: Manages global music and sound effect playback.

### Factory Method / Simple Factory
*Defines an interface for creating an object, but lets subclasses decide which class to instantiate, or centralizes object creation in a single class.*

- `com.kuroyale.model.entities.CardFactory`: Creates `Card` objects from configuration data.
- `com.kuroyale.model.entities.ChallengeFactory`: Creates predefined `Challenge` scenarios.
- `com.kuroyale.util.ButtonFactory`: Creates consistently styled JavaFX buttons for the UI.
- `com.kuroyale.util.ServiceFactory`: Acts as a factory for providing service instances (e.g., `QuestService`, `AchievementService`).

---

## Behavioral Patterns

### Strategy
*Defines a family of interchangeable algorithms and encapsulates each one.*

- `com.kuroyale.service.PathfindingStrategy`: Interface for troop movement algorithms.
- `com.kuroyale.service.GroundPathfindingStrategy`: Implements A* or tile-based movement for ground units.
- `com.kuroyale.service.AirDirectPathfindingStrategy`: Implements direct-line movement for flying units.

### Observer
*Defines a one-to-many dependency between objects so that when one object changes state, all its dependents are notified.*

- `com.kuroyale.event.GameEventBus`: Acts as the subject that broadcasts game events (card played, tower destroyed).
- `com.kuroyale.service.QuestService`: Observer that updates quest progress based on game events.
- `com.kuroyale.service.AchievementService`: Observer that tracks long-term player achievements.
- `BattleController` UI listeners: JavaFX event filters listening to grid clicks and card selections.

---

## Structural Patterns

### Facade
*Provides a simplified interface to a larger body of code.*

- `com.kuroyale.model.logic.BattleModel`: Provides a simple interface for the `BattleController` to interact with multiple complex services (Arena, Auth, Save, CardCatalog).
- `com.kuroyale.util.GameAssets`: Provides a clean API for loading various asset types without exposing low-level I/O.

### Flyweight
*Uses sharing to support large numbers of fine-grained objects efficiently.*

- `com.kuroyale.service.CardCatalog`: Caches unique `Card` metadata. Multiple troop instances in `GameState` reference a single `Card` object for stats.
- `com.kuroyale.model.entities.Arena`: Uses the `TileType` enum to represent shared terrain properties across thousands of grid cells.

---

## Data Patterns

### Repository
*Encapsulates the logic required to access data sources.*

- `com.kuroyale.repository.UserRepository`: Interface defining user data operations.
- `com.kuroyale.repository.JsonUserRepository`: Concrete implementation that handles persistence to JSON files.
