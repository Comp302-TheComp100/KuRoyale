package com.kuroyale.model.state;

import com.kuroyale.model.arena.Arena;
import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.ElixirManager;
import com.kuroyale.model.logic.TurnManager;
import com.kuroyale.event.GameEventBus;

import java.util.Set;
import java.util.HashSet;

/* Game state for Local Player vs Player battles.
 * Replaces BotLogic with second human player controls.
 * Uses TurnManager for turn-based gameplay.*/
public class PvPGameState extends AbstractGameState {

    // Player 1 (left side - bottom half of arena)
    private final Hand player1Hand, player2Hand;
    private final ElixirManager player1Elixir, player2Elixir;

    private final TurnManager turnManager;

    private int player1Score = 0, player2Score = 0;
    private TurnManager.Turn winner = null;

    private Set<Tower> scoredTowers = new HashSet<>();

    // Combo Service specific to PvP? Or should be general?
    // Original had it here.
    private final com.kuroyale.service.battle.logic.ComboService comboService = new com.kuroyale.service.battle.logic.ComboService();

    public PvPGameState(Deck player1Deck, Deck player2Deck, Arena arena) {
        super(arena);
        this.player1Hand = new Hand(player1Deck);
        this.player1Elixir = new ElixirManager();

        this.player2Hand = new Hand(player2Deck);
        this.player2Elixir = new ElixirManager();

        this.turnManager = new TurnManager();

        // Initialize Combo Service
        this.comboService.setGameState(this);
    }

    public com.kuroyale.service.battle.logic.ComboService getComboService() {
        return comboService;
    }

    public void cleanup() {
        if (comboService != null) {
            comboService.cleanup();
        }
    }

    @Override
    public void update(double deltaTime) {
        updateGameTimer(deltaTime);

        // Both players' elixir regenerates simultaneously (not turn-based)
        player1Elixir.update(deltaTime);
        player2Elixir.update(deltaTime);

        super.updateEntities(deltaTime);
        super.handleCombat(deltaTime);
        super.cleanupEntities();
        checkWinConditions();
    }

    private void updateGameTimer(double deltaTime) {
        // Handle tiebreaker mode: all towers drain health until one reaches 0
        if (isTiebreakerMode) {
            super.updateTiebreakerMode(deltaTime);
            return;
        }

        if (gameTime > 0) {
            gameTime -= deltaTime;

            // Double Elixir in last 60 seconds
            if (gameTime <= 60.0 && !isDoubleElixir) {
                isDoubleElixir = true;
                player1Elixir.setDoubleElixir(true);
                player2Elixir.setDoubleElixir(true);
            }

            if (gameTime <= 0) {
                gameTime = 0;
                if (!isGameOver) {
                    if (player1Score > player2Score) {
                        isGameOver = true;
                        winner = TurnManager.Turn.PLAYER_1;
                    } else if (player2Score > player1Score) {
                        isGameOver = true;
                        winner = TurnManager.Turn.PLAYER_2;
                    } else {
                        // Equal scores: Enter tiebreaker mode
                        // All remaining towers start losing health rapidly
                        isTiebreakerMode = true;
                        // Clear all troops and buildings so they don't affect tiebreaker
                        clearArenaUnits();
                    }
                }
            }
        }
    }

    @Override
    protected void checkTiebreakerWinCondition() {
        // Count all towers that reached 0 health and score them
        int player1TowersDied = 0;
        int player2TowersDied = 0;
        boolean anyKingDied = false;
        boolean player1KingDied = false;
        boolean player2KingDied = false;

        for (Tower tower : arena.getAllTowers()) {
            if (tower.getCurrentHealth() <= 0) {
                boolean isPlayer1Tower = tower.isPlayerSide();
                boolean isKingTower = tower.getType() == Tower.TowerType.KING;

                if (isKingTower) {
                    anyKingDied = true;
                    if (isPlayer1Tower) {
                        player1KingDied = true;
                    } else {
                        player2KingDied = true;
                    }
                }

                if (isPlayer1Tower) {
                    player1TowersDied++;
                } else {
                    player2TowersDied++;
                }
            }
        }

        // If any towers died, end the game
        if (player1TowersDied > 0 || player2TowersDied > 0) {
            // Award crowns based on what died
            if (anyKingDied) {
                // King tower death = 3 crowns
                if (player1KingDied) {
                    player2Score = 3;
                }
                if (player2KingDied) {
                    player1Score = 3;
                }
            } else {
                // Princess towers = 1 crown each
                player2Score += player1TowersDied;
                player1Score += player2TowersDied;
            }

            // End the game
            isGameOver = true;

            // Determine winner based on final scores
            if (player1Score > player2Score) {
                winner = TurnManager.Turn.PLAYER_1;
            } else if (player2Score > player1Score) {
                winner = TurnManager.Turn.PLAYER_2;
            } else {
                // Still tied after simultaneous deaths = draw
                winner = null;
            }
        }
    }

