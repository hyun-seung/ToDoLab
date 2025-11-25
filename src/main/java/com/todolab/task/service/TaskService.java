package com.todolab.task.service;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Mono<List<TaskResponse>> getTasks(TaskQueryRequest request) {
        final TaskQueryType type = request.getType();
        final String strDate = request.getDate();

        DateRange range = type.calculate(strDate);

        return taskRepository.findByDateRange(range.getStart(), range.getEnd())
                .map(TaskResponse::from)
                .collectList();
    }

    public Mono<TaskResponse> create(TaskCreateRequest req) {
        Task task = Task.builder()
                .title(req.title())
                .description(req.description())
                .taskDate(req.date())
                .taskTime(req.time())
                .build();

        return taskRepository.save(task)
                .map(saved -> TaskResponse.builder()
                        .title(saved.getTitle())
                        .description(saved.getDescription())
                        .date(saved.getTaskDate())
                        .time(saved.getTaskTime())
                        .createdAt(saved.getCreatedAt())
                        .build());
    }

}
