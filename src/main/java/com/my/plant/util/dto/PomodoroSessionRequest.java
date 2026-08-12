package com.my.plant.util.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class PomodoroSessionRequest {

    private Instant startedAt;
    private Instant endedAt;
    private boolean cancelled;
    private long plannedSeconds;
    private List<String> goalStepIds;
    private List<String> blockNames;

    public PomodoroSessionRequest() {
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public long getPlannedSeconds() {
        return plannedSeconds;
    }

    public void setPlannedSeconds(long plannedSeconds) {
        this.plannedSeconds = plannedSeconds;
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
