package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.common.api.ErrorCode;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest request) {
        request.validate();
        TaskResponse res = taskService.create(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        try {
            TaskResponse res = taskService.getTask(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(res));
        } catch (TaskNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date
    ) {
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType(type)
                .rawDate(date)
                .build();

        List<TaskResponse> res = taskService.getTasks(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        request.validate();

        try {
            TaskResponse res = taskService.update(id, request);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(res));
        } catch (TaskNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
        }
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getUnscheduledTasks() {
        List<TaskResponse> res = taskService.getUnscheduledTasks();
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> deleteTask(@PathVariable Long id) {
        try {
            taskService.delete(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(TaskResponse.builder().id(id).build()));
        } catch (TaskNotFoundException _) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
        }
    }
}
