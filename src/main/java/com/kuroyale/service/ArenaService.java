package com.kuroyale.service;

import com.kuroyale.model.Arena;
import com.kuroyale.model.ArenaLayout;
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
    private ArenaLayout savedLayout;

    public ArenaService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
     * Saves an arena layout.
     * 
     * @param layout The layout to save
     */
    public void saveArenaLayout(ArenaLayout layout) {
        this.savedLayout = layout;
        System.out.println("Saving arena layout: " + layout.getName());
    }

    /**
     * Loads the saved arena layout.
     * 
     * @return The saved ArenaLayout, or a new default one if none exists.
     */
    public ArenaLayout loadArenaLayout() {
        if (savedLayout == null) {
            savedLayout = createDefaultLayout();
        }
        return savedLayout;
    }
}
