package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.common.api.ErrorCode;
import com.todolab.task.dto.TaskCategoryGroupResponse;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest request) {
        log.info("[API] createTask request :: title={}, category={}, allDay={}",
                request.title(), request.category(), request.allDay());

        request.validate();

        TaskResponse res = taskService.create(request);

        log.info("[API] createTask success :: id={}", res.id());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable Long id) {
        log.info("[API] getTask request :: id={}", id);
        try {
            TaskResponse res = taskService.getTask(id);
            log.info("[API] getTask success :: id={}", id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(res));
        } catch (TaskNotFoundException _) {
            log.warn("[API] getTask failed :: id={}, reason=task not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskCategoryGroupResponse>>> getTasks(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String date
    ) {
        log.info("[API] getTasks :: Type : {} , Date : {}", type, date);
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType(type)
                .rawDate(date)
                .build();

        List<TaskCategoryGroupResponse> res = taskService.getTasks(request);
        log.info("[API] getTasks success :: type={}, date={}, groupCount={}", type, date, res.size());
        log.debug("[API] getTasks categories :: {}",  res.stream().map(TaskCategoryGroupResponse::category).toList());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request
    ) {
        log.info("[API] updateTask request :: id={}, title={}, category={}, allDay={}",
                id, request.title(), request.category(), request.allDay());

        request.validate();

        try {
            TaskResponse res = taskService.update(id, request);
            log.info("[API] updateTask success :: id={}", id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(res));
        } catch (TaskNotFoundException _) {
            log.warn("[API] updateTask failed :: id={}, reason=task not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
        }
    }

    @GetMapping("/unscheduled")
    public ResponseEntity<ApiResponse<List<TaskCategoryGroupResponse>>> getUnscheduledTasks() {
        log.info("[API] getUnscheduledTasks request");

        List<TaskCategoryGroupResponse> res = taskService.getUnscheduledTasks();

        log.info("[API] getUnscheduledTasks success :: groupCount={}", res.size());
        log.debug("[API] getUnscheduledTasks categories :: {}",
                res.stream().map(TaskCategoryGroupResponse::category).toList());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(res));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> deleteTask(@PathVariable String id) {
        log.info("[API] deleteTask request :: id={}", id);

        try {
            Long longId = Long.valueOf(id);
            taskService.delete(longId);
            log.info("[API] deleteTask success :: id={}", id);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(TaskResponse.builder().id(longId).build()));
        } catch (TaskNotFoundException _) {
            log.warn("[API] deleteTask failed :: id={}, reason=task not found", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.TASK_NOT_FOUND));
        }
    }
}
