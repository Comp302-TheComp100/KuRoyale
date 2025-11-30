package com.kuroyale.model;

public class Building {
    private final GridPosition position; // top-left grid position
    private final int width; // tiles
    private final int height; // tiles
    private final boolean playerSide; // owner
    private double maxHealth;
    private double currentHealth;
    private final String imagePath; // optional image path from card

    public Building(GridPosition position, int width, int height, boolean playerSide, double maxHealth, String imagePath) {
        this.position = position;
        this.width = width;
        this.height = height;
        this.playerSide = playerSide;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.imagePath = imagePath;
    }

    public GridPosition getPosition() { return position; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public boolean isPlayerSide() { return playerSide; }
    public double getMaxHealth() { return maxHealth; }
    public double getCurrentHealth() { return currentHealth; }
    public String getImagePath() { return imagePath; }

    public void takeDamage(double amount) {
        currentHealth = Math.max(0, currentHealth - amount);
    }

    public boolean isAlive() { return currentHealth > 0; }
}
