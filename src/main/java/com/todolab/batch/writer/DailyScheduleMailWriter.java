package com.todolab.batch.writer;

import com.todolab.batch.domain.ScheduleMailSectionContent;
import com.todolab.batch.domain.ScheduleSectionType;
import com.todolab.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DailyScheduleMailWriter implements ItemWriter<ScheduleMailSectionContent> {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    private final MailService mailService;

    @Value("${app.mail.daily-summary.to}")
    private String toEmail;

    @Override
    public void write(Chunk<? extends ScheduleMailSectionContent> chunk) throws Exception {
        List<? extends ScheduleMailSectionContent> items = chunk.getItems();

        if (items == null || items.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(ZONE_ID);
        String subject = "[ToDoLab] " + today + " 일정 요약";

        StringBuilder body = new StringBuilder();
        body.append("안녕하세요. ToDoLab 일정 요약입니다.\n\n");
        body.append("기준일: ").append(today).append("\n\n");

        items.stream()
                .sorted(Comparator.comparingInt(item -> order(item.type())))
                .forEach(item -> {
                    body.append("[").append(item.title()).append("]\n");
                    body.append(item.content()).append("\n");
                });

        mailService.sendText(toEmail, subject, body.toString());
    }

    private int order(ScheduleSectionType type) {
        return switch (type) {
            case SEED -> 1;
            case TODAY -> 2;
            case WEEK -> 3;
        };
    }
}
