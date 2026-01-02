package com.kuroyale.service;

import com.kuroyale.model.dto.SavedGameState;
import com.kuroyale.model.entities.ArenaLayout;
import com.kuroyale.model.entities.GridPosition;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Unit tests for GameSaveService - Save & Load functionality.
 * Tests the core persistence layer for game state serialization.
 */
class GameSaveServiceTest {

    private GameSaveService gameSaveService;
    private Path testSaveDirectory;
    
    @BeforeEach
    void setUp() {
        gameSaveService = new GameSaveService();
        // Get the save directory path for test cleanup
        String userHome = System.getProperty("user.home");
        testSaveDirectory = Paths.get(userHome, ".kuroyale", "saved_games");
    }

    @AfterEach
    void tearDown() {
        // Clean up any test save files created during tests
        cleanupTestSaveFiles();
    }

    /**
     * Test Case 1: SavedGameState serialization and deserialization
     * Verifies that SavedGameState can be correctly serialized to bytes
     * and deserialized back with all data intact.
     */
    @Test
    @DisplayName("SavedGameState should serialize and deserialize correctly")
    void testSavedGameStateSerialization() throws IOException, ClassNotFoundException {
        // Arrange - Create a SavedGameState with test data
        ArenaLayout layout = new ArenaLayout("TestArena");
        layout.setKingTowerPosition(8, 28);
        layout.addPrincessTowerPosition(3, 26);
        layout.addPrincessTowerPosition(14, 26);
        
        List<String> playerDeck = Arrays.asList("Knight", "Archers", "Giant", "Fireball");
        List<String> playerHand = Arrays.asList("Knight", "Archers", "Giant", "Fireball");
        List<String> playerDrawPile = new ArrayList<>();
        List<String> botDeck = Arrays.asList("Knight", "Archers", "Giant", "Fireball");
        List<String> botHand = new ArrayList<>();
        List<String> botDrawPile = new ArrayList<>();
        
        List<SavedGameState.SavedTower> towers = Arrays.asList(
            new SavedGameState.SavedTower("KING", true, 2400, 2400, 8, 28),
            new SavedGameState.SavedTower("PRINCESS", true, 1400, 1400, 3, 26)
        );
        
        List<SavedGameState.SavedTroop> troops = Arrays.asList(
            new SavedGameState.SavedTroop("Knight", true, 600, 5, 20, "MOVING")
        );
        
        List<SavedGameState.SavedBuilding> buildings = Arrays.asList(
            new SavedGameState.SavedBuilding("Cannon", true, 500, 7, 24, 3, 3, 30.0)
        );
        
        SavedGameState originalState = new SavedGameState(
            "testPlayer",
            120.0,  // 2 minutes remaining
            false,  // not double elixir
            1,      // player score
            0,      // bot score
            5.5,    // player elixir
            playerDeck, playerHand, playerDrawPile,
            6.0,    // bot elixir
            botDeck, botHand, botDrawPile,
            layout,
            towers, troops, buildings
        );
        
        // Act - Serialize and deserialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(originalState);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        SavedGameState deserializedState = (SavedGameState) ois.readObject();
        ois.close();
        
        // Assert - Verify all fields are correctly preserved
        assertNotNull(deserializedState.getSaveId(), "Save ID should not be null");
        assertEquals("testPlayer", deserializedState.getPlayerUsername());
        assertEquals(120.0, deserializedState.getGameTime(), 0.01);
        assertFalse(deserializedState.isDoubleElixir());
        assertEquals(1, deserializedState.getPlayerScore());
        assertEquals(0, deserializedState.getBotScore());
        assertEquals(5.5, deserializedState.getPlayerElixir(), 0.01);
        assertEquals(6.0, deserializedState.getBotElixir(), 0.01);
        
        // Verify collections
        assertEquals(4, deserializedState.getPlayerDeckCards().size());
        assertTrue(deserializedState.getPlayerDeckCards().contains("Knight"));
        assertEquals(2, deserializedState.getTowers().size());
        assertEquals(1, deserializedState.getActiveTroops().size());
        assertEquals(1, deserializedState.getActiveBuildings().size());
        
        // Verify arena layout
        assertNotNull(deserializedState.getArenaLayout());
        assertEquals("TestArena", deserializedState.getArenaLayout().getName());
    }

