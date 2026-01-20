package com.kuroyale.repository;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.entities.User;

/**
 * Defines the contract for user data persistence operations.
 * <p>
 * <b>Repository Pattern:</b><br>
 * Encapsulates the logic required to access data sources, providing a
 * collection-like interface for accessing objects.
 * </p>
 * <p>
 * <b>GRASP Pattern: Protected Variations</b><br>
 * This interface isolates the business logic from the details of the
 * persistence mechanism. Client code
 * depends on this stable interface rather than concrete storage implementations
 * (e.g., JSON, SQL).
 * </p>
 */
public interface UserRepository {

    // Finds a user by username
    User findByUsername(String username) throws IOException;

    // Saves a user (creates or updates)
    void save(User user) throws IOException;

    // Loads all users from storage
    List<User> findAll() throws IOException;

    // Checks if a username already exists
    boolean existsByUsername(String username) throws IOException;
}
