package com.yourorg.javadoc_generator_ai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs/dashboard")
public class DashboardController {

    @GetMapping
    public String getDashboard(Model model) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("description", "Here are some metrics specific to the project.");
        // Add your metrics to the model here
        return "dashboard";
    }
}