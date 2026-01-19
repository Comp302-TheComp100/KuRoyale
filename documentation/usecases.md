# KU Royale - Use Case Documentation

## Actors
*   **Player**: The human user playing the game.
*   **Computer Opponent**: The AI-controlled opponent.
*   **Game Manager**: The system component responsible for game state, rules, and logic execution.

---

## 1. UC1: Start New Match
**Primary Actor:** Player
**Supporting Actor:** Game Manager
**Offstage Actor:** Computer Opponent

**Description:**
The player starts a new single-player battle against the computer.

**Preconditions:**
*   The player is in the Main Menu.
*   A valid arena layout has been saved by the Player.

**Postconditions:**
*   A new match has begun.
*   The match timer starts counting down from 3:00.

**Main Flow (Success Scenario):**
1.  The player selects "New Game".
2.  The system verifies an existing saved arena layout exists.
3.  The system presents the "Deck Building" interface with all 28 cards.
4.  The player selects exactly 8 unique cards.
5.  The player confirms the deck; the system validates it.
6.  The system assigns a valid deck to the Computer Opponent.
7.  The match begins (Arena loaded, 5 Elixir, 4 starting cards).

**Extensions:**
*   **2a. No saved arena layout:**
    *   System informs the player they must design an arena first.
    *   System returns to Main Menu.
*   **5a. Invalid Deck (Not 8 cards):**
    *   System displays an error requesting exactly 8 unique cards.
    *   Player continues deck selection.
*   **System Failure/Crash:**
    *   System attempts to recover state; if impossible, resets to clean state.

---

## 2. UC2: Build Deck
**Primary Actor:** Player
**Supporting Actor:** Game Manager

**Description:**
The player selects cards to form a battle deck.

**Preconditions:**
*   "Start New Match" use case is active.

**Postconditions:**
*   A valid 8-card deck is saved for the match.

**Main Flow (Success Scenario):**
1.  System displays all 28 available cards and 8 empty deck slots.
2.  Player selects a card from the library.
3.  System adds the card to an empty slot.
4.  Player repeats until 8 slots are filled.
5.  Player confirms the deck.
6.  System validates the deck (8 unique cards).
7.  System saves the deck for the current match.

**Extensions:**
*   **Player quits:** Returns to Main Menu.
*   **View Card Info:** Player clicks a card; System shows DPS/Health stats.
*   **Remove Card:** Player selects "Remove"; System frees the slot.
*   **Duplicate Card:** System ignores selection or warns player.
*   **Confirm with < 8 cards:** System displays error.

---

## 3. UC3: Design Arena
**Primary Actor:** Player
**Supporting Actor:** Game Manager

**Description:**
The player designs their defensive arena layout.

**Preconditions:**
*   Player is in the Main Menu.

**Postconditions:**
*   Arena layout is saved and persistent.
*   Computer Opponent will use a mirrored version of this layout in battles.

**Main Flow (Success Scenario):**
1.  Player selects "Arena Editor".
2.  System displays arena grid.
3.  Player places 2 Crown Towers and 1 King Tower validly.
4.  Player places 1 to 3 bridges on the river.
5.  System validates layout (pathing, required buildings).
6.  Player confirms save.
7.  System saves specific positions of towers and bridges.

**Extensions:**
*   **Invalid Placement:** System blocks placement and alerts user (e.g., overlapping, invalid tile).
*   **Missing Features:** If confirming without required towers/bridges, System alerts user.

---

## 4. UC4: Deploy Card
**Primary Actor:** Player
**Supporting Actor:** Game Manager
**Offstage Actor:** Computer Opponent

**Description:**
The player places a troop, building, or spell onto the arena.

**Preconditions:**
*   Match is in progress.
*   Player has the card in hand and sufficient Elixir.

**Postconditions:**
*   Elixir is spent.
*   Card is removed from hand; Entity appears on Arena.
*   Next card from deck enters hand.

**Main Flow (Success Scenario):**
1.  Player selects a card from hand.
2.  Player clicks/drags to a valid tile on the arena.
3.  System verifies sufficient Elixir.
4.  System deducts cost, removes card from hand, spawns entity at location.
5.  System draws the next card into the hand.

**Extensions:**
*   **Insufficient Elixir:** System warns player; no action taken.
*   **Invalid Tile:** System warns player (e.g., troop interaction rules); no action taken.

