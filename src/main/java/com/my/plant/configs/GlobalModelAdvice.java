package com.my.plant.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects global model attributes into every view-rendering response.
 * Currently exposes {@code appEnv} so templates can show environment indicators (e.g. DEV badge).
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @Value("${app.env:}")
    private String appEnv;

    @ModelAttribute("appEnv")
    public String appEnv() {
        return appEnv;
    }
}
