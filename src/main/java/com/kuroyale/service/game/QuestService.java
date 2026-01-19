package com.kuroyale.service.game;

import com.kuroyale.event.GameEventBus;
import com.kuroyale.event.GameEventListener;
import com.kuroyale.model.core.entities.Card;
import com.kuroyale.model.core.entities.Quest;
import com.kuroyale.model.core.entities.Tower;
import com.kuroyale.model.core.enums.CardType;
import com.kuroyale.model.core.enums.QuestType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

/*Service for managing daily quests.
 * Handles quest generation, progress tracking, and persistence.*/
public class QuestService implements GameEventListener {

    private static final String QUEST_FILE_TEMPLATE = "quests_%s.json";
    private static final int DAILY_QUEST_COUNT = 3;

    private final Path dataPath;
    private List<Quest> dailyQuests;
    private LocalDate lastResetDate;
    private String currentUsername;

    // Per-match tracking
    private int cardsPlayedThisMatch = 0;
    private int currentWinStreak = 0;

    public QuestService() {
        String userHome = System.getProperty("user.home");
        this.dataPath = Paths.get(userHome, ".kuroyale");
        this.dailyQuests = new ArrayList<>();
        // Removed initial loadQuests() call as it requires a username

        // Register to game events
        GameEventBus.getInstance().subscribe(this);
    }

    // Loads quest data for the specified user.

    public void loadForUser(String username) {
        this.currentUsername = username;
        loadQuests();
        checkAndResetDaily();
    }

    // Gets the current daily quests, generating new ones if needed.
    public List<Quest> getDailyQuests() {
        if (currentUsername == null) {
            return Collections.emptyList();
        }
        checkAndResetDaily();
        return Collections.unmodifiableList(dailyQuests);
    }

    // Checks if a daily reset is needed and performs it.
    public void checkAndResetDaily() {
        if (currentUsername == null)
            return;

        LocalDate today = LocalDate.now();
        if (lastResetDate == null || !lastResetDate.equals(today)) {
            generateNewDailyQuests();
            lastResetDate = today;
            saveQuests();
        }
    }

    // Generates 3 random daily quests.
    private void generateNewDailyQuests() {
        dailyQuests.clear();
        List<QuestType> allTypes = new ArrayList<>(Arrays.asList(QuestType.values()));
        Collections.shuffle(allTypes, new Random());

        for (int i = 0; i < DAILY_QUEST_COUNT && i < allTypes.size(); i++) {
            String id = UUID.randomUUID().toString();
            dailyQuests.add(new Quest(id, allTypes.get(i)));
        }
    }

    // Updates progress for a specific quest type.
    public void updateProgress(QuestType type, int amount) {
        if (currentUsername == null)
            return;

        for (Quest quest : dailyQuests) {
            if (quest.getType() == type && !quest.isClaimed()) {
                quest.addProgress(amount);
            }
        }
        saveQuests();
    }

    // Claims a quest reward.
    public int claimReward(String questId) {
        if (currentUsername == null)
            return 0;

        for (Quest quest : dailyQuests) {
            if (quest.getId().equals(questId) && quest.isCompleted() && !quest.isClaimed()) {
                quest.setClaimed(true);
                saveQuests();
                return quest.getGoldReward();
            }
        }
        return 0;
    }

    // Gets the time remaining until daily reset.
    public String getTimeUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        long hours = ChronoUnit.HOURS.between(now, midnight);
        long minutes = ChronoUnit.MINUTES.between(now, midnight) % 60;
        long seconds = ChronoUnit.SECONDS.between(now, midnight) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Loads quests from disk.
    private void loadQuests() {
        if (currentUsername == null)
            return;

        try {
            Path filePath = dataPath.resolve(String.format(QUEST_FILE_TEMPLATE, currentUsername));
            if (Files.exists(filePath)) {
                String content = Files.readString(filePath);
                JSONObject json = new JSONObject(content);

                // Load reset date
                if (json.has("lastResetDate")) {
                    lastResetDate = LocalDate.parse(json.getString("lastResetDate"));
                }

                // Load win streak
                if (json.has("currentWinStreak")) {
                    currentWinStreak = json.getInt("currentWinStreak");
                }

                // Load quests
                if (json.has("quests")) {
                    JSONArray questsArray = json.getJSONArray("quests");
                    dailyQuests.clear();
                    for (int i = 0; i < questsArray.length(); i++) {
                        JSONObject q = questsArray.getJSONObject(i);
                        QuestType type = QuestType.valueOf(q.getString("type"));
                        Quest quest = new Quest(q.getString("id"), type);
                        quest.setProgress(q.getInt("progress"));
                        quest.setCompleted(q.getBoolean("completed"));
                        quest.setClaimed(q.getBoolean("claimed"));
                        dailyQuests.add(quest);
                    }
                }
            } else {
                // No existing file for user, start fresh
                lastResetDate = null;
                dailyQuests.clear();
            }
        } catch (Exception e) {
            System.err.println("Failed to load quests: " + e.getMessage());
            dailyQuests = new ArrayList<>();
        }
    }

