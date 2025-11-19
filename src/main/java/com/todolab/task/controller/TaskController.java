package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public Mono<ResponseEntity<ApiResponse<TaskResponse>>> create(@Valid @RequestBody TaskCreateRequest request) {
        return Mono.fromCallable(() -> {
                    request.validate();
                    return request;
                })
                .flatMap(taskService::create)
                .map(ApiResponse::success)
                .map(ResponseEntity::ok);
    }
}
