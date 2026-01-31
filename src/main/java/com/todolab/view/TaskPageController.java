package com.todolab.view;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.StringWriter;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TaskPageController {

    private final SpringTemplateEngine templateEngine;
    private final RestClient restClient;

    // ===========================
    //  일정 등록 모달
    // ===========================
    @GetMapping(
            value = "/tasks/create",
            headers = "X-Requested-With=fetch",
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public String createFragment() {
        Context ctx = new Context();

        StringWriter writer = new StringWriter();
        templateEngine.process(
                "pages/task/create",
                Set.of("#create-page"),
                ctx,
                writer
        );

        return writer.toString();
    }

    // ===========================
    //  일정 상세 모달
    // ===========================
    @GetMapping(
            value = "/tasks/detail",
            headers = "X-Requested-With=fetch",
            produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String detailFragment(@RequestParam Long id) {

        ApiResponse<TaskResponse> resp = restClient.get()
                .uri("/api/tasks/{id}", id)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        TaskResponse task = resp.data();

        Context ctx = new Context();
        ctx.setVariable("task", task);

        StringWriter writer = new StringWriter();
        templateEngine.process(
                "pages/task/detail",
                Set.of("#detail-fragment"),
                ctx,
                writer
        );

        return writer.toString();
    }

    // ===========================
    //  일정 미정 페이지
    // ===========================
    @GetMapping("/tasks/unscheduled")
    public String unscheduled(Model model) {
        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", "ToDoLab");
        model.addAttribute("activeTab", "unscheduled");

        model.addAttribute("contentView", "pages/task/unscheduled");

        return "layout/base";
    }


    // ===========================
    //  일간 일정 페이지
    // ===========================
    @GetMapping("/tasks/day")
    public String day(
            @RequestParam(name = "move", required = false) String move,
            @RequestParam(name = "date", required = false) String date,
            Model model
    ) {
        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) targetDate = targetDate.minusDays(1);
        if ("next".equals(move)) targetDate = targetDate.plusDays(1);

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle",
                targetDate.getYear() + "년 "
                        + targetDate.getMonthValue() + "월 "
                        + targetDate.getDayOfMonth() + "일");
        model.addAttribute("activeTab", "day");

        model.addAttribute("date", targetDate);
        model.addAttribute("isToday", targetDate.equals(LocalDate.now()));

        model.addAttribute("contentView", "pages/task/day");

        return "layout/base";
    }


    // ===========================
    //  주간 일정 페이지
    // ===========================
    @GetMapping("/tasks/week")
    public String week(
            @RequestParam(name = "move", required = false) String move,
            @RequestParam(name = "date", required = false) String date,
            Model model
    ) {
        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) targetDate = targetDate.minusWeeks(1);
        if ("next".equals(move)) targetDate = targetDate.plusWeeks(1);

        LocalDate weekStart = targetDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        final LocalDate finalTargetDate = targetDate;

        ApiResponse<List<TaskResponse>> resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tasks")
                        .queryParam("type", "WEEK")
                        .queryParam("date", finalTargetDate.toString())
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        List<TaskResponse> taskList = (resp != null && resp.data() != null) ? resp.data() : List.of();

        // ✅ week.html이 바로 쓸 수 있게 “요일별로 묶어서” 내려줌
        List<DaySchedule> weeklyTasks = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);

            List<TaskUi> uiTasks = taskList.stream()
                    .filter(t -> occursOn(t, day))  // 기간 일정 포함
                    .map(this::toUi)
                    .toList();

            weeklyTasks.add(new DaySchedule(day, uiTasks));
        }
        log.info("## weeklyTasks = {}", weeklyTasks);

        LocalDate selectedDate = targetDate;
        if (selectedDate.isBefore(weekStart) || selectedDate.isAfter(weekEnd)) {
            selectedDate = weekStart;
        }

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", targetDate.getYear() + "년 " + targetDate.getMonthValue() + "월");

        // - 기존: activeTab=week
        // - 변경: “달력” 탭이면 calendar로 두는 게 맞음
        model.addAttribute("activeTab", "week");

        model.addAttribute("currentDate", targetDate);
        model.addAttribute("monday", weekStart);
        model.addAttribute("sunday", weekEnd);
        model.addAttribute("weekRange", weekStart + " ~ " + weekEnd);

        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("weeklyTasks", weeklyTasks);

        model.addAttribute("contentView", "pages/task/week");
        return "layout/base";
    }


    // ===========================
    // 일정이 특정 날짜(day)에 "발생/겹침"하는지 판단
    // - 미정(startAt null)은 제외
    // - 단일: startAt이 그 날짜에 포함되면 true
    // - 기간: [startAt, endAt) 과 [dayStart, dayEnd) 가 겹치면 true
    // ===========================
    private boolean occursOn(TaskResponse t, LocalDate day) {
        if (t == null) return false;
        if (t.unscheduled()) return false;
        if (t.startAt() == null) return false;

        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay(); // [dayStart, dayEnd)

        LocalDateTime start = t.startAt();
        LocalDateTime end = t.endAt();

        // endAt == null : 단일 시점/단일 일정 → startAt이 그 날에 들어오면 포함
        if (end == null) {
            return !start.isBefore(dayStart) && start.isBefore(dayEnd);
        }

        // endAt != null : 기간 일정 → overlap 판단 (DB 쿼리와 동일)
        return start.isBefore(dayEnd) && end.isAfter(dayStart);
    }

    // ===========================
    // TaskResponse -> TaskUi 변환
    // - 템플릿 호환을 위해 date/time 파생 제공
    // - allDay면 time은 null로 내려서 "종일" 표시 유도
    // ===========================
    private TaskUi toUi(TaskResponse t) {
        LocalDateTime startAt = t.startAt();

        LocalDate date = (startAt != null) ? startAt.toLocalDate() : null;
        LocalTime time = (startAt != null && !t.allDay()) ? startAt.toLocalTime() : null;

        return new TaskUi(
                t.id(),
                t.title(),
                t.description(),
                date,
                time,
                t.allDay(),
                t.startAt(),
                t.endAt(),
                t.category(),
                pickColor(t.id())
        );
    }

    // ===========================
    // 색상 알고리즘 (✅ id 기반)
    // ===========================
    private String pickColor(Long id) {
        String[] colors = {
                "#BFDBFE", "#C4B5FD", "#FDE68A",
                "#FBCFE8", "#BBF7D0"
        };
        if (id == null) {
            return colors[0];
        }
        int idx = Math.floorMod(id.hashCode(), colors.length);
        return colors[idx];
    }

    public record TaskUi(
            Long id,
            String title,
            String description,
            LocalDate date,
            LocalTime time,
            boolean allDay,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String category,
            String color
    ) {
    }

    public record DaySchedule(
            LocalDate date,
            List<TaskUi> tasks
    ) {
    }

    // ===========================
    // 월 캘린더 셀 DTO
    // ===========================
    public record CalendarCell(
            LocalDate date,
            boolean inMonth,
            List<TaskUi> tasks
    ) {
    }
}
