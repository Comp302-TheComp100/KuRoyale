package com.kuroyale.repository;

import java.io.IOException;
import java.util.List;

import com.kuroyale.model.User;

/** Repository interface for User persistence operations
 * Follows Pure Fabrication and Polymorphism patterns, provides abstraction for data access*/
public interface UserRepository {
    
    //Finds a user by username
    User findByUsername(String username) throws IOException;
    
    //Saves a user (creates or updates)
    void save(User user) throws IOException;
    
    //Loads all users from storage
    List<User> findAll() throws IOException;
    
    //Checks if a username already exists
    boolean existsByUsername(String username) throws IOException;
}