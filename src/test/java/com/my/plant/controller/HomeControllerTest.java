package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.service.BlockService;
import com.my.plant.service.HistoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HomeControllerTest {

    @Mock
    private BlockService blockService;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private HomeController homeController;

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
    public void getHome_addsYesterdayAndWeekAgoScores() {
        LocalDate today = LocalDateTime.now().minusHours(3).toLocalDate();
        LocalDate weekAgo = today.minusDays(7);
        when(blockService.getAllBlocks("alice")).thenReturn(new ArrayList<Block>());
        when(historyService.countDistinctBlocksOnDate("alice", today.minusDays(1))).thenReturn(5);
        when(historyService.countDistinctBlocksOnDate("alice", weekAgo)).thenReturn(3);

        ModelAndView model = homeController.getHome(new ModelAndView());

        assertEquals("index", model.getViewName());
        assertEquals(0L, model.getModel().get("todayScore"));
        assertEquals(5, model.getModel().get("yesterdayScore"));
        assertEquals(3, model.getModel().get("weekAgoScore"));
        assertEquals("Last " + weekAgo.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                model.getModel().get("weekAgoLabel"));
    }

    @Test
    public void getHome_fallsBackToZeroScores_whenHistoryFails() {
        when(blockService.getAllBlocks("alice")).thenReturn(new ArrayList<Block>());
        when(historyService.countDistinctBlocksOnDate(eq("alice"), any(LocalDate.class)))
                .thenThrow(new RuntimeException("db down"));

        ModelAndView model = homeController.getHome(new ModelAndView());

        assertNotNull(model.getModel().get("errorMessage"));
        assertEquals(0L, model.getModel().get("todayScore"));
        assertEquals(0, model.getModel().get("yesterdayScore"));
        assertEquals(0, model.getModel().get("weekAgoScore"));
    }
}
