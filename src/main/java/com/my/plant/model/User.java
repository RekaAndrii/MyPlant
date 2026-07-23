package com.my.plant.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * MongoDB document representing a registered user account.
 * Each user has unique username and email.
 */
@Document(collection = "user")
public class User {
    
    @Id
    private String id;                          // MongoDB ObjectId as String
    
    @Indexed(unique = true)
    private String username;                    // Unique username
    
    @Indexed(unique = true)
    private String email;                       // Unique email address
    
    private String passwordHash;                // BCrypt hashed password
    
    private Set<String> roles;                  // User roles (default: ["USER"])
    
    private LocalDateTime createdAt;            // Registration timestamp
    
    // Constructors
    public User() {
    }
    
    public User(String username, String email, String passwordHash, 
                Set<String> roles, LocalDateTime createdAt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    public Set<String> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", createdAt=" + createdAt +
                '}';
    }
}
