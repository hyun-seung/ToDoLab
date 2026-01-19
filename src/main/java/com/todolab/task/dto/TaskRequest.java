package com.todolab.task.dto;

import com.todolab.task.exception.TaskValidationException;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.LocalTime;

public record TaskRequest(
        @NotBlank(message = "제목은 필수값입니다")
        String title,

        String description,
        LocalDate date,
        LocalTime time
) {
    public void validate() throws TaskValidationException {

    }
}
