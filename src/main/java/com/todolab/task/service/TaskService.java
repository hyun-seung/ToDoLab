package com.todolab.task.service;

import com.todolab.common.api.ErrorCode;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final Scheduler jpaScheduler;

    public Mono<TaskResponse> create(TaskCreateRequest req) {
        Task task = Task.builder()
                .title(req.title())
                .description(req.description())
                .taskDate(req.date())
                .taskTime(req.time())
                .build();

        return Mono.fromCallable(() -> taskRepository.save(task))
                .publishOn(jpaScheduler)
                .map(TaskResponse::from);
    }

    public Mono<TaskResponse> getTask(Long id) {
        return Mono.fromCallable(() -> taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(ErrorCode.TASK_NOT_FOUND, "ID (" + id + ") 가 없습니다.")))
                .publishOn(jpaScheduler)
                .map(TaskResponse::from);
    }

    public Mono<List<TaskResponse>> getTasks(TaskQueryRequest request) {
        final TaskQueryType type = request.getType();
        final String strDate = request.getDate();

        DateRange range = type.calculate(strDate);

        return Mono.fromCallable(() -> taskRepository.findByDateRange(range.getStart(), range.getEnd()))
                .publishOn(jpaScheduler)
                .flatMapMany(Flux::fromIterable)
                .map(TaskResponse::from)
                .collectList();
    }

}