    /**
     * Test Case 2: Load all saved games returns empty list when no saves exist
     * Verifies that loadAllSavedGames gracefully returns an empty list
     * when the save directory has no save files.
     */
    @Test
    @DisplayName("loadAllSavedGames should return empty list when no saves exist")
    void testLoadAllSavedGamesReturnsEmptyWhenNoSaves() {
        // Arrange - Clean any existing save files
        cleanupTestSaveFiles();
        
        // Act
        List<SavedGameState> savedGames = gameSaveService.loadAllSavedGames();
        
        // Assert
        assertNotNull(savedGames, "Returned list should not be null");
        assertTrue(savedGames.isEmpty(), "List should be empty when no saves exist");
    }

    /**
     * Test Case 3: Load saved games for specific player filters correctly
     * Verifies that loadSavedGamesForPlayer correctly filters saved games
     * to only return games for the specified username.
     */
    @Test
    @DisplayName("loadSavedGamesForPlayer should filter by username correctly")
    void testLoadSavedGamesForPlayerFiltering() throws IOException {
        // Arrange - Create and save games for different players
        SavedGameState game1 = createTestSavedGameState("player1");
        SavedGameState game2 = createTestSavedGameState("player2");
        SavedGameState game3 = createTestSavedGameState("player1");
        
        // Save the games to disk
        saveToDisk(game1);
        saveToDisk(game2);
        saveToDisk(game3);
        
        // Act
        List<SavedGameState> player1Games = gameSaveService.loadSavedGamesForPlayer("player1");
        List<SavedGameState> player2Games = gameSaveService.loadSavedGamesForPlayer("player2");
        List<SavedGameState> nonExistentPlayerGames = gameSaveService.loadSavedGamesForPlayer("nonexistent");
        
        // Assert
        assertEquals(2, player1Games.size(), "Player1 should have 2 saved games");
        assertEquals(1, player2Games.size(), "Player2 should have 1 saved game");
        assertTrue(nonExistentPlayerGames.isEmpty(), "Non-existent player should have no saved games");
        
        // Verify all returned games belong to the correct player
        for (SavedGameState game : player1Games) {
            assertEquals("player1", game.getPlayerUsername());
        }
        for (SavedGameState game : player2Games) {
            assertEquals("player2", game.getPlayerUsername());
        }
    }

    /**
     * Test Case 4: Delete saved game removes the correct file
     * Verifies that deleteSavedGame correctly removes a saved game by its ID.
     */
    @Test
    @DisplayName("deleteSavedGame should remove the correct save file")
    void testDeleteSavedGame() throws IOException {
        // Arrange - Create and save a game
        SavedGameState gameToDelete = createTestSavedGameState("deleteTestPlayer");
        saveToDisk(gameToDelete);
        String saveId = gameToDelete.getSaveId();
        
        // Verify it exists
        List<SavedGameState> gamesBefore = gameSaveService.loadSavedGamesForPlayer("deleteTestPlayer");
        assertEquals(1, gamesBefore.size(), "Game should exist before deletion");
        
        // Act
        boolean deleteResult = gameSaveService.deleteSavedGame(saveId);
        
        // Assert
        assertTrue(deleteResult, "Delete operation should return true on success");
        
        List<SavedGameState> gamesAfter = gameSaveService.loadSavedGamesForPlayer("deleteTestPlayer");
        assertTrue(gamesAfter.isEmpty(), "No games should exist after deletion");
    }

    /**
     * Test Case 5: Delete non-existent save returns false
     * Verifies that deleteSavedGame returns false when trying to delete
     * a save file that doesn't exist.
     */
    @Test
    @DisplayName("deleteSavedGame should return false for non-existent save ID")
    void testDeleteNonExistentSaveReturnsFalse() {
        // Arrange - Use a random UUID that doesn't correspond to any save
        String nonExistentSaveId = UUID.randomUUID().toString();
        
        // Act
        boolean result = gameSaveService.deleteSavedGame(nonExistentSaveId);
        
        // Assert
        assertFalse(result, "Delete should return false for non-existent save ID");
    }

