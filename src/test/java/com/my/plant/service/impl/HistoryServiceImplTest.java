package com.my.plant.service.impl;

import com.my.plant.model.HistoryAction;
import com.my.plant.model.HistoryItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HistoryServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private HistoryItem item(String blockName, LocalDateTime time) {
        return new HistoryItem("alice", blockName, HistoryAction.EXECUTED, time);
    }

    @Test
    public void countDistinctBlocksOnDate_countsEachBlockOnce() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 15, 10, 0);
        when(mongoTemplate.find(any(Query.class), eq(HistoryItem.class))).thenReturn(Arrays.asList(
                item("Water", time),
                item("Water", time.plusHours(2)),
                item("Read", time.plusHours(3))
        ));

        int count = historyService.countDistinctBlocksOnDate("alice", LocalDate.of(2026, 10, 15));

        assertEquals(2, count);
    }

    @Test
    public void countDistinctBlocksOnDate_returnsZero_whenNoHistory() {
        when(mongoTemplate.find(any(Query.class), eq(HistoryItem.class))).thenReturn(Collections.emptyList());

        assertEquals(0, historyService.countDistinctBlocksOnDate("alice", LocalDate.of(2026, 10, 15)));
    }

    @Test
    public void countDistinctBlocksOnDate_scopesQueryByUserAndShiftedDayWindow() {
        when(mongoTemplate.find(any(Query.class), eq(HistoryItem.class))).thenReturn(Collections.emptyList());

        historyService.countDistinctBlocksOnDate("alice", LocalDate.of(2026, 10, 15));

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(HistoryItem.class));
        String query = captor.getValue().getQueryObject().toString();
        assertTrue(query.contains("userName"), query);
        assertTrue(query.contains("alice"), query);
        assertTrue(query.contains("2026-10-15T03:00"), query);
        assertTrue(query.contains("2026-10-16T03:00"), query);
    }
}
