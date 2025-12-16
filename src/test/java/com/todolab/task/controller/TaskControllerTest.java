package com.todolab.task.controller;

import com.todolab.common.api.ApiExceptionHandler;
import com.todolab.common.api.ApiResponse;
import com.todolab.common.api.ErrorCode;
import com.todolab.support.TestMockConfig;
import com.todolab.task.dto.TaskCreateRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebFluxTest(controllers = TaskController.class)
@Import({
        ApiExceptionHandler.class,
        TestMockConfig.class
})
class TaskControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TaskService taskService;

    /*******************
     *  일정 등록
     *******************/

    @Test
    @DisplayName("일정 등록 성공")
    void createTask_success() {
        TaskResponse mockRes = TaskResponse.builder()
                .id(1L)
                .title("테스트 코드 작성")
                .description("까먹지 말자..!")
                .date(LocalDate.of(2025,11, 18))
                .time(LocalTime.of(10, 42))
                .build();

        Mockito.when(taskService.create(any()))
                .thenReturn(Mono.just(mockRes));

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
                .expectBody(new ParameterizedTypeReference<ApiResponse<TaskResponse>>() {
                })
                .value(res -> {
                    assert res != null;
                    assert res.status().equals("success");

                    TaskResponse data = res.data();
                    assert data != null;
                    assert data.id().equals(1L);
                    assert data.title().equals("테스트 코드 작성");
                    assert data.description().equals("까먹지 말자..!");
                });
    }

    @Test
    @DisplayName("일정 등록 실패 - title은 필수이며 없을 경우 400, 10001 에러를 반환한다")
    void createTask_fail_titleMissing() {
        TaskCreateRequest req = new TaskCreateRequest(
                null, "desc", null, null
        );

        webTestClient.post()
                .uri("/tasks")
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }

    /*******************
     *  일정 조회 (단건)
     *******************/

    @Test
    @DisplayName("일정 단건 조회 성공 - 존재하는 일정이면 200과 data를 반환한다")
    void getTask_success() {
        long id = 1L;

        TaskResponse resp = TaskResponse.builder()
                .id(11L)
                .title("테스트")
                .description("설명")
                .date(LocalDate.of(2025, 12, 15))
                .time(LocalTime.of(10, 0))
                .build();

        given(taskService.getTask(id)).willReturn(Mono.just(resp));

        webTestClient.get()
                .uri("/tasks/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<TaskResponse>>() {})
                .value(res -> {
                    assert res.status().equals("success");
                    assert res.data().id().equals(11L);
                    assert res.data().title().equals("테스트");
                    assert res.data().description().equals("설명");
                    assert res.data().date().equals(LocalDate.of(2025, 12, 15));
                    assert res.data().time().equals(LocalTime.of(10, 0));
                });
    }

    @Test
    @DisplayName("일정 단건 조회 실패 - 존재하지 않으면 400과 TASK_NOT_FOUND를 반환한다")
    void getTask_taskNotFound() {
        long id = 999L;

        given(taskService.getTask(id)).willReturn(Mono.error(new TaskNotFoundException(ErrorCode.TASK_NOT_FOUND, "ID (" + id + ") 가 없습니다.")));

        webTestClient.get()
                .uri("/tasks/{id}", id)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<TaskResponse>>() {})
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == ErrorCode.TASK_NOT_FOUND.getCode();
                });
    }

    @Test
    @DisplayName("일정 단건 조회 실패 - PathVariable 타입이 잘못되면 400 에러를 반환한다")
    void getTask_invalidPathVariable() {
        webTestClient.get()
                .uri("/tasks/{id}", "abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<TaskResponse>>() {})
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == ErrorCode.INVALID_INPUT.getCode();
                });
    }

    /*******************
     *  일정 조회 (DAY / WEEK / MONTH)
     *******************/

    @Test
    @DisplayName("일정 조회 성공 - DAY 타입으로 정상 조회된다")
    void getTasks_DAY_success() {
        List<TaskResponse> dummy = List.of(
                TaskResponse.builder()
                        .id(999L)
                        .title("일정 조회 DAY")
                        .description("일정 조회")
                        .date(LocalDate.of(2025, 11, 25))
                        .time(LocalTime.of(10, 30))
                        .createdAt(null)
                        .build()
        );

        Mockito.when(taskService.getTasks(any()))
                .thenReturn(Mono.just(dummy));

        webTestClient.get()
                .uri(uri -> uri.path("/tasks")
                        .queryParam("type", "DAY")
                        .queryParam("date" , "2025-11-25")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<TaskResponse>>>() {})
                .value(res -> {
                    assert res.status().equals("success");
                    assert res.data().size() == 1;
                    assert res.data().getFirst().id().equals(999L);
                    assert res.data().getFirst().title().equals("일정 조회 DAY");
                    assert res.data().getFirst().description().equals("일정 조회");
                    assert res.data().getFirst().date().equals(LocalDate.of(2025, 11, 25));
                    assert res.data().getFirst().time().equals(LocalTime.of(10, 30));
                });
    }

    @Test
    @DisplayName("일정 조회 성공 - WEEK 타입으로 정상 조회된다")
    void getTasks_WEEK_success() {
        List<TaskResponse> dummy = List.of(
                TaskResponse.builder()
                        .id(11L)
                        .title("WEEK 일정 1")
                        .description("설명1")
                        .date(LocalDate.of(2025, 11, 24))
                        .time(LocalTime.of(1, 0))
                        .createdAt(null)
                        .build(),
                TaskResponse.builder()
                        .id(21L)
                        .title("WEEK 일정 2")
                        .description("설명2")
                        .date(LocalDate.of(2025, 11, 30))
                        .time(LocalTime.of(23, 0))
                        .createdAt(null)
                        .build()
        );

        Mockito.when(taskService.getTasks(any()))
                .thenReturn(Mono.just(dummy));

        webTestClient.get()
                .uri(uri -> uri.path("/tasks")
                        .queryParam("type", "DAY")
                        .queryParam("date" , "2025-11-25")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<TaskResponse>>>() {})
                .value(res -> {
                    assert res.status().equals("success");
                    assert res.data().size() == 2;
                    assert res.data().getFirst().id().equals(11L);
                    assert res.data().getFirst().title().equals("WEEK 일정 1");
                    assert res.data().getFirst().description().equals("설명1");
                    assert res.data().getFirst().date().equals(LocalDate.of(2025, 11, 24));
                    assert res.data().getFirst().time().equals(LocalTime.of(1, 0));

                    assert res.data().get(1).id().equals(21L);
                    assert res.data().get(1).title().equals("WEEK 일정 2");
                    assert res.data().get(1).description().equals("설명2");
                    assert res.data().get(1).date().equals(LocalDate.of(2025, 11, 30));
                    assert res.data().get(1).time().equals(LocalTime.of(23, 0));
                });
    }

    @Test
    @DisplayName("일정 조회 성공 - MONTH 타입으로 정상 조회된다")
    void getTasks_MONTH_success() {
        List<TaskResponse> dummy = List.of(
                TaskResponse.builder()
                        .id(111L)
                        .title("MONTH 일정 1")
                        .description("설명1")
                        .date(LocalDate.of(2025, 11, 3))
                        .time(LocalTime.of(8, 0))
                        .createdAt(null)
                        .build(),
                TaskResponse.builder()
                        .id(112L)
                        .title("MONTH 일정 2")
                        .description("설명2")
                        .date(LocalDate.of(2025, 11, 10))
                        .time(LocalTime.of(9, 30))
                        .createdAt(null)
                        .build(),
                TaskResponse.builder()
                        .id(113L)
                        .title("MONTH 일정 3")
                        .description("설명3")
                        .date(LocalDate.of(2025, 11, 18))
                        .time(LocalTime.of(14, 0))
                        .createdAt(null)
                        .build(),
                TaskResponse.builder()
                        .id(114L)
                        .title("MONTH 일정 4")
                        .description("설명4")
                        .date(LocalDate.of(2025, 11, 28))
                        .time(LocalTime.of(19, 45))
                        .createdAt(null)
                        .build()
        );

        Mockito.when(taskService.getTasks(any()))
                .thenReturn(Mono.just(dummy));

        webTestClient.get()
                .uri(uri -> uri.path("/tasks")
                        .queryParam("type", "MONTH")
                        .queryParam("date", "2025-11")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<ApiResponse<List<TaskResponse>>>() {})
                .value(res -> {
                    assert res.status().equals("success");
                    assert res.data().size() == 4;

                    // 1번 요소
                    assert res.data().getFirst().id().equals(111L);
                    assert res.data().getFirst().title().equals("MONTH 일정 1");
                    assert res.data().getFirst().description().equals("설명1");
                    assert res.data().getFirst().date().equals(LocalDate.of(2025, 11, 3));
                    assert res.data().getFirst().time().equals(LocalTime.of(8, 0));

                    // 2번 요소
                    assert res.data().get(1).id().equals(112L);
                    assert res.data().get(1).title().equals("MONTH 일정 2");
                    assert res.data().get(1).description().equals("설명2");
                    assert res.data().get(1).date().equals(LocalDate.of(2025, 11, 10));
                    assert res.data().get(1).time().equals(LocalTime.of(9, 30));

                    // 3번 요소
                    assert res.data().get(2).id().equals(113L);
                    assert res.data().get(2).title().equals("MONTH 일정 3");
                    assert res.data().get(2).description().equals("설명3");
                    assert res.data().get(2).date().equals(LocalDate.of(2025, 11, 18));
                    assert res.data().get(2).time().equals(LocalTime.of(14, 0));

                    // 4번 요소
                    assert res.data().get(3).id().equals(114L);
                    assert res.data().get(3).title().equals("MONTH 일정 4");
                    assert res.data().get(3).description().equals("설명4");
                    assert res.data().get(3).date().equals(LocalDate.of(2025, 11, 28));
                    assert res.data().get(3).time().equals(LocalTime.of(19, 45));
                });
    }

    @Test
    @DisplayName("일정 조회 실패 - 잘못된 type이면 400, 10001 에러를 반환한다")
    void getTasks_fail_invalidType() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tasks")
                        .queryParam("type", "INVALID")
                        .queryParam("date", "2025-11-24")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }

    @Test
    @DisplayName("일정 조회 실패 - type이 누락되면 400, 10001 에러를 반환한다")
    void getTasks_fail_missingType() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tasks")
                        .queryParam("date", "2025-11-24")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }

    @Test
    @DisplayName("일정 조회 실패 - date가 누락되면 400, 10001 에러를 반환한다")
    void getTasks_fail_missingDate() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tasks")
                        .queryParam("type", "DAY")
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-11", "20251127", "25-11-27"})
    @DisplayName("일정 조회 실패 - DAY는 yyyy-MM-dd 형식을 요구한다")
    void getTasks_DAY_fail_invalidDateFormat(String invalidDate) {
        webTestClient.get()
                .uri(uri -> uri.path("/tasks")
                        .queryParam("type", "DAY")
                        .queryParam("date", invalidDate)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-11", "20251127", "25-11-27"})
    @DisplayName("일정 조회 실패 - WEEK는 yyyy-MM-dd 형식을 요구한다")
    void getTasks_WEEK_fail_invalidDateFormat(String invalidDate) {
        webTestClient.get()
                .uri(uri -> uri.path("/tasks")
                        .queryParam("type", "WEEK")
                        .queryParam("date", invalidDate)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-11-27", "202511", "25-11"})
    @DisplayName("일정 조회 실패 - MONTH는 yyyy-MM-dd 형식을 요구한다")
    void getTasks_MONTH_fail_invalidDateFormat(String invalidDate) {
        webTestClient.get()
                .uri(uri -> uri.path("/tasks")
                        .queryParam("type", "MONTH")
                        .queryParam("date", invalidDate)
                        .build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(new ParameterizedTypeReference<ApiResponse<Void>>() {
                })
                .value(res -> {
                    assert res.status().equals("fail");
                    assert res.error().code() == 10001;
                });
    }
}