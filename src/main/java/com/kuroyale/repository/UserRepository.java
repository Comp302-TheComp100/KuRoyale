package com.kuroyale.repository;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.User;

/**
 * Repository interface for User persistence operations
 * Follows Pure Fabrication and Polymorphism GRASP patterns
 * Provides abstraction for data access, supporting Protected Variations
 */
public interface UserRepository {
    
    /**
     * Finds a user by username
     * @param username The username to search for
     * @return The User object if found, null otherwise
     * @throws IOException If there's an error reading data
     */
    User findByUsername(String username) throws IOException;
    
    /**
     * Saves a user (creates or updates)
     * @param user The user to save
     * @throws IOException If there's an error writing data
     */
    void save(User user) throws IOException;
    
    /**
     * Loads all users from storage
     * @return List of all users
     * @throws IOException If there's an error reading data
     */
    List<User> findAll() throws IOException;
    
    /**
     * Checks if a username already exists
     * @param username The username to check
     * @return true if username exists, false otherwise
     * @throws IOException If there's an error reading data
     */
    boolean existsByUsername(String username) throws IOException;
}



