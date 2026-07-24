package com.my.plant.util.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by User on 24.07.2026.
 */
public class ExecuteBlockResponse {

    private boolean hasError;
    private String message;
    @JsonProperty("isChallenge")
    private boolean isChallenge;
    private Integer remainingExecutions;
    private boolean completed;

    public ExecuteBlockResponse() {
    }

    public ExecuteBlockResponse(boolean hasError, String message, boolean isChallenge,
                                Integer remainingExecutions, boolean completed) {
        this.hasError = hasError;
        this.message = message;
        this.isChallenge = isChallenge;
        this.remainingExecutions = remainingExecutions;
        this.completed = completed;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isChallenge() {
        return isChallenge;
    }

    public void setChallenge(boolean challenge) {
        isChallenge = challenge;
    }

    public Integer getRemainingExecutions() {
        return remainingExecutions;
    }

    public void setRemainingExecutions(Integer remainingExecutions) {
        this.remainingExecutions = remainingExecutions;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
