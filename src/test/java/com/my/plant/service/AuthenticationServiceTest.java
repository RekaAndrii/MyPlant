package com.my.plant.service;

import com.my.plant.configs.MyPlantApplication;
import com.my.plant.model.User;
import com.my.plant.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for authentication mechanisms: CustomUserDetailsService and hybrid authentication.
 * Tests that both MongoDB users and in-memory fallback users work correctly.
 * 
 * Strategy: 1 positive test + 1 negative test per authentication scenario.
 */
@SpringBootTest(classes = MyPlantApplication.class)
class AuthenticationServiceTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @BeforeEach
    void setUp() {
        // Clear database before each test to ensure isolation
        userRepository.deleteAll();
    }
    
    // ============ MONGODB USER AUTHENTICATION TESTS ============
    
    /**
     * POSITIVE: User registered in MongoDB should authenticate successfully.
     * CustomUserDetailsService should load MongoDB user with correct authorities.
     */
    @Test
    void loadValidMongoUserSucceeds() {
        // GIVEN: User registered in MongoDB with bcrypt password
        User mongoUser = new User();
        mongoUser.setUsername("mongo_user");
        mongoUser.setEmail("mongo@example.com");
        mongoUser.setPasswordHash(passwordEncoder.encode("password123"));
        mongoUser.setRoles(Set.of("USER"));
        mongoUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(mongoUser);
        
        // WHEN: Load user by username via UserDetailsService
        UserDetails userDetails = userDetailsService.loadUserByUsername("mongo_user");
        
        // THEN: User loaded successfully from MongoDB
        assertNotNull(userDetails, "MongoDB user should be found");
        assertEquals("mongo_user", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
            "User should have ROLE_USER authority");
        
        // AND: Password is bcrypt hash (can be verified)
        assertTrue(passwordEncoder.matches("password123", userDetails.getPassword()),
            "Password should be bcrypt hashed and verifiable");
        
        // AND: Account is enabled
        assertTrue(userDetails.isEnabled(), "Account should be enabled");
    }
    
    /**
     * NEGATIVE: Non-existent MongoDB user should fail to load.
     * Should throw UsernameNotFoundException after checking MongoDB and in-memory fallback.
     */
    @Test
    void loadInvalidMongoUserFails() {
        // WHEN/THEN: Non-existent user throws exception
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonexistent_mongo");
        });
        
        // AND: Exception message indicates user not found
        assertTrue(exception.getMessage().contains("User not found"));
    }
    
    // ============ IN-MEMORY USER AUTHENTICATION TESTS ============
    
    /**
     * POSITIVE: Existing in-memory user should authenticate successfully (backward compatibility).
     * Should load "andrii" user from in-memory store as fallback.
     */
    @Test
    void loadValidInMemoryUserSucceeds() {
        // WHEN: Load in-memory user "andrii"
        UserDetails userDetails = userDetailsService.loadUserByUsername("andrii");
        
        // THEN: In-memory user loaded successfully
        assertNotNull(userDetails, "In-memory user should be found");
        assertEquals("andrii", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
            "User should have ROLE_USER authority");
        
        // AND: Account is enabled
        assertTrue(userDetails.isEnabled(), "Account should be enabled");
    }
    
    /**
     * NEGATIVE: Non-existent in-memory user should fail to load.
     * Should throw UsernameNotFoundException.
     */
    @Test
    void loadInvalidInMemoryUserFails() {
        // WHEN/THEN: Non-existent in-memory user throws exception
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonexistent_user");
        });
        
        // AND: Exception message indicates user not found
        assertTrue(exception.getMessage().contains("User not found"));
    }
    
    // ============ HYBRID AUTHENTICATION TESTS ============
    
    /**
     * POSITIVE: MongoDB users should take precedence over in-memory users.
     * If MongoDB has a user, it should be returned instead of in-memory fallback.
     */
    @Test
    void mongoUserTakesPrecedenceOverInMemory() {
        // GIVEN: MongoDB user with same name as in-memory user
        User mongoUser = new User();
        mongoUser.setUsername("taras");  // Same as in-memory user
        mongoUser.setEmail("taras@example.com");
        mongoUser.setPasswordHash(passwordEncoder.encode("newpassword"));
        mongoUser.setRoles(Set.of("ADMIN"));  // Different role than in-memory
        mongoUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(mongoUser);
        
        // WHEN: Load user by username
        UserDetails userDetails = userDetailsService.loadUserByUsername("taras");
        
        // THEN: MongoDB user returned (not in-memory user)
        assertNotNull(userDetails);
        assertEquals("taras", userDetails.getUsername());
        
        // AND: Has ADMIN role from MongoDB, not USER role from in-memory
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
            "Should have ADMIN role from MongoDB user, not USER from in-memory");
    }
    
    /**
     * NEGATIVE: Invalid user should fail in both MongoDB and in-memory stores.
     * Even with fallback mechanism, completely invalid user should throw exception.
     */
    @Test
    void invalidUserFailsInBothStores() {
        // WHEN/THEN: User not in MongoDB and not in in-memory fails
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("completely_invalid_user");
        });
        
        // AND: Exception is thrown after checking both stores
        assertTrue(exception.getMessage().contains("User not found"));
    }
    
    // ============ MULTIPLE ROLE HANDLING TESTS ============
    
    /**
     * POSITIVE: User with multiple roles should load all roles correctly.
     * Roles should be converted to Spring Security GrantedAuthority objects.
     */
    @Test
    void loadUserWithMultipleRolesSucceeds() {
        // GIVEN: User with multiple roles
        User multiRoleUser = new User();
        multiRoleUser.setUsername("admin_user");
        multiRoleUser.setEmail("admin@example.com");
        multiRoleUser.setPasswordHash(passwordEncoder.encode("password"));
        multiRoleUser.setRoles(Set.of("USER", "ADMIN", "MODERATOR"));
        multiRoleUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(multiRoleUser);
        
        // WHEN: Load user by username
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin_user");
        
        // THEN: All roles are loaded as authorities
        assertNotNull(userDetails);
        assertEquals(3, userDetails.getAuthorities().size(), "All 3 roles should be loaded");
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR")));
    }
}
