package com.todolab.task.controller;

import com.todolab.common.api.ApiExceptionHandler;
import com.todolab.common.api.ErrorCode;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ApiExceptionHandler.class)
@WebMvcTest(controllers = TaskController.class)
class TaskControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    TaskService taskService;

    /*******************
     *  일정 등록
     *******************/
    @Test
    @DisplayName("일정 등록 성공")
    void createTask_success() throws Exception {
        TaskRequest req = new TaskRequest(
                "테스트 코드 작성",
                "까먹지 말자..!",
                LocalDate.of(2025, 11, 18),
                LocalTime.of(10, 42)
        );

        TaskResponse mockRes = TaskResponse.builder()
                .id(1L)
                .title("테스트 코드 작성")
                .description("까먹지 말자..!")
                .date(LocalDate.of(2025, 11, 18))
                .time(LocalTime.of(10, 42))
                .build();

        given(taskService.create(any(TaskRequest.class))).willReturn(mockRes);

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("테스트 코드 작성"))
                .andExpect(jsonPath("$.data.description").value("까먹지 말자..!"));

        then(taskService).should().create(any(TaskRequest.class));
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 등록 실패 - title은 필수이며 없을 경우 400, 10001 에러를 반환한다")
    void createTask_fail_titleMissing() throws Exception {
        TaskRequest req = new TaskRequest(
                null, "desc", null, null
        );

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(10001));

        then(taskService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 조회 (단건)
     *******************/
    @Test
    @DisplayName("일정 단건 조회 성공")
    void getTask_success() throws Exception {
        long id = 11L;

        TaskResponse resp = TaskResponse.builder()
                .id(11L)
                .title("테스트")
                .description("설명")
                .date(LocalDate.of(2025, 12, 15))
                .time(LocalTime.of(10, 0))
                .build();

        given(taskService.getTask(id)).willReturn(resp);

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.title").value("테스트"))
                .andExpect(jsonPath("$.data.description").value("설명"))
                .andExpect(jsonPath("$.data.date").value("2025-12-15"))
                .andExpect(jsonPath("$.data.time").value("10:00:00"));

        then(taskService).should().getTask(id);
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 단건 조회 실패 - 존재하지 않으면 404와 TASK_NOT_FOUND를 반환한다")
    void getTask_taskNotFound() throws Exception {
        long id = 999L;

        given(taskService.getTask(id)).willThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.TASK_NOT_FOUND.getCode()));

        then(taskService).should().getTask(id);
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 단건 조회 실패 - PathVariable 타입이 잘못되면 400 에러를 반환한다")
    void getTask_invalidPathVariable() throws Exception {
        mockMvc.perform(get("/tasks/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.INVALID_INPUT.getCode()));

        then(taskService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 조회 (DAY / WEEK / MONTH)
     *******************/
    @Test
    @DisplayName("일정 조회 성공 - DAY 타입으로 정상 조회된다")
    void getTasks_DAY_success() throws Exception {
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

        given(taskService.getTasks(any())).willReturn(dummy);

        mockMvc.perform(get("/tasks")
                        .param("type", "DAY")
                        .param("date", "2025-11-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(999))
                .andExpect(jsonPath("$.data[0].title").value("일정 조회 DAY"));

        then(taskService).should().getTasks(any());
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - WEEK 타입으로 정상 조회된다")
    void getTasks_WEEK_success() throws Exception {
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

        given(taskService.getTasks(any())).willReturn(dummy);

        mockMvc.perform(get("/tasks")
                        .param("type", "WEEK")
                        .param("date", "2025-11-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[1].id").value(21));

        then(taskService).should().getTasks(any());
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - MONTH 타입으로 정상 조회된다")
    void getTasks_MONTH_success() throws Exception {
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

        given(taskService.getTasks(any())).willReturn(dummy);

        mockMvc.perform(get("/tasks")
                        .param("type", "MONTH")
                        .param("date", "2025-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].id").value(111))
                .andExpect(jsonPath("$.data[0].title").value("MONTH 일정 1"))
                .andExpect(jsonPath("$.data[1].id").value(112))
                .andExpect(jsonPath("$.data[1].title").value("MONTH 일정 2"))
                .andExpect(jsonPath("$.data[2].id").value(113))
                .andExpect(jsonPath("$.data[2].title").value("MONTH 일정 3"))
                .andExpect(jsonPath("$.data[3].id").value(114))
                .andExpect(jsonPath("$.data[3].title").value("MONTH 일정 4"));

        then(taskService).should().getTasks(any());
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 조회 실패 - 잘못된 type이면 400, 10001 에러를 반환한다")
    void getTasks_fail_invalidType() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("type", "INVALID")
                        .param("date", "2025-11-24"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value("10001"));

        then(taskService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 실패 - type이 누락되면 400, 10001 에러를 반환한다")
    void getTasks_fail_missingType() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("type", "DAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value("10001"));

        then(taskService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 실패 - date가 누락되면 400, 10001 에러를 반환한다")
    void getTasks_fail_missingDate() throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("type", "DAY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value("10001"));

        then(taskService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-11", "20251127", "25-11-27"})
    @DisplayName("일정 조회 실패 - DAY는 yyyy-MM-dd 형식을 요구한다")
    void getTasks_DAY_fail_invalidDateFormat(String invalidDate) throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("type", "DAY")
                        .param("date", invalidDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value("10001"));

        then(taskService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-11", "20251127", "25-11-27"})
    @DisplayName("일정 조회 실패 - WEEK는 yyyy-MM-dd 형식을 요구한다")
    void getTasks_WEEK_fail_invalidDateFormat(String invalidDate) throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("type", "WEEK")
                        .param("date", invalidDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value("10001"));

        then(taskService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-11-27", "202511", "25-11"})
    @DisplayName("일정 조회 실패 - MONTH는 yyyy-MM 형식을 요구한다")
    void getTasks_MONTH_fail_invalidDateFormat(String invalidDate) throws Exception {
        mockMvc.perform(get("/tasks")
                        .param("type", "MONTH")
                        .param("date", invalidDate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value("10001"));

        then(taskService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 수정
     *******************/
    @Test
    @DisplayName("일정 정상 수정")
    void updateTask_success() throws Exception {
        long id = 10L;

        TaskRequest req = new TaskRequest("updated title", null, null, null);

        TaskResponse serviceRes = TaskResponse.builder()
                .id(id)
                .title("updated title2")
                .build();

        given(taskService.update(eq(id), any(TaskRequest.class))).willReturn(serviceRes);

        mockMvc.perform(put("/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.title").value("updated title2"));

        then(taskService).should().update(eq(id), any(TaskRequest.class));
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 수정 실패 - 없는 id일 경우 TaskNotFoundException 발생")
    void updateTask_NotExistId() throws Exception {
        // given
        long id = 10L;

        TaskRequest req = new TaskRequest("title", null, null, null);

        given(taskService.update(eq(id), any(TaskRequest.class)))
                .willThrow(new TaskNotFoundException(id));

        // when & then
        mockMvc.perform(put("/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.TASK_NOT_FOUND.getCode()));

        then(taskService).should().update(eq(id), any(TaskRequest.class));
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 삭제 성공 - 존재하는 id면 200과 삭제된 id를 반환한다")
    void deleteTask_success() throws Exception {
        // given
        long id = 1L;
        willDoNothing().given(taskService).delete(id);

        // when & then
        mockMvc.perform(delete("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value((int) id));

        then(taskService).should().delete(id);
        then(taskService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 id면 404와 TASK_NOT_FOUND를 반환한다")
    void deleteTask_notFound() throws Exception {
        // given
        long id = 999L;
        willThrow(new TaskNotFoundException(id)).given(taskService).delete(id);

        // when & then
        mockMvc.perform(delete("/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.TASK_NOT_FOUND.getCode()));

        then(taskService).should().delete(id);
        then(taskService).shouldHaveNoMoreInteractions();
    }


}