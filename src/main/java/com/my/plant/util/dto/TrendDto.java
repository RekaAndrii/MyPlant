package com.my.plant.util.dto;

import java.util.List;

/**
 * Created by User on 02.07.2017.
 */
public class TrendDto <T> {

    private T data;

    private List<String> yValues;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getyValues() {
        return yValues;
    }

    public void setyValues(List<String> yValues) {
        this.yValues = yValues;
    }

    public TrendDto(T data, List<String> yValues) {
        this.data = data;
        this.yValues = yValues;
    }
}
