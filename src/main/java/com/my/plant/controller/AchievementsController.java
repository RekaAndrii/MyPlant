package com.my.plant.controller;

import com.my.plant.model.Achievement;
import com.my.plant.service.AchievementService;
import com.my.plant.util.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by User on 24.07.2026.
 */
@Controller
@RequestMapping("/achievements")
public class AchievementsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementsController.class);

    @Autowired
    private AchievementService achievementService;

    @GetMapping("")
    public ModelAndView getAchievementsPage(ModelAndView model) {
        List<Achievement> achievements = null;
        try {
            achievements = achievementService.getAll(UserUtil.getLogginedUserName());
        } catch (Exception ex) {
            LOGGER.error("Failed to load achievements", ex);
            model.addObject("errorMessage", "Unable to load achievements. Please try again.");
        }
        model.addObject("achievements", achievements);
        model.setViewName("achievements");
        return model;
    }

    @GetMapping("/all")
    @Operation(summary = "getAllAchievements", operationId = "getAllAchievements")
    public @ResponseBody List<Achievement> getAll() {
        return achievementService.getAll(UserUtil.getLogginedUserName());
    }
}
