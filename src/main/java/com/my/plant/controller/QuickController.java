package com.my.plant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by User on 09.06.2017.
 */
@Controller
@RequestMapping("/quick")
public class QuickController {

    @RequestMapping(path = "home", method = RequestMethod.GET)

    public ModelAndView getHome(ModelAndView model, @RequestParam(value = "loginAs") String login){
        return model;
    }
}
