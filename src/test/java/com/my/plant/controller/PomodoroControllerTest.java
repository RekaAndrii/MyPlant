package com.my.plant.controller;

import com.my.plant.model.PomodoroSession;
import com.my.plant.service.PomodoroSessionService;
import com.my.plant.util.dto.AjaxResponse;
import com.my.plant.util.dto.PomodoroSessionRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PomodoroControllerTest {

    @Mock
    private PomodoroSessionService pomodoroSessionService;

    @InjectMocks
    private PomodoroController pomodoroController;

    @BeforeEach
    public void setUpAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "password")
        );
    }

    @AfterEach
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void getPomodoroPage_returnsTimerView() {
        ModelAndView model = pomodoroController.getPomodoroPage(new ModelAndView());

        assertEquals("pomodoro", model.getViewName());
    }

    @Test
    public void saveSession_persistsCompletedFocusForLoggedInUser() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:25:00Z",
                false
        );

        AjaxResponse response = pomodoroController.saveSession(request);

        assertFalse(response.isHasError());
        ArgumentCaptor<PomodoroSession> sessionCaptor = ArgumentCaptor.forClass(PomodoroSession.class);
        verify(pomodoroSessionService).save(sessionCaptor.capture());
        PomodoroSession saved = sessionCaptor.getValue();
        assertEquals("alice", saved.getUserName());
        assertEquals(1500, saved.getElapsedSeconds());
        assertFalse(saved.isCancelled());
    }

    @Test
    public void saveSession_persistsCancelledFocusWithActualDuration() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:05:00Z",
                true
        );

        AjaxResponse response = pomodoroController.saveSession(request);

        assertFalse(response.isHasError());
        ArgumentCaptor<PomodoroSession> sessionCaptor = ArgumentCaptor.forClass(PomodoroSession.class);
        verify(pomodoroSessionService).save(sessionCaptor.capture());
        assertEquals(300, sessionCaptor.getValue().getElapsedSeconds());
        assertTrue(sessionCaptor.getValue().isCancelled());
    }

    @Test
    public void saveSession_rejectsIncompleteSessionMarkedComplete() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:24:59Z",
                false
        );

        AjaxResponse response = pomodoroController.saveSession(request);

        assertTrue(response.isHasError());
        verify(pomodoroSessionService, never()).save(org.mockito.ArgumentMatchers.any(PomodoroSession.class));
    }

    private PomodoroSessionRequest request(String startedAt, String endedAt, boolean cancelled) {
        PomodoroSessionRequest request = new PomodoroSessionRequest();
        request.setStartedAt(Instant.parse(startedAt));
        request.setEndedAt(Instant.parse(endedAt));
        request.setCancelled(cancelled);
        return request;
    }
}
