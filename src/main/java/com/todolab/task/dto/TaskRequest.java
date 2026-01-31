package com.todolab.task.dto;

import com.todolab.task.exception.TaskValidationException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record TaskRequest(
        @NotBlank(message = "제목은 필수값입니다")
        @Size(max = 30, message = "제목은 30자 이하여야 합니다")
        String title,

        @Size(max = 300, message = "설명은 300자 이하여야 합니다")
        String description,

        LocalDateTime startAt,
        LocalDateTime endAt,

        @Size(max = 10, message = "카테고리는 10자 이하여야 합니다")
        String category,
        boolean allDay
) {
    public void validate() throws TaskValidationException {
        // 1) 종료만 있는 경우
        if (endAt != null && startAt == null) {
            throw new TaskValidationException("종료 시간만 설정할 수 없습니다. 시작 시간을 함께 설정해주세요.");
        }

        // 2) allDay=true 이면 시간은 입력하지 않는 의미 -> 들어오더라도 00:00만 허용(정규화 정책이 없다면)
        if (allDay) {
            if (startAt != null && !isMidnight(startAt)) {
                throw new TaskValidationException("종일 일정은 시간을 입력할 수 없습니다. 시작일은 00:00 기준이어야 합니다.");
            }
            if (endAt != null && !isMidnight(endAt)) {
                throw new TaskValidationException("종일 일정은 시간을 입력할 수 없습니다. 종료일은 00:00 기준이어야 합니다.");
            }
        }

        // 3) 범위 역전 (도메인에서도 막지만 요청 단계에서 메시지 품질을 위해 선제)
        if (startAt != null && endAt != null && (endAt.isBefore(startAt) || endAt.isEqual(startAt))) {
            throw new TaskValidationException("종료 시간은 시작 시간 이후여야 합니다.");
        }

        // 4) 미정인데 allDay=true 금지
        if (startAt == null && endAt == null && allDay) {
            throw new TaskValidationException("미정 일정에는 종일 설정을 할 수 없습니다.");
        }
    }

    private boolean isMidnight(LocalDateTime dt) {
        return dt.toLocalTime().equals(LocalTime.MIDNIGHT);
    }
}
