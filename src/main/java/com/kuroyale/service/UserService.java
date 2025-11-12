package com.kuroyale.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.kuroyale.model.User;
import com.kuroyale.util.PasswordUtil;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service class for managing user accounts and persistence
 * Stores users in JSON format in user's home directory
 */
public class UserService {
    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".kuroyale";
    private static final String USERS_FILE = DATA_DIR + File.separator + "users.json";
    
    private static User currentUser = null;
    
    /**
     * Gets the current logged-in user
     * @return The current user, or null if not logged in
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Sets the current logged-in user
     * @param user The user to set as current
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    
    /**
     * Checks if a user is currently logged in
     * @return true if a user is logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Creates a new user account
     * @param username The username
     * @param password The plain text password
     * @return The created User object, or null if username already exists
     * @throws IOException If there's an error reading/writing the file
     */
    public static User createUser(String username, String password) throws IOException {
        // Check if username already exists
        if (findUserByUsername(username) != null) {
            return null;
        }
        
        // Create new user
        String passwordHash = PasswordUtil.hashPassword(password);
        User user = new User(username, passwordHash);
        
        // Load existing users
        List<User> users = loadUsers();
        
        // Add new user
        users.add(user);
        
        // Save all users
        saveUsers(users);
        
        return user;
    }
    
    /**
     * Attempts to log in a user
     * @param username The username
     * @param password The plain text password
     * @return The User object if login successful, null otherwise
     * @throws IOException If there's an error reading the file
     */
    public static User login(String username, String password) throws IOException {
        User user = findUserByUsername(username);
        if (user == null) {
            return null;
        }
        
        // Verify password
        if (PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
            return user;
        }
        
        return null;
    }
    
    /**
     * Finds a user by username
     * @param username The username to search for
     * @return The User object if found, null otherwise
     * @throws IOException If there's an error reading the file
     */
    public static User findUserByUsername(String username) throws IOException {
        List<User> users = loadUsers();
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
    
    /**
     * Saves a user's data (updates existing user in file)
     * @param user The user to save
     * @throws IOException If there's an error writing the file
     */
    public static void saveUser(User user) throws IOException {
        List<User> users = loadUsers();
        
        // Find and update existing user, or add new one
        boolean found = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equals(user.getUsername())) {
                users.set(i, user);
                found = true;
                break;
            }
        }
        
        if (!found) {
            users.add(user);
        }
        
        saveUsers(users);
    }
    
    /**
     * Loads all users from the JSON file
     * @return List of User objects
     * @throws IOException If there's an error reading the file
     */
    private static List<User> loadUsers() throws IOException {
        List<User> users = new ArrayList<>();
        
        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Create empty file if it doesn't exist
        File usersFile = new File(USERS_FILE);
        if (!usersFile.exists()) {
            usersFile.createNewFile();
            return users; // Return empty list
        }
        
        // Read file content
        String content = new String(Files.readAllBytes(Paths.get(USERS_FILE)));
        if (content.trim().isEmpty()) {
            return users; // Return empty list if file is empty
        }
        
        // Parse JSON
        JSONArray jsonArray = new JSONArray(content);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonUser = jsonArray.getJSONObject(i);
            User user = new User();
            user.setUsername(jsonUser.getString("username"));
            user.setPasswordHash(jsonUser.getString("passwordHash"));
            
            // Load deck
            if (jsonUser.has("deck")) {
                JSONArray deckArray = jsonUser.getJSONArray("deck");
                List<String> deck = new ArrayList<>();
                for (int j = 0; j < deckArray.length(); j++) {
                    deck.add(deckArray.getString(j));
                }
                user.setDeck(deck);
            }
            
            users.add(user);
        }
        
        return users;
    }
    
    /**
     * Saves all users to the JSON file
     * @param users List of User objects to save
     * @throws IOException If there's an error writing the file
     */
    private static void saveUsers(List<User> users) throws IOException {
        // Create data directory if it doesn't exist
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // Create JSON array
        JSONArray jsonArray = new JSONArray();
        for (User user : users) {
            JSONObject jsonUser = new JSONObject();
            jsonUser.put("username", user.getUsername());
            jsonUser.put("passwordHash", user.getPasswordHash());
            
            // Save deck
            JSONArray deckArray = new JSONArray();
            for (String cardName : user.getDeck()) {
                deckArray.put(cardName);
            }
            jsonUser.put("deck", deckArray);
            
            jsonArray.put(jsonUser);
        }
        
        // Write to file
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            writer.write(jsonArray.toString(2)); // Pretty print with 2-space indent
        }
    }
}

