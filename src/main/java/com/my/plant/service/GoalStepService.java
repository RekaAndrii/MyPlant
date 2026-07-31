package com.my.plant.service;

import com.my.plant.model.GoalStep;

import java.util.List;

/**
 * Created by User on 31.07.2026.
 */
public interface GoalStepService {

    List<GoalStep> getByGoalId(String goalId, String userName);

    void save(GoalStep step);

    void update(String id, String newName, List<String> linkedBlockNames, String userName);

    void markDone(String id, boolean done, String userName);

    void moveUp(String id, String userName);

    void moveDown(String id, String userName);

    void delete(String id, String userName);

    void deleteByGoalId(String goalId, String userName);

    void markDoneByBlockName(String blockName, String userName);
}
