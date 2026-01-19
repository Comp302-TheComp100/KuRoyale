package com.kuroyale.model.core.entities;

import com.kuroyale.model.core.enums.*;

/* Represents a permanent achievement.
 * Tracks lifetime progress toward achievement goals.*/
public class Achievement {
    private final AchievementType type;
    private int progress;
    private boolean unlocked;
    private boolean claimed;

    public Achievement(AchievementType type) {
        this.type = type;
        this.progress = 0;
        this.unlocked = false;
        this.claimed = false;
    }

    public AchievementType getType() {
        return type;
    }

    public String getName() {
        return type.getDisplayName();
    }

    public String getDescription() {
        return unlocked ? type.getDescription() : "???";
    }

    public String getFullDescription() {
        return type.getDescription();
    }

    public int getTarget() {
        return type.getTargetValue();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
        if (this.progress >= getTarget() && !unlocked) {
            this.unlocked = true;
        }
    }

    public void addProgress(int amount) {
        setProgress(this.progress + amount);
    }

    public int getGoldReward() {
        return type.getGoldReward();
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean isClaimed() {
        return claimed;
    }

    public void setClaimed(boolean claimed) {
        this.claimed = claimed;
    }

    public double getProgressPercentage() {
        if (getTarget() == 0)
            return 1.0;
        return Math.min(1.0, (double) progress / getTarget());
    }

    public String getProgressText() {
        return progress + "/" + getTarget();
    }
}
