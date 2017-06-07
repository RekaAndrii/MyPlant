package com.my.plant.util.dto;

/**
 * Created by User on 04.06.2017.
 */
public class AjaxResponse {

    private boolean hasError;
    private String message;

    public AjaxResponse() {
    }

    public AjaxResponse(boolean hasError, String message) {
        this.hasError = hasError;
        this.message = message;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
