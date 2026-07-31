package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

/**
 * Created by User on 31.07.2026.
 */
@Document(collection = "goal")
public class Goal {

    @Id
    @JsonIgnore
    private String _id;
    private String userName;
    private String name;
    private boolean done;
    private LocalDate createdDate;

    public Goal() {
    }

    public Goal(String userName, String name, LocalDate createdDate) {
        this.userName = userName;
        this.name = name;
        this.done = false;
        this.createdDate = createdDate;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
}
