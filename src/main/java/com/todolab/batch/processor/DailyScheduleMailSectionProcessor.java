package com.todolab.batch.processor;

import com.todolab.batch.domain.ScheduleMailSection;
import com.todolab.batch.domain.ScheduleMailSectionContent;
import com.todolab.batch.domain.ScheduleSectionType;
import com.todolab.task.dto.TaskResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DailyScheduleMailSectionProcessor implements ItemProcessor<ScheduleMailSection, ScheduleMailSectionContent> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public @Nullable ScheduleMailSectionContent process(ScheduleMailSection item) {
        String title = getTitle(item.type());
        String content = buildSectionContent(item.tasks());
        return new ScheduleMailSectionContent(item.type(), title, content);
    }

    private String getTitle(ScheduleSectionType type) {
        return switch (type) {
            case SEED -> "씨드 일정";
            case TODAY -> "오늘 일정";
            case WEEK -> "이번 주 일정";
        };
    }

    private String buildSectionContent(List<TaskResponse> taskResponses) {
        if (taskResponses == null || taskResponses.isEmpty()) {
            return "- 없음\n";
        }

        StringBuilder sb = new StringBuilder();
        for (TaskResponse task : taskResponses) {
            sb.append("- ").append(task.title());

            if (task.startAt() != null) {
                sb.append(" [").append(formatSchedule(task)).append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatSchedule(TaskResponse taskResponse) {
        if (taskResponse.startAt() == null) {
            return "미정";
        }

        if (taskResponse.endAt() == null) {
            return taskResponse.startAt().format(DATE_TIME_FORMATTER);
        }

        return taskResponse.startAt().format(DATE_TIME_FORMATTER)
                + " ~ "
                + taskResponse.endAt().format(DATE_TIME_FORMATTER);
    }
}
