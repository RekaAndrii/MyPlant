package com.my.plant.service.impl;

import com.my.plant.model.PomodoroSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PomodoroSessionServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PomodoroSessionServiceImpl pomodoroSessionService;

    @Test
    public void save_persistsPomodoroSession() {
        PomodoroSession session = new PomodoroSession(
                "alice",
                LocalDateTime.of(2026, 8, 7, 10, 0),
                LocalDateTime.of(2026, 8, 7, 10, 25),
                1500,
                false
        );

        pomodoroSessionService.save(session);

        verify(mongoTemplate).save(session);
    }

    @Test
    public void getAll_queriesOnlyCurrentUsersSessions() {
        when(mongoTemplate.find(org.mockito.ArgumentMatchers.any(Query.class), eq(PomodoroSession.class)))
                .thenReturn(Collections.emptyList());

        pomodoroSessionService.getAll("alice");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(queryCaptor.capture(), eq(PomodoroSession.class));
        assertEquals("alice", queryCaptor.getValue().getQueryObject().getString("userName"));
    }
}
