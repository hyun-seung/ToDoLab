package com.todolab.task.service;

import com.todolab.task.domain.Task;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskTxService {

    private final TaskRepository taskRepository;

    @Transactional
    public Task updateTx(Long id, TaskRequest req) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.update(req.title(), req.description(), req.normalizedType(), req.startAt(), req.endAt(), req.allDay(), req.category());
        return taskRepository.save(task);
    }

    @Transactional
    public Task moveToTodayTx(Long id, LocalDate targetDate) {
        Task task = findTask(id);
        task.moveToToday(targetDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task completeTx(Long id, LocalDateTime completedAt) {
        Task task = findTask(id);
        task.complete(completedAt);
        return taskRepository.save(task);
    }

    @Transactional
    public Task carryOverTx(Long id, LocalDate nextDate) {
        Task task = findTask(id);
        task.carryOverTo(nextDate);
        return taskRepository.save(task);
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
