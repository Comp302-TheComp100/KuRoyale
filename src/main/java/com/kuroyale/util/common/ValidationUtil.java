package com.kuroyale.util.common;

/*Utility class for validation of business rules
 * Pure Fabrication - created to handle validation concerns
 * High Cohesion - focused solely on validation logic*/
public class ValidationUtil {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;

    /*
     * Validates a password against business rules
     * Rules: At least 8 characters, contains at least one number and one letter
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

    /*
     * Validates a username against business rules
     * Rules: 3-20 characters, alphanumeric only
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

    // Gets a descriptive message for password validation requirements
    public static String getPasswordRequirements() {
        return "Password must be at least " + MIN_PASSWORD_LENGTH
                + " characters and contain at least one number and one letter";
    }

    // Gets a descriptive message for username validation requirements
    public static String getUsernameRequirements() {
        return "Username must be " + MIN_USERNAME_LENGTH + "-" + MAX_USERNAME_LENGTH + " alphanumeric characters";
    }
}
