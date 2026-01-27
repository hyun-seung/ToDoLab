package com.todolab.view;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
                .uri("/tasks/{id}", id)
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
    //  일간 일정 페이지
    // ===========================
//    @GetMapping("/tasks/unscheduled")
//    public String unscheduled(Model model) {
//
//    }



    // ===========================
    //  일간 일정 페이지
    // ===========================
    @GetMapping("/tasks/day")
    public String day(
            @RequestParam(name = "move", required = false) String move,   // prev | next
            @RequestParam(name = "date", required = false) String date,   // YYYY-MM-DD
            Model model
    ) {
        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) targetDate = targetDate.minusDays(1);
        if ("next".equals(move)) targetDate = targetDate.plusDays(1);

        final LocalDate finalTargetDate = targetDate; // 람다 캡처용 고정

        String queryDate = finalTargetDate.toString(); // YYYY-MM-DD

        ApiResponse<List<TaskResponse>> resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tasks")
                        .queryParam("type", "DAY")
                        .queryParam("date", queryDate)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<TaskResponse> taskList = resp.data();

        List<TaskUi> tasks = taskList.stream()
                .filter(t -> occursOn(t, finalTargetDate)) // 기간 일정 포함 + 미정 제외
                .map(this::toUi)
                .toList();

        Context ctx = new Context();
        ctx.setVariable("date", finalTargetDate);
        ctx.setVariable("tasks", tasks);
        ctx.setVariable("isToday", finalTargetDate.equals(LocalDate.now()));

        String bodyHtml = templateEngine.process("pages/task/day", ctx);

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle",
                finalTargetDate.getYear() + "년 "
                        + finalTargetDate.getMonthValue() + "월 "
                        + finalTargetDate.getDayOfMonth() + "일");
        model.addAttribute("activeTab", "day");
        model.addAttribute("body", bodyHtml);

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
        LocalDate computedDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) computedDate = computedDate.minusWeeks(1);
        if ("next".equals(move)) computedDate = computedDate.plusWeeks(1);

        final LocalDate finalComputedDate = computedDate; // 람다 캡처용 고정
        String queryDate = finalComputedDate.toString();

        ApiResponse<List<TaskResponse>> resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tasks")
                        .queryParam("type", "WEEK")
                        .queryParam("date", queryDate)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<TaskResponse> taskList = resp.data();

        LocalDate monday = finalComputedDate.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        String weekRange = monday + " ~ " + sunday;

        List<DaySchedule> weekly = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            final LocalDate finalDay = day; // 람다 캡처용 고정

            List<TaskUi> uiTasks = taskList.stream()
                    .filter(t -> occursOn(t, finalDay)) // 기간 일정까지 포함
                    .map(this::toUi)
                    .toList();

            weekly.add(new DaySchedule(finalDay, uiTasks));
        }

        Context ctx = new Context();
        ctx.setVariable("weeklyTasks", weekly);
        ctx.setVariable("weekRange", weekRange);
        ctx.setVariable("currentDate", finalComputedDate);

        String bodyHtml = templateEngine.process("pages/task/week", ctx);

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle",
                finalComputedDate.getYear() + "년 " + finalComputedDate.getMonthValue() + "월");
        model.addAttribute("activeTab", "week");
        model.addAttribute("monthTitle",
                finalComputedDate.getMonthValue() + "월 " + finalComputedDate.getYear());
        model.addAttribute("body", bodyHtml);

        return "layout/base";
    }

    // ===========================
    //  월간 일정 페이지
    // ===========================
    @GetMapping("/tasks/month")
    public String month(
            @RequestParam(name = "move", required = false) String move,   // prev | next
            @RequestParam(name = "month", required = false) String month,  // YYYY-MM
            Model model
    ) {
        YearMonth ym = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.from(LocalDate.now());

        if ("prev".equals(move)) ym = ym.minusMonths(1);
        if ("next".equals(move)) ym = ym.plusMonths(1);

        final YearMonth finalYm = ym; // 직접 람다 캡처는 없지만 일관성

        String queryDate = finalYm.toString(); // YYYY-MM

        ApiResponse<List<TaskResponse>> resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tasks")
                        .queryParam("type", "MONTH")
                        .queryParam("date", queryDate)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        List<TaskResponse> taskList = resp.data();

        LocalDate firstDay = finalYm.atDay(1);
        LocalDate lastDay = finalYm.atEndOfMonth();

        LocalDate start = firstDay.with(DayOfWeek.MONDAY);
        LocalDate end = lastDay.with(DayOfWeek.SUNDAY);

        List<CalendarCell> cells = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            final LocalDate finalDate = d; // 람다 캡처용 고정
            boolean inMonth = (finalDate.getMonthValue() == finalYm.getMonthValue());

            // 기간 일정까지 고려: 날짜 셀마다 "겹치는 일정"을 필터링
            List<TaskUi> tasks = taskList.stream()
                    .filter(t -> occursOn(t, finalDate))
                    .map(this::toUi)
                    .toList();

            cells.add(new CalendarCell(finalDate, inMonth, tasks));
        }

        Context ctx = new Context();
        ctx.setVariable("yearMonth", finalYm); // YYYY-MM
        ctx.setVariable("cells", cells);
        ctx.setVariable("today", LocalDate.now());

        String bodyHtml = templateEngine.process("pages/task/month", ctx);

        model.addAttribute("title", "ToDoLab");
        model.addAttribute("showBaseHeader", false);
        model.addAttribute("headerTitle", finalYm.getYear() + "년 " + finalYm.getMonthValue() + "월");
        model.addAttribute("activeTab", "month");

        model.addAttribute("monthTitle", finalYm.getMonthValue() + "월 " + finalYm.getYear());

        model.addAttribute("body", bodyHtml);

        return "layout/base";
    }

    // ===========================
    // 일정이 특정 날짜(day)에 "발생/겹침"하는지 판단
    // - 미정(startAt null)은 제외
    // - 단일: startAt이 그 날짜에 포함되면 true
    // - 기간: [startAt, endAt) 과 [dayStart, dayEnd) 가 겹치면 true
    // ===========================
    private boolean occursOn(TaskResponse t, LocalDate day) {
        if (t == null || t.startAt() == null) {
            return false; // 미정 제외
        }

        LocalDateTime dayStart = day.atStartOfDay();
        LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

        LocalDateTime startAt = t.startAt();
        LocalDateTime endAt = t.endAt();

        // 단일 일정
        if (endAt == null) {
            return !startAt.isBefore(dayStart) && startAt.isBefore(dayEnd);
        }

        // 기간 일정: 겹침 여부
        return startAt.isBefore(dayEnd) && endAt.isAfter(dayStart);
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
