package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.model.Goal;
import com.my.plant.model.GoalStep;
import com.my.plant.model.PomodoroSession;
import com.my.plant.service.BlockService;
import com.my.plant.service.GoalService;
import com.my.plant.service.GoalStepService;
import com.my.plant.service.PomodoroSessionService;
import com.my.plant.util.dto.AjaxResponse;
import com.my.plant.util.dto.PomodoroSessionRequest;
import com.my.plant.util.dto.PomodoroSessionTagUpdateRequest;
import com.my.plant.util.dto.PomodoroTagOptionsResponse;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PomodoroControllerTest {

    @Mock
    private PomodoroSessionService pomodoroSessionService;

    @Mock
    private GoalService goalService;

    @Mock
    private GoalStepService goalStepService;

    @Mock
    private BlockService blockService;

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
        assertEquals(1500, saved.getPlannedSeconds());
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

    @Test
    public void saveSession_acceptsCompletedSessionForFiftyMinutePreset() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:50:00Z",
                false
        );
        request.setPlannedSeconds(50 * 60);

        AjaxResponse response = pomodoroController.saveSession(request);

        assertFalse(response.isHasError());
        ArgumentCaptor<PomodoroSession> sessionCaptor = ArgumentCaptor.forClass(PomodoroSession.class);
        verify(pomodoroSessionService).save(sessionCaptor.capture());
        assertEquals(3000, sessionCaptor.getValue().getElapsedSeconds());
        assertEquals(3000, sessionCaptor.getValue().getPlannedSeconds());
    }

    @Test
    public void saveSession_rejectsUnknownDurationPreset() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:10:00Z",
                false
        );
        request.setPlannedSeconds(10 * 60);

        AjaxResponse response = pomodoroController.saveSession(request);

        assertTrue(response.isHasError());
        verify(pomodoroSessionService, never()).save(org.mockito.ArgumentMatchers.any(PomodoroSession.class));
    }

    @Test
    public void saveSession_defaultsToTwentyFiveMinutesWhenPlannedSecondsAbsent() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:25:00Z",
                false
        );

        AjaxResponse response = pomodoroController.saveSession(request);

        assertFalse(response.isHasError());
        ArgumentCaptor<PomodoroSession> sessionCaptor = ArgumentCaptor.forClass(PomodoroSession.class);
        verify(pomodoroSessionService).save(sessionCaptor.capture());
        assertEquals(1500, sessionCaptor.getValue().getPlannedSeconds());
    }

    @Test
    public void saveSession_persistsPreSelectedTags() {
        PomodoroSessionRequest request = request(
                "2026-08-07T08:00:00Z",
                "2026-08-07T08:25:00Z",
                false
        );
        request.setGoalStepIds(Arrays.asList("step-1", "step-1"));
        request.setBlockNames(Arrays.asList("Morning", ""));

        AjaxResponse response = pomodoroController.saveSession(request);

        assertFalse(response.isHasError());
        ArgumentCaptor<PomodoroSession> sessionCaptor = ArgumentCaptor.forClass(PomodoroSession.class);
        verify(pomodoroSessionService).save(sessionCaptor.capture());
        assertEquals(Arrays.asList("step-1"), sessionCaptor.getValue().getGoalStepIds());
        assertEquals(Arrays.asList("Morning"), sessionCaptor.getValue().getBlockNames());
    }

    @Test
    public void deleteSession_callsServiceForLoggedInUser() {
        AjaxResponse response = pomodoroController.deleteSession("session-1");

        assertFalse(response.isHasError());
        verify(pomodoroSessionService).delete("session-1", "alice");
    }

    @Test
    public void updateSessionTags_callsServiceWithDistinctLists() {
        PomodoroSessionTagUpdateRequest request = new PomodoroSessionTagUpdateRequest();
        request.setGoalStepIds(Arrays.asList("step-1", "", "step-1", "step-2"));
        request.setBlockNames(Arrays.asList("Morning", "Morning", "Deep Work"));

        AjaxResponse response = pomodoroController.updateSessionTags("session-2", request);

        assertFalse(response.isHasError());
        verify(pomodoroSessionService).updateTags(
                "session-2",
                "alice",
                Arrays.asList("step-1", "step-2"),
                Arrays.asList("Morning", "Deep Work")
        );
    }

    @Test
    public void getTagOptions_returnsGoalStepAndBlockOptions() {
        Goal goal = new Goal("alice", "Fitness", LocalDate.of(2026, 8, 7));
        goal.set_id("goal-1");

        GoalStep goalStep = new GoalStep("alice", "goal-1", "Morning walk", null, 0);
        goalStep.set_id("step-1");

        Block block = new Block();
        block.setName("Routine block");

        when(goalService.getAll("alice")).thenReturn(Collections.singletonList(goal));
        when(goalStepService.getByGoalId("goal-1", "alice")).thenReturn(Collections.singletonList(goalStep));
        when(blockService.getAllBlocks("alice")).thenReturn(Collections.singletonList(block));

        PomodoroTagOptionsResponse response = pomodoroController.getTagOptions();

        assertNotNull(response);
        assertEquals(1, response.getGoalSteps().size());
        assertEquals("step-1", response.getGoalSteps().get(0).getId());
        assertEquals("Fitness: Morning walk", response.getGoalSteps().get(0).getLabel());
        assertEquals(Arrays.asList("Routine block"), response.getBlockNames());
    }

    private PomodoroSessionRequest request(String startedAt, String endedAt, boolean cancelled) {
        PomodoroSessionRequest request = new PomodoroSessionRequest();
        request.setStartedAt(Instant.parse(startedAt));
        request.setEndedAt(Instant.parse(endedAt));
        request.setCancelled(cancelled);
        return request;
    }
}
