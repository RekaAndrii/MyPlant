package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.my.plant.util.constant.BlockColor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

/**
 * Created by User on 03.06.2017.
 */
@Document(collection = "block")
public class Block {

    @Id
    @JsonIgnore
    private String _id;
    private String name;
    private String userName;
    @Transient
    private BlockColor color;
    private LocalDate creationDate;
    private LocalDate lastExecution;
    @JsonProperty("isChallenge")
    private boolean isChallenge;
    private Integer targetExecutions;
    private Integer remainingExecutions;
    private boolean completed;
    private List<Integer> scheduledDays;
    private boolean disabled;

    public Block() {
    }

    public Block(String name) {
        this.name = name;
    }

    public Block(String name, String userName, LocalDate creationDate, LocalDate lastExecution) {
        this.name = name;
        this.userName = userName;
        this.creationDate = creationDate;
        this.lastExecution = lastExecution;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDate getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(LocalDate lastExecution) {
        this.lastExecution = lastExecution;
    }

    public BlockColor getColor() {
        return color;
    }

    public void setColor(BlockColor color) {
        this.color = color;
    }

    public boolean isChallenge() {
        return isChallenge;
    }

    public void setChallenge(boolean challenge) {
        isChallenge = challenge;
    }

    public Integer getTargetExecutions() {
        return targetExecutions;
    }

    public void setTargetExecutions(Integer targetExecutions) {
        this.targetExecutions = targetExecutions;
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

    public List<Integer> getScheduledDays() {
        return scheduledDays;
    }

    public void setScheduledDays(List<Integer> scheduledDays) {
        this.scheduledDays = scheduledDays;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
