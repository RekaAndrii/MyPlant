package com.my.plant.controller;

import com.my.plant.model.Achievement;
import com.my.plant.model.Block;
import com.my.plant.model.Goal;
import com.my.plant.model.GoalStep;
import com.my.plant.service.AchievementService;
import com.my.plant.service.BlockService;
import com.my.plant.service.GoalService;
import com.my.plant.service.GoalStepService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by User on 31.07.2026.
 */
@Controller
@RequestMapping("/goals")
public class GoalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoalController.class);
    private static final String AJAX_OK = "ok";

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalStepService goalStepService;

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private BlockService blockService;

    @GetMapping("")
    public ModelAndView getGoalsPage(ModelAndView model) {
        String userName = UserUtil.getLogginedUserName();
        try {
            List<Goal> goals = goalService.getAll(userName);

            Map<String, List<GoalStep>> stepsByGoalId = new LinkedHashMap<>();
            for (Goal goal : goals) {
                List<GoalStep> steps = goalStepService.getByGoalId(goal.get_id(), userName);
                stepsByGoalId.put(goal.get_id(), steps);
            }

            List<Achievement> unlinkedAchievements = achievementService.getUnlinked(userName);

            List<Block> allBlocks = blockService.getAllBlocks(userName);
            List<String> availableBlocks = new ArrayList<>();
            for (Block b : allBlocks) {
                availableBlocks.add(b.getName());
            }

            model.addObject("goals", goals);
            model.addObject("stepsByGoalId", stepsByGoalId);
            model.addObject("unlinkedAchievements", unlinkedAchievements);
            model.addObject("availableBlocks", availableBlocks);
        } catch (Exception ex) {
            LOGGER.error("Failed to load goals page", ex);
            model.addObject("errorMessage", "Unable to load goals. Please try again.");
        }
        model.setViewName("goals");
        return model;
    }

    // ── Goal CRUD ─────────────────────────────────────────────────────────────

    @PostMapping("/")
    @ResponseBody
    public AjaxResponse createGoal(@RequestBody Map<String, String> body) {
        String userName = UserUtil.getLogginedUserName();
        String name = body.get("name");
        Goal goal = new Goal(userName, name, LocalDate.now());
        goalService.save(goal);
        return new AjaxResponse(false, AJAX_OK);
    }

    @PutMapping("/{id}")
    @ResponseBody
    public AjaxResponse updateGoal(@PathVariable("id") String id,
                                   @RequestBody Map<String, String> body) {
        String userName = UserUtil.getLogginedUserName();
        goalService.update(id, body.get("name"), userName);
        return new AjaxResponse(false, AJAX_OK);
    }

    @PutMapping("/{id}/done")
    @ResponseBody
    public AjaxResponse toggleGoalDone(@PathVariable("id") String id,
                                       @RequestBody Map<String, Object> body) {
        String userName = UserUtil.getLogginedUserName();
        boolean done = Boolean.TRUE.equals(body.get("done"));
        goalService.markDone(id, done, userName);
        return new AjaxResponse(false, AJAX_OK);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public AjaxResponse deleteGoal(@PathVariable("id") String id) {
        String userName = UserUtil.getLogginedUserName();
        goalStepService.deleteByGoalId(id, userName);
        goalService.delete(id, userName);
        return new AjaxResponse(false, AJAX_OK);
    }

    // ── Step CRUD ─────────────────────────────────────────────────────────────

    @PostMapping("/{goalId}/steps")
    @ResponseBody
    public AjaxResponse addStep(@PathVariable("goalId") String goalId,
                                @RequestBody Map<String, Object> body) {
        String userName = UserUtil.getLogginedUserName();
        List<String> linkedBlockNames = parseStringList(body.get("linkedBlockNames"));
        GoalStep step = new GoalStep(
                userName,
                goalId,
                (String) body.get("name"),
                linkedBlockNames.isEmpty() ? null : linkedBlockNames,
                0
        );
        goalStepService.save(step);
        return new AjaxResponse(false, AJAX_OK);
    }

    @PutMapping("/steps/{stepId}")
    @ResponseBody
    public AjaxResponse updateStep(@PathVariable("stepId") String stepId,
                                   @RequestBody Map<String, Object> body) {
        String userName = UserUtil.getLogginedUserName();
        List<String> linkedBlockNames = parseStringList(body.get("linkedBlockNames"));
        goalStepService.update(stepId, (String) body.get("name"), linkedBlockNames, userName);
        return new AjaxResponse(false, AJAX_OK);
    }

    @PutMapping("/steps/{stepId}/done")
    @ResponseBody
    public AjaxResponse toggleStepDone(@PathVariable("stepId") String stepId,
                                       @RequestBody Map<String, Object> body) {
        String userName = UserUtil.getLogginedUserName();
        boolean done = Boolean.TRUE.equals(body.get("done"));
        goalStepService.markDone(stepId, done, userName);
        return new AjaxResponse(false, AJAX_OK);
    }

    @PutMapping("/steps/{stepId}/move")
    @ResponseBody
    public AjaxResponse moveStep(@PathVariable("stepId") String stepId,
                                 @RequestBody Map<String, String> body) {
        String userName = UserUtil.getLogginedUserName();
        String direction = body.get("direction");
        if ("up".equals(direction)) {
            goalStepService.moveUp(stepId, userName);
        } else if ("down".equals(direction)) {
            goalStepService.moveDown(stepId, userName);
        }
        return new AjaxResponse(false, AJAX_OK);
    }

    @DeleteMapping("/steps/{stepId}")
    @ResponseBody
    public AjaxResponse deleteStep(@PathVariable("stepId") String stepId) {
        String userName = UserUtil.getLogginedUserName();
        goalStepService.delete(stepId, userName);
        return new AjaxResponse(false, AJAX_OK);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object raw) {
        if (raw instanceof List) {
            return ((List<?>) raw).stream()
                    .filter(o -> o instanceof String && !((String) o).isEmpty())
                    .map(o -> (String) o)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
