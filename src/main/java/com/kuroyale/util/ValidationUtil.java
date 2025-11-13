package com.kuroyale.util;

/**
 * Utility class for validation of business rules
 * Follows Pure Fabrication GRASP pattern - created to handle validation concerns
 * Follows High Cohesion - focused solely on validation logic
 */
public class ValidationUtil {
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;
    
    /**
     * Validates a password against business rules
     * Rules: At least 8 characters, contains at least one number and one letter
     * @param password The password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        
        boolean hasNumber = false;
        boolean hasLetter = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                hasNumber = true;
            }
            if (Character.isLetter(c)) {
                hasLetter = true;
            }
        }
        
        return hasNumber && hasLetter;
    }
    
    /**
     * Validates a username against business rules
     * Rules: 3-20 characters, alphanumeric only
     * @param username The username to validate
     * @return true if username meets requirements, false otherwise
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = username.trim();
        
        // Check length
        if (trimmed.length() < MIN_USERNAME_LENGTH || trimmed.length() > MAX_USERNAME_LENGTH) {
            return false;
        }
        
        // Check alphanumeric
        for (char c : trimmed.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Gets a descriptive message for password validation requirements
     * @return Password requirements message
     */
    public static String getPasswordRequirements() {
        return "Password must be at least " + MIN_PASSWORD_LENGTH + 
               " characters and contain at least one number and one letter";
    }
    
    /**
     * Gets a descriptive message for username validation requirements
     * @return Username requirements message
     */
    public static String getUsernameRequirements() {
        return "Username must be " + MIN_USERNAME_LENGTH + "-" + MAX_USERNAME_LENGTH + 
               " alphanumeric characters";
    }
}

