package com.my.plant.service;

import com.my.plant.model.PomodoroSession;

import java.util.List;

public interface PomodoroSessionService {

    void save(PomodoroSession session);

    List<PomodoroSession> getAll(String userName);
}
