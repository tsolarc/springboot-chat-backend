package com.Backend.User.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/logout")
    public String logout(org.springframework.ui.Model model, jakarta.servlet.http.HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }

    @GetMapping("/profile")
    public String viewProfile() {
        return "/profile/profile";
    }
}