    /**
     * Test Case 6: SavedGameState helper classes serialize correctly
     * Tests that inner classes (SavedTower, SavedTroop, SavedBuilding) serialize correctly.
     */
    @Test
    @DisplayName("SavedTower, SavedTroop, and SavedBuilding should serialize correctly")
    void testInnerClassesSerialization() throws IOException, ClassNotFoundException {
        // Test SavedTower
        SavedGameState.SavedTower tower = new SavedGameState.SavedTower(
            "PRINCESS", true, 1200, 1400, 3, 26
        );
        SavedGameState.SavedTower deserializedTower = serializeAndDeserialize(tower);
        assertEquals("PRINCESS", deserializedTower.getTowerType());
        assertTrue(deserializedTower.isPlayerSide());
        assertEquals(1200, deserializedTower.getCurrentHealth());
        assertEquals(1400, deserializedTower.getMaxHealth());
        assertEquals(3, deserializedTower.getGridX());
        assertEquals(26, deserializedTower.getGridY());
        
        // Test SavedTroop
        SavedGameState.SavedTroop troop = new SavedGameState.SavedTroop(
            "Giant", true, 2000, 10, 15, "ATTACKING"
        );
        SavedGameState.SavedTroop deserializedTroop = serializeAndDeserialize(troop);
        assertEquals("Giant", deserializedTroop.getCardName());
        assertTrue(deserializedTroop.isPlayerSide());
        assertEquals(2000, deserializedTroop.getCurrentHealth());
        assertEquals(10, deserializedTroop.getGridX());
        assertEquals(15, deserializedTroop.getGridY());
        assertEquals("ATTACKING", deserializedTroop.getState());
        
        // Test SavedBuilding
        SavedGameState.SavedBuilding building = new SavedGameState.SavedBuilding(
            "Tesla", false, 800, 12, 8, 3, 3, 25.5
        );
        SavedGameState.SavedBuilding deserializedBuilding = serializeAndDeserialize(building);
        assertEquals("Tesla", deserializedBuilding.getCardName());
        assertFalse(deserializedBuilding.isPlayerSide());
        assertEquals(800, deserializedBuilding.getCurrentHealth());
        assertEquals(12, deserializedBuilding.getGridX());
        assertEquals(8, deserializedBuilding.getGridY());
        assertEquals(3, deserializedBuilding.getWidth());
        assertEquals(3, deserializedBuilding.getHeight());
        assertEquals(25.5, deserializedBuilding.getRemainingLifetime(), 0.01);
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a test SavedGameState with minimal required data.
     */
    private SavedGameState createTestSavedGameState(String playerUsername) {
        ArenaLayout layout = new ArenaLayout("TestArena");
        layout.setKingTowerPosition(8, 28);
        
        return new SavedGameState(
            playerUsername,
            180.0,  // 3 minutes
            false,
            0, 0,
            5.0,
            Arrays.asList("Knight", "Archers", "Giant", "Fireball"),
            Arrays.asList("Knight", "Archers", "Giant", "Fireball"),
            new ArrayList<>(),
            5.0,
            Arrays.asList("Knight", "Archers", "Giant", "Fireball"),
            new ArrayList<>(),
            new ArrayList<>(),
            layout,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>()
        );
    }

    /**
     * Saves a SavedGameState to disk for testing purposes.
     */
    private void saveToDisk(SavedGameState savedGame) throws IOException {
        String fileName = String.format("test_%s_%s.krsave", 
            savedGame.getPlayerUsername(), 
            savedGame.getSaveId().substring(0, 8));
        Path filePath = testSaveDirectory.resolve(fileName);
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(savedGame);
        }
    }

    /**
     * Cleans up all test save files from the save directory.
     */
    private void cleanupTestSaveFiles() {
        try {
            if (Files.exists(testSaveDirectory)) {
                Files.list(testSaveDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("test_") 
                                 || path.getFileName().toString().contains("testPlayer")
                                 || path.getFileName().toString().contains("player1")
                                 || path.getFileName().toString().contains("player2")
                                 || path.getFileName().toString().contains("deleteTestPlayer"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete test file: " + path);
                        }
                    });
            }
        } catch (IOException e) {
            System.err.println("Failed to cleanup test save files: " + e.getMessage());
        }
    }

    /**
     * Helper method to serialize and deserialize an object.
     */
    @SuppressWarnings("unchecked")
    private <T extends Serializable> T serializeAndDeserialize(T obj) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        T result = (T) ois.readObject();
        ois.close();
        
        return result;
    }
}

