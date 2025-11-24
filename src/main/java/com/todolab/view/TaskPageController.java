package com.todolab.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;

@Controller
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskPageController {

    private final SpringWebFluxTemplateEngine templateEngine;

    @GetMapping("/create")
    public String createPage(Model model) {
        // create.html을 먼저 렌더링하여 "body" HTML 생성
        Context ctx = new Context();
        String bodyHtml = templateEngine.process("pages/task/create", ctx);

        // base.html에 넘길 값들
        model.addAttribute("title", "일정 등록");
        model.addAttribute("body", bodyHtml);

        return "layout/base"; // base.html
    }
}
