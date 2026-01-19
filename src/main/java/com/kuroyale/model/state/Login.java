package com.kuroyale.model.state;

import com.kuroyale.model.entities.*;

import java.io.IOException;
import com.kuroyale.service.AuthenticationService;
import com.kuroyale.util.ServiceFactory;
import com.kuroyale.util.ValidationUtil;

// The Model component for the Login screen. Encapsulates authentication and validation business rules.
public class Login {
    private final AuthenticationService authService;

    public Login() {
        // Initialize dependencies (matching existing code structure)
        this.authService = ServiceFactory.getInstance().getAuthenticationService();
    }

    // Attempts to log in a user.
    public User authenticateUser(String username, String password) throws IOException {
        User user = authService.authenticate(username, password);

        // If successful, set the current user in the service layer
        if (user != null) {
            authService.setCurrentUser(user);
        }
        return user;
    }

    // Attempts to register a new user.
    public User registerUser(String username, String password) throws IOException {
        User user = authService.register(username, password);

        // If successful, set the current user in the service layer
        if (user != null) {
            authService.setCurrentUser(user);
        }
        return user;
    }

    // Validates input fields and returns a specific error message.
    public String validateCredentials(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            return "Please enter both username and password";
        }
        if (!ValidationUtil.isValidUsername(username)) {
            return ValidationUtil.getUsernameRequirements();
        }
        if (!ValidationUtil.isValidPassword(password)) {
            return ValidationUtil.getPasswordRequirements();
        }
        return ""; // Success
    }
}
