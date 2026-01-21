# KU Royale

A Clash Royale clone game built with Java and JavaFX, featuring user authentication, deck building, arena design, and persistent user data storage.

## 🎮 Features

### User Management
- **User Authentication** - Secure login and registration system
- **Password Security** - Passwords are hashed using bcrypt for maximum security
- **Persistent Storage** - User data saved to JSON files (username, decks, arena layouts)

### Deck Building System
- **28 Unique Cards** - Complete card catalog with detailed stats
  - 15 Troops (Knight, Musketeer, Mini P.E.K.K.A, Giant, Hog Rider, and more)
  - 9 Buildings (Cannon, Tesla, Mortar, Inferno Tower, Elixir Collector, etc.)
  - 4 Spells (Zap, Arrows, Fireball, Rocket)
- **Interactive Deck Builder** - Drag-and-drop interface to build your 8-card deck
- **Card Information** - View detailed stats for each card (HP, damage, speed, range, etc.)
- **Deck Persistence** - Your deck is automatically saved to your user profile

### Arena Design
- **Custom Arena Designer** - Design your own battle arena layout
- **Tile-Based System** - Place different tile types to create unique arenas
- **Layout Persistence** - Custom arena designs are saved per user

### Audio System
- **Background Music** - Immersive game soundtracks
- **Sound Effects** - Interactive button clicks and UI feedback
- **Audio Controls** - Manage music and effects independently

## 🛠️ Technologies

- **Java 21 LTS** - Latest Long-Term Support release
- **JavaFX 21.0.5 LTS** - Modern UI framework for rich desktop applications
- **Maven 3.9.11** - Build automation (via wrapper)
- **JSON** - User data persistence (org.json library)
- **Bcrypt** - Secure password hashing

## 📋 Prerequisites

- **Java 21 LTS** - The latest Long-Term Support release
- **Maven is NOT required** - this project uses Maven Wrapper (`mvnw`)

> **Note:** This project uses Java 21 LTS and JavaFX 21 LTS - all LTS versions for maximum stability!

## 🚀 Getting Started

### 1. Clone the repository
```bash
git clone https://github.com/yourusername/KuRoyale.git
cd KuRoyale
```

### 2. Build the project
On Windows:
```bash
.\mvnw.cmd clean install
```

On Linux/Mac:
```bash
./mvnw clean install
```

### 3. Run the application
On Windows:
```bash
.\mvnw.cmd javafx:run
```

On Linux/Mac:
```bash
./mvnw javafx:run
```

## 📁 Project Structure

```
KuRoyale/
├── .mvn/                      # Maven wrapper files
├── src/
│   └── main/
│       ├── java/com/kuroyale/
│       │   ├── Launcher.java           # Application entry point
│       │   ├── Main.java               # JavaFX application setup
│       │   ├── controller/             # UI Controllers (MVC Pattern)
│       │   │   ├── ArenaDesignController.java
│       │   │   ├── DeckBuilderController.java
│       │   │   ├── LoginController.java
│       │   │   ├── MainMenuController.java
│       │   │   └── SettingsController.java
│       │   ├── model/                  # Data Models
│       │   │   ├── Arena.java
│       │   │   ├── ArenaLayout.java
│       │   │   ├── Card.java
│       │   │   ├── CardFactory.java    # Creates all 28 cards
│       │   │   ├── CardType.java
│       │   │   ├── Deck.java
│       │   │   ├── SpeedType.java
│       │   │   ├── TargetType.java
│       │   │   ├── Tile.java
│       │   │   ├── TileType.java
│       │   │   └── User.java
│       │   ├── repository/             # Data Persistence Layer
│       │   │   ├── JsonUserRepository.java
│       │   │   └── UserRepository.java
│       │   ├── service/                # Business Logic Services
│       │   │   ├── ArenaService.java
│       │   │   ├── AuthenticationService.java
│       │   │   ├── CardCatalog.java
│       │   │   └── DeckManagementService.java
│       │   ├── util/                   # Utility Classes
│       │   │   ├── AudioManager.java
│       │   │   ├── ButtonFactory.java
│       │   │   ├── PasswordUtil.java
│       │   │   ├── ServiceFactory.java # Dependency Injection
│       │   │   ├── SoundEffectUtil.java
│       │   │   ├── StyleHelper.java
│       │   │   └── ValidationUtil.java
│       │   └── view/                   # Custom View Components
│       │       ├── CardInfoDialog.java
│       │       ├── CardView.java
│       │       └── DeckSlotView.java
│       └── resources/
│           ├── fonts/                  # Custom fonts (Clash)
│           ├── fxml/                   # FXML UI layouts
│           │   ├── arena-design.fxml
│           │   ├── deck-builder.fxml
│           │   ├── login.fxml
│           │   ├── main-menu.fxml
│           │   └── settings.fxml
│           ├── images/                 # Game assets
│           ├── musics/                 # Background music
│           ├── sfx/                    # Sound effects
│           └── styles/                 # CSS stylesheets
├── documentation/                      # Project documentation
├── mvnw                               # Maven wrapper script (Unix)
├── mvnw.cmd                          # Maven wrapper script (Windows)
├── pom.xml                           # Maven project configuration
└── README.md
```

