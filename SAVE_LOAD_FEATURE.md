# Save & Reload Match Feature

## Overview
The game now supports saving and reloading matches, allowing players to pause a game and continue from where they left off at a later time.

## Features Implemented

### 1. Save Match (UC10)
Players can save their current match progress during gameplay.

**How to Save:**
1. During a battle, click the **"Pause"** button in the left panel
2. The pause menu will appear with the following options:
   - **RESUME**: Continue playing without saving
   - **SAVE & RESUME**: Save the game and continue playing
   - **SAVE & EXIT**: Save the game and return to main menu
   - **EXIT WITHOUT SAVING**: Return to main menu without saving

**What Gets Saved:**
- Current game time and timer state
- Elixir levels for both player and bot
- Current scores
- Tower health (all towers including King and Princess towers)
- Active troops on the battlefield (position, health, state)
- Active buildings on the battlefield (position, health, remaining lifetime)
- Player's deck and current hand
- Arena layout and configuration

### 2. Reload Match (UC9)
Players can view all saved matches and resume any of them.

**How to Reload:**
1. From the main menu, click **"RESUME GAME"**
2. You'll see a list of all your saved games with the following information:
   - Save date and time
   - Player name
   - Time remaining in match
   - Current score
   - Current elixir level
3. Click **"LOAD GAME"** on any saved game to resume it
4. The match will continue from exactly where you left off

**Additional Options:**
- **DELETE**: Permanently remove a saved game
- **BACK TO MENU**: Return to the main menu

## Technical Implementation

### Files Created
1. **SavedGameState.java**: Model class that holds all match data
   - Includes inner classes for SavedTower, SavedTroop, and SavedBuilding
   - Implements Serializable for file persistence

2. **GameSaveService.java**: Service for saving/loading game states
   - Saves games to `~/.kuroyale/saved_games/` directory
   - Uses Java serialization for persistence
   - Provides methods to list, load, and delete saved games

3. **SavedGamesController.java**: Controller for the saved games screen
   - Displays list of saved games
   - Handles loading and deleting saved games
   - Shows detailed information about each save

4. **saved-games.fxml**: UI layout for the saved games screen

### Files Modified
1. **BattleController.java**:
   - Added pause/resume functionality
   - Added save game handlers
   - Added restore from saved game functionality
   - Pause menu overlay

2. **MainMenuController.java**:
   - Added "Resume Game" button
   - Handler to navigate to saved games screen

3. **ServiceFactory.java**:
   - Added GameSaveService to the service factory

4. **GameState.java**:
   - Added `restoreFromSaved()` method to restore game state
   - Added `restoreTowerHealth()` method to restore tower health

5. **ElixirManager.java**:
   - Added `setCurrentElixir()` setter for loading saved elixir values

6. **Tower.java**:
   - Added `setCurrentHealth()` setter for restoring tower health

7. **battle.fxml**:
   - Added Pause button
   - Added pause menu overlay container

8. **main-menu.fxml**:
   - Added Resume Game button

## Usage Flow

### Saving a Match
```
Battle Screen → Click "Pause" → Select Save Option → Game Saved
```

### Loading a Match
```
Main Menu → Click "Resume Game" → Select Saved Game → Click "Load Game" → Battle Resumes
```

## Save File Location
Saved games are stored in:
- **macOS/Linux**: `~/.kuroyale/saved_games/`
- **Windows**: `%USERPROFILE%\.kuroyale\saved_games\`

Each save file is named: `{username}_{timestamp}.krsave`

## Notes
- Saved games are user-specific when viewing from the saved games screen
- Each save includes a unique ID to prevent conflicts
- The system validates that all game data is properly restored
- Saves persist across application restarts
- Active spell effects are NOT saved (they're temporary visual effects)

## Future Enhancements (Optional)
- Auto-save functionality
- Multiple save slots per match
- Cloud save support
- Save file compression
- Preview images for saved games
- Match replay functionality

