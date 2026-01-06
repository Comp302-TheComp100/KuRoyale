package com.kuroyale.service;

import com.kuroyale.model.dto.SavedGameState;
import com.kuroyale.model.entities.ArenaLayout;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Unit tests for GameSaveService.deleteSavedGame() method.
 * 
 * Tests the deleteSavedGame(String saveId) method which is responsible for
 * removing saved game files from the file system based on their unique UUID.
 * 
 * Method Under Test: deleteSavedGame(String saveId)
 * 
 * Specification:
 *   REQUIRES: saveId is a non-null, non-empty String representing a valid UUID
 *   MODIFIES: File system - deletes the .krsave file matching the saveId
 *   EFFECTS:  Returns true if deletion successful, false otherwise
 */
class GameSaveServiceDeleteTest {

    private GameSaveService gameSaveService;
    private Path testSaveDirectory;
    private List<String> createdTestFiles;

    @BeforeEach
    void setUp() {
        gameSaveService = new GameSaveService();
        String userHome = System.getProperty("user.home");
        testSaveDirectory = Paths.get(userHome, ".kuroyale", "saved_games");
        createdTestFiles = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        // Clean up any test files created during tests
        for (String fileName : createdTestFiles) {
            try {
                Files.deleteIfExists(testSaveDirectory.resolve(fileName));
            } catch (IOException e) {
                System.err.println("Failed to cleanup test file: " + fileName);
            }
        }
    }

    // ==================== Test Cases for deleteSavedGame ====================

    /**
     * Test Case 1: Successfully delete an existing saved game
     * 
     * Tests the normal case where a valid saveId matches an existing save file.
     * Verifies that the method returns true and the file is actually removed.
     * 
     * Covers EFFECTS clause: "If a save file with matching saveId exists and 
     * is successfully deleted: returns true"
     */
    @Test
    @DisplayName("deleteSavedGame should return true and remove file when saveId exists")
    void testDeleteExistingSaveReturnsTrue() throws IOException {
        // Arrange: Create and save a test game
        SavedGameState testSave = createTestSavedGameState("deleteTestUser1");
        String saveId = testSave.getSaveId();
        String fileName = saveToDisk(testSave);
        
        // Verify the file exists before deletion
        Path filePath = testSaveDirectory.resolve(fileName);
        assertTrue(Files.exists(filePath), "Test file should exist before deletion");
        
        // Act: Delete the saved game
        boolean result = gameSaveService.deleteSavedGame(saveId);
        
        // Assert: Should return true and file should no longer exist
        assertTrue(result, "deleteSavedGame should return true for existing save");
        assertFalse(Files.exists(filePath), "File should be deleted from disk");
        
        // Remove from cleanup list since it's already deleted
        createdTestFiles.remove(fileName);
    }

    /**
     * Test Case 2: Attempt to delete with null saveId
     * 
     * Tests the boundary case where saveId is null.
     * Verifies that the method safely handles null input without throwing exceptions.
     * 
     * Covers REQUIRES clause validation and EFFECTS clause:
     * "If saveId is null or empty: returns false without modifying any files"
     */
    @Test
    @DisplayName("deleteSavedGame should return false when saveId is null")
    void testDeleteWithNullSaveIdReturnsFalse() {
        // Arrange: null saveId
        String nullSaveId = null;
        
        // Act: Attempt to delete with null
        boolean result = gameSaveService.deleteSavedGame(nullSaveId);
        
        // Assert: Should return false without throwing exception
        assertFalse(result, "deleteSavedGame should return false for null saveId");
    }

    /**
     * Test Case 3: Attempt to delete with empty saveId
     * 
     * Tests the boundary case where saveId is an empty string.
     * Verifies that the method handles empty input appropriately.
     * 
     * Covers REQUIRES clause validation and EFFECTS clause:
     * "If saveId is null or empty: returns false without modifying any files"
     */
    @Test
    @DisplayName("deleteSavedGame should return false when saveId is empty string")
    void testDeleteWithEmptySaveIdReturnsFalse() {
        // Arrange: empty saveId
        String emptySaveId = "";
        
        // Act: Attempt to delete with empty string
        boolean result = gameSaveService.deleteSavedGame(emptySaveId);
        
        // Assert: Should return false
        assertFalse(result, "deleteSavedGame should return false for empty saveId");
    }

