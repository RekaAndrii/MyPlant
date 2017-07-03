package com.my.plant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by User on 03.07.2017.
 */
@Controller
@RequestMapping("/")
public class LoginController {


    @RequestMapping(path = "login", method = RequestMethod.GET)
    public ModelAndView getLoginPage(ModelAndView modelAndView){
        modelAndView.setViewName("login");
        return modelAndView;
    }
}
