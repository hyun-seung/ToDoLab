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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import reactor.core.publisher.Mono;

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

    private final SpringWebFluxTemplateEngine templateEngine;
    private final WebClient webClient;

    // ===========================
    // 🔵 일정 등록 모달
    // ===========================
    @GetMapping(
            value = "/tasks/create",
            headers = "X-Requested-With=fetch",
            produces = MediaType.TEXT_HTML_VALUE
    )
    @ResponseBody
    public Mono<String> createFragment(ServerWebExchange exchange) {

        Context ctx = new Context();

        StringWriter writer = new StringWriter();
        templateEngine.process(
                "pages/task/create",
                Set.of("#create-page"),
                ctx,
                writer
        );

        return Mono.just(writer.toString());
    }

    // ===========================
    // 🟣 일정 상세 모달
    // ===========================
    @GetMapping(
            value = "/tasks/detail",
            headers = "X-Requested-With=fetch",
            produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Mono<String> detailFragment(
            @RequestParam Long id,
            ServerWebExchange exchange
    ) {
        return webClient.get()
                .uri(uri -> uri.path("/tasks/{id}").build(id))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<TaskResponse>>() {})
                .map(ApiResponse::data)
                .map(task -> {
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
                });
    }


    // ===========================
    // 🔵 일간 일정 페이지
    // ===========================
    @GetMapping("/tasks/day")
    public Mono<String> day(
            @RequestParam(required = false) String move,   // prev | next
            @RequestParam(required = false) String date,   // YYYY-MM-DD
            Model model
    ) {
        LocalDate targetDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) {
            targetDate = targetDate.minusDays(1);
        } else if ("next".equals(move)) {
            targetDate = targetDate.plusDays(1);
        }

        LocalDate finalDate = targetDate;
        String queryDate = finalDate.toString(); // YYYY-MM-DD

        return webClient.get()
                .uri(uri -> uri
                        .path("/tasks")
                        .queryParam("type", "DAY")
                        .queryParam("date", queryDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<TaskResponse>>>() {})
                .map(ApiResponse::data)
                .map(taskList -> {

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
                    ctx.setVariable("date", finalDate);
                    ctx.setVariable("tasks", tasks);
                    ctx.setVariable("isToday", finalDate.equals(LocalDate.now()));

                    String bodyHtml = templateEngine.process("pages/task/day", ctx);

                    model.addAttribute("title", "일간 일정 - ToDoLab");
                    model.addAttribute("headerTitle",
                            finalDate.getYear() + "년 "
                                    + finalDate.getMonthValue() + "월 "
                                    + finalDate.getDayOfMonth() + "일");
                    model.addAttribute("activeTab", "day");
                    model.addAttribute("body", bodyHtml);

                    return "layout/base";
                });
    }

    // ===========================
    // 🔵 주간 일정 페이지
    // ===========================
    @GetMapping("/tasks/week")
    public Mono<String> week(
            @RequestParam(required = false) String move,
            @RequestParam(required = false) String date,
            Model model
    ) {

        LocalDate computedDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date)
                : LocalDate.now();

        if ("prev".equals(move)) {
            computedDate = computedDate.minusWeeks(1);
        } else if ("next".equals(move)) {
            computedDate = computedDate.plusWeeks(1);
        }

        LocalDate finalDate = computedDate;
        String queryDate = finalDate.toString();

        return webClient.get()
                .uri(uri -> uri
                        .path("/tasks")
                        .queryParam("type", "WEEK")
                        .queryParam("date", queryDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<TaskResponse>>>() {})
                .map(ApiResponse::data)
                .map(taskList -> {

                    LocalDate monday = finalDate.with(DayOfWeek.MONDAY);
                    LocalDate sunday = monday.plusDays(6);

                    String weekRange = monday + " ~ " + sunday;

                    List<DaySchedule> weekly = new ArrayList<>();

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
                    ctx.setVariable("currentDate", finalDate);

                    String bodyHtml = templateEngine.process("pages/task/week", ctx);

                    model.addAttribute("title", "주간 일정 - ToDoLab");
                    model.addAttribute("headerTitle",
                            finalDate.getYear() + "년 " + finalDate.getMonthValue() + "월");
                    model.addAttribute("activeTab", "week");

                    // ✅ 기존 레이아웃 호환(남겨둬도 무방)
                    model.addAttribute("monthTitle",
                            finalDate.getMonthValue() + "월 " + finalDate.getYear());

                    model.addAttribute("body", bodyHtml);

                    return "layout/base";
                });
    }

    // ===========================
    // 🟣 월간 일정 페이지
    // ===========================
    @GetMapping("/tasks/month")
    public Mono<String> month(
            @RequestParam(required = false) String move,   // prev | next
            @RequestParam(required = false) String month,  // YYYY-MM
            Model model
    ) {
        YearMonth ym = (month != null && !month.isBlank())
                ? YearMonth.parse(month)
                : YearMonth.from(LocalDate.now());

        if ("prev".equals(move)) {
            ym = ym.minusMonths(1);
        } else if ("next".equals(move)) {
            ym = ym.plusMonths(1);
        }

        String queryDate = ym.toString();

        YearMonth finalYm = ym;

        return webClient.get()
                .uri(uri -> uri
                        .path("/tasks")
                        .queryParam("type", "MONTH")
                        .queryParam("date", queryDate)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<TaskResponse>>>() {})
                .map(ApiResponse::data)
                .map(taskList -> {

                    LocalDate firstDay = finalYm.atDay(1);
                    LocalDate lastDay = finalYm.atEndOfMonth();

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
                        boolean inMonth = (d.getMonthValue() == finalYm.getMonthValue());
                        List<TaskUi> tasks = byDate.getOrDefault(d, List.of());
                        cells.add(new CalendarCell(d, inMonth, tasks));
                    }

                    Context ctx = new Context();
                    ctx.setVariable("yearMonth", finalYm); // YYYY-MM
                    ctx.setVariable("cells", cells);
                    ctx.setVariable("today", LocalDate.now());

                    String bodyHtml = templateEngine.process("pages/task/month", ctx);

                    model.addAttribute("title", "월간 일정 - ToDoLab");
                    model.addAttribute("headerTitle", finalYm.getYear() + "년 " + finalYm.getMonthValue() + "월");
                    model.addAttribute("activeTab", "month");

                    // ✅ 기존 호환 변수(남겨둬도 됨)
                    model.addAttribute("monthTitle", finalYm.getMonthValue() + "월 " + finalYm.getYear());

                    model.addAttribute("body", bodyHtml);

                    return "layout/base";
                });
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
    ) {}

    public record DaySchedule(
            LocalDate date,
            List<TaskUi> tasks
    ) {}

    // ===========================
    // 월 캘린더 셀 DTO
    // ===========================
    public record CalendarCell(
            LocalDate date,
            boolean inMonth,
            List<TaskUi> tasks
    ) {}
}
