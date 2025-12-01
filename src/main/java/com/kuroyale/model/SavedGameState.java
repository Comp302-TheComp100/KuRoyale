package com.kuroyale.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a saved game state that can be persisted and reloaded.
 * Contains all necessary data to restore a match from where it was saved.
 */
public class SavedGameState implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String saveId;
    private final LocalDateTime saveTime;
    private final String playerUsername;
    
    // Game time and state
    private final double gameTime;
    private final boolean isDoubleElixir;
    private final int playerScore;
    private final int botScore;
    
    // Player state
    private final double playerElixir;
    private final List<String> playerDeckCards;
    private final List<String> playerHandCards;
    private final List<String> playerDrawPileCards;
    
    // Bot state
    private final double botElixir;
    private final List<String> botDeckCards;
    private final List<String> botHandCards;
    private final List<String> botDrawPileCards;
    
    // Arena state
    private final ArenaLayout arenaLayout;
    private final List<SavedTower> towers;
    
    // Active units on field
    private final List<SavedTroop> activeTroops;
    private final List<SavedBuilding> activeBuildings;
    
    /**
     * Constructor for creating a saved game state
     */
    public SavedGameState(String playerUsername, double gameTime, boolean isDoubleElixir,
                         int playerScore, int botScore,
                         double playerElixir, List<String> playerDeckCards, 
                         List<String> playerHandCards, List<String> playerDrawPileCards,
                         double botElixir, List<String> botDeckCards,
                         List<String> botHandCards, List<String> botDrawPileCards,
                         ArenaLayout arenaLayout, List<SavedTower> towers,
                         List<SavedTroop> activeTroops, List<SavedBuilding> activeBuildings) {
        this.saveId = UUID.randomUUID().toString();
        this.saveTime = LocalDateTime.now();
        this.playerUsername = playerUsername;
        this.gameTime = gameTime;
        this.isDoubleElixir = isDoubleElixir;
        this.playerScore = playerScore;
        this.botScore = botScore;
        this.playerElixir = playerElixir;
        this.playerDeckCards = new ArrayList<>(playerDeckCards);
        this.playerHandCards = new ArrayList<>(playerHandCards);
        this.playerDrawPileCards = new ArrayList<>(playerDrawPileCards);
        this.botElixir = botElixir;
        this.botDeckCards = new ArrayList<>(botDeckCards);
        this.botHandCards = new ArrayList<>(botHandCards);
        this.botDrawPileCards = new ArrayList<>(botDrawPileCards);
        this.arenaLayout = arenaLayout;
        this.towers = new ArrayList<>(towers);
        this.activeTroops = new ArrayList<>(activeTroops);
        this.activeBuildings = new ArrayList<>(activeBuildings);
    }
    
    // Getters
    public String getSaveId() { return saveId; }
    public LocalDateTime getSaveTime() { return saveTime; }
    public String getPlayerUsername() { return playerUsername; }
    public double getGameTime() { return gameTime; }
    public boolean isDoubleElixir() { return isDoubleElixir; }
    public int getPlayerScore() { return playerScore; }
    public int getBotScore() { return botScore; }
    public double getPlayerElixir() { return playerElixir; }
    public List<String> getPlayerDeckCards() { return new ArrayList<>(playerDeckCards); }
    public List<String> getPlayerHandCards() { return new ArrayList<>(playerHandCards); }
    public List<String> getPlayerDrawPileCards() { return new ArrayList<>(playerDrawPileCards); }
    public double getBotElixir() { return botElixir; }
    public List<String> getBotDeckCards() { return new ArrayList<>(botDeckCards); }
    public List<String> getBotHandCards() { return new ArrayList<>(botHandCards); }
    public List<String> getBotDrawPileCards() { return new ArrayList<>(botDrawPileCards); }
    public ArenaLayout getArenaLayout() { return arenaLayout; }
    public List<SavedTower> getTowers() { return new ArrayList<>(towers); }
    public List<SavedTroop> getActiveTroops() { return new ArrayList<>(activeTroops); }
    public List<SavedBuilding> getActiveBuildings() { return new ArrayList<>(activeBuildings); }
    
    /**
     * Gets a formatted string showing the time remaining
     */
    public String getFormattedTimeRemaining() {
        int minutes = (int) gameTime / 60;
        int seconds = (int) gameTime % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Gets a formatted string showing the save date/time
     */
    public String getFormattedSaveTime() {
        return saveTime.toString().replace("T", " ").substring(0, 19);
    }
    
    /**
     * Represents a saved tower state
     */
    public static class SavedTower implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String towerType; // "KING" or "PRINCESS"
        private final boolean isPlayerSide;
        private final int currentHealth;
        private final int maxHealth;
        private final int gridX;
        private final int gridY;
        
        public SavedTower(String towerType, boolean isPlayerSide, int currentHealth, 
                         int maxHealth, int gridX, int gridY) {
            this.towerType = towerType;
            this.isPlayerSide = isPlayerSide;
            this.currentHealth = currentHealth;
            this.maxHealth = maxHealth;
            this.gridX = gridX;
            this.gridY = gridY;
        }
        
        public String getTowerType() { return towerType; }
        public boolean isPlayerSide() { return isPlayerSide; }
        public int getCurrentHealth() { return currentHealth; }
        public int getMaxHealth() { return maxHealth; }
        public int getGridX() { return gridX; }
        public int getGridY() { return gridY; }
    }
    
    /**
     * Represents a saved troop state
     */
    public static class SavedTroop implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String cardName;
        private final boolean isPlayerSide;
        private final int currentHealth;
        private final int gridX;
        private final int gridY;
        private final String state; // "MOVING" or "ATTACKING"
        
        public SavedTroop(String cardName, boolean isPlayerSide, int currentHealth,
                         int gridX, int gridY, String state) {
            this.cardName = cardName;
            this.isPlayerSide = isPlayerSide;
            this.currentHealth = currentHealth;
            this.gridX = gridX;
            this.gridY = gridY;
            this.state = state;
        }
        
        public String getCardName() { return cardName; }
        public boolean isPlayerSide() { return isPlayerSide; }
        public int getCurrentHealth() { return currentHealth; }
        public int getGridX() { return gridX; }
        public int getGridY() { return gridY; }
        public String getState() { return state; }
    }
    
    /**
     * Represents a saved building state
     */
    public static class SavedBuilding implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String cardName;
        private final boolean isPlayerSide;
        private final int currentHealth;
        private final int gridX;
        private final int gridY;
        private final int width;
        private final int height;
        private final double remainingLifetime;
        
        public SavedBuilding(String cardName, boolean isPlayerSide, int currentHealth,
                            int gridX, int gridY, int width, int height, double remainingLifetime) {
            this.cardName = cardName;
            this.isPlayerSide = isPlayerSide;
            this.currentHealth = currentHealth;
            this.gridX = gridX;
            this.gridY = gridY;
            this.width = width;
            this.height = height;
            this.remainingLifetime = remainingLifetime;
        }
        
        public String getCardName() { return cardName; }
        public boolean isPlayerSide() { return isPlayerSide; }
        public int getCurrentHealth() { return currentHealth; }
        public int getGridX() { return gridX; }
        public int getGridY() { return gridY; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public double getRemainingLifetime() { return remainingLifetime; }
    }
}

