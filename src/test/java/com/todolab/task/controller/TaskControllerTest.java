package com.todolab.task.controller;

import com.todolab.common.api.ApiResponse;
import com.todolab.support.ControllerTestSupport;
import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.time.LocalTime;

class TaskControllerTest extends ControllerTestSupport {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void 일정_등록() {
        TaskCreateRequest req = new TaskCreateRequest(
                "테스트 코드 작성",
                "까먹지 말자..!",
                LocalDate.of(2025, 11, 18),
                LocalTime.of(10, 42)
        );

        webTestClient.post()
                .uri("/tasks")
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<TaskResponse>>() {})
                .value(res -> {
                    assert res != null;
                    assert res.status().equals("success");

                    TaskResponse data = res.data();
                    assert data != null;

                    assert data.id() != null;
                    assert data.title().equals("테스트 코드 작성");
                    assert data.description().equals("까먹지 말자..!");
                });
    }
}