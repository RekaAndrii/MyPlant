package com.my.plant.service.impl;

import com.my.plant.model.User;
import com.my.plant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService implementing hybrid authentication:
 * 1. First attempts to load user from MongoDB (newly registered users)
 * 2. Falls back to in-memory user store (backward compatibility for andrii, taras)
 * 
 * This enables seamless coexistence of legacy in-memory users with new 
 * database-persisted users while maintaining existing authentication flow.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    // Will be set by WebSecurityConfig
    private static InMemoryUserDetailsManager inMemoryUserDetailsManager;
    
    /**
     * Set the in-memory manager from WebSecurityConfig.
     * Called during bean initialization to avoid circular dependency.
     */
    public static void setInMemoryUserDetailsManager(InMemoryUserDetailsManager manager) {
        inMemoryUserDetailsManager = manager;
    }
    
    /**
     * Load user by username, trying MongoDB first, then in-memory fallback.
     *
     * @param username The username to load
     * @return UserDetails object containing user information and authorities
     * @throws UsernameNotFoundException if user not found in either store
     */
    @Override
    public UserDetails loadUserByUsername(String username) 
            throws UsernameNotFoundException {
        
        // Step 1: Try to load from MongoDB
        Optional<User> mongoUser = userRepository.findByUsername(username);
        if (mongoUser.isPresent()) {
            User user = mongoUser.get();
            
            // Convert roles to Spring Security authorities
            List<GrantedAuthority> authorities = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(
                    role.startsWith("ROLE_") ? role : "ROLE_" + role
                ))
                .collect(Collectors.toList());
            
            // Return UserDetails with bcrypt password hash
            return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())  // Already bcrypt hashed
                .authorities(authorities)
                .build();
        }
        
        // Step 2: Fall back to in-memory users (backward compatibility)
        if (inMemoryUserDetailsManager != null) {
            try {
                return inMemoryUserDetailsManager.loadUserByUsername(username);
            } catch (UsernameNotFoundException e) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
        }
        
        // No in-memory manager set yet
        throw new UsernameNotFoundException("User not found: " + username);
    }
}

