package com.kuroyale.service;

import java.io.IOException;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.User;
import com.kuroyale.repository.UserRepository;

/**
 * Service for managing Arena operations.
 * GRASP Patterns:
 * - Pure Fabrication: Manages arena operations without being a domain entity
 * itself.
 * - Creator: Creates Arena instances from ArenaLayout.
 */
public class ArenaService {
    private final UserRepository userRepository;
    private ArenaLayout savedLayout; // Fallback for when no user is logged in
    private User currentUser; // Track the current user

    public ArenaService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Sets the current user for arena operations
     * 
     * @param user The current logged-in user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Creates a default arena layout.
     * 
     * @return A default ArenaLayout
     */
    public ArenaLayout createDefaultLayout() {
        ArenaLayout layout = new ArenaLayout("Default Arena");
        // No default bridges as per requirements
        return layout;
    }

    /**
     * Creates an Arena instance from a layout.
     * 
     * @param layout The layout to use
     * @return A new Arena instance
     */
    public Arena createArena(ArenaLayout layout) {
        if (layout == null) {
            layout = createDefaultLayout();
        }
        return new Arena(layout);
    }

    /**
     * Saves an arena layout to the current user's profile.
     * 
     * @param layout The layout to save
     * @throws IOException if there's an error saving to disk
     */
    public void saveArenaLayout(ArenaLayout layout) throws IOException {
        if (currentUser != null && userRepository != null) {
            // Save to current user's profile
            currentUser.setArenaLayout(layout);
            userRepository.save(currentUser);
            System.out.println("Saving arena layout to user profile: " + layout.getName());
        } else {
            // Fallback to in-memory storage if no user logged in
            this.savedLayout = layout;
            System.out.println("Saving arena layout (in-memory): " + layout.getName());
        }
    }

    /**
     * Loads the arena layout from the current user's profile.
     * 
     * @return The user's saved ArenaLayout, or a new default one if none exists.
     */
    public ArenaLayout loadArenaLayout() {
        // Try to load from current user first
        if (currentUser != null && currentUser.hasArenaLayout()) {
            return currentUser.getArenaLayout();
        }

        // Fallback to in-memory storage
        if (savedLayout == null) {
            savedLayout = createDefaultLayout();
        }
        return savedLayout;
    }
}
