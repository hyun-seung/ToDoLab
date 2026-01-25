package com.todolab.task.dto;

import com.todolab.common.api.ErrorCode;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.exception.TaskValidationException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.YearMonth;

@Getter
public class TaskQueryRequest {

    private final TaskQueryType type;
    private final String date;

    @Builder
    public TaskQueryRequest(String rawType, String rawDate) {
        type = parseType(rawType);
        validateDateFormat(type, rawDate);
        date = rawDate;
    }

    private TaskQueryType parseType(String rawType) {
        try {
            return TaskQueryType.from(rawType);
        } catch (Exception e) {
            throw new TaskValidationException("올바르지 않은 Type 값입니다.");
        }
    }

    private void validateDateFormat(TaskQueryType type, String rawDate) {
        try {
            if (TaskQueryType.MONTH.equals(type)) {
                YearMonth.parse(rawDate);
            } else {
                LocalDate.parse(rawDate);
            }
        } catch (Exception e) {
            throw new TaskValidationException("올바르지 않은 date 값입니다.");
        }
    }
}
