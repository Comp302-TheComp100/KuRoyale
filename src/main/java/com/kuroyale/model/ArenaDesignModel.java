package com.kuroyale.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.kuroyale.service.ArenaService;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.ServiceFactory;

/*The Model component for the Arena Design screen.
 * Encapsulates business logic for arena layout operations.*/
public class ArenaDesignModel {
    
    private final AuthenticationService authService;
    private final ArenaService arenaService;
    
    public ArenaDesignModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getAuthenticationService();
        this.arenaService = factory.getArenaService();
    }
    
    //Checks if a user is logged in
    public boolean isLoggedIn() {
        return authService.isLoggedIn();
    }
    
    //Gets the current logged-in user
    public User getCurrentUser() {
        return authService.getCurrentUser();
    }
    
    //Sets the current user in arena service to load their saved layout
    public void setCurrentUserInArenaService(User user) {
        arenaService.setCurrentUser(user);
    }
    
    //Loads the arena layout for the current user
    public ArenaLayout loadArenaLayout() {
        return arenaService.loadArenaLayout();
    }
    
    //Creates an arena from the given layout
    public Arena createArena(ArenaLayout layout) {
        return arenaService.createArena(layout);
    }
    
    //Saves the arena layout for the current user
    public void saveArenaLayout(ArenaLayout layout) throws IOException {
        arenaService.saveArenaLayout(layout);
    }
    
    //Validates the arena layout and returns a list of validation errors (empty if valid)
    public List<String> validateLayout(ArenaLayout layout) {
        List<String> errors = new ArrayList<>();
        
        if (layout == null) {
            errors.add("Layout is null");
            return errors;
        }
        
        if (layout.getBridgePositions().isEmpty()) {
            errors.add("You must place at least one bridge.");
        }
        
        if (layout.getPrincessTowerPositions().size() != 2) {
            errors.add("You must place exactly two Princess towers.");
        }
        
        if (layout.getKingTowerPosition() == null) {
            errors.add("You must place one King tower.");
        }
        
        return errors;
    }
}




