package com.my.plant.util.dto;

/**
 * Created by User on 04.06.2017.
 */
public class AjaxResponse {

    private boolean hasError;
    private String message;

    public AjaxResponse(boolean hasError, String message) {
        this.hasError = hasError;
        this.message = message;
    }


}
