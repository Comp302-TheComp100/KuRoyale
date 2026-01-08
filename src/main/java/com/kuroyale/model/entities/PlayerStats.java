package com.kuroyale.model.entities;

/*Tracks lifetime player statistics for achievements and quests.
 * Persisted to disk to maintain progress across sessions.*/
public class PlayerStats {
    // Match stats
    private int totalMatches;
    private int totalWins;
    private int currentWinStreak;
    private int bestWinStreak;

    // Tower stats
    private int towersDestroyed;
    private int kingTowersDestroyed;

    // Card stats
    private int spellsPlayed;
    private int troopsDeployed;
    private int buildingsPlayed;
    private int swarmTroopsDeployed;
    private int cardsPlayedInCurrentMatch;

    // Damage stats
    private int totalSpellDamage;
    private int totalElixirSpent;

    // Challenge stats
    private int challengesCompleted;
    private boolean hasThreeStarChallenge;

    // Multiplayer stats
    private int networkMatchesWon;
    private int pvpMatchesWon;

    // Economy stats
    private int totalGoldEarned;

    // Combo stats
    private int combosTriggered;

    public PlayerStats() {
        // All fields default to 0/false
    }

    // Match stats
    public int getTotalMatches() {
        return totalMatches;
    }

    public void incrementTotalMatches() {
        totalMatches++;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public void incrementTotalWins() {
        totalWins++;
        currentWinStreak++;
        if (currentWinStreak > bestWinStreak) {
            bestWinStreak = currentWinStreak;
        }
    }

    public void recordLoss() {
        currentWinStreak = 0;
    }

    public int getCurrentWinStreak() {
        return currentWinStreak;
    }

    public int getBestWinStreak() {
        return bestWinStreak;
    }

    // Tower stats
    public int getTowersDestroyed() {
        return towersDestroyed;
    }

    public void addTowersDestroyed(int count) {
        towersDestroyed += count;
    }

    public int getKingTowersDestroyed() {
        return kingTowersDestroyed;
    }

    public void incrementKingTowersDestroyed() {
        kingTowersDestroyed++;
    }

    // Card stats
    public int getSpellsPlayed() {
        return spellsPlayed;
    }

    public void incrementSpellsPlayed() {
        spellsPlayed++;
    }

    public int getTroopsDeployed() {
        return troopsDeployed;
    }

    public void addTroopsDeployed(int count) {
        troopsDeployed += count;
    }

    public int getBuildingsPlayed() {
        return buildingsPlayed;
    }

    public void incrementBuildingsPlayed() {
        buildingsPlayed++;
    }

    public int getSwarmTroopsDeployed() {
        return swarmTroopsDeployed;
    }

    public void addSwarmTroopsDeployed(int count) {
        swarmTroopsDeployed += count;
    }

    public int getCardsPlayedInCurrentMatch() {
        return cardsPlayedInCurrentMatch;
    }

    public void incrementCardsPlayedInCurrentMatch() {
        cardsPlayedInCurrentMatch++;
    }

    public void resetCardsPlayedInCurrentMatch() {
        cardsPlayedInCurrentMatch = 0;
    }

    // Damage stats
    public int getTotalSpellDamage() {
        return totalSpellDamage;
    }

    public void addSpellDamage(int damage) {
        totalSpellDamage += damage;
    }

    public int getTotalElixirSpent() {
        return totalElixirSpent;
    }

    public void addElixirSpent(int elixir) {
        totalElixirSpent += elixir;
    }

    // Challenge stats
    public int getChallengesCompleted() {
        return challengesCompleted;
    }

    public void incrementChallengesCompleted() {
        challengesCompleted++;
    }

    public boolean hasThreeStarChallenge() {
        return hasThreeStarChallenge;
    }

    public void setHasThreeStarChallenge(boolean value) {
        hasThreeStarChallenge = value;
    }

    // Multiplayer stats
    public int getNetworkMatchesWon() {
        return networkMatchesWon;
    }

    public void incrementNetworkMatchesWon() {
        networkMatchesWon++;
    }

    public int getPvpMatchesWon() {
        return pvpMatchesWon;
    }

    public void incrementPvpMatchesWon() {
        pvpMatchesWon++;
    }

    // Economy stats
    public int getTotalGoldEarned() {
        return totalGoldEarned;
    }

    public void addGoldEarned(int gold) {
        totalGoldEarned += gold;
    }

    // Combo stats
    public int getCombosTriggered() {
        return combosTriggered;
    }

    public void incrementCombosTriggered() {
        combosTriggered++;
    }

    // Setters for JSON deserialization
    public void setTotalMatches(int v) {
        totalMatches = v;
    }

    public void setTotalWins(int v) {
        totalWins = v;
    }

    public void setCurrentWinStreak(int v) {
        currentWinStreak = v;
    }

    public void setBestWinStreak(int v) {
        bestWinStreak = v;
    }

    public void setTowersDestroyed(int v) {
        towersDestroyed = v;
    }

    public void setKingTowersDestroyed(int v) {
        kingTowersDestroyed = v;
    }

    public void setSpellsPlayed(int v) {
        spellsPlayed = v;
    }

    public void setTroopsDeployed(int v) {
        troopsDeployed = v;
    }

    public void setBuildingsPlayed(int v) {
        buildingsPlayed = v;
    }

    public void setSwarmTroopsDeployed(int v) {
        swarmTroopsDeployed = v;
    }

    public void setTotalSpellDamage(int v) {
        totalSpellDamage = v;
    }

    public void setTotalElixirSpent(int v) {
        totalElixirSpent = v;
    }

    public void setChallengesCompleted(int v) {
        challengesCompleted = v;
    }

    public void setNetworkMatchesWon(int v) {
        networkMatchesWon = v;
    }

    public void setPvpMatchesWon(int v) {
        pvpMatchesWon = v;
    }

    public void setTotalGoldEarned(int v) {
        totalGoldEarned = v;
    }

    public void setCombosTriggered(int v) {
        combosTriggered = v;
    }
}