    // Saves quests to disk.
    private void saveQuests() {
        if (currentUsername == null)
            return;

        try {
            Files.createDirectories(dataPath);

            JSONObject json = new JSONObject();
            json.put("lastResetDate", lastResetDate != null ? lastResetDate.toString() : LocalDate.now().toString());

            JSONArray questsArray = new JSONArray();
            for (Quest quest : dailyQuests) {
                JSONObject q = new JSONObject();
                q.put("id", quest.getId());
                q.put("type", quest.getType().name());
                q.put("progress", quest.getProgress());
                q.put("completed", quest.isCompleted());
                q.put("claimed", quest.isClaimed());
                questsArray.put(q);
            }
            json.put("quests", questsArray);
            json.put("currentWinStreak", currentWinStreak);

            Files.writeString(dataPath.resolve(String.format(QUEST_FILE_TEMPLATE, currentUsername)), json.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save quests: " + e.getMessage());
        }
    }

    @Override
    public void onCardPlayed(boolean isPlayer, Card card,
            java.util.List<com.kuroyale.model.core.entities.ICombatant> spawnedUnits) {
        if (!isPlayer)
            return;

        // Track cards played this match
        cardsPlayedThisMatch++;

        if (card.getType() == CardType.SPELL) {
            updateProgress(QuestType.PLAY_SPELL_CARDS, 1);
        } else if (card.getType() == CardType.TROOP) {
            updateProgress(QuestType.DEPLOY_TROOP_CARDS, 1);
        } else if (card.getType() == CardType.BUILDING) {
            updateProgress(QuestType.PLAY_BUILDING_CARDS, 1);
        }
    }

    @Override
    public void onTowerDestroyed(boolean isPlayerTower, Tower tower) {
        if (isPlayerTower)
            return; // Only track enemy towers destroyed

        updateProgress(QuestType.DESTROY_CROWN_TOWERS, 1);
        if (tower.getType() == Tower.TowerType.KING) {
            updateProgress(QuestType.DESTROY_KING_TOWER, 1);
        }
    }

    @Override
    public void onElixirSpent(boolean isPlayer, int amount) {
        if (isPlayer) {
            updateProgress(QuestType.SPEND_ELIXIR, amount);
        }
    }

    @Override
    public void onSpellDamageDealt(boolean isPlayer, int damage) {
        if (isPlayer) {
            updateProgress(QuestType.DEAL_SPELL_DAMAGE, damage);
        }
    }

    @Override
    public void onMatchStart() {
        // Reset per-match counters
        cardsPlayedThisMatch = 0;
    }

    @Override
    public void onMatchEnd(boolean playerWon) {
        // Check PLAY_CARDS_IN_SINGLE_MATCH quest
        if (cardsPlayedThisMatch >= 20) {
            for (Quest quest : dailyQuests) {
                if (quest.getType() == QuestType.PLAY_CARDS_IN_SINGLE_MATCH && !quest.isClaimed()) {
                    quest.setProgress(quest.getTarget());
                    break;
                }
            }
        }

        // Track WIN_MATCHES_IN_ROW
        if (playerWon) {
            currentWinStreak++;
            for (Quest quest : dailyQuests) {
                if (quest.getType() == QuestType.WIN_MATCHES_IN_ROW && !quest.isClaimed()) {
                    quest.setProgress(currentWinStreak);
                    break;
                }
            }
        } else {
            currentWinStreak = 0;
            for (Quest quest : dailyQuests) {
                if (quest.getType() == QuestType.WIN_MATCHES_IN_ROW && !quest.isClaimed()) {
                    quest.setProgress(0);
                    break;
                }
            }
        }

        cardsPlayedThisMatch = 0;
        saveQuests();
    }
}
