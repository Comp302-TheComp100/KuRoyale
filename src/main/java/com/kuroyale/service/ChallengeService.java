package com.kuroyale.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.kuroyale.model.entities.Challenge;
import com.kuroyale.model.entities.ChallengeFactory;

/* Service for managing challenge progression and state.
 * Handles saving and loading challenge progress (unlocks, stars, attempts) locally.*/
public class ChallengeService {
    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".kuroyale";
    private static final String SAVE_FILE_TEMPLATE = DATA_DIR + File.separator + "challenges_%s.json";

    private final ChallengeFactory challengeFactory;
    private List<Challenge> challenges;
    private String currentUsername;

    public ChallengeService() {
        this.challengeFactory = new ChallengeFactory();
        ensureDataDirectoryExists();
        initializeChallenges();
        // Removed initial loadProgress() as it requires username
    }

    //Loads challenge data for the specified user.
    public void loadForUser(String username) {
        this.currentUsername = username;

        // Reset to default state
        initializeChallenges();

        // Load user progress
        loadProgress();
    }

    // Initializes challenges.
    private void initializeChallenges() {
        // Create fresh challenge instances
        this.challenges = challengeFactory.getAllChallenges();
    }

    public List<Challenge> getAllChallenges() {
        return challenges;
    }

    public Challenge getChallenge(int id) {
        return challenges.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /* Records a completed attempt for a challenge.
     * Updates stats and unlocks the next challenge if won.*/
    public void recordAttempt(int challengeId, boolean won, int timeSeconds, int damageDealt) {
        if (currentUsername == null)
            return;

        Challenge challenge = getChallenge(challengeId);
        if (challenge != null) {
            challenge.recordAttempt(won, timeSeconds, damageDealt);

            if (won) {
                unlockNextChallenge(challengeId);
            }

            saveProgress();
        }
    }

    private void unlockNextChallenge(int currentId) {
        Challenge next = getChallenge(currentId + 1);
        if (next != null && !next.isUnlocked()) {
            next.setUnlocked(true);
        }
    }

    //Saves challenge progress to simple JSON.
    public void saveProgress() {
        if (currentUsername == null)
            return;

        ensureDataDirectoryExists();

        JSONArray jsonArray = new JSONArray();
        for (Challenge c : challenges) {
            JSONObject obj = new JSONObject();
            obj.put("id", c.getId());
            obj.put("unlocked", c.isUnlocked());
            obj.put("completed", c.isCompleted());
            obj.put("stars", c.getStarsEarned());
            obj.put("attempts", c.getAttempts());
            obj.put("completions", c.getCompletions());
            jsonArray.put(obj);
        }

        try (FileWriter writer = new FileWriter(String.format(SAVE_FILE_TEMPLATE, currentUsername))) {
            writer.write(jsonArray.toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Loads progress from JSON and applies it to current challenge instances.
    private void loadProgress() {
        if (currentUsername == null)
            return;

        String filename = String.format(SAVE_FILE_TEMPLATE, currentUsername);
        File file = new File(filename);
        if (!file.exists())
            return;

        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            if (content.trim().isEmpty())
                return;

            JSONArray jsonArray = new JSONArray(content);

            // Map ID to JSON object for easy lookup
            Map<Integer, JSONObject> progressMap = new HashMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                progressMap.put(obj.getInt("id"), obj);
            }

            for (Challenge c : challenges) {
                JSONObject p = progressMap.get(c.getId());
                if (p != null) {
                    c.setUnlocked(p.getBoolean("unlocked"));
                    c.setCompleted(p.getBoolean("completed"));
                    c.setStarsEarned(p.getInt("stars"));
                    c.setAttempts(p.getInt("attempts"));
                    c.setCompletions(p.getInt("completions"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureDataDirectoryExists() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
}