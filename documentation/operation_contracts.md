# Operation Contracts

## Contract CO1: chooseCard()
*   **Operation:** Cross `chooseCard(selectedCard: Cards)`
*   **References:** Start New Match, Build Deck
*   **Preconditions:**
    *   A Game instance `m` exists and its state is "deck_building".
    *   A Deck instance `pd` is associated with `m`.
    *   The number of Cards currently associated with `pd` is less than 8.
    *   The selected Card is not already associated with `pd`.
*   **Postconditions:**
    *   The selected Card was associated with the `pd` (association formed).
    *   The card count in `pd` was incremented (attribute modification).

## Contract CO2: confirmDeck()
*   **Operation:** Cross `confirmDeck()`
*   **References:** Start New Match, Build Deck
*   **Preconditions:**
    *   A Game instance `m` exists and its state is "deck_building".
    *   A Deck instance `pd` is associated with `m`.
    *   The number of Cards currently associated with `pd` is exactly 8.
    *   All 8 cards in `pd` are valid (no duplicates, all exist in Card Library).
*   **Postconditions:**
    *   The Deck is marked as “ready”.
    *   The association between `m` and `pd` is locked (no further modifications allowed).
    *   The system updates `m.state` from "deck_building" to "ready_to_start".
    *   A confirmation message “Deck saved successfully” is displayed to the player.
    *   The deck configuration is persisted (e.g., saved to file or database).

## Contract CO3: validateLayout()
*   **Operation:** Cross `validateLayout(minFeatures: Map)`
*   **References:** Design arena
*   **Preconditions:**
    *   The player has placed all desired arena elements (towers, bridges, etc.)
    *   The current layout Arena, `a`, is accessible and contains all placed elements with their positions.
    *   `minFeatures` defines the minimum required elements for a valid layout (e.g., 2 Crown Towers, 1 King Tower, at least 1 Bridge).
*   **Postconditions:**
    *   The `a` is checked against the `minFeatures` requirements.
    *   If `a` meets all requirements, it is marked as valid and can be saved.
    *   If `a` fails any requirement, a list of missing or invalid items is returned.
    *   The system is ready to either save `a` or return feedback to the player.

## Contract CO4: showDeployedEntity()
*   **Operation:** Cross `showDeployedEntity(Card: card, Position: tile)`
*   **References:** Deploy Card
*   **Preconditions:**
    *   A Card instance `card` has been played.
    *   A Position instance `tile` has been selected for deployment.
    *   `Card.cost` compared with player’s `elixir_amount`, and `elixir_amount` validated.
    *   By checking the Arena, the `tile` was validated.
*   **Postconditions:**
    *   The corresponding Troop instance `troop` created. (instance creation.)
    *   `Card.cost` amount of elixir deducted from `elixir_amount`. (attribute modification.)

## Contract CO5: findNearestTarget()
*   **Operation:** Cross `findNearestTarget(troop: Troop)`
*   **References:** Move Troop, Combat System
*   **Preconditions:**
    *   A Troop instance `troop` exists and is currently registered in the game manager system.
    *   The troop is active in the arena and its position is known.
    *   The Arena contains potential enemies (targets).
*   **Postconditions:**
    *   The nearest valid target has been identified (calculation performed).
    *   A reference to the identified target was returned to the calling troop.
    *   No modification has been done to the troop's position or state.
    *   If there is no valid target, “no target” response is returned.

## Contract CO6: saveMatchData()
*   **Operation:** Cross `saveMatchData()`
*   **References:** Save Match
*   **Preconditions:**
    *   A Game instance `m` exists and its state is "in_progress" or "paused".
    *   A Player instance `p` is associated with `m` as the human player.
    *   A Opponent instance `co` is associated with `m`.
    *   Arena instance `a` for `m` is accessible and contains current arena data (towers HP, units, positions, spawners).
    *   Timer/elixir/deck states for both `p` and `co` are available in memory.
