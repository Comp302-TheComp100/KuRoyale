package com.kuroyale.service;

import java.io.IOException;
import java.util.Set;

import com.kuroyale.model.entities.User;
import com.kuroyale.repository.UserRepository;
import com.kuroyale.util.PasswordUtil;
import com.kuroyale.util.ValidationUtil;

/*Service for handling user authentication and registration
 * Low Coupling: uses repository interface, not concrete implementation
 * High Cohesion: focused solely on authentication concerns*/
public class AuthenticationService {
    private final UserRepository userRepository;
    private final CardCatalog cardCatalog;
    private User currentUser;

    // Creates an AuthenticationService with the given repository
    public AuthenticationService(UserRepository userRepository, CardCatalog cardCatalog) {
        this.userRepository = userRepository;
        this.cardCatalog = cardCatalog;
        this.currentUser = null;
    }

    private void ensureAllCardLevelsInitialized(User user) {
        if (user == null || cardCatalog == null) {
            return;
        }
        Set<String> allNames = cardCatalog.getAllCardNames();
        for (String name : allNames) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (!user.getCardLevels().containsKey(name)) {
                user.setCardLevel(name, 1);
            }
        }
    }

    // Registers a new user account. Creator pattern: creates User objects with
    // initialization data
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

        ensureAllCardLevelsInitialized(newUser);

        // Persist user
        userRepository.save(newUser);
        return newUser;
    }

    // Authenticates a user with username and password
    public User authenticate(String username, String password) throws IOException {
        // Find user by username
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }

        // Delegate password validation to User (Information Expert)
        if (user.validatePassword(password)) {
            ensureAllCardLevelsInitialized(user);
            userRepository.save(user);
            return user;
        }

        return null;
    }

    // Sets the currently logged-in user
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && cardCatalog != null) {
            cardCatalog.applyUserLevels(user);
        }
    }

    // Gets the currently logged-in user
    public User getCurrentUser() {
        return currentUser;
    }

    // Checks if a user is currently logged in
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    // Logs out the current user
    public void logout() {
        this.currentUser = null;
    }

    public void saveCurrentUser() throws IOException {
        if (currentUser == null) {
            throw new IllegalStateException("No user is currently logged in");
        }
        userRepository.save(currentUser);
    }

    // Awards gold to the current user and saves
    public void awardGoldToCurrentUser(int amount) throws IOException {
        if (currentUser == null) {
            return;
        }
        currentUser.setGold(currentUser.getGold() + amount);
        saveCurrentUser();
    }
}
