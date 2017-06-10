package com.my.plant.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Created by User on 11.06.2017.
 */
@Document
public class HistoryItem {

    private String userName;

    private HistoryAction action;

    private LocalDateTime time;

    public HistoryItem() {
    }

    public HistoryItem(String userName, HistoryAction action, LocalDateTime time) {
        this.userName = userName;
        this.action = action;
        this.time = time;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public HistoryAction getAction() {
        return action;
    }

    public void setAction(HistoryAction action) {
        this.action = action;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
