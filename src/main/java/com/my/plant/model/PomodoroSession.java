package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pomodoroSession")
public class PomodoroSession {

    @Id
    @JsonIgnore
    private String _id;
    private String userName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private long elapsedSeconds;
    private boolean cancelled;
    private List<String> goalStepIds;
    private List<String> blockNames;

    public PomodoroSession() {
    }

    public PomodoroSession(String userName, LocalDateTime startedAt, LocalDateTime endedAt,
                           long elapsedSeconds, boolean cancelled) {
        this.userName = userName;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.elapsedSeconds = elapsedSeconds;
        this.cancelled = cancelled;
        this.goalStepIds = new ArrayList<>();
        this.blockNames = new ArrayList<>();
    }

    @JsonProperty("id")
    public String getId() {
        return _id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(long elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
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
}
