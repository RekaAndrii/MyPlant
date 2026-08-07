package com.my.plant.util.dto;

import java.util.ArrayList;
import java.util.List;

public class PomodoroTagOptionsResponse {

    private List<PomodoroTagOption> goalSteps;
    private List<String> blockNames;

    public PomodoroTagOptionsResponse() {
        this.goalSteps = new ArrayList<>();
        this.blockNames = new ArrayList<>();
    }

    public PomodoroTagOptionsResponse(List<PomodoroTagOption> goalSteps, List<String> blockNames) {
        this.goalSteps = goalSteps;
        this.blockNames = blockNames;
    }

    public List<PomodoroTagOption> getGoalSteps() {
        return goalSteps;
    }

    public void setGoalSteps(List<PomodoroTagOption> goalSteps) {
        this.goalSteps = goalSteps;
    }

    public List<String> getBlockNames() {
        return blockNames;
    }

    public void setBlockNames(List<String> blockNames) {
        this.blockNames = blockNames;
    }
}
