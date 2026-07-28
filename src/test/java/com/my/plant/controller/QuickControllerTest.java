package com.my.plant.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QuickControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private QuickController quickController;

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── GET /quick/home ───────────────────────────────────────────────────────

    @Test
    public void getHome_redirectsToHome_onSuccessfulAuth() {
        Authentication auth = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(request.getSession(true)).thenReturn(session);

        String result = quickController.getHome(request, "user@example.com", "password");

        assertEquals("redirect:/home", result);
    }

    @Test
    public void getHome_authenticatesWithProvidedCredentials() {
        Authentication auth = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(true)).thenReturn(session);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        quickController.getHome(request, "user@example.com", "secret");

        verify(authenticationManager).authenticate(argThat(token ->
            token instanceof UsernamePasswordAuthenticationToken &&
            "user@example.com".equals(token.getPrincipal()) &&
            "secret".equals(token.getCredentials())
        ));
    }

    @Test
    public void getHome_setsAuthenticationInSecurityContext() {
        Authentication auth = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(request.getSession(true)).thenReturn(session);

        quickController.getHome(request, "user@example.com", "password");

        assertEquals(auth, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void getHome_savesSecurityContextInSession() {
        Authentication auth = mock(Authentication.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(request.getSession(true)).thenReturn(session);

        quickController.getHome(request, "user@example.com", "password");

        verify(session).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any());
    }

    @Test
    public void getHome_throwsBadCredentialsException_whenAuthFails() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
            () -> quickController.getHome(request, "user@example.com", "wrongpassword"));
    }
}
