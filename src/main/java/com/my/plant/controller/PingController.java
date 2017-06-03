package com.my.plant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by User on 18.04.2017.
 */
@Controller
public class PingController {

    @RequestMapping(path = "ping", method = RequestMethod.GET)
    public String ping(){
        return "index";
    }

}
