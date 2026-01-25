package com.todolab.task.dto;

import com.todolab.task.domain.Task;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record TaskResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        boolean unscheduled,
        String category,
        LocalDateTime createdAt
) {
    public static TaskResponse from(Task t) {
        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .startAt(t.getStartAt())
                .endAt(t.getEndAt())
                .allDay(t.isAllDay())
                .unscheduled(t.isUnscheduled())
                .category(t.getCategory())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
