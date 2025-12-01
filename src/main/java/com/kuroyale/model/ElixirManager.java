package com.kuroyale.model;

/*Manages elixir state and regeneration.
 * Implements 2x Elixir mode (1 elixir every 1.4 seconds).*/
public class ElixirManager {
    public static final double MAX_ELIXIR = 10.0;
    public static final double STARTING_ELIXIR = 5.0;

    // Standard is 2.8s per elixir. 2x mode is 1.4s per elixir.
    // Rate = 1 elixir / 1.4 seconds = ~0.714 elixir per second
    private static final double REGENERATION_RATE_NORMAL = 1.0 / 2.8;
    private static final double REGENERATION_RATE_DOUBLE = 1.0 / 1.4;

    private double currentElixir;
    private boolean doubleElixirActive = false;

    public ElixirManager() {
        this.currentElixir = STARTING_ELIXIR;
    }

    public void setDoubleElixir(boolean active) {
        this.doubleElixirActive = active;
    }

    public boolean isDoubleElixir() {
        return doubleElixirActive;
    }

    //Updates elixir based on time passed.
    public void update(double deltaTime) {
        if (currentElixir < MAX_ELIXIR) {
            double rate = doubleElixirActive ? REGENERATION_RATE_DOUBLE : REGENERATION_RATE_NORMAL;
            currentElixir += rate * deltaTime;
            if (currentElixir > MAX_ELIXIR) {
                currentElixir = MAX_ELIXIR;
            }
        }
    }

    //Checks if enough elixir is available.
    public boolean canAfford(int cost) {return currentElixir >= cost;}

    //Spends elixir if available
    public boolean spend(int cost) {
        if (canAfford(cost)) {
            currentElixir -= cost;
            return true;
        }
        return false;
    }

    public int getCurrentElixirInt() {return (int) currentElixir;}

    public double getCurrentElixir() {return currentElixir;}
    
    //Sets the current elixir (used for loading saved games)
    public void setCurrentElixir(double elixir) {
        this.currentElixir = Math.max(0, Math.min(elixir, MAX_ELIXIR));
    }
}
