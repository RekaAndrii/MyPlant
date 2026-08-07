package com.my.plant.util.dto;

import java.util.ArrayList;
import java.util.List;

public class PomodoroSessionTagUpdateRequest {

    private List<String> goalStepIds;
    private List<String> blockNames;

    public PomodoroSessionTagUpdateRequest() {
    }

    public List<String> getGoalStepIds() {
        return goalStepIds;
    }

    public void setGoalStepIds(List<String> goalStepIds) {
        this.goalStepIds = goalStepIds;
    }

    public List<String> getBlockNames() {
        return blockNames;
    }

    public void setBlockNames(List<String> blockNames) {
        this.blockNames = blockNames;
    }

    public List<String> safeGoalStepIds() {
        return goalStepIds == null ? new ArrayList<>() : goalStepIds;
    }

    public List<String> safeBlockNames() {
        return blockNames == null ? new ArrayList<>() : blockNames;
    }
}
