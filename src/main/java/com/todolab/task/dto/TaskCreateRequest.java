package com.todolab.task.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskCreateRequest (
    String title,
    String description,
    LocalDate date,
    LocalTime time
) {}
