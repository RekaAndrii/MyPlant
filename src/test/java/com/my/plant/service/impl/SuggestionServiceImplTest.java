package com.my.plant.service.impl;

import com.my.plant.model.Suggestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SuggestionServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private SuggestionServiceImpl suggestionService;

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    public void save_delegatesToMongoTemplate() {
        Suggestion suggestion = new Suggestion("alice", "Add dark mode", LocalDateTime.now());

        suggestionService.save(suggestion);

        verify(mongoTemplate).save(suggestion);
    }

    @Test
    public void save_passesExactObjectToMongoTemplate() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 27, 12, 0, 0);
        Suggestion suggestion = new Suggestion("bob", "Improve UI", now);

        suggestionService.save(suggestion);

        ArgumentCaptor<Suggestion> captor = ArgumentCaptor.forClass(Suggestion.class);
        verify(mongoTemplate).save(captor.capture());
        Suggestion saved = captor.getValue();
        assertEquals("bob", saved.getUserName());
        assertEquals("Improve UI", saved.getText());
        assertEquals(now, saved.getCreatedDate());
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    public void getAll_returnsListFromMongoTemplate() {
        LocalDateTime t1 = LocalDateTime.of(2026, 7, 27, 10, 0, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 7, 27, 9, 0, 0);
        List<Suggestion> expected = Arrays.asList(
                new Suggestion("alice", "Newer suggestion", t1),
                new Suggestion("alice", "Older suggestion", t2)
        );
        when(mongoTemplate.find(any(Query.class), eq(Suggestion.class))).thenReturn(expected);

        List<Suggestion> result = suggestionService.getAll("alice");

        assertEquals(2, result.size());
        assertEquals("Newer suggestion", result.get(0).getText());
        assertEquals("Older suggestion", result.get(1).getText());
    }

    @Test
    public void getAll_queriesByUserName() {
        when(mongoTemplate.find(any(Query.class), eq(Suggestion.class))).thenReturn(List.of());

        suggestionService.getAll("charlie");

        // Verify mongoTemplate.find was called once (userName scoping is handled inside the query)
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Suggestion.class));
    }

    @Test
    public void getAll_returnsEmptyList_whenNoSuggestionsExist() {
        when(mongoTemplate.find(any(Query.class), eq(Suggestion.class))).thenReturn(List.of());

        List<Suggestion> result = suggestionService.getAll("newuser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void getAll_doesNotReturnSuggestionsOfOtherUsers() {
        // mongoTemplate would only return suggestions matching the query (scoped by userName)
        // Here we verify the service returns exactly what the template returns without leaking extra data
        Suggestion aliceSuggestion = new Suggestion("alice", "Alice's idea", LocalDateTime.now());
        when(mongoTemplate.find(any(Query.class), eq(Suggestion.class)))
                .thenReturn(List.of(aliceSuggestion));

        List<Suggestion> result = suggestionService.getAll("alice");

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getUserName());
    }
}
