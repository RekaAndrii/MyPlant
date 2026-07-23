package com.my.plant.controller;

import com.my.plant.configs.MyPlantApplication;
import com.my.plant.model.User;
import com.my.plant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RegistrationController.
 * Tests HTTP endpoints for user registration form and submission.
 * 
 * Strategy: 1 positive test + 1 negative test per endpoint.
 */
@SpringBootTest(classes = MyPlantApplication.class)
@AutoConfigureMockMvc
class RegistrationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @BeforeEach
    void setUp() {
        // Clear database before each test to ensure isolation
        userRepository.deleteAll();
    }
    
    // ============ GET /register TESTS ============
    
    /**
     * POSITIVE: GET /register should display registration form.
     * Should return 200 OK with registration template and model attributes.
     */
    @Test
    void getRegistrationFormSucceeds() throws Exception {
        // WHEN: GET /register
        mockMvc.perform(get("/register"))
        
        // THEN: Form displayed with 200 status
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            
        // AND: Model has empty form attributes
            .andExpect(model().attributeExists("username", "email"));
    }
    
    // ============ POST /register - SUCCESSFUL REGISTRATION TESTS ============
    
    /**
     * POSITIVE: POST /register with valid credentials should succeed.
     * Should register user and redirect to login page with success message.
     */
    @Test
    void postValidRegistrationSucceeds() throws Exception {
        // WHEN: POST with valid credentials
        mockMvc.perform(post("/register")
            .param("username", "newuser")
            .param("email", "newuser@example.com")
            .param("password", "password123"))
        
        // THEN: Redirect to /login
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        
        // AND: User saved in database
        assertEquals(1, userRepository.count(), "User should be saved in database");
        
        // AND: Email is in database
        User savedUser = userRepository.findByEmail("newuser@example.com").orElseThrow();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("newuser@example.com", savedUser.getEmail());
    }
    
    // ============ POST /register - INVALID EMAIL FORMAT TESTS ============
    
    /**
     * NEGATIVE: POST /register with invalid email format should fail.
     * Should re-render form with error message and not save user.
     */
    @Test
    void postInvalidEmailFormatFails() throws Exception {
        // WHEN: POST with invalid email format
        mockMvc.perform(post("/register")
            .param("username", "newuser")
            .param("email", "invalid-email")
            .param("password", "password123"))
            // THEN: Re-render form with 200 status (not redirect)
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            // AND: Error message displayed
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", 
                containsString("Invalid email format")))
            // AND: Form data preserved (for re-entry)
            .andExpect(model().attribute("username", "newuser"))
            .andExpect(model().attribute("email", "invalid-email"));
        
        // AND: User not saved
        assertEquals(0, userRepository.count(), "No user should be saved on validation failure");
    }
    
    // ============ POST /register - DUPLICATE EMAIL TESTS ============
    
    /**
     * NEGATIVE: POST /register with duplicate email should fail.
     * Should re-render form with error message and not save duplicate user.
     */
    @Test
    void postDuplicateEmailFails() throws Exception {
        // GIVEN: User already registered with this email
        User existingUser = new User();
        existingUser.setUsername("existing");
        existingUser.setEmail("taken@example.com");
        existingUser.setPasswordHash(passwordEncoder.encode("password"));
        existingUser.setRoles(Set.of("USER"));
        existingUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(existingUser);
        
        // WHEN: POST with duplicate email
        mockMvc.perform(post("/register")
            .param("username", "newuser")
            .param("email", "taken@example.com")
            .param("password", "password123"))
            // THEN: Re-render form with error
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            // AND: Error message indicates email already registered
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", 
                containsString("Email already registered")))
            // AND: Form data preserved
            .andExpect(model().attribute("username", "newuser"))
            .andExpect(model().attribute("email", "taken@example.com"));
        
        // AND: Only original user in database (no duplicate)
        assertEquals(1, userRepository.count(), "Only original user should exist");
    }
    
    // ============ POST /register - SHORT PASSWORD TESTS ============
    
    /**
     * NEGATIVE: POST /register with password < 4 characters should fail.
     * Should re-render form with error message and not save user.
     */
    @Test
    void postPasswordTooShortFails() throws Exception {
        // WHEN: POST with password < 4 characters
        mockMvc.perform(post("/register")
            .param("username", "newuser")
            .param("email", "newuser@example.com")
            .param("password", "abc"))  // Only 3 characters
            // THEN: Re-render form with error
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            // AND: Error message mentions password requirement
            .andExpect(model().attributeExists("error"))
            .andExpect(model().attribute("error", 
                containsString("at least 4 characters")))
            // AND: Form data preserved (except password for security)
            .andExpect(model().attribute("username", "newuser"))
            .andExpect(model().attribute("email", "newuser@example.com"));
        
        // AND: User not saved
        assertEquals(0, userRepository.count(), "No user should be saved on validation failure");
    }
    
    // ============ POST /register - EDGE CASES ============
    
    /**
     * NEGATIVE: POST /register with empty password should fail.
     * Should re-render form with error message.
     */
    @Test
    void postEmptyPasswordFails() throws Exception {
        // WHEN: POST with empty password
        mockMvc.perform(post("/register")
            .param("username", "newuser")
            .param("email", "newuser@example.com")
            .param("password", ""))  // Empty password
        
        // THEN: Re-render form with error
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeExists("error"));
        
        // AND: User not saved
        assertEquals(0, userRepository.count());
    }
    
    /**
     * POSITIVE: POST /register with minimum password length (4 chars) should succeed.
     * Password of exactly 4 characters should be accepted.
     */
    @Test
    void postMinimumPasswordLengthSucceeds() throws Exception {
        // WHEN: POST with 4-character password (minimum)
        mockMvc.perform(post("/register")
            .param("username", "minuser")
            .param("email", "min@example.com")
            .param("password", "1234"))  // Exactly 4 characters
        
        // THEN: Should redirect to login (success)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
        
        // AND: User saved
        assertEquals(1, userRepository.count());
        
        User savedUser = userRepository.findByEmail("min@example.com").orElseThrow();
        assertEquals("minuser", savedUser.getUsername());
    }
    
    /**
     * NEGATIVE: Multiple validation errors (invalid email AND short password).
     * Should fail on first validation (email format).
     */
    @Test
    void postMultipleValidationErrorsFails() throws Exception {
        // WHEN: POST with multiple validation errors
        mockMvc.perform(post("/register")
            .param("username", "newuser")
            .param("email", "invalid")  // Bad format
            .param("password", "ab"))  // Too short
        
        // THEN: Fails with error message
            .andExpect(status().isOk())
            .andExpect(view().name("register"))
            .andExpect(model().attributeExists("error"));
        
        // AND: User not saved
        assertEquals(0, userRepository.count());
    }
}
