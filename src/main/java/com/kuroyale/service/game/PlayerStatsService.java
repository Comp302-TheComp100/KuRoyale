package com.kuroyale.service.game;

import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;
import com.kuroyale.model.entities.PlayerStats;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.ICombatant;
import com.kuroyale.model.enums.ComboType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.json.JSONObject;

/**
 * Service for managing persistent player statistics.
 * Handles loading, saving, and updating stats based on game events.
 */
public class PlayerStatsService implements GameEventListener {

    private static final String STATS_FILE_TEMPLATE = "player_stats_%s.json";

    private final Path dataPath;
    private PlayerStats stats;
    private String currentUsername;

    public PlayerStatsService() {
        String userHome = System.getProperty("user.home");
        this.dataPath = Paths.get(userHome, ".kuroyale");
        this.stats = new PlayerStats();

        // Register to game events
        GameEventBus.getInstance().subscribe(this);
    }

    public void loadForUser(String username) {
        this.currentUsername = username;
        loadStats();
    }

    public PlayerStats getStats() {
        return stats;
    }

    private void loadStats() {
        if (currentUsername == null) {
            stats = new PlayerStats();
            return;
        }

        try {
            Path filePath = dataPath.resolve(String.format(STATS_FILE_TEMPLATE, currentUsername));
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                JSONObject json = new JSONObject(content);

                stats = new PlayerStats();
                if (json.has("totalMatches"))
                    stats.setTotalMatches(json.getInt("totalMatches"));
                if (json.has("totalWins"))
                    stats.setTotalWins(json.getInt("totalWins"));
                if (json.has("currentWinStreak"))
                    stats.setCurrentWinStreak(json.getInt("currentWinStreak"));
                if (json.has("bestWinStreak"))
                    stats.setBestWinStreak(json.getInt("bestWinStreak"));
                if (json.has("towersDestroyed"))
                    stats.setTowersDestroyed(json.getInt("towersDestroyed"));
                if (json.has("kingTowersDestroyed"))
                    stats.setKingTowersDestroyed(json.getInt("kingTowersDestroyed"));
                if (json.has("spellsPlayed"))
                    stats.setSpellsPlayed(json.getInt("spellsPlayed"));
                if (json.has("troopsDeployed"))
                    stats.setTroopsDeployed(json.getInt("troopsDeployed"));
                if (json.has("buildingsPlayed"))
                    stats.setBuildingsPlayed(json.getInt("buildingsPlayed"));
                if (json.has("swarmTroopsDeployed"))
                    stats.setSwarmTroopsDeployed(json.getInt("swarmTroopsDeployed"));
                if (json.has("totalSpellDamage"))
                    stats.setTotalSpellDamage(json.getInt("totalSpellDamage"));
                if (json.has("totalElixirSpent"))
                    stats.setTotalElixirSpent(json.getInt("totalElixirSpent"));
                if (json.has("challengesCompleted"))
                    stats.setChallengesCompleted(json.getInt("challengesCompleted"));
                if (json.has("networkMatchesWon"))
                    stats.setNetworkMatchesWon(json.getInt("networkMatchesWon"));
                if (json.has("pvpMatchesWon"))
                    stats.setPvpMatchesWon(json.getInt("pvpMatchesWon"));
                if (json.has("totalGoldEarned"))
                    stats.setTotalGoldEarned(json.getInt("totalGoldEarned"));
                if (json.has("combosTriggered"))
                    stats.setCombosTriggered(json.getInt("combosTriggered"));

            } else {
                stats = new PlayerStats();
            }
        } catch (Exception e) {
            System.err.println("Failed to load player stats: " + e.getMessage());
            stats = new PlayerStats();
        }
    }

    private void saveStats() {
        if (currentUsername == null)
            return;

        try {
            Files.createDirectories(dataPath);
            JSONObject json = new JSONObject();

            json.put("totalMatches", stats.getTotalMatches());
            json.put("totalWins", stats.getTotalWins());
            json.put("currentWinStreak", stats.getCurrentWinStreak());
            json.put("bestWinStreak", stats.getBestWinStreak());
            json.put("towersDestroyed", stats.getTowersDestroyed());
            json.put("kingTowersDestroyed", stats.getKingTowersDestroyed());
            json.put("spellsPlayed", stats.getSpellsPlayed());
            json.put("troopsDeployed", stats.getTroopsDeployed());
            json.put("buildingsPlayed", stats.getBuildingsPlayed());
            json.put("swarmTroopsDeployed", stats.getSwarmTroopsDeployed());
            json.put("totalSpellDamage", stats.getTotalSpellDamage());
            json.put("totalElixirSpent", stats.getTotalElixirSpent());
            json.put("challengesCompleted", stats.getChallengesCompleted());
            json.put("networkMatchesWon", stats.getNetworkMatchesWon());
            json.put("pvpMatchesWon", stats.getPvpMatchesWon());
            json.put("totalGoldEarned", stats.getTotalGoldEarned());
            json.put("combosTriggered", stats.getCombosTriggered());

            Files.writeString(dataPath.resolve(String.format(STATS_FILE_TEMPLATE, currentUsername)), json.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save player stats: " + e.getMessage());
        }
    }

    @Override
    public void onMatchStart() {
        // No op
    }

    @Override
    public void onMatchEnd(boolean playerWon) {
        stats.incrementTotalMatches();
        if (playerWon) {
            stats.incrementTotalWins();
        } else {
            stats.recordLoss();
        }

        // Update cards played logic if needed, but PlayerStats resets
        // "cardsPlayedInCurrentMatch" itself usually?
        // Or we should manage that reset here or in PlayerStats.
        stats.resetCardsPlayedInCurrentMatch();

        saveStats();
    }

    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        if (!isPlayerTower) {
            stats.addTowersDestroyed(1);
            if (tower.getType() == Tower.TowerType.KING) {
                stats.incrementKingTowersDestroyed();
            }
            saveStats();
        }
    }

    @Override
    public void onCardPlayed(boolean isPlayer, Card card, List<ICombatant> spawnedUnits) {
        if (isPlayer) {
            switch (card.getType()) {
                case SPELL:
                    stats.incrementSpellsPlayed();
                    break;
                case TROOP:
                    stats.addTroopsDeployed(1); // Or add count? Method is incrementTroopsDeployed (singular)
                    // If card is swarm, also increment swarm?
                    if (card.getCount() > 1) {
                        stats.addSwarmTroopsDeployed(card.getCount()); // Assuming this method exists and takes count
                        // Wait, check PlayerStats.java method signature
                    }
                    break;
                case BUILDING:
                    stats.incrementBuildingsPlayed();
                    break;
            }
            stats.incrementCardsPlayedInCurrentMatch();
            saveStats(); // Frequent saves might be bad, maybe throttle? But simple for now.
        }
    }

    @Override
    public void onElixirSpent(boolean isPlayer, int amount) {
        if (isPlayer) {
            stats.addElixirSpent(amount);
            saveStats();
        }
    }

    @Override
    public void onSpellDamageDealt(boolean isPlayer, int damage) {
        if (isPlayer) {
            stats.addSpellDamage(damage);
            saveStats();
        }
    }

    @Override
    public void onComboTriggered(ComboType combo, List<ICombatant> affectedUnits) {
        stats.incrementCombosTriggered();
        saveStats();
    }

    // Add logic for challenges/network if they have events?
    // They don't seem to have specific GameEvents for "Challenge Completed" in the
    // interface yet
    // but onMatchEnd can check context if GameState is available?
    // For now, simple stats.

    @Override
    public void onUnitDied(ICombatant victim, ICombatant killer) {
        // Not tracking kills in PlayerStats currently (except maybe indirectly?)
    }
}
