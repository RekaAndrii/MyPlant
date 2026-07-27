package com.my.plant.controller;

import com.my.plant.model.Suggestion;
import com.my.plant.service.SuggestionService;
import com.my.plant.util.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by User on 27.07.2026.
 */
@Controller
@RequestMapping("/suggestions")
public class SuggestionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuggestionController.class);

    @Autowired
    private SuggestionService suggestionService;

    @GetMapping("")
    public ModelAndView getSuggestionsPage(ModelAndView model) {
        List<Suggestion> suggestions = null;
        try {
            suggestions = suggestionService.getAll(UserUtil.getLogginedUserName());
        } catch (Exception ex) {
            LOGGER.error("Failed to load suggestions", ex);
            model.addObject("errorMessage", "Unable to load suggestions. Please try again.");
        }
        model.addObject("suggestions", suggestions);
        model.setViewName("suggestions");
        return model;
    }

    @PostMapping("")
    public String addSuggestion(@RequestParam(value = "text", required = false) String text, ModelAndView model) {
        if (text != null && !text.trim().isEmpty()) {
            try {
                Suggestion suggestion = new Suggestion(
                        UserUtil.getLogginedUserName(),
                        text.trim(),
                        LocalDateTime.now()
                );
                suggestionService.save(suggestion);
            } catch (Exception ex) {
                LOGGER.error("Failed to save suggestion", ex);
            }
        }
        return "redirect:/suggestions";
    }

    @GetMapping("/all")
    @Operation(summary = "getAllSuggestions", operationId = "getAllSuggestions")
    public @ResponseBody List<Suggestion> getAll() {
        return suggestionService.getAll(UserUtil.getLogginedUserName());
    }
}
