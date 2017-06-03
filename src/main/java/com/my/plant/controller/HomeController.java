package com.my.plant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by User on 21.04.2017.
 */
@Controller
public class HomeController {

    @RequestMapping(path = "home", method = RequestMethod.GET)
    public String getHome(){
        //model.addAttribute("home_message", "Hello to your factory");
        return "index";
    }
}
