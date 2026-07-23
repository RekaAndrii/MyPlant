package com.my.plant.service;

import com.my.plant.configs.MyPlantApplication;
import com.my.plant.exception.DuplicateEmailException;
import com.my.plant.exception.InvalidEmailException;
import com.my.plant.exception.InvalidPasswordException;
import com.my.plant.model.User;
import com.my.plant.repository.UserRepository;
import com.my.plant.service.impl.RegistrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RegistrationService.
 * Tests validation and user registration functionality.
 * 
 * Strategy: 1 positive test + 1 negative test per method.
 */
@SpringBootTest(classes = MyPlantApplication.class)
class RegistrationServiceTest {
    
    @Autowired
    private RegistrationService registrationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        // Clear database before each test to ensure isolation
        userRepository.deleteAll();
    }
    
    // ============ REGISTER USER TESTS ============
    
    /**
     * POSITIVE: Register valid user should succeed.
     * User should be saved with all fields populated and password hashed.
     */
    @Test
    void registerValidUserSucceeds() {
        // WHEN: Register user with valid inputs
        User result = registrationService.registerUser(
            "john_doe", 
            "john@example.com", 
            "password123"
        );
        
        // THEN: User saved successfully
        assertNotNull(result.getId(), "Saved user should have MongoDB ID");
        assertEquals("john_doe", result.getUsername());
        assertEquals("john@example.com", result.getEmail());
        assertTrue(passwordEncoder.matches("password123", result.getPasswordHash()),
            "Password should be bcrypt hashed");
        assertEquals(Set.of("USER"), result.getRoles(), "Default role should be USER");
        assertNotNull(result.getCreatedAt(), "Creation timestamp should be set");
        
        // AND: User persisted in database
        assertTrue(userRepository.existsByEmail("john@example.com"));
    }
    
    /**
     * NEGATIVE: Register with invalid email format should fail.
     * Should throw InvalidEmailException and not save user.
     */
    @Test
    void registerInvalidEmailFormatFails() {
        // WHEN/THEN: Invalid email format throws exception
        InvalidEmailException exception = assertThrows(InvalidEmailException.class, () -> {
            registrationService.registerUser(
                "john_doe", 
                "invalid-email", 
                "password123"
            );
        });
        
        // AND: Exception message contains email
        assertTrue(exception.getMessage().contains("invalid-email"));
        
        // AND: User not saved in database
        assertEquals(0, userRepository.count(), "No users should be saved on validation failure");
    }
    
    // ============ EMAIL VALIDATION TESTS ============
    
    /**
     * POSITIVE: Valid email format should pass validation.
     * Should not throw exception.
     */
    @Test
    void validateEmailValidFormatSucceeds() {
        // WHEN/THEN: Valid email formats do not throw
        assertDoesNotThrow(() -> registrationService.validateEmail("test@example.com"));
        assertDoesNotThrow(() -> registrationService.validateEmail("user.name@domain.co.uk"));
        assertDoesNotThrow(() -> registrationService.validateEmail("a@b.c"));
    }
    
    /**
     * NEGATIVE: Invalid email format should fail validation.
     * Should throw InvalidEmailException for malformed emails.
     */
    @Test
    void validateEmailInvalidFormatFails() {
        // WHEN/THEN: Invalid email formats throw exception
        assertThrows(InvalidEmailException.class, () -> 
            registrationService.validateEmail("invalid"));
        
        assertThrows(InvalidEmailException.class, () -> 
            registrationService.validateEmail("@domain.com"));
        
        assertThrows(InvalidEmailException.class, () -> 
            registrationService.validateEmail("user@"));
        
        assertThrows(InvalidEmailException.class, () -> 
            registrationService.validateEmail(null));
    }
    
    // ============ EMAIL UNIQUENESS TESTS ============
    
    /**
     * POSITIVE: Unique email should pass validation.
     * Email not in database should be allowed.
     */
    @Test
    void validateEmailNotExistsSucceeds() {
        // WHEN/THEN: Unique email does not throw
        assertDoesNotThrow(() -> 
            registrationService.validateEmailNotExists("newuser@example.com"));
    }
    
    /**
     * NEGATIVE: Duplicate email should fail validation.
     * Email already in database should throw DuplicateEmailException.
     */
    @Test
    void validateEmailNotExistsFails() {
        // GIVEN: User already registered with this email
        User existingUser = new User();
        existingUser.setUsername("existing");
        existingUser.setEmail("taken@example.com");
        existingUser.setPasswordHash("hash");
        existingUser.setRoles(Set.of("USER"));
        userRepository.save(existingUser);
        
        // WHEN/THEN: Duplicate email throws exception
        DuplicateEmailException exception = assertThrows(DuplicateEmailException.class, () -> {
            registrationService.validateEmailNotExists("taken@example.com");
        });
        
        // AND: Exception message contains email
        assertTrue(exception.getMessage().contains("taken@example.com"));
    }
    
    // ============ PASSWORD VALIDATION TESTS ============
    
    /**
     * POSITIVE: Valid password (>= 4 characters) should pass validation.
     * Should not throw exception.
     */
    @Test
    void validatePasswordValidLengthSucceeds() {
        // WHEN/THEN: Valid passwords do not throw
        assertDoesNotThrow(() -> registrationService.validatePassword("1234"));  // Minimum length
        assertDoesNotThrow(() -> registrationService.validatePassword("password"));
        assertDoesNotThrow(() -> registrationService.validatePassword("P@ssw0rd!"));
    }
    
    /**
     * NEGATIVE: Invalid password (< 4 characters) should fail validation.
     * Should throw InvalidPasswordException.
     */
    @Test
    void validatePasswordTooShortFails() {
        // WHEN/THEN: Passwords < 4 chars throw exception
        InvalidPasswordException exception = assertThrows(InvalidPasswordException.class, () -> {
            registrationService.validatePassword("abc");  // Only 3 characters
        });
        
        // AND: Exception message mentions minimum length
        assertTrue(exception.getMessage().contains("at least 4 characters"));
        
        // AND: Null password also fails
        assertThrows(InvalidPasswordException.class, () -> 
            registrationService.validatePassword(null));
        
        // AND: Empty password fails
        assertThrows(InvalidPasswordException.class, () -> 
            registrationService.validatePassword(""));
    }
    
    // ============ DUPLICATE EMAIL IN FULL REGISTRATION TESTS ============
    
    /**
     * NEGATIVE: Registering with duplicate email should fail.
     * Even if other fields are valid, duplicate email should prevent registration.
     */
    @Test
    void registerDuplicateEmailFails() {
        // GIVEN: User already registered with this email
        registrationService.registerUser(
            "jane_doe", 
            "jane@example.com", 
            "password123"
        );
        
        // WHEN/THEN: Duplicate email throws exception
        DuplicateEmailException exception = assertThrows(DuplicateEmailException.class, () -> {
            registrationService.registerUser(
                "john_doe", 
                "jane@example.com", 
                "password123"
            );
        });
        
        // AND: Exception message contains email
        assertTrue(exception.getMessage().contains("jane@example.com"));
        
        // AND: Only original user exists in database
        assertEquals(1, userRepository.count());
    }
    
    /**
     * NEGATIVE: Registering with password < 4 characters should fail.
     * Password validation should prevent registration before saving.
     */
    @Test
    void registerPasswordTooShortFails() {
        // WHEN/THEN: Short password throws exception
        InvalidPasswordException exception = assertThrows(InvalidPasswordException.class, () -> {
            registrationService.registerUser(
                "john_doe", 
                "john@example.com", 
                "abc"  // Only 3 characters
            );
        });
        
        // AND: Exception message mentions requirement
        assertTrue(exception.getMessage().contains("at least 4 characters"));
        
        // AND: User not saved
        assertEquals(0, userRepository.count());
    }
}
