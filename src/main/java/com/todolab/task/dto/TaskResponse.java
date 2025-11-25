package com.todolab.task.dto;

import com.todolab.task.domain.Task;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record TaskResponse(
        String title,
        String description,
        LocalDate date,
        LocalTime time,
        LocalDateTime createdAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getTitle(),
                t.getDescription(),
                t.getTaskDate(),
                t.getTaskTime(),
                t.getCreatedAt()
        );
    }
}
