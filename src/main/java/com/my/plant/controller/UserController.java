package com.my.plant.controller;

import com.my.plant.service.UserService;
import com.my.plant.util.UserUtil;
import com.my.plant.util.dto.AjaxResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by User on 23.07.2026.
 */
@Controller
@ResponseBody
@RequestMapping("/user")
public class UserController {

    private static final String AJAX_OK_MESSAGE = "ok";

    @Autowired
    private UserService userService;

    @DeleteMapping("/me")
    @Operation(summary = "deleteCurrentUser", operationId = "deleteCurrentUser")
    public AjaxResponse deleteCurrentUser(HttpServletRequest request) {
        String userName = UserUtil.getLogginedUserName();
        userService.deleteCurrentUser(userName);

        // Invalidate session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        return new AjaxResponse(false, AJAX_OK_MESSAGE);
    }
}
