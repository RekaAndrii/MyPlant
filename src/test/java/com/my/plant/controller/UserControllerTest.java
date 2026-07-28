package com.my.plant.controller;

import com.my.plant.service.UserService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private static final String TEST_USER = "alice";

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ── DELETE /user/me ───────────────────────────────────────────────────────

    @Test
    public void deleteCurrentUser_returnsAjaxOk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            AjaxResponse response = userController.deleteCurrentUser(request);

            assertNotNull(response);
            assertFalse(response.isHasError());
            assertEquals("ok", response.getMessage());
        }
    }

    @Test
    public void deleteCurrentUser_callsServiceWithCorrectUserName() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            userController.deleteCurrentUser(request);

            verify(userService).deleteCurrentUser(TEST_USER);
        }
    }

    @Test
    public void deleteCurrentUser_invalidatesSession_whenSessionExists() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            userController.deleteCurrentUser(request);

            verify(session).invalidate();
        }
    }

    @Test
    public void deleteCurrentUser_doesNotThrow_whenSessionIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            assertDoesNotThrow(() -> userController.deleteCurrentUser(request));
        }
    }

    @Test
    public void deleteCurrentUser_clearsSecurityContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            userController.deleteCurrentUser(request);

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }
}
