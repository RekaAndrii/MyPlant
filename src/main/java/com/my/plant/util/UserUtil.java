package com.my.plant.util;

import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by User on 03.07.2017.
 */
public class UserUtil {

    private UserUtil() {
        super();
    }

    public static String getLogginedUserName(){
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
