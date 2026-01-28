package com.todolab.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String redirectToUnscheduled() {
        return "redirect:/tasks/unscheduled";
    }
}
