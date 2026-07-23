package com.my.plant.controller;

import com.my.plant.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by User on 23.07.2026.
 */
@Controller
@RequestMapping("/register")
public class RegisterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterController.class);

    @Autowired
    private UserService userService;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView getRegisterPage(ModelAndView model) {
        model.setViewName("register");
        return model;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView register(
            @RequestParam(value = "userName") String userName,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "password") String password,
            ModelAndView model) {
        try {
            userService.register(userName, email, password);
            return new ModelAndView("redirect:/login?registered");
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Registration failed", ex);
            model.addObject("errorMessage", ex.getMessage());
            model.setViewName("register");
            return model;
        }
    }
}