## 🏗️ Architecture

This project strictly follows **GRASP (General Responsibility Assignment Software Patterns)** principles and implements standard GOF Design Patterns to ensure maintainability, scalability, and testability.

### 🏛️ Architectural Patterns
- **Model-View-Controller (MVC)**
  - **Model**: `GameState`, `BattleModel` (Business logic & state)
  - **View**: `BattleArenaView`, FXML files (UI rendering)
  - **Controller**: `BattleController`, `MainMenuController` (Input handling & mediation)

### 🧩 Design Patterns
- **Singleton**
  - `GameEventBus`: Global event dispatcher.
  - `ServiceFactory`: Central dependency injection container.
  - `GameAssets`: Facade for asset loading and caching.
- **Factory Method**
  - `CardFactory`: Encapsulates complex logic for creating 28+ unique troops and buildings.
- **Strategy**
  - `PathfindingStrategy`: Interchangeable algorithms for movement (e.g., `GroundPathfindingStrategy` vs. Air movement).
- **Observer**
  - **Subject**: `GameEventBus` broadcasts events (card played, tower destroyed).
  - **Observers**: `QuestService` (tracks progress), `BattleController` (updates UI), `AchievementService`.
- **Flyweight**
  - `CardCatalog`: Stores shared immutable metadata for cards preventing memory duplication.
  - `TileType`: Enums used by grid cells to share common terrain properties.
- **Repository**
  - `UserRepository`: Interface for data access.
  - `JsonUserRepository`: Concrete implementation handling JSON persistence.

### 📏 GRASP Principles
- **Information Expert**: Classes like `User` and `Deck` manage and validate their own data.
- **Creator**: `CardFactory` handles `Card` instantiation; `ServiceFactory` manages Service lifecycles.
- **Low Coupling**: Dependencies are injected via `ServiceFactory`; Layers communicate via interfaces.
- **High Cohesion**: Services are specialized (`AuthenticationService`, `DeckManagementService`) with single responsibilities.
- **Pure Fabrication**: `ServiceFactory` and `JsonUserRepository` exist to support architectural needs (DI, Persistence) rather than representing domain concepts.
- **Protected Variations**: `UserRepository` interface isolates business logic from storage implementation details.
- **Controller**: Dedicated controllers (`BattleController`) mediate between UI and Domain logic.

## 🎴 Available Cards

### Troops (15 cards)
Knight, Musketeer, Mini P.E.K.K.A, Giant, Hog Rider, Bomber, Valkyrie, Wizard, Skeletons, Goblins, Spear Goblins, Archers, Minions, Minion Horde, Barbarians

### Buildings (9 cards)
Cannon, Tesla, Mortar, Bomb Tower, Inferno Tower, Tombstone, Goblin Hut, Barbarian Hut, Elixir Collector

### Spells (4 cards)
Zap, Arrows, Fireball, Rocket

## 💾 Data Persistence

User data is automatically saved to `users.json` in the application directory. This includes:
- Username and hashed password
- Custom card decks (8 cards per user)
- Custom arena layouts

## 🔧 Development

The Maven wrapper automatically downloads the correct Maven version, so you don't need to install Maven manually.

### Useful Commands

- `.\mvnw.cmd clean` - Clean build artifacts
- `.\mvnw.cmd compile` - Compile the project
- `.\mvnw.cmd test` - Run tests
- `.\mvnw.cmd package` - Create JAR package
- `.\mvnw.cmd javafx:run` - Run the application

## 📝 License

This project is a educational clone created for learning purposes.