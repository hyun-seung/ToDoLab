package com.todolab.task.controller;

import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.time.LocalTime;

@SpringBootTest
@AutoConfigureWebTestClient
class TaskControllerTest {

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
                .exchange().expectStatus().isOk()
                .expectBody(TaskResponse.class)
                .value(res -> {
                    assert res.id() != null;
                    assert res.title().equals("테스트 코드 작성");
                    assert res.description().equals("까먹지 말자..!");
                });
    }
}