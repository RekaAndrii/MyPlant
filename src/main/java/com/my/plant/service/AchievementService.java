package com.my.plant.service;

import com.my.plant.model.Achievement;

import java.util.List;

/**
 * Created by User on 24.07.2026.
 */
public interface AchievementService {

    void save(Achievement achievement);

    List<Achievement> getAll(String userName);
}