    private void checkWinConditions() {
        if (!isGameOver) {
            boolean player1KingAlive = arena.isPlayerKingAlive();
            boolean player2KingAlive = arena.isBotKingAlive(); // "Bot" side is Player 2

            if (!player1KingAlive) {
                isGameOver = true;
                winner = TurnManager.Turn.PLAYER_2;
            } else if (!player2KingAlive) {
                isGameOver = true;
                winner = TurnManager.Turn.PLAYER_1;
            }
        }
    }

    // Place a card for a player. In PvP, we check if it's the player's turn.
    public boolean placeCard(boolean isPlayer1, int handIndex, int x, int y) {
        Hand hand = isPlayer1 ? player1Hand : player2Hand;
        ElixirManager elixir = isPlayer1 ? player1Elixir : player2Elixir;

        // Validate position bounds
        if (x < 0 || x >= Arena.WIDTH || y < 0 || y >= Arena.HEIGHT) {
            return false;
        }

        Card card = hand.getCard(handIndex);
        if (card == null)
            return false;

        boolean isSpell = card.getType() == CardType.SPELL;

        // Validate terrain
        if (!isSpell && !arena.getCell(x, y).canPlaceUnit()) {
            return false;
        }

        // Validate side: Player 1 bottom half (y >= HEIGHT/2), Player 2 top half (y <
        // HEIGHT/2)
        if (isPlayer1 && !isSpell && y < Arena.HEIGHT / 2) {
            return false;
        }
        if (!isPlayer1 && !isSpell && y >= Arena.HEIGHT / 2) {
            return false;
        }

        int cost = card.getCost();
        if (elixir.getCurrentElixir() >= cost) {
            java.util.List<ICombatant> spawned = spawnUnit(isPlayer1, card, x, y);

            if (spawned != null) {
                elixir.spend(cost);
                hand.playCard(handIndex);
                GameEventBus.getInstance().publishElixirSpent(isPlayer1, cost);
                return true;
            }
        }

        return false;
    }

    @Override
    protected void checkAndScoreDestroyedTowers() {
        Set<Tower> towers = arena.getAllTowers();

        for (Tower tower : towers) {
            if (scoredTowers.contains(tower) || tower.isAlive())
                continue;

            scoredTowers.add(tower);

            boolean isPlayer1Tower = tower.isPlayerSide();
            boolean isKingTower = tower.getType() == Tower.TowerType.KING;

            if (isKingTower) {
                if (isPlayer1Tower) {
                    player2Score = 3;
                    isGameOver = true;
                    winner = TurnManager.Turn.PLAYER_2;
                } else {
                    player1Score = 3;
                    isGameOver = true;
                    winner = TurnManager.Turn.PLAYER_1;
                }
            } else {
                if (isPlayer1Tower) {
                    player2Score++;
                } else {
                    player1Score++;
                }
            }

            GameEventBus.getInstance().publishTowerDestroyed(isPlayer1Tower, tower);
        }
    }

    // Getters
    public Hand getPlayer1Hand() {
        return player1Hand;
    }

    public Hand getPlayer2Hand() {
        return player2Hand;
    }

    public ElixirManager getPlayer1Elixir() {
        return player1Elixir;
    }

    public ElixirManager getPlayer2Elixir() {
        return player2Elixir;
    }

    public TurnManager getTurnManager() {
        return turnManager;
    }

    @Override
    public ElixirManager getElixirManager(boolean isPlayer1) {
        return isPlayer1 ? player1Elixir : player2Elixir;
    }

    // Arena, GameTime, isDoubleElixir, isGameOver, isTiebreakerMode getters are now
    // inherited from AbstractGameState

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public TurnManager.Turn getWinner() {
        return winner;
    }

}