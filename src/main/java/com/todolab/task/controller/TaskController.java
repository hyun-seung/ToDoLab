package com.todolab.task.controller;

import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public Mono<TaskResponse> create(@RequestBody TaskCreateRequest request) {
        return taskService.create(request);
    }
}
