package com.kuroyale.model.entities;

import java.util.List;

import com.kuroyale.model.enums.*;

/**
 * Represents a challenge in Challenge Mode.
 * Uses Strategy Pattern: each challenge subclass implements its own deck
 * validation rules.
 * 
 * Design Pattern: Strategy Pattern
 * validateDeck() method is the strategy that varies per challenge type
 * Each concrete challenge provides its own validation logic
 */
public abstract class Challenge {
    private final int id;
    private final ChallengeType type;
    private final String description;
    private final String rules;

    // Progression tracking
    private boolean unlocked;
    private boolean completed;
    private int starsEarned; // 0-3 stars
    private int attempts;
    private int completions;

    // Star thresholds (can be customized per challenge)
    protected int twoStarTimeSeconds = 120; // 2 minutes for 2 stars
    protected int threeStarTimeSeconds = 60; // 1 minute for 3 stars

    public Challenge(int id, ChallengeType type, String description, String rules) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.rules = rules;
        this.unlocked = (id == 1); // First challenge is always unlocked
        this.completed = false;
        this.starsEarned = 0;
        this.attempts = 0;
        this.completions = 0;
    }

    /*
     * Strategy Pattern: Validates if the given deck meets the challenge
     * requirements.
     * Each concrete challenge implements its own validation logic.
     */
    public abstract List<String> validateDeck(List<Card> deck);

    /*
     * Gets the current progress text for display (e.g., "Swarm cards: 2/5").
     * Default implementation returns empty string. Subclasses override for specific
     * display.
     */
    public String getProgressText(List<Card> deck) {
        return "";
    }

    /*
     * Checks if the challenge-specific requirement is met (not counting the 8-card
     * requirement).
     * Default returns true if no errors. Subclasses can override for custom logic.
     */
    public boolean isRequirementMet(List<Card> deck) {
        return validateDeck(deck).isEmpty();
    }

    /*
     * Gets the list of allowed cards for this challenge (for UI display).
     * Default implementation returns null (all cards allowed).
     * Subclasses override to restrict card selection.
     */
    public List<String> getAllowedCardNames() {
        return null;
    }

    // Calculates stars earned based on completion time and damage taken.
    public int calculateStars(int completionTimeSeconds, int damageTaken) {
        if (damageTaken == 0 || completionTimeSeconds <= threeStarTimeSeconds) {
            return 3;
        } else if (completionTimeSeconds <= twoStarTimeSeconds) {
            return 2;
        }
        return 1;
    }

    // Records a challenge attempt result.
    public void recordAttempt(boolean won, int completionTimeSeconds, int damageTaken) {
        attempts++;
        if (won) {
            completions++;
            completed = true;
            int newStars = calculateStars(completionTimeSeconds, damageTaken);
            if (newStars > starsEarned) {
                starsEarned = newStars;
            }
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public ChallengeType getType() {
        return type;
    }

    public String getName() {
        return type.getDisplayName();
    }

    public String getDescription() {
        return description;
    }

    public String getRules() {
        return rules;
    }

    public int getGoldReward() {
        return type.getGoldReward();
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getStarsEarned() {
        return starsEarned;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getCompletions() {
        return completions;
    }

    // Setters for progression
    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setStarsEarned(int stars) {
        this.starsEarned = Math.min(3, Math.max(0, stars));
    }

    public int getTwoStarTimeSeconds() {
        return twoStarTimeSeconds;
    }

    public int getThreeStarTimeSeconds() {
        return threeStarTimeSeconds;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setCompletions(int completions) {
        this.completions = completions;
    }
}
