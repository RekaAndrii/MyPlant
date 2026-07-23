package com.my.plant.service.impl;

import com.my.plant.exception.DuplicateEmailException;
import com.my.plant.exception.InvalidEmailException;
import com.my.plant.exception.InvalidPasswordException;
import com.my.plant.model.User;
import com.my.plant.repository.UserRepository;
import com.my.plant.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Implementation of RegistrationService for user account creation and validation.
 * Handles email validation, uniqueness checking, password validation, and secure storage.
 */
@Service
public class RegistrationServiceImpl implements RegistrationService {
    
    private static final String EMAIL_REGEX = "^[^@]+@[^@]+\\.[^@]+$";
    private static final int MIN_PASSWORD_LENGTH = 4;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    /**
     * Register a new user after validating all inputs.
     * Password is hashed using BCrypt before storage.
     */
    @Override
    public User registerUser(String username, String email, String password) {
        // Validate all inputs
        validateEmail(email);
        validateEmailNotExists(email);
        validatePassword(password);
        
        // Hash password using BCrypt
        String passwordHash = passwordEncoder.encode(password);
        
        // Create and save new user
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(passwordHash);
        newUser.setRoles(Set.of("USER"));
        newUser.setCreatedAt(LocalDateTime.now());
        
        return userRepository.save(newUser);
    }
    
    /**
     * Validate email format using regex pattern.
     * Pattern: ^[^@]+@[^@]+\.[^@]+$
     * Matches: user@example.com, name.surname@domain.co.uk
     * Rejects: invalid, @domain.com, user@, user.name@example
     */
    @Override
    public void validateEmail(String email) {
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new InvalidEmailException(email);
        }
    }
    
    /**
     * Check that email is not already registered in database.
     */
    @Override
    public void validateEmailNotExists(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }
    }
    
    /**
     * Validate password meets minimum length requirement of 4 characters.
     */
    @Override
    public void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidPasswordException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long"
            );
        }
    }
}
