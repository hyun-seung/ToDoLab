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
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        String queryDate = targetDate.toString(); // YYYY-MM-DD

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
                .map(t -> new TaskUi(
                        t.id(),
                        t.title(),
                        t.description(),
                        t.date(),
                        t.time(),
                        pickColor(t.id())
                ))
                .toList();

        Context ctx = new Context();
        ctx.setVariable("date", targetDate);
        ctx.setVariable("tasks", tasks);
        ctx.setVariable("isToday", targetDate.equals(LocalDate.now()));

        String bodyHtml = templateEngine.process("pages/task/day", ctx);

        model.addAttribute("title", "일간 일정 - ToDoLab");
        model.addAttribute("headerTitle",
                targetDate.getYear() + "년 "
                        + targetDate.getMonthValue() + "월 "
                        + targetDate.getDayOfMonth() + "일");
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

        String queryDate = computedDate.toString();

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

        LocalDate monday = computedDate.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        String weekRange = monday + " ~ " + sunday;

        List<DaySchedule> weekly = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);

            List<TaskUi> uiTasks = taskList.stream()
                    .filter(t -> t.date().equals(day))
                    .map(t -> new TaskUi(
                            t.id(),
                            t.title(),
                            t.description(),
                            t.date(),
                            t.time(),
                            pickColor(t.id())
                    ))
                    .toList();

            weekly.add(new DaySchedule(day, uiTasks));
        }

        Context ctx = new Context();
        ctx.setVariable("weeklyTasks", weekly);
        ctx.setVariable("weekRange", weekRange);
        ctx.setVariable("currentDate", computedDate);

        String bodyHtml = templateEngine.process("pages/task/week", ctx);

        model.addAttribute("title", "주간 일정 - ToDoLab");
        model.addAttribute("headerTitle",
                computedDate.getYear() + "년 " + computedDate.getMonthValue() + "월");
        model.addAttribute("activeTab", "week");
        model.addAttribute("monthTitle",
                computedDate.getMonthValue() + "월 " + computedDate.getYear());
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

        String queryDate = ym.toString();

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

        LocalDate firstDay = ym.atDay(1);
        LocalDate lastDay = ym.atEndOfMonth();

        LocalDate start = firstDay.with(DayOfWeek.MONDAY);
        LocalDate end = lastDay.with(DayOfWeek.SUNDAY);

        Map<LocalDate, List<TaskUi>> byDate = taskList.stream()
                .collect(Collectors.groupingBy(
                        TaskResponse::date,
                        Collectors.mapping(t -> new TaskUi(
                                t.id(),
                                t.title(),
                                t.description(),
                                t.date(),
                                t.time(),
                                pickColor(t.id())
                        ), Collectors.toList())
                ));

        List<CalendarCell> cells = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean inMonth = (d.getMonthValue() == ym.getMonthValue());
            List<TaskUi> tasks = byDate.getOrDefault(d, List.of());
            cells.add(new CalendarCell(d, inMonth, tasks));
        }

        Context ctx = new Context();
        ctx.setVariable("yearMonth", ym); // YYYY-MM
        ctx.setVariable("cells", cells);
        ctx.setVariable("today", LocalDate.now());

        String bodyHtml = templateEngine.process("pages/task/month", ctx);

        model.addAttribute("title", "월간 일정 - ToDoLab");
        model.addAttribute("headerTitle", ym.getYear() + "년 " + ym.getMonthValue() + "월");
        model.addAttribute("activeTab", "month");

        // ✅ 기존 호환 변수(남겨둬도 됨)
        model.addAttribute("monthTitle", ym.getMonthValue() + "월 " + ym.getYear());

        model.addAttribute("body", bodyHtml);

        return "layout/base";
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
