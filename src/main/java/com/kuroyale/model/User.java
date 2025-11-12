package com.kuroyale.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user account with username, password hash, and saved deck
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
}

