package com.kuroyale.model.entities;

import com.kuroyale.model.enums.*;

/*Represents a daily quest instance.
 * Tracks progress toward a specific quest goal.*/
public class Quest {
    private final String id;
    private final QuestType type;
    private int progress;
    private boolean completed;
    private boolean claimed;

    public Quest(String id, QuestType type) {
        this.id = id;
        this.type = type;
        this.progress = 0;
        this.completed = false;
        this.claimed = false;
    }

    public String getId() {
        return id;
    }

    public QuestType getType() {
        return type;
    }

    public String getDescription() {
        return type.getDescription();
    }

    public int getTarget() {
        return type.getTargetValue();
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = Math.min(progress, getTarget());
        if (this.progress >= getTarget()) {
            this.completed = true;
        }
    }

    public void addProgress(int amount) {
        setProgress(this.progress + amount);
    }

    public int getGoldReward() {
        return type.getGoldReward();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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
        return (double) progress / getTarget();
    }

    public String getProgressText() {
        return progress + "/" + getTarget();
    }
}
