package com.my.plant.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class LoginControllerTest {

    @InjectMocks
    private LoginController loginController;

    // ── GET /login ────────────────────────────────────────────────────────────

    @Test
    public void getLoginPage_returnsLoginViewName() {
        ModelAndView mav = loginController.getLoginPage(new ModelAndView());

        assertEquals("login", mav.getViewName());
    }

    @Test
    public void getLoginPage_returnsNonNullModelAndView() {
        ModelAndView mav = loginController.getLoginPage(new ModelAndView());

        assertNotNull(mav);
    }
}
