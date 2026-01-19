package com.kuroyale.service.game;

import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.kuroyale.model.entities.Achievement;
import com.kuroyale.model.entities.Card;
import com.kuroyale.model.entities.Tower;
import com.kuroyale.model.enums.AchievementType;

/*Service for managing permanent achievements.
 * Handles achievement tracking, unlocking, and persistence.*/
public class AchievementService implements GameEventListener {

    private static final String ACHIEVEMENT_FILE_TEMPLATE = "achievements_%s.json";

    private final Path dataPath;
    private final Map<AchievementType, Achievement> achievements;
    private String currentUsername;

    // Win streak tracking for UNDEFEATED achievement
    private int currentWinStreak = 0;

    public AchievementService() {
        String userHome = System.getProperty("user.home");
        this.dataPath = Paths.get(userHome, ".kuroyale");
        this.achievements = new EnumMap<>(AchievementType.class);
        initializeAchievements();
        // Register to game events
        GameEventBus.getInstance().subscribe(this);
    }

    // Loads achievement data for the specified user.
    public void loadForUser(String username) {
        this.currentUsername = username;

        // Reset achievements to default first
        initializeAchievements();

        // Load user progress
        loadAchievements();
    }

    // Initializes all achievements with default values.
    private void initializeAchievements() {
        achievements.clear();
        for (AchievementType type : AchievementType.values()) {
            achievements.put(type, new Achievement(type));
        }
    }

    // Gets all achievements.
    public List<Achievement> getAllAchievements() {
        if (currentUsername == null) {
            return new ArrayList<>(achievements.values()); // Returns initialized defaults
        }
        return new ArrayList<>(achievements.values());
    }

    // Gets a specific achievement.
    public Achievement getAchievement(AchievementType type) {
        return achievements.get(type);
    }

    // Updates progress for a specific achievement.
    public void updateProgress(AchievementType type, int amount) {
        if (currentUsername == null)
            return;

        Achievement achievement = achievements.get(type);
        if (achievement != null && !achievement.isClaimed()) {
            achievement.addProgress(amount);
            saveAchievements();
        }
    }

    // Sets progress for a specific achievement (for milestone achievements).
    public void setProgress(AchievementType type, int progress) {
        if (currentUsername == null)
            return;

        Achievement achievement = achievements.get(type);
        if (achievement != null && !achievement.isClaimed()) {
            achievement.setProgress(progress);
            saveAchievements();
        }
    }

    // Claims an achievement reward.
    public int claimReward(AchievementType type) {
        if (currentUsername == null)
            return 0;

        Achievement achievement = achievements.get(type);
        if (achievement != null && achievement.isUnlocked() && !achievement.isClaimed()) {
            achievement.setClaimed(true);
            saveAchievements();
            return achievement.getGoldReward();
        }
        return 0;
    }

    // Checks if there are any unclaimed unlocked achievements.
    public boolean hasUnclaimedAchievements() {
        return achievements.values().stream()
                .anyMatch(a -> a.isUnlocked() && !a.isClaimed());
    }

    // Gets count of completed achievements.
    public int getCompletedCount() {
        return (int) achievements.values().stream()
                .filter(Achievement::isClaimed)
                .count();
    }

    // Loads achievements from disk.
    private void loadAchievements() {
        if (currentUsername == null)
            return;

        try {
            Path filePath = dataPath.resolve(String.format(ACHIEVEMENT_FILE_TEMPLATE, currentUsername));
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                JSONObject json = new JSONObject(content);

                for (AchievementType type : AchievementType.values()) {
                    if (json.has(type.name())) {
                        JSONObject a = json.getJSONObject(type.name());
                        Achievement achievement = achievements.get(type);
                        if (achievement != null) {
                            achievement.setProgress(a.getInt("progress"));
                            achievement.setUnlocked(a.getBoolean("unlocked"));
                            achievement.setClaimed(a.getBoolean("claimed"));
                        }
                    }
                }

                // Load win streak
                if (json.has("currentWinStreak")) {
                    currentWinStreak = json.getInt("currentWinStreak");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load achievements: " + e.getMessage());
        }
    }

    // Saves achievements to disk.
    private void saveAchievements() {
        if (currentUsername == null)
            return;

        try {
            Files.createDirectories(dataPath);

            JSONObject json = new JSONObject();
            for (Map.Entry<AchievementType, Achievement> entry : achievements.entrySet()) {
                JSONObject a = new JSONObject();
                a.put("progress", entry.getValue().getProgress());
                a.put("unlocked", entry.getValue().isUnlocked());
                a.put("claimed", entry.getValue().isClaimed());
                json.put(entry.getKey().name(), a);
            }

            Files.writeString(dataPath.resolve(String.format(ACHIEVEMENT_FILE_TEMPLATE, currentUsername)),
                    json.toString(2));

            // Save win streak separately in the same file
            json.put("currentWinStreak", currentWinStreak);
            Files.writeString(dataPath.resolve(String.format(ACHIEVEMENT_FILE_TEMPLATE, currentUsername)),
                    json.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save achievements: " + e.getMessage());
        }
    }

    @Override
    public void onCardPlayed(boolean isPlayer, Card card,
            java.util.List<com.kuroyale.model.entities.ICombatant> spawnedUnits) {
        if (!isPlayer)
            return;

        if (card.getType() == com.kuroyale.model.enums.CardType.TROOP && card.getCount() > 1) {
            updateProgress(AchievementType.ARMY_BUILDER, card.getCount());
        }
    }

    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        if (isPlayerTower)
            return; // Only track enemy towers destroyed

        updateProgress(AchievementType.TOWER_HUNTER, 1);
    }

    @Override
    public void onSpellDamageDealt(boolean isPlayer, int damage) {
        if (isPlayer) {
            updateProgress(AchievementType.SPELL_MASTER, damage);
        }
    }

    @Override
    public void onComboTriggered(com.kuroyale.model.enums.ComboType combo,
            java.util.List<com.kuroyale.model.entities.ICombatant> affectedUnits) {
        updateProgress(AchievementType.COMBO_EXPERT, 1);
    }

    @Override
    public void onMatchEnd(boolean playerWon) {
        if (playerWon) {
            currentWinStreak++;
            // Set progress to current streak (UNDEFEATED needs 5 in a row)
            setProgress(AchievementType.UNDEFEATED, currentWinStreak);
        } else {
            currentWinStreak = 0;
            // Reset UNDEFEATED progress on loss (if not already unlocked)
            Achievement undefeated = achievements.get(AchievementType.UNDEFEATED);
            if (undefeated != null && !undefeated.isUnlocked()) {
                undefeated.setProgress(0);
            }
        }
        saveAchievements();
    }
}