*   **Postconditions:**
    *   A SavedGame instance `sm` was created or updated for match `m` (instance creation / update).
    *   `sm` now stores:
        *   `m.timerState`
        *   `p.elixirState`, `p.deckState` (including cards in hand)
        *   `co.elixirState`, `co.deckState`
        *   `a.state` (tower health, troop positions/HP, spawner state)
    *   `sm` is written to storage
    *   `m.state` remains "paused" or is set to "paused"
    *   A confirmation message “Match saved successfully” is displayed to `p`.

## Contract CO7: detectKingTowerDestruction()
*   **Operation:** Cross `detectKingTowerDestruction()`
*   **References:** Win Match (By Destroying the King Tower)
*   **Preconditions:**
    *   A Game instance `m` exists and its state is "in_progress"
    *   An Arena instance `a` is associated with `m` and tracks all towers’ current HP.
    *   At least one King Tower (player’s or opponent’s) in `a` has HP ≤ 0, or an internal arena event has been raised indicating “KingTowerDestroyed”
    *   No terminal outcome has yet been recorded for `m`.
*   **Postconditions:**
    *   The side (player or opponent) whose King Tower reached HP ≤ 0 was identified.
    *   A King Tower Destruction event/object is created in the game context for `m`.

## Contract CO8: determineOutcome()
*   **Operation:** Cross `determineOutcome()`
*   **References:** Determine Match Outcome After Timeout
*   **Preconditions:**
    *   A Game instance `m` exists and timer expired.
    *   Arena instance `a` for `m` is accessible and contains crown tower data for both sides.
    *   `m` has not already been assigned a final winner.
*   **Postconditions:**
    *   The system compared player’s destroyed tower count with opponent’s destroyed-tower count (calculation performed)
    *   If `playerDestroyedTowers > opponentDestroyedTowers` → `m.winner` was set to player.
    *   Else if `opponentDestroyedTowers > playerDestroyedTowers` → `m.winner` was set to opponent.
    *   Else (the number of destroyed towers is the same): The system compared the remaining tower health of player vs. opponent. If `player’s remaining tower health > opponent’s` then `m.winner` was set to player. Else if `opponent’s remaining tower health > player’s` then `m.winner` was set to opponent.
    *   `m.state` was set from "in_progress" to "ended"

## Contract CO9: loadMatchData()
*   **Operation:** Cross `loadMatchData(selectedMatchID: Game.ID)`
*   **References:** Reload Match
*   **Preconditions:**
    *   A Game instance exists in the system and its state is “saved”.
    *   There is a Player instance and the player is in the “Main Menu” state currently.
    *   The saved match data (arena, deck, elixir, time etc.) is valid and not corrupted.
    *   There is at least one saved match record accessible in the system’s save directory.
*   **Postconditions:**
    *   A new Game instance `m` is created based on the selected saved match. (object creation)
    *   The attributes of `m` (MatchStatus, Timer, Tower HP etc.) are restored from the saved data. (attribute modification)
    *   The game status `Game.MatchStatus` has changed from “saved” to “in_progress”. (state change)
    *   The UI is refreshed to display the restored match state (arena visuals, tower HP, timer bar, etc.). (system update)
    *   The progress bar reaches 100%, and control is handed back to the player to continue gameplay.

## Contract CO10: moveAlongPath()
*   **Operation:** `moveAlongPath()`
*   **References:** Move Troop, Combat System
*   **Preconditions:**
    *   A valid Game instance exists and its state is "in_progress".
    *   `troop` is active and present in the Arena (`troop.remainHp > 0`).
    *   A valid path is already calculated for the troop: `troop.currentPath ≠ null`.
    *   The troop is currently in "move" state.
    *   No higher priority action (like combat engagement or death) interrupts movement.
*   **Postconditions:**
    *   The troop’s position is updated to the next position along the current path. (attribute modification)
    *   The troop orientation is adjusted to face the new movement direction. (state refinement)
    *   The system checks if the troop reached the final destination. If destination is reached, troop transitions to "idle" or "engage target" state depending on context. (state transition)
    *   The updated troop position is reflected in the UI and Arena. (system update & association update)
    *   If the path becomes invalid due to new obstacle or destroyed targets, troop requests a new path. (conditional behavior update)
