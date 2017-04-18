package com.my.plant.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by User on 18.04.2017.
 */
@RestController
public class PingController {

    @RequestMapping(path = "ping", method = RequestMethod.GET)
    public String ping(){
        return "pong";
    }

}
