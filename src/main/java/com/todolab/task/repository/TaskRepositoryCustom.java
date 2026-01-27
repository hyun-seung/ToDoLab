package com.todolab.task.repository;

import com.todolab.task.domain.Task;

import java.util.List;

public interface TaskRepositoryCustom {
    List<Task> findUnscheduledTask();
}
