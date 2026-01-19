package com.kuroyale.model.core.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kuroyale.service.auth.PasswordUtil;

/*Represents a user account with username, password hash, and saved deck
 * Information Expert - User knows about its own data and has responsibility for operations on that data*/
public class User {
    private String username;
    private String passwordHash;
    private List<String> deck; // List of card names
    private ArenaLayout arenaLayout; // User's custom arena layout
    private int gold;
    private Map<String, Integer> cardLevels;

    public User() {
        this.deck = new ArrayList<>();
        this.gold = 0;
        this.cardLevels = new HashMap<>();
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.deck = new ArrayList<>();
        this.gold = 0;
        this.cardLevels = new HashMap<>();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public List<String> getDeck() {
        return deck;
    }

    public void setDeck(List<String> deck) {
        this.deck = deck != null ? deck : new ArrayList<>();
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public Map<String, Integer> getCardLevels() {
        return cardLevels;
    }

    public void setCardLevels(Map<String, Integer> cardLevels) {
        if (cardLevels == null) {
            this.cardLevels = new HashMap<>();
            return;
        }
        this.cardLevels = new HashMap<>();
        for (Map.Entry<String, Integer> entry : cardLevels.entrySet()) {
            int clampedLevel = Math.max(Card.MIN_LEVEL, Math.min(Card.MAX_LEVEL, entry.getValue()));
            this.cardLevels.put(entry.getKey(), clampedLevel);
        }
    }

    // Gets the saved level for a specific card (defaults to 1)
    public int getCardLevel(String cardName) {
        if (cardName == null) {
            return 1;
        }
        Integer level = cardLevels.get(cardName);
        return level != null ? level : 1;
    }

    // Sets the saved level for a specific card
    public void setCardLevel(String cardName, int level) {
        if (cardName == null || cardName.isEmpty()) {
            return;
        }
        int clampedLevel = Math.max(Card.MIN_LEVEL, Math.min(Card.MAX_LEVEL, level));
        cardLevels.put(cardName, clampedLevel);
    }

    // Information Expert: User knows its own password and can validate it
    // Validates a plain text password against this user's stored hash
    public boolean validatePassword(String password) {
        return PasswordUtil.verifyPassword(password, this.passwordHash);
    }

    // Information Expert: User knows if it has a deck
    // Checks if the user has a deck configured
    public boolean hasDeck() {
        return deck != null && !deck.isEmpty() && deck.stream().anyMatch(card -> card != null && !card.isEmpty());
    }

    // Information Expert: User manages its own deck
    // Updates the user's deck with new card names
    public void updateDeck(List<String> cardNames) {
        this.deck = cardNames != null ? new ArrayList<>(cardNames) : new ArrayList<>();
    }

    // Clears all cards from the user's deck
    public void clearDeck() {
        this.deck.clear();
    }

    // Gets the number of cards in the user's deck
    public int getDeckSize() {
        if (deck == null) {
            return 0;
        }
        return (int) deck.stream().filter(card -> card != null && !card.isEmpty()).count();
    }

    // Gets the user's custom arena layout
    public ArenaLayout getArenaLayout() {
        return arenaLayout;
    }

    // Sets the user's custom arena layout
    public void setArenaLayout(ArenaLayout arenaLayout) {
        this.arenaLayout = arenaLayout;
    }

    // Checks if the user has a custom arena layout
    public boolean hasArenaLayout() {
        return arenaLayout != null;
    }
}
