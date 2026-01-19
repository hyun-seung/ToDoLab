package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public Mono<ResponseEntity<ApiResponse<TaskResponse>>> createTask(@Valid @RequestBody TaskRequest request) {
        request.validate();

        return taskService.create(request)
                .map(res -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ApiResponse.success(res)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<TaskResponse>>> getTask(@PathVariable Long id) {
        return taskService.getTask(id)
                .map(task -> ResponseEntity.ok(ApiResponse.success(task)))
                .onErrorResume(TaskNotFoundException.class, e -> Mono.just(
                        ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.failure(e.getErrorCode()))
                ));
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
                .map(res -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ApiResponse.success(res)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<TaskResponse>>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        request.validate();

        return taskService.update(id, request)
                .map(m -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ApiResponse.success(m)))
                .onErrorResume(TaskNotFoundException.class, e -> Mono.just(
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.failure(e.getErrorCode()))
                ));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<TaskResponse>>> deleteTask(@PathVariable Long id) {
        return taskService.delete(id)
                .map(m -> ResponseEntity
                        .status(HttpStatus.OK)
                        .body(ApiResponse.success(TaskResponse.builder().id(id).build())));
    }
}
