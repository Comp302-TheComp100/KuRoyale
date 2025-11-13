package com.kuroyale.service;

import java.io.IOException;

import com.kuroyale.model.User;
import com.kuroyale.repository.UserRepository;
import com.kuroyale.util.PasswordUtil;
import com.kuroyale.util.ValidationUtil;

/**
 * Service for handling user authentication and registration
 * Follows Controller GRASP pattern - coordinates authentication operations
 * Follows Low Coupling - uses repository interface, not concrete implementation
 * Follows High Cohesion - focused solely on authentication concerns
 */
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private User currentUser;
    
    /**
     * Creates an AuthenticationService with the given repository
     * Follows Low Coupling via Dependency Injection
     * @param userRepository The repository for user persistence
     */
    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.currentUser = null;
    }
    
    /**
     * Registers a new user account
     * Follows Creator pattern - creates User objects with initialization data
     * @param username The username for the new account
     * @param password The plain text password
     * @return The created User if successful, null if username exists or validation fails
     * @throws IOException If there's an error with persistence
     */
    public User register(String username, String password) throws IOException {
        // Validate username
        if (!ValidationUtil.isValidUsername(username)) {
            return null;
        }
        
        // Validate password
        if (!ValidationUtil.isValidPassword(password)) {
            return null;
        }
        
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            return null;
        }
        
        // Create new user with hashed password
        // Creator: Service has the initialization data (username, password hash)
        String passwordHash = PasswordUtil.hashPassword(password);
        User newUser = new User(username, passwordHash);
        
        // Persist user
        userRepository.save(newUser);
        
        return newUser;
    }
    
    /**
     * Authenticates a user with username and password
     * @param username The username
     * @param password The plain text password
     * @return The authenticated User if successful, null if authentication fails
     * @throws IOException If there's an error reading user data
     */
    public User authenticate(String username, String password) throws IOException {
        // Find user by username
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        
        // Delegate password validation to User (Information Expert)
        if (user.validatePassword(password)) {
            return user;
        }
        
        return null;
    }
    
    /**
     * Sets the currently logged-in user
     * @param user The user to set as current
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    /**
     * Gets the currently logged-in user
     * @return The current user, or null if not logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Checks if a user is currently logged in
     * @return true if a user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Logs out the current user
     */
    public void logout() {
        this.currentUser = null;
    }
    
    /**
     * Saves the current user's data
     * @throws IOException If there's an error saving
     * @throws IllegalStateException If no user is logged in
     */
    public void saveCurrentUser() throws IOException {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in");
        }
        userRepository.save(currentUser);
    }
}

