package com.kuroyale.model;

import java.util.ArrayDeque;
import java.util.Deque;

public class Troop {
    private final Card baseCard;
    private GridPosition position;
    private GridPosition targetPosition;
    private final boolean isPlayer;
    private final boolean isAirUnit;
    private final boolean buildingOnly;
    private double currentHealth;
    private final double moveSpeed; // cells per second
    private final double attackRange; // in cells approximate
    private final Deque<GridPosition> path;

    public Troop(Card card, GridPosition spawn, boolean isPlayer) {
        this.baseCard = card;
        this.position = spawn;
        this.targetPosition = null;
        this.isPlayer = isPlayer;
        this.isAirUnit = card.isAirUnit();
        this.buildingOnly = card.getTarget() == TargetType.BUILDINGS;
        this.currentHealth = card.getHp();
        this.moveSpeed = mapSpeed(card.getSpeed());
        this.attackRange = card.getRange();
        this.path = new ArrayDeque<>();
    }

    private double mapSpeed(SpeedType speedType) {
        switch (speedType) {
            case VERY_SLOW: return 0.8;
            case SLOW: return 1.0;
            case MEDIUM: return 1.3;
            case FAST: return 1.6;
            case VERY_FAST: return 2.0;
            default: return 0.0;
        }
    }

    public void setTargetPosition(GridPosition target) { this.targetPosition = target; }
    public GridPosition getTargetPosition() { return targetPosition; }

    public void clearPath() { path.clear(); }
    public void setPath(Deque<GridPosition> newPath) { path.clear(); path.addAll(newPath); }
    public Deque<GridPosition> getPath() { return path; }
    public boolean isAirUnit() { return isAirUnit; }
    public boolean isBuildingOnly() { return buildingOnly; }

    public Card getBaseCard() { return baseCard; }
    public GridPosition getPosition() { return position; }
    public void setPosition(GridPosition pos) { this.position = pos; }
    public boolean isPlayerSide() { return isPlayer; }

    public double getCurrentHealth() { return currentHealth; }
    public void takeDamage(double amount) { currentHealth = Math.max(0, currentHealth - amount); }
    public boolean isAlive() { return currentHealth > 0; }

    public double getMoveSpeed() { return moveSpeed; }
    public double getAttackRange() { return attackRange; }
}