    /**
     * Test Case 4: Attempt to delete non-existent saveId
     * 
     * Tests the case where saveId is a valid UUID format but doesn't match any file.
     * Verifies that the method returns false when no matching save is found.
     * 
     * Covers EFFECTS clause: "If no save file matches the saveId: returns false, 
     * all files remain unchanged"
     */
    @Test
    @DisplayName("deleteSavedGame should return false when saveId does not exist")
    void testDeleteNonExistentSaveIdReturnsFalse() {
        // Arrange: Generate a random UUID that doesn't correspond to any save
        String nonExistentSaveId = UUID.randomUUID().toString();
        
        // Act: Attempt to delete non-existent save
        boolean result = gameSaveService.deleteSavedGame(nonExistentSaveId);
        
        // Assert: Should return false
        assertFalse(result, "deleteSavedGame should return false for non-existent saveId");
    }

    /**
     * Test Case 5: Delete does not affect other saved games
     * 
     * Tests that deleting one save file does not impact other save files.
     * Creates multiple saves, deletes one, and verifies the others remain intact.
     * 
     * Covers MODIFIES clause: ensures only the targeted file is modified
     */
    @Test
    @DisplayName("deleteSavedGame should only delete the targeted save, not others")
    void testDeleteOnlyAffectsTargetedSave() throws IOException {
        // Arrange: Create multiple test saves
        SavedGameState save1 = createTestSavedGameState("isolationTestUser1");
        SavedGameState save2 = createTestSavedGameState("isolationTestUser2");
        SavedGameState save3 = createTestSavedGameState("isolationTestUser3");
        
        String fileName1 = saveToDisk(save1);
        String fileName2 = saveToDisk(save2);
        String fileName3 = saveToDisk(save3);
        
        Path path1 = testSaveDirectory.resolve(fileName1);
        Path path2 = testSaveDirectory.resolve(fileName2);
        Path path3 = testSaveDirectory.resolve(fileName3);
        
        // Verify all files exist
        assertTrue(Files.exists(path1), "Save 1 should exist");
        assertTrue(Files.exists(path2), "Save 2 should exist");
        assertTrue(Files.exists(path3), "Save 3 should exist");
        
        // Act: Delete only save2
        boolean result = gameSaveService.deleteSavedGame(save2.getSaveId());
        
        // Assert: Only save2 should be deleted
        assertTrue(result, "Delete should succeed");
        assertTrue(Files.exists(path1), "Save 1 should still exist after deleting save 2");
        assertFalse(Files.exists(path2), "Save 2 should be deleted");
        assertTrue(Files.exists(path3), "Save 3 should still exist after deleting save 2");
        
        // Update cleanup list
        createdTestFiles.remove(fileName2);
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a minimal SavedGameState for testing purposes.
     */
    private SavedGameState createTestSavedGameState(String playerUsername) {
        ArenaLayout layout = new ArenaLayout("TestArena");
        layout.setKingTowerPosition(8, 28);

        return new SavedGameState(
                playerUsername,
                180.0,  // 3 minutes remaining
                false,  // not double elixir
                0, 0,   // scores
                5.0,    // player elixir
                Arrays.asList("Knight", "Archers", "Giant", "Fireball"),
                Arrays.asList("Knight", "Archers", "Giant", "Fireball"),
                new ArrayList<>(),
                5.0,    // bot elixir
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
     * Saves a SavedGameState to disk and returns the filename.
     * Tracks the file for cleanup after the test.
     */
    private String saveToDisk(SavedGameState savedGame) throws IOException {
        String fileName = String.format("test_%s_%s.krsave",
                savedGame.getPlayerUsername(),
                savedGame.getSaveId().substring(0, 8));
        Path filePath = testSaveDirectory.resolve(fileName);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(savedGame);
        }

        createdTestFiles.add(fileName);
        return fileName;
    }
}

