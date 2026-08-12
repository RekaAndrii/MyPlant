package com.my.plant.controller;

import com.my.plant.model.Block;
import com.my.plant.model.Goal;
import com.my.plant.model.GoalStep;
import com.my.plant.model.PomodoroSession;
import com.my.plant.service.BlockService;
import com.my.plant.service.GoalService;
import com.my.plant.service.GoalStepService;
import com.my.plant.service.PomodoroSessionService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import com.my.plant.util.dto.PomodoroSessionRequest;
import com.my.plant.util.dto.PomodoroSessionTagUpdateRequest;
import com.my.plant.util.dto.PomodoroTagOption;
import com.my.plant.util.dto.PomodoroTagOptionsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/pomodoro")
public class PomodoroController {

    private static final long DEFAULT_FOCUS_DURATION_SECONDS = 25 * 60;
    private static final Set<Long> VALID_FOCUS_DURATIONS_SECONDS = new LinkedHashSet<>(Arrays.asList(
            15 * 60L,
            25 * 60L,
            50 * 60L
    ));

    @Autowired
    private PomodoroSessionService pomodoroSessionService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalStepService goalStepService;

    @Autowired
    private BlockService blockService;

    @GetMapping("")
    public ModelAndView getPomodoroPage(ModelAndView model) {
        model.setViewName("pomodoro");
        return model;
    }

    @GetMapping("/sessions")
    public @ResponseBody List<PomodoroSession> getAll() {
        return pomodoroSessionService.getAll(UserUtil.getLogginedUserName());
    }

    @PostMapping("/sessions")
    public @ResponseBody AjaxResponse saveSession(@RequestBody PomodoroSessionRequest request) {
        if (request.getStartedAt() == null || request.getEndedAt() == null) {
            return new AjaxResponse(true, "Focus session start and end times are required.");
        }

        long plannedSeconds = request.getPlannedSeconds() > 0
                ? request.getPlannedSeconds()
                : DEFAULT_FOCUS_DURATION_SECONDS;
        if (!VALID_FOCUS_DURATIONS_SECONDS.contains(plannedSeconds)) {
            return new AjaxResponse(true, "Invalid focus session duration preset.");
        }

        long elapsedSeconds = Duration.between(request.getStartedAt(), request.getEndedAt()).getSeconds();
        if (!isValidDuration(elapsedSeconds, plannedSeconds, request.isCancelled())) {
            return new AjaxResponse(true, "Invalid focus session duration.");
        }

        LocalDateTime startedAt = LocalDateTime.ofInstant(request.getStartedAt(), ZoneId.systemDefault());
        LocalDateTime endedAt = LocalDateTime.ofInstant(request.getEndedAt(), ZoneId.systemDefault());
        PomodoroSession session = new PomodoroSession(
                UserUtil.getLogginedUserName(),
                startedAt,
                endedAt,
                elapsedSeconds,
                plannedSeconds,
                request.isCancelled()
        );
        session.setGoalStepIds(uniqueNonEmpty(request.safeGoalStepIds()));
        session.setBlockNames(uniqueNonEmpty(request.safeBlockNames()));
        pomodoroSessionService.save(session);
        return new AjaxResponse(false, "ok");
    }

    @DeleteMapping("/sessions/{id}")
    public @ResponseBody AjaxResponse deleteSession(@PathVariable("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            return new AjaxResponse(true, "Session id is required.");
        }
        pomodoroSessionService.delete(id, UserUtil.getLogginedUserName());
        return new AjaxResponse(false, "ok");
    }

    @PutMapping("/sessions/{id}/tags")
    public @ResponseBody AjaxResponse updateSessionTags(
            @PathVariable("id") String id,
            @RequestBody PomodoroSessionTagUpdateRequest request
    ) {
        if (id == null || id.trim().isEmpty()) {
            return new AjaxResponse(true, "Session id is required.");
        }

        pomodoroSessionService.updateTags(
                id,
                UserUtil.getLogginedUserName(),
                uniqueNonEmpty(request.safeGoalStepIds()),
                uniqueNonEmpty(request.safeBlockNames())
        );
        return new AjaxResponse(false, "ok");
    }

    @GetMapping("/tags/options")
    public @ResponseBody PomodoroTagOptionsResponse getTagOptions() {
        String userName = UserUtil.getLogginedUserName();
        List<Goal> goals = goalService.getAll(userName);

        List<PomodoroTagOption> goalStepOptions = new ArrayList<>();
        for (Goal goal : goals) {
            List<GoalStep> goalSteps = goalStepService.getByGoalId(goal.get_id(), userName);
            for (GoalStep goalStep : goalSteps) {
                String label = goal.getName() + ": " + goalStep.getName();
                goalStepOptions.add(new PomodoroTagOption(goalStep.get_id(), label));
            }
        }

        List<Block> blocks = blockService.getAllBlocks(userName);
        List<String> blockNames = new ArrayList<>();
        for (Block block : blocks) {
            blockNames.add(block.getName());
        }

        return new PomodoroTagOptionsResponse(goalStepOptions, blockNames);
    }

    private boolean isValidDuration(long elapsedSeconds, long plannedSeconds, boolean cancelled) {
        if (cancelled) {
            return elapsedSeconds >= 0 && elapsedSeconds < plannedSeconds;
        }
        return elapsedSeconds == plannedSeconds;
    }

    private List<String> uniqueNonEmpty(List<String> values) {
        Set<String> uniqueValues = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                uniqueValues.add(value);
            }
        }
        return new ArrayList<>(uniqueValues);
    }
}
