package com.hibol.miette.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GlossaryController {

    @GetMapping("/ca-veut-dire-quoi")
    public String glossary() {
        return "glossaire";
    }
}
