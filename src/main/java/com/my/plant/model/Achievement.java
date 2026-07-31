package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Created by User on 24.07.2026.
 */
@Document(collection = "achievement")
public class Achievement {

    @Id
    @JsonIgnore
    private String _id;
    private String userName;
    private String goalName;
    private int targetExecutions;
    private LocalDate achievedDate;
    private String goalStepId;

    public Achievement() {
    }

    public Achievement(String userName, String goalName, int targetExecutions, LocalDate achievedDate) {
        this.userName = userName;
        this.goalName = goalName;
        this.targetExecutions = targetExecutions;
        this.achievedDate = achievedDate;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public int getTargetExecutions() {
        return targetExecutions;
    }

    public void setTargetExecutions(int targetExecutions) {
        this.targetExecutions = targetExecutions;
    }

    public LocalDate getAchievedDate() {
        return achievedDate;
    }

    public void setAchievedDate(LocalDate achievedDate) {
        this.achievedDate = achievedDate;
    }

    public String getGoalStepId() {
        return goalStepId;
    }

    public void setGoalStepId(String goalStepId) {
        this.goalStepId = goalStepId;
    }
}
