package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Created by User on 27.07.2026.
 */
@Document(collection = "suggestion")
public class Suggestion {

    @Id
    @JsonIgnore
    private String _id;
    private String userName;
    private String text;
    private LocalDateTime createdDate;

    public Suggestion() {
    }

    public Suggestion(String userName, String text, LocalDateTime createdDate) {
        this.userName = userName;
        this.text = text;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
