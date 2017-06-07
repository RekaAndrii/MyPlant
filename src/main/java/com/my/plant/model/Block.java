package com.my.plant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.my.plant.util.constant.BlockColor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

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
}
