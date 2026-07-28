package com.my.plant.service.impl;

import com.my.plant.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompositeUserDetailsServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private CompositeUserDetailsService compositeUserDetailsService;

    // ── loadUserByUsername ────────────────────────────────────────────────────

    @Test
    public void loadUserByUsername_returnsMongoUser_whenFoundByEmail() {
        User mongoUser = new User();
        mongoUser.setUserName("Andrii");
        mongoUser.setEmail("andrii@example.com");
        mongoUser.setPassword("{bcrypt}hashedpassword");
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(mongoUser);

        UserDetails details = compositeUserDetailsService.loadUserByUsername("andrii@example.com");

        assertNotNull(details);
        assertEquals("Andrii", details.getUsername());
    }

    @Test
    public void loadUserByUsername_throwsUsernameNotFoundException_whenNotFoundInMongo() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> compositeUserDetailsService.loadUserByUsername("nobody@nowhere.com"));
    }
}
