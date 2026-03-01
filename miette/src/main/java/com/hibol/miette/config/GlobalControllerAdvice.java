package com.hibol.miette.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleNotFound(ResponseStatusException ex, Model model) {
        model.addAttribute("error", ex.getReason());
        return "error";
    }
}
