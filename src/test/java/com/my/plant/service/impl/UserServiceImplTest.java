package com.my.plant.service.impl;

import com.my.plant.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    // ── emailExists ──────────────────────────────────────────────────────────

    @Test
    public void emailExists_returnsTrue_whenUserFoundInDb() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class)))
                .thenReturn(new User("alice", "alice@example.com", "encoded"));

        assertTrue(userService.emailExists("alice@example.com"));
    }

    @Test
    public void emailExists_returnsFalse_whenUserNotInDb() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class)))
                .thenReturn(null);

        assertFalse(userService.emailExists("unknown@example.com"));
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    public void register_savesUserWithEncodedPassword_whenInputIsValid() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class))).thenReturn(null);
        when(passwordEncoder.encode("pass1234")).thenReturn("{bcrypt}hashedpassword");

        userService.register("alice", "alice@example.com", "pass1234");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(mongoTemplate).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("alice", saved.getUserName());
        assertEquals("alice@example.com", saved.getEmail());
        assertEquals("{bcrypt}hashedpassword", saved.getPassword());
    }

    @Test
    public void register_throwsIllegalArgumentException_whenEmailAlreadyExists() {
        when(mongoTemplate.findOne(any(Query.class), eq(User.class)))
                .thenReturn(new User("alice", "alice@example.com", "encoded"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice", "alice@example.com", "pass1234"));

        assertEquals("An account with this email already exists.", ex.getMessage());
        verify(mongoTemplate, never()).save(any(User.class));
    }
}
