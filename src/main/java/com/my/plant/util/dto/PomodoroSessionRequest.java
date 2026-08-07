package com.my.plant.util.dto;

import java.time.Instant;

public class PomodoroSessionRequest {

    private Instant startedAt;
    private Instant endedAt;
    private boolean cancelled;

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
}
