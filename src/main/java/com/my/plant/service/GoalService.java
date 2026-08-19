package com.my.plant.service;

import com.my.plant.model.Goal;

import java.util.List;

/**
 * Created by User on 31.07.2026.
 */
public interface GoalService {

    List<Goal> getAll(String userName);

    Goal findById(String id, String userName);

    void save(Goal goal);

    void update(String id, String newName, String userName);

    void markDone(String id, boolean done, String userName);

    void delete(String id, String userName);

    void reorder(List<String> goalIds, String userName);
}
