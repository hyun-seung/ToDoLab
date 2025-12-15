package com.todolab.view;

import com.todolab.common.api.ApiResponse;
import com.todolab.task.dto.TaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringWebFluxTemplateEngine;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TaskPageController {

    private final SpringWebFluxTemplateEngine templateEngine;
    private final WebClient webClient;

    // ===========================
    // 🔵 일정 등록 페이지
    // ===========================
    @GetMapping("/tasks/create")
    public Mono<String> createPage(Model model) {

        Context ctx = new Context(); // 특별한 데이터 없음
        String body = templateEngine.process("pages/task/create", ctx);

        // ✅ base.html에서 사용하는 공용 모델 값들
        model.addAttribute("title", "일정 등록 - ToDoLab");
        model.addAttribute("headerTitle", "일정 등록");
        model.addAttribute("activeTab", ""); // create는 탭 강조 안 함

        // ✅ 기존 레이아웃 호환(남겨둬도 무방)
        model.addAttribute("monthTitle", "");

        model.addAttribute("body", body);

        return Mono.just("layout/base");
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

        LocalDate computedDate = (date != null)
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

                        // TaskResponse -> TaskUi 변환
                        List<TaskUi> uiTasks = taskList.stream()
                                .filter(t -> t.date().equals(day))
                                .map(t -> new TaskUi(
                                        t.title(),
                                        t.description(),
                                        t.date(),
                                        t.time(),
                                        pickColor(t.title(), t.date(), t.time())
                                ))
                                .toList();

                        weekly.add(new DaySchedule(day, uiTasks));
                    }

                    Context ctx = new Context();
                    ctx.setVariable("weeklyTasks", weekly);
                    ctx.setVariable("weekRange", weekRange);
                    ctx.setVariable("currentDate", finalDate);

                    String bodyHtml = templateEngine.process("pages/task/week", ctx);

                    // ✅ base.html에서 사용하는 공용 모델 값들
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
    // 내부 DTO
    // ===========================

    public record TaskUi(
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
    // 색상 알고리즘
    // ===========================
    private String pickColor(String title, LocalDate date, LocalTime time) {
        String[] colors = {
                "#BFDBFE", "#C4B5FD", "#FDE68A",
                "#FBCFE8", "#BBF7D0"
        };

        String key = title + date + (time != null ? time.toString() : "");
        return colors[Math.abs(key.hashCode() % colors.length)];
    }
}
