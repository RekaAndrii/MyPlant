package com.my.plant.service;

import com.my.plant.model.User;
import com.my.plant.exception.DuplicateEmailException;
import com.my.plant.exception.InvalidEmailException;
import com.my.plant.exception.InvalidPasswordException;
import com.my.plant.exception.RegistrationException;

/**
 * Service interface for user registration, validation, and account creation.
 */
public interface RegistrationService {
    
    /**
     * Register a new user with username, email, and password.
     * Validates email format, checks uniqueness, and validates password strength.
     *
     * @param username User's unique username
     * @param email User's unique email address
     * @param password User's password (plaintext, will be hashed)
     * @return Saved User object with id populated
     * @throws InvalidEmailException if email format is invalid
     * @throws DuplicateEmailException if email already exists in database
     * @throws InvalidPasswordException if password does not meet requirements
     * @throws RegistrationException for other registration errors
     */
    User registerUser(String username, String email, String password);
    
    /**
     * Validate that an email matches the required format pattern.
     * Uses regex: ^[^@]+@[^@]+\.[^@]+$
     *
     * @param email Email address to validate
     * @throws InvalidEmailException if email format is invalid
     */
    void validateEmail(String email);
    
    /**
     * Validate that an email is not already registered in the database.
     *
     * @param email Email address to check
     * @throws DuplicateEmailException if email already exists
     */
    void validateEmailNotExists(String email);
    
    /**
     * Validate that a password meets minimum length requirement (4 characters).
     *
     * @param password Password to validate
     * @throws InvalidPasswordException if password is too short or null
     */
    void validatePassword(String password);
}
