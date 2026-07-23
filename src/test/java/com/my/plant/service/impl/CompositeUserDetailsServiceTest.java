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
    public void loadUserByUsername_returnsInMemoryUser_whenEmailNotInMongo() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(null);

        // "taras" is a hardcoded in-memory user — existing auth must still work
        UserDetails details = compositeUserDetailsService.loadUserByUsername("taras");

        assertNotNull(details);
        assertEquals("taras", details.getUsername());
    }

    @Test
    public void loadUserByUsername_throwsUsernameNotFoundException_whenNotFoundAnywhere() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(null);

        assertThrows(UsernameNotFoundException.class,
                () -> compositeUserDetailsService.loadUserByUsername("nobody@nowhere.com"));
    }
}
