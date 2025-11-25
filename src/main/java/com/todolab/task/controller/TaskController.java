package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

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

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<TaskResponse>>>> getTasks(
            @RequestParam String type,
            @RequestParam String date
    ) {
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType(type)
                .rawDate(date)
                .build();

        return taskService.getTasks(request)
                .map(ApiResponse::success)
                .map(ResponseEntity::ok);
    }
}
