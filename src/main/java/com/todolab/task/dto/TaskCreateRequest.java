package com.todolab.task.dto;

import com.todolab.task.exception.TaskValidationException;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskCreateRequest(
        String title,

        String description,
        LocalDate date,
        LocalTime time
) {
    public void validate() throws TaskValidationException {

    }
}
