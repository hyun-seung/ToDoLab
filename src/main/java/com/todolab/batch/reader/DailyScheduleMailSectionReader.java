package com.todolab.batch.reader;

import com.todolab.batch.domain.ScheduleMailSection;
import com.todolab.batch.domain.ScheduleSectionType;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class DailyScheduleMailSectionReader implements ItemReader<ScheduleMailSection> {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final TaskService taskservice;

    private int index = 0;
    private LocalDate baseDate;

    @Override
    public ScheduleMailSection read() {
        if (baseDate == null) {
            baseDate = LocalDate.now(ZONE_ID);
        }

        index += 1;

        return switch (index) {
            case 1 -> new ScheduleMailSection(
                    ScheduleSectionType.SEED,
                    baseDate,
                    taskservice.getUnscheduledTasks()
            );
            case 2 -> new ScheduleMailSection(
                    ScheduleSectionType.TODAY,
                    baseDate,
                    taskservice.getTasks(new TaskQueryRequest(TaskQueryType.DAY, baseDate.toString()))
            );
            case 3 -> new ScheduleMailSection(
                    ScheduleSectionType.WEEK,
                    baseDate,
                    taskservice.getTasks(new TaskQueryRequest(TaskQueryType.WEEK, baseDate.toString()))
            );
            default -> null;
        };
    }

}
