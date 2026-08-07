package com.my.plant.controller;

import com.my.plant.model.PomodoroSession;
import com.my.plant.service.PomodoroSessionService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import com.my.plant.util.dto.PomodoroSessionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Controller
@RequestMapping("/pomodoro")
public class PomodoroController {

    private static final long FOCUS_DURATION_SECONDS = 25 * 60;

    @Autowired
    private PomodoroSessionService pomodoroSessionService;

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

        long elapsedSeconds = Duration.between(request.getStartedAt(), request.getEndedAt()).getSeconds();
        if (!isValidDuration(elapsedSeconds, request.isCancelled())) {
            return new AjaxResponse(true, "Invalid focus session duration.");
        }

        LocalDateTime startedAt = LocalDateTime.ofInstant(request.getStartedAt(), ZoneId.systemDefault());
        LocalDateTime endedAt = LocalDateTime.ofInstant(request.getEndedAt(), ZoneId.systemDefault());
        pomodoroSessionService.save(new PomodoroSession(
                UserUtil.getLogginedUserName(),
                startedAt,
                endedAt,
                elapsedSeconds,
                request.isCancelled()
        ));
        return new AjaxResponse(false, "ok");
    }

    private boolean isValidDuration(long elapsedSeconds, boolean cancelled) {
        if (cancelled) {
            return elapsedSeconds >= 0 && elapsedSeconds < FOCUS_DURATION_SECONDS;
        }
        return elapsedSeconds == FOCUS_DURATION_SECONDS;
    }
}
