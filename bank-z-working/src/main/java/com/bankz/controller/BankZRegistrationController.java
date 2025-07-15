package com.bankz.controller;

import com.bankz.entity.BankZUser;
import com.bankz.service.BankZUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class BankZRegistrationController {

    @Autowired
    private BankZUserService userService;

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new BankZUser());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") BankZUser user,
                              BindingResult result,
                              @RequestParam("confirmPassword") String confirmPassword,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match");
            return "register";
        }

        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "Username already exists");
            return "register";
        }

        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email already exists");
            return "register";
        }

        // Validate SA ID number if provided
        if (user.getSaIdNumber() != null && !user.getSaIdNumber().trim().isEmpty()) {
            String saId = user.getSaIdNumber().trim();
            if (!saId.matches("\\d{13}")) {
                model.addAttribute("error", "South African ID number must be exactly 13 digits");
                return "register";
            }
            
            // Check if SA ID already exists
            if (userService.existsBySaIdNumber(saId)) {
                model.addAttribute("error", "South African ID number already registered");
                return "register";
            }
            user.setSaIdNumber(saId);
        } else {
            user.setSaIdNumber(null);
        }

        try {
            userService.createUser(user);
            
            redirectAttributes.addFlashAttribute("message", 
                "Registration successful! You can now login with your credentials.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Registration failed: " + e.getMessage());
            return "register";
        }
    }
}