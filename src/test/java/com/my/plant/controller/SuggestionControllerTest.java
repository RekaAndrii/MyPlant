package com.my.plant.controller;

import com.my.plant.model.Suggestion;
import com.my.plant.service.SuggestionService;
import com.my.plant.util.UserUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SuggestionControllerTest {

    @Mock
    private SuggestionService suggestionService;

    @InjectMocks
    private SuggestionController suggestionController;

    private static final String TEST_USER = "alice";

    // ── GET /suggestions ──────────────────────────────────────────────────────

    @Test
    public void getSuggestionsPage_returnsCorrectViewName() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);
            when(suggestionService.getAll(TEST_USER)).thenReturn(List.of());

            ModelAndView mav = suggestionController.getSuggestionsPage(new ModelAndView());

            assertEquals("suggestions", mav.getViewName());
        }
    }

    @Test
    public void getSuggestionsPage_addsSuggestionsToModel() {
        List<Suggestion> suggestions = Arrays.asList(
                new Suggestion(TEST_USER, "Idea one", LocalDateTime.now()),
                new Suggestion(TEST_USER, "Idea two", LocalDateTime.now().minusHours(1))
        );

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);
            when(suggestionService.getAll(TEST_USER)).thenReturn(suggestions);

            ModelAndView mav = suggestionController.getSuggestionsPage(new ModelAndView());

            Object modelSuggestions = mav.getModel().get("suggestions");
            assertNotNull(modelSuggestions);
            assertEquals(suggestions, modelSuggestions);
        }
    }

    @Test
    public void getSuggestionsPage_addsErrorMessage_whenServiceThrows() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);
            when(suggestionService.getAll(TEST_USER)).thenThrow(new RuntimeException("DB error"));

            ModelAndView mav = suggestionController.getSuggestionsPage(new ModelAndView());

            assertEquals("suggestions", mav.getViewName());
            assertNotNull(mav.getModel().get("errorMessage"));
        }
    }

    // ── POST /suggestions ─────────────────────────────────────────────────────

    @Test
    public void addSuggestion_savesAndRedirects_whenTextIsValid() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            String result = suggestionController.addSuggestion("Great idea", new ModelAndView());

            verify(suggestionService).save(any(Suggestion.class));
            assertEquals("redirect:/suggestions", result);
        }
    }

    @Test
    public void addSuggestion_savesWithCorrectUserAndText() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            suggestionController.addSuggestion("  Trimmed text  ", new ModelAndView());

            verify(suggestionService).save(argThat(s ->
                    TEST_USER.equals(s.getUserName()) &&
                    "Trimmed text".equals(s.getText()) &&
                    s.getCreatedDate() != null
            ));
        }
    }

    @Test
    public void addSuggestion_doesNotSave_whenTextIsBlank() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            String result = suggestionController.addSuggestion("   ", new ModelAndView());

            verify(suggestionService, never()).save(any(Suggestion.class));
            assertEquals("redirect:/suggestions", result);
        }
    }

    @Test
    public void addSuggestion_doesNotSave_whenTextIsNull() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);

            String result = suggestionController.addSuggestion(null, new ModelAndView());

            verify(suggestionService, never()).save(any(Suggestion.class));
            assertEquals("redirect:/suggestions", result);
        }
    }

    @Test
    public void addSuggestion_redirectsEvenWhenServiceThrows() {
        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);
            doThrow(new RuntimeException("DB error")).when(suggestionService).save(any(Suggestion.class));

            String result = suggestionController.addSuggestion("Some idea", new ModelAndView());

            assertEquals("redirect:/suggestions", result);
        }
    }

    // ── GET /suggestions/all ──────────────────────────────────────────────────

    @Test
    public void getAll_returnsCurrentUserSuggestions() {
        List<Suggestion> suggestions = List.of(
                new Suggestion(TEST_USER, "Only mine", LocalDateTime.now())
        );

        try (MockedStatic<UserUtil> userUtil = mockStatic(UserUtil.class)) {
            userUtil.when(UserUtil::getLogginedUserName).thenReturn(TEST_USER);
            when(suggestionService.getAll(TEST_USER)).thenReturn(suggestions);

            List<Suggestion> result = suggestionController.getAll();

            assertEquals(1, result.size());
            assertEquals("Only mine", result.get(0).getText());
            verify(suggestionService).getAll(TEST_USER);
        }
    }
}
