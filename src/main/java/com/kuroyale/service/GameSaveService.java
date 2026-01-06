package com.kuroyale.service;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.*;
import com.kuroyale.model.logic.*;
import com.kuroyale.model.dto.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/*Service responsible for saving and loading game states.
 * Manages persistence of match data to allow players to pause and resume games.*/
public class GameSaveService {
    private static final String SAVE_DIRECTORY = "saved_games";
    private static final String SAVE_FILE_EXTENSION = ".krsave";

    private final Path saveDirectory;

    public GameSaveService() {
        // Create saves directory in user home or application directory
        String userHome = System.getProperty("user.home");
        this.saveDirectory = Paths.get(userHome, ".kuroyale", SAVE_DIRECTORY);

        // Ensure directory exists
        try {
            Files.createDirectories(saveDirectory);
        } catch (IOException e) {
            System.err.println("Failed to create save directory: " + e.getMessage());
        }
    }

    // Saves the current game state to disk
    public SavedGameState saveGame(GameState gameState, User user, ArenaLayout layout) {
        try {
            // Capture current game state
            SavedGameState savedGame = captureGameState(gameState, user, layout);

            // Write to file
            String fileName = generateFileName(savedGame);
            Path filePath = saveDirectory.resolve(fileName);

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new FileOutputStream(filePath.toFile()))) {
                oos.writeObject(savedGame);
            }

            return savedGame;

        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Loads all saved games for the current or any user
    public List<SavedGameState> loadAllSavedGames() {
        List<SavedGameState> savedGames = new ArrayList<>();

        try {
            if (!Files.exists(saveDirectory)) {
                return savedGames;
            }

            List<Path> saveFiles = Files.list(saveDirectory)
                    .filter(path -> path.toString().endsWith(SAVE_FILE_EXTENSION)).collect(Collectors.toList());

            for (Path saveFile : saveFiles) {
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(saveFile.toFile()))) {
                    SavedGameState savedGame = (SavedGameState) ois.readObject();
                    savedGames.add(savedGame);
                } catch (ClassNotFoundException | IOException e) {
                    System.err.println("Failed to load save file: " + saveFile.getFileName());
                    e.printStackTrace();
                }
            }

