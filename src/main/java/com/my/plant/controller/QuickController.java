package com.my.plant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Created by User on 09.06.2017.
 */
@Controller
@RequestMapping("/quick")
public class QuickController {

    @Autowired
    AuthenticationManager authenticationManager;

    @RequestMapping(path = "/home", method = RequestMethod.GET)
    public String getHome(HttpServletRequest request, @RequestParam(value = "login") String login, @RequestParam(value = "password") String password){

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(login, password);

        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(authRequest);
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        // Create a new session and add the security context.
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        return "redirect:/home";
    }

}
