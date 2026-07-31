package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Created by User on 31.07.2026.
 */
@Document(collection = "goalStep")
public class GoalStep {

    @Id
    @JsonIgnore
    private String _id;
    private String userName;
    private String goalId;
    private String name;
    private List<String> linkedBlockNames;
    private boolean done;
    private int order;

    public GoalStep() {
    }

    public GoalStep(String userName, String goalId, String name, List<String> linkedBlockNames, int order) {
        this.userName = userName;
        this.goalId = goalId;
        this.name = name;
        this.linkedBlockNames = linkedBlockNames;
        this.done = false;
        this.order = order;
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

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getLinkedBlockNames() {
        return linkedBlockNames;
    }

    public void setLinkedBlockNames(List<String> linkedBlockNames) {
        this.linkedBlockNames = linkedBlockNames;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