            // Sort by save time (newest first)
            savedGames.sort((a, b) -> b.getSaveTime().compareTo(a.getSaveTime()));

        } catch (IOException e) {
            System.err.println("Failed to list saved games: " + e.getMessage());
        }

        return savedGames;
    }

    // Loads saved games for a specific player.
    public List<SavedGameState> loadSavedGamesForPlayer(String playerUsername) {
        return loadAllSavedGames().stream().filter(save -> save.getPlayerUsername().equals(playerUsername))
                .collect(Collectors.toList());
    }

    /**
     * Deletes a saved game file from disk by its unique save ID.
     * 
     * This method searches through all saved game files in the save directory,
     * deserializes each to find the matching save ID, and deletes the corresponding file.
     * 
     * @param saveId The unique identifier (UUID) of the saved game to delete
     * @return true if the save was found and successfully deleted, false otherwise
     * 
     * REQUIRES:
     *   - saveId is a non-null, non-empty String representing a valid UUID
     *   - The save directory exists and is accessible
     * 
     * MODIFIES:
     *   - File system: Deletes the .krsave file that contains the matching saveId
     *   - No modification if saveId is null, empty, or doesn't match any existing save
     * 
     * EFFECTS:
     *   - If saveId is null or empty: returns false without modifying any files
     *   - If no save file matches the saveId: returns false, all files remain unchanged
     *   - If a save file with matching saveId exists and is successfully deleted: returns true
     *   - If a save file matches but deletion fails (e.g., permission error): returns false
     *   - If the save directory cannot be read: returns false
     *   - Corrupted or unreadable save files are skipped during the search
     */
    public boolean deleteSavedGame(String saveId) {
        // Handle null or empty saveId
        if (saveId == null || saveId.trim().isEmpty()) {
            System.err.println("Cannot delete save: saveId is null or empty");
            return false;
        }
        try {
            List<Path> files = Files.list(saveDirectory)
                    .filter(path -> path.toString().endsWith(SAVE_FILE_EXTENSION))
                    .collect(Collectors.toList());

            for (Path file : files) {
                // First, read the file to check if it matches
                String fileSaveId = null;
                try (ObjectInputStream ois = new ObjectInputStream(
                        new FileInputStream(file.toFile()))) {
                    SavedGameState savedGame = (SavedGameState) ois.readObject();
                    fileSaveId = savedGame.getSaveId();
                } catch (ClassNotFoundException e) {
                    System.err.println("Failed to read save file (class not found): " + file.getFileName());
                    continue;
                } catch (IOException e) {
                    System.err.println(
                            "Failed to read save file (IO error): " + file.getFileName() + " - " + e.getMessage());
                    continue;
                }

                // Now that the stream is closed, we can safely delete the file
                if (fileSaveId != null && fileSaveId.equals(saveId)) {
                    try {
                        Files.delete(file);
                        return true;
                    } catch (IOException e) {
                        System.err.println("Failed to delete file: " + file.getFileName() + " - " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to list saved games for deletion: " + e.getMessage());
            e.printStackTrace();
        }
        System.err.println("Save file with ID " + saveId + " not found");
        return false;
    }

    // Captures the current game state into a SavedGameState object
    private SavedGameState captureGameState(GameState gameState, User user, ArenaLayout layout) {
        // Capture player hand and deck
        Hand playerHand = gameState.getPlayerHand();
        List<String> playerHandCards = playerHand.getCards().stream().map(Card::getName).collect(Collectors.toList());

        // Get deck from user
        List<String> playerDeckCards = user != null ? new ArrayList<>(user.getDeck()) : new ArrayList<>();

        // Capture full draw pile (all remaining cards not in hand)
        List<String> playerDrawPileCards = playerHand.getDrawPileCards().stream()
                .map(Card::getName)
                .collect(Collectors.toList());

        // Capture bot state
        List<String> botDeckCards = new ArrayList<>(playerDeckCards);
        List<String> botHandCards = new ArrayList<>();
        List<String> botDrawPileCards = new ArrayList<>();

        // Capture towers
        List<SavedGameState.SavedTower> savedTowers = new ArrayList<>();
        Arena arena = gameState.getArena();

        // Collect unique towers
        Map<Tower, List<GridCell>> towerGroups = new HashMap<>();
        for (GridCell cell : arena.getAllCells()) {
            TileType tt = cell.getTileType();
            boolean isTowerTile = tt == TileType.PRINCESS_TOWER_USER || tt == TileType.PRINCESS_TOWER_COMPUTER
                    || tt == TileType.KING_TOWER_USER || tt == TileType.KING_TOWER_COMPUTER;
            if (!isTowerTile)
                continue;

            Tower tower = arena.getTowerAt(cell.getPosition().getX(), cell.getPosition().getY());
            if (tower == null)
                continue;

            towerGroups.computeIfAbsent(tower, k -> new ArrayList<>()).add(cell);
        }

        // Convert to SavedTower objects
        for (Map.Entry<Tower, List<GridCell>> entry : towerGroups.entrySet()) {
            Tower tower = entry.getKey();
            List<GridCell> cells = entry.getValue();

            if (cells.isEmpty())
                continue;

            // Determine position (top-left of tower)
            int minX = cells.stream().mapToInt(c -> c.getPosition().getX()).min().orElse(0);
            int minY = cells.stream().mapToInt(c -> c.getPosition().getY()).min().orElse(0);

            // Determine if player side
            TileType firstTileType = cells.get(0).getTileType();
            boolean isPlayerSide = firstTileType == TileType.PRINCESS_TOWER_USER
                    || firstTileType == TileType.KING_TOWER_USER;

            savedTowers.add(new SavedGameState.SavedTower(tower.getType().name(), isPlayerSide,
                    (int) tower.getCurrentHealth(), (int) tower.getMaxHealth(), minX, minY));
        }

        // Capture active troops
        List<SavedGameState.SavedTroop> savedTroops = gameState.getActiveTroops().stream()
                .filter(Troop::isAlive)
                .map(troop -> new SavedGameState.SavedTroop(
                        troop.getBaseCard().getName(),
                        troop.isPlayerSide(),
                        (int) troop.getCurrentHealth(),
                        troop.getPosition().getX(),
                        troop.getPosition().getY(),
                        troop.getUnitState().name()))
                .collect(Collectors.toList());

        // Capture active buildings
        List<SavedGameState.SavedBuilding> savedBuildings = gameState.getActiveBuildings().stream()
                .filter(Building::isAlive)
                .map(building -> new SavedGameState.SavedBuilding(
                        building.getCardName() != null ? building.getCardName()
                                : building.getImagePath().replaceAll(".*/", "").replace(".png", ""), // Fallback to
                                                                                                     // image path
                        building.isPlayerSide(),
                        (int) building.getCurrentHealth(),
                        building.getPosition().getX(), building.getPosition().getY(),
                        building.getWidth(), building.getHeight(),
                        building.getRemainingLifetime()))
                .collect(Collectors.toList());

        return new SavedGameState(
                user.getUsername(),
                gameState.getGameTime(),
                gameState.isDoubleElixir(),
                gameState.getPlayerScore(),
                gameState.getBotScore(),
                gameState.getPlayerElixir().getCurrentElixir(),
                playerDeckCards, playerHandCards, playerDrawPileCards,
                10.0, // Bot elixir - would need to expose from GameState
                botDeckCards, botHandCards, botDrawPileCards,
                layout,
                savedTowers, savedTroops, savedBuildings);
    }

    // Generates a unique filename for a saved game
    private String generateFileName(SavedGameState savedGame) {
        String timestamp = savedGame.getSaveTime().toString().replaceAll("[:\\-.]", "").replace("T", "_");

        return String.format("%s_%s%s", savedGame.getPlayerUsername(), timestamp, SAVE_FILE_EXTENSION);
    }
}
