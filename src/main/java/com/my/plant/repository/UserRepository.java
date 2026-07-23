package com.my.plant.repository;

import com.my.plant.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * MongoDB repository for User document persistence.
 * Provides query methods for finding users by username or email.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {
    
    /**
     * Find a user by their unique username.
     *
     * @param username The username to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by their unique email address.
     *
     * @param email The email to search for
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user with the given email already exists.
     *
     * @param email The email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}
