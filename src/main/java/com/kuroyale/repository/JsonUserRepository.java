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

import com.kuroyale.model.ArenaLayout;
import com.kuroyale.model.GridPosition;
import com.kuroyale.model.User;

/**
 * JSON-based implementation of UserRepository
 * Handles persistence of User objects to JSON file
 * Follows Pure Fabrication - created to handle persistence concerns
 * Follows Low Coupling - separated from business logic
 */
public class JsonUserRepository implements UserRepository {

    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".kuroyale";
    private static final String USERS_FILE = DATA_DIR + File.separator + "users.json";

    // Constructor ensures data directory exists
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

            // Load arena layout
            if (jsonUser.has("arenaLayout")) {
                JSONObject arenaJson = jsonUser.getJSONObject("arenaLayout");
                ArenaLayout layout = new ArenaLayout(arenaJson.getString("name"));

                // Load bridge positions
                if (arenaJson.has("bridgePositions")) {
                    JSONArray bridgesArray = arenaJson.getJSONArray("bridgePositions");
                    for (int j = 0; j < bridgesArray.length(); j++) {
                        JSONObject bridgePos = bridgesArray.getJSONObject(j);
                        layout.addBridgePosition(bridgePos.getInt("x"), bridgePos.getInt("y"));
                    }
                }

                // Load Princess tower positions
                if (arenaJson.has("princessTowers")) {
                    JSONArray towersArray = arenaJson.getJSONArray("princessTowers");
                    for (int j = 0; j < towersArray.length(); j++) {
                        JSONObject towerPos = towersArray.getJSONObject(j);
                        layout.addPrincessTowerPosition(towerPos.getInt("x"), towerPos.getInt("y"));
                    }
                }

                // Load King tower position
                if (arenaJson.has("kingTower")) {
                    JSONObject kingTower = arenaJson.getJSONObject("kingTower");
                    layout.setKingTowerPosition(kingTower.getInt("x"), kingTower.getInt("y"));
                }

                user.setArenaLayout(layout);
            }

            users.add(user);
        }

        return users;
    }

    @Override
    public boolean existsByUsername(String username) throws IOException {
        return findByUsername(username) != null;
    }

    // Saves all users to the JSON file
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

            // Save arena layout
            if (user.getArenaLayout() != null) {
                JSONObject arenaJson = new JSONObject();
                ArenaLayout layout = user.getArenaLayout();
                arenaJson.put("name", layout.getName());

                // Save bridge positions
                JSONArray bridgesArray = new JSONArray();
                for (GridPosition bridge : layout.getBridgePositions()) {
                    JSONObject bridgePos = new JSONObject();
                    bridgePos.put("x", bridge.getX());
                    bridgePos.put("y", bridge.getY());
                    bridgesArray.put(bridgePos);
                }
                arenaJson.put("bridgePositions", bridgesArray);

                // Save Princess tower positions
                JSONArray princessArray = new JSONArray();
                for (GridPosition tower : layout.getPrincessTowerPositions()) {
                    JSONObject towerPos = new JSONObject();
                    towerPos.put("x", tower.getX());
                    towerPos.put("y", tower.getY());
                    princessArray.put(towerPos);
                }
                arenaJson.put("princessTowers", princessArray);

                // Save King tower position
                if (layout.getKingTowerPosition() != null) {
                    JSONObject kingTower = new JSONObject();
                    kingTower.put("x", layout.getKingTowerPosition().getX());
                    kingTower.put("y", layout.getKingTowerPosition().getY());
                    arenaJson.put("kingTower", kingTower);
                }

                jsonUser.put("arenaLayout", arenaJson);
            }

            jsonArray.put(jsonUser);
        }

        // Write to file
        try (FileWriter writer = new FileWriter(USERS_FILE)) {
            writer.write(jsonArray.toString(2)); // Pretty print with 2-space indent
        }
    }

    // Ensures the data directory exists
    private void ensureDataDirectoryExists() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
}
