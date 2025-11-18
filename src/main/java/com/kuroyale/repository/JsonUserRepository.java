package com.kuroyale.repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.kuroyale.model.User;

/**
 * JSON-based implementation of UserRepository
 * Handles persistence of User objects to JSON file
 * Follows Pure Fabrication GRASP pattern - created to handle persistence concerns
 * Follows Low Coupling - separated from business logic
 */
public class JsonUserRepository implements UserRepository {
    
    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".kuroyale";
    private static final String USERS_FILE = DATA_DIR + File.separator + "users.json";
    
    /**
     * Constructor ensures data directory exists
     */
    public JsonUserRepository() {
        ensureDataDirectoryExists();
    }
    
    @Override
    public User findByUsername(String username) throws IOException {
        List<User> users = findAll();
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }
    
    @Override
    public void save(User user) throws IOException {
        List<User> users = findAll();
        
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
        
        saveAll(users);
    }
    
    @Override
    public List<User> findAll() throws IOException {
        List<User> users = new ArrayList<>();
        
        ensureDataDirectoryExists();
        
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
    
    @Override
    public boolean existsByUsername(String username) throws IOException {
        return findByUsername(username) != null;
    }
    
    /**
     * Saves all users to the JSON file
     * @param users List of User objects to save
     * @throws IOException If there's an error writing the file
     */
    private void saveAll(List<User> users) throws IOException {
        ensureDataDirectoryExists();
        
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
    
    /**
     * Ensures the data directory exists
     */
    private void ensureDataDirectoryExists() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
}



