package com.my.plant.controller;

import com.my.plant.exception.DuplicateEmailException;
import com.my.plant.exception.InvalidEmailException;
import com.my.plant.exception.InvalidPasswordException;
import com.my.plant.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller handling user registration page and form submission.
 * Endpoints:
 * - GET /register: Display registration form
 * - POST /register: Process registration form submission
 */
@Controller
@RequestMapping("/register")
public class RegistrationController {
    
    @Autowired
    private RegistrationService registrationService;
    
    /**
     * Display registration form (GET /register)
     * Returns the registration form template with empty model attributes.
     *
     * @param model Spring MVC Model
     * @return View name "register"
     */
    @GetMapping
    public String showRegistrationForm(Model model) {
        model.addAttribute("username", "");
        model.addAttribute("email", "");
        return "register";
    }
    
    /**
     * Handle registration form submission (POST /register)
     * 
     * Validates inputs:
     * - Email format (regex validation)
     * - Email uniqueness (database check)
     * - Password length (minimum 4 characters)
     * 
     * On success: Redirects to /login with flash message
     * On failure: Re-renders form with error message
     *
     * @param username Username from form
     * @param email Email from form
     * @param password Password from form
     * @param model Spring MVC Model (for re-render on error)
     * @param redirectAttributes RedirectAttributes (for flash message on success)
     * @return View name "register" on error, or redirect to "/login" on success
     */
    @PostMapping
    public String registerUser(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Attempt registration with validations
            registrationService.registerUser(username, email, password);
            
            // Success: Redirect to login with success message
            redirectAttributes.addFlashAttribute("message", 
                "Account created successfully! Please log in.");
            return "redirect:/login";
            
        } catch (InvalidEmailException e) {
            // Email format is invalid - display error in single alert box
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
            
        } catch (DuplicateEmailException e) {
            // Email already registered - display error in single alert box
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
            
        } catch (InvalidPasswordException e) {
            // Password too short - display error in single alert box
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
            
        } catch (Exception e) {
            // Generic/unexpected error
            model.addAttribute("error", 
                "Registration failed. Please try again.");
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            return "register";
        }
    }
}
