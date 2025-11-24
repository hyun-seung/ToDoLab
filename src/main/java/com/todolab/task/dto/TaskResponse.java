package com.todolab.task.dto;

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
) {}
