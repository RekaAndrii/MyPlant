package com.my.plant.util.constant;

/**
 * Created by User on 03.06.2017.
 */
public enum BlockColor {

    RED("red"), YELLOW("yellow"), GREEN("green");

    private String value;

    BlockColor(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
