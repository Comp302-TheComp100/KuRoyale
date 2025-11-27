package com.kuroyale.model;

/**
 * Manages elixir state and regeneration.
 * Implements 2x Elixir mode (1 elixir every 1.4 seconds).
 */
public class ElixirManager {
    public static final double MAX_ELIXIR = 10.0;
    public static final double STARTING_ELIXIR = 5.0;

    // Standard is 2.8s per elixir. 2x mode is 1.4s per elixir.
    // Rate = 1 elixir / 1.4 seconds = ~0.714 elixir per second
    private static final double REGENERATION_RATE = 1.0 / 1.4;

    private double currentElixir;

    public ElixirManager() {
        this.currentElixir = STARTING_ELIXIR;
    }

    /**
     * Updates elixir based on time passed.
     * 
     * @param deltaTime Time passed in seconds
     */
    public void update(double deltaTime) {
        if (currentElixir < MAX_ELIXIR) {
            currentElixir += REGENERATION_RATE * deltaTime;
            if (currentElixir > MAX_ELIXIR) {
                currentElixir = MAX_ELIXIR;
            }
        }
    }

    /**
     * Checks if enough elixir is available.
     * 
     * @param cost Cost to check
     * @return true if affordable
     */
    public boolean canAfford(int cost) {
        return currentElixir >= cost;
    }

    /**
     * Spends elixir if available.
     * 
     * @param cost Amount to spend
     * @return true if spent successfully, false if not enough
     */
    public boolean spend(int cost) {
        if (canAfford(cost)) {
            currentElixir -= cost;
            return true;
        }
        return false;
    }

    public int getCurrentElixirInt() {
        return (int) currentElixir;
    }

    public double getCurrentElixir() {
        return currentElixir;
    }
}
