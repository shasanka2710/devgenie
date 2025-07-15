package com.devgenie.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                model.addAttribute("errorTitle", "Page Not Found");
                model.addAttribute("errorMessage", "The page you're looking for doesn't exist.");
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                model.addAttribute("errorTitle", "Internal Server Error");
                model.addAttribute("errorMessage", "Something went wrong on our end. Please try again later.");
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                model.addAttribute("errorTitle", "Access Denied");
                model.addAttribute("errorMessage", "You don't have permission to access this resource.");
            } else {
                model.addAttribute("errorTitle", "Error " + statusCode);
                model.addAttribute("errorMessage", "An unexpected error occurred.");
            }

            model.addAttribute("statusCode", statusCode);
        } else {
            model.addAttribute("errorTitle", "Error");
            model.addAttribute("errorMessage", "An unexpected error occurred.");
        }

        return "error";
    }
}