---

## 5. UC5: Move Troop
**Primary Actor:** Troop (Unit)
**Supporting Actor:** Game Manager

**Description:**
Units move automatically toward targets.

**Preconditions:**
*   Unit is deployed and mobile.

**Postconditions:**
*   Unit moves toward nearest valid target.

**Main Flow (Success Scenario):**
1.  Troop identifies nearest valid enemy target.
2.  If no local enemy, targets enemy Tower.
3.  Troop finds path to nearest Bridge (if crossing river needed).
4.  Troop moves along path continuously.

**Extensions:**
*   **New Target Appears:** Troop re-evaluates nearest target and may change path.
*   **Building-Targeter:** Ignores troops, targets nearest building.
*   **Air Unit:** Flies directly over obstacles.

---

## 6. UC6: Combat System
**Primary Actor:** Troop/Tower
**Supporting Actor:** Game Manager

**Description:**
Units engage in combat when in range.

**Preconditions:**
*   Enemy unit enters attack range.

**Postconditions:**
*   Damage is dealt; Health reduced.
*   Unit destroyed if HP <= 0.

**Main Flow (Success Scenario):**
1.  System detects enemy in range.
2.  Unit stops moving and attacks.
3.  System calculates damage (DMG) vs Health (HP).
4.  Target HP updated (UI health bar reduces).
5.  Unit waits for "Hit Speed" duration.
6.  Repeat until Target HP <= 0.
7.  Target destroyed and removed from Arena.

**Extensions:**
*   **Target Moves Out of Range:** Unit stops attacking and resumes movement.
*   **Simultaneous Death:** Both units removed (Standard rules).

---

## 7. UC7: Win Match
**Primary Actor:** Player
**Supporting Actor:** Game Manager

**Description:**
Winning by destroying the King Tower.

**Preconditions:**
*   Match is in progress.

**Postconditions:**
*   Match ends.
*   Victory/Defeat screen shown.

**Main Flow (Success Scenario):**
1.  Player destroys Opponent's King Tower.
2.  System detects destruction.
3.  System immediately ends match.
4.  System displays Victory screen.

**Extensions:**
*   **Player's King Tower Destroyed:** System displays Defeat screen.
*   **Simultaneous Destruction:** Draw declared.

---

## 8. UC8: Determine Match Outcome (Timeout)
**Primary Actor:** Player
**Supporting Actor:** Game Manager

**Description:**
Determining winner when time expires.

**Preconditions:**
*   3:00 minute timer expires.
*   King Towers still standing.

**Postconditions:**
*   Match ends with result.

**Main Flow (Success Scenario):**
1.  Timer reaches 0:00.
2.  System compares "Crowns" (Destroyed Princess Towers).
3.  System declares winner based on higher crown count.

**Extensions:**
*   **Equal Crowns:** Sudden Death (Time extended/Tower health decay).
    *   First to lose a tower loses match.
*   **Still Equal:** Draw declared.

---

## 9. UC9: Reload Match
**Primary Actor:** Player
**Supporting Actor:** Game Manager

**Description:**
Resuming a previously saved game.

**Preconditions:**
*   Saved match file exists.
*   Player in Main Menu.

**Postconditions:**
*   Match resumes from exact state (Time, HP, Elixir, Hand).

**Main Flow (Success Scenario):**
1.  Player selects "Resume Game".
2.  System shows saved game info.
3.  Player confirms selection.
4.  System loads full state (Arena, Units, Resources).
5.  Match resumes gameplay.

**Extensions:**
*   **Corrupt/Missing Save:** System alerts user and returns to menu.
*   **Player exits:** Cancels load.

---

## 10. UC10: Save Match
**Primary Actor:** Player
**Supporting Actor:** Game Manager

**Description:**
Pausing and saving current progress.

**Preconditions:**
*   Match in progress.

**Postconditions:**
*   Match data serialized to storage.
*   Game paused/exited.

**Main Flow (Success Scenario):**
1.  Player pauses game.
2.  Selects "Save Match".
3.  System captures all state (Entity positions, HP, Deck cycle, Timer).
4.  System writes to storage.
5.  System confirms "Saved Successfully".

**Extensions:**
*   **Match Ended:** Cannot save completed match.
