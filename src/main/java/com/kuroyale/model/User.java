package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

import com.kuroyale.util.PasswordUtil;

/**
 * Represents a user account with username, password hash, and saved deck
 * Follows Information Expert GRASP pattern - User knows about its own data
 * and has responsibility for operations on that data
 */
public class User {
    private String username;
    private String passwordHash;
    private List<String> deck; // List of card names

    public User() {
        this.deck = new ArrayList<>();
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.deck = new ArrayList<>();
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
    
    // Information Expert: User knows its own password and can validate it
    /**
     * Validates a plain text password against this user's stored hash
     * @param password The plain text password to validate
     * @return true if password matches, false otherwise
     */
    public boolean validatePassword(String password) {
        return PasswordUtil.verifyPassword(password, this.passwordHash);
    }
    
    // Information Expert: User knows if it has a deck
    /**
     * Checks if the user has a deck configured
     * @return true if user has at least one card in deck, false otherwise
     */
    public boolean hasDeck() {
        return deck != null && !deck.isEmpty() && deck.stream().anyMatch(card -> card != null && !card.isEmpty());
    }
    
    // Information Expert: User manages its own deck
    /**
     * Updates the user's deck with new card names
     * @param cardNames List of card names to set as deck
     */
    public void updateDeck(List<String> cardNames) {
        this.deck = cardNames != null ? new ArrayList<>(cardNames) : new ArrayList<>();
    }
    
    /**
     * Clears all cards from the user's deck
     */
    public void clearDeck() {
        this.deck.clear();
    }
    
    /**
     * Gets the number of cards in the user's deck
     * @return Number of non-empty card slots
     */
    public int getDeckSize() {
        if (deck == null) {
            return 0;
        }
        return (int) deck.stream().filter(card -> card != null && !card.isEmpty()).count();
    }
}

