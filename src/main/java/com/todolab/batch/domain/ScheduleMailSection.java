package com.todolab.batch.domain;

import com.todolab.task.dto.TaskResponse;

import java.time.LocalDate;
import java.util.List;

public record ScheduleMailSection(
        ScheduleSectionType type,
        LocalDate baseDate,
        List<TaskResponse> tasks
) {
}
