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
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void delete_removesOnlyRequestedUsersSession() {
        pomodoroSessionService.delete("session-1", "alice");

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).remove(queryCaptor.capture(), eq(PomodoroSession.class));

        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertTrue(queryJson.contains("session-1"));
        assertTrue(queryJson.contains("alice"));
    }

    @Test
    public void updateTags_updatesTagsForRequestedUsersSession() {
        pomodoroSessionService.updateTags("session-2", "alice",
                Arrays.asList("step-1", "step-2"), Arrays.asList("Morning", "Focus"));

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(queryCaptor.capture(), updateCaptor.capture(), eq(PomodoroSession.class));

        String queryJson = queryCaptor.getValue().getQueryObject().toJson();
        assertTrue(queryJson.contains("session-2"));
        assertTrue(queryJson.contains("alice"));
        assertEquals(Arrays.asList("step-1", "step-2"), updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class).get("goalStepIds"));
        assertEquals(Arrays.asList("Morning", "Focus"), updateCaptor.getValue().getUpdateObject().get("$set", org.bson.Document.class).get("blockNames"));
    }
}
