package com.todolab.task.service;

import com.todolab.common.api.ErrorCode;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskResponse;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    TaskTxService taskTxService;

    TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskTxService, taskRepository);
    }

    /*******************
     *  일정 등록
     *******************/
    @Test
    @DisplayName("일정 등록 성공")
    void createTask_success() {
        Task saved = Task.builder()
                .title("title")
                .description("desc")
                .taskDate(LocalDate.of(2025, 11, 27))
                .taskTime(LocalTime.of(10, 30))
                .build();

        given(taskRepository.save(any())).willReturn(saved);

        TaskRequest request = new TaskRequest("title", "desc", LocalDate.of(2025, 11, 27), LocalTime.of(10, 30));

        TaskResponse res = taskService.create(request);

        assertThat(res.title()).isEqualTo("title");
        assertThat(res.description()).isEqualTo("desc");
        assertThat(res.date()).isEqualTo(LocalDate.of(2025, 11, 27));
        assertThat(res.time()).isEqualTo(LocalTime.of(10, 30));

        then(taskRepository).should(times(1)).save(any(Task.class));
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 조회 (단건)
     *******************/
    @Test
    @DisplayName("일정 조회(단건) 성공")
    void getTask_success() {
        Long taskId = 1L;

        Task task = Task.builder()
                .title("테스트 일정")
                .description("설명")
                .taskDate(LocalDate.of(2025, 12, 16))
                .taskTime(LocalTime.of(10, 0))
                .build();

        given(taskRepository.findById(taskId)).willReturn(Optional.of(task));

        TaskResponse res = taskService.getTask(taskId);

        assertThat(res.title()).isEqualTo("테스트 일정");
        assertThat(res.description()).isEqualTo("설명");
        assertThat(res.date()).isEqualTo(LocalDate.of(2025, 12, 16));
        assertThat(res.time()).isEqualTo(LocalTime.of(10, 0));

        then(taskRepository).should(times(1)).findById(taskId);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoMoreInteractions();
    }


    @Test
    @DisplayName("일정 조회(단건) 실패 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void getTask_notFound() {
        Long taskId = 999L;

        given(taskRepository.findById(taskId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(taskId))
                .isInstanceOf(TaskNotFoundException.class)
                .satisfies(ex -> {
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                });

        then(taskRepository).should(times(1)).findById(taskId);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 조회 (DAY / WEEK / MONTH)
     *******************/
    @Test
    @DisplayName("일정 조회 성공 - DAY 범위에 해당하는 데이터가 반환된다")
    void getTasks_day_success() {
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("DAY")
                .rawDate("2025-11-27")
                .build();

        LocalDate day = LocalDate.of(2025, 11, 27);

        List<Task> dummy = List.of(
                new Task("일정1", "desc1", day, LocalTime.of(0, 0)),
                new Task("일정3", "desc2", day, LocalTime.of(23, 0)),
                new Task("일정2", "desc3", day, LocalTime.of(23, 0))
        );

        given(taskRepository.findByDateRange(day, day)).willReturn(dummy);

        List<TaskResponse> res = taskService.getTasks(request);

        assertThat(res).hasSize(3);
        assertThat(res).extracting(TaskResponse::title)
                .containsExactly("일정1", "일정3", "일정2");
        assertThat(res).extracting(TaskResponse::description)
                .containsExactly("desc1", "desc2", "desc3");

        then(taskRepository).should(times(1)).findByDateRange(day, day);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - WEEK 범위에 해당하는 데이터가 반환된다")
    void getTasks_week_success() {
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("WEEK")
                .rawDate("2025-11-27")
                .build();

        DateRange expectedRange = TaskQueryType.WEEK.calculate("2025-11-27");

        List<Task> dummy = List.of(
                new Task("월요일", "desc1", expectedRange.getStart(), LocalTime.of(12, 0)),
                new Task("일요일", "desc2", expectedRange.getEnd(), LocalTime.of(23, 30)),
                new Task("수요일", "desc3", expectedRange.getEnd(), LocalTime.of(22, 30))
        );

        given(taskRepository.findByDateRange(expectedRange.getStart(), expectedRange.getEnd()))
                .willReturn(dummy);

        List<TaskResponse> res = taskService.getTasks(request);

        assertThat(res).hasSize(3);
        assertThat(res.getFirst().title()).isEqualTo("월요일");
        assertThat(res.getFirst().date()).isEqualTo(expectedRange.getStart());
        assertThat(res.getFirst().time()).isEqualTo(LocalTime.of(12, 0));
        assertThat(res.get(1).title()).isEqualTo("일요일");
        assertThat(res.get(1).date()).isEqualTo(expectedRange.getEnd());
        assertThat(res.get(1).time()).isEqualTo(LocalTime.of(23, 30));
        assertThat(res.get(2).title()).isEqualTo("수요일");
        assertThat(res.get(2).date()).isEqualTo(expectedRange.getEnd());
        assertThat(res.get(2).time()).isEqualTo(LocalTime.of(22, 30));

        then(taskRepository).should(times(1))
                .findByDateRange(expectedRange.getStart(), expectedRange.getEnd());
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 조회 성공 - MONTH 범위에 해당하는 데이터가 반환된다")
    void getTasks_month_success() {
        TaskQueryRequest request = TaskQueryRequest.builder()
                .rawType("MONTH")
                .rawDate("2025-11")
                .build();

        DateRange expectedRange = TaskQueryType.MONTH.calculate("2025-11");

        List<Task> dummy = List.of(
                new Task("1일 - 1", "desc1", expectedRange.getStart(), LocalTime.of(10, 0)),
                new Task("1일 - 3", "desc2", expectedRange.getStart(), LocalTime.of(20, 30)),
                new Task("1일 - 2", "desc3", expectedRange.getStart(), LocalTime.of(15, 0)),
                new Task("30일", "desc4", expectedRange.getEnd(), LocalTime.of(12, 30))
        );

        given(taskRepository.findByDateRange(expectedRange.getStart(), expectedRange.getEnd()))
                .willReturn(dummy);

        List<TaskResponse> res = taskService.getTasks(request);

        assertThat(res).hasSize(4);
        assertThat(res).extracting(TaskResponse::title)
                .containsExactly("1일 - 1", "1일 - 3", "1일 - 2", "30일");
        assertThat(res.getFirst().date()).isEqualTo(expectedRange.getStart());
        assertThat(res.get(1).date()).isEqualTo(expectedRange.getStart());
        assertThat(res.get(2).date()).isEqualTo(expectedRange.getStart());
        assertThat(res.get(3).date()).isEqualTo(expectedRange.getEnd());

        then(taskRepository).should(times(1))
                .findByDateRange(expectedRange.getStart(), expectedRange.getEnd());
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 수정
     *******************/
    @Test
    @DisplayName("일정 수정 성공")
    void updateTask_success() {
        long id = 10L;

        Task updated = Task.builder()
                .title("수정 title")
                .description("수정 desc")
                .taskDate(LocalDate.of(2026, 1, 20))
                .taskTime(LocalTime.of(1, 20))
                .build();

        TaskRequest req = new TaskRequest("수정 title", "수정 desc", LocalDate.of(2026, 12, 20), LocalTime.of(9, 15));

        given(taskTxService.updateTx(id, req)).willReturn(updated);

        TaskResponse res = taskService.update(id, req);

        assertThat(res.title()).isEqualTo("수정 title");
        assertThat(res.description()).isEqualTo("수정 desc");
        assertThat(res.date()).isEqualTo(LocalDate.of(2026, 1, 20));
        assertThat(res.time()).isEqualTo(LocalTime.of(1, 20));

        then(taskTxService).should(times(1)).updateTx(id, req);
        then(taskRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 수정 실패 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void updateTask_notFound() {
        long id = 999L;
        TaskRequest req = new TaskRequest("t", "d", LocalDate.of(2026, 1, 20), LocalTime.of(1, 25));

        given(taskTxService.updateTx(id, req)).willThrow(new TaskNotFoundException(id));

        assertThatThrownBy(() -> taskService.update(id, req))
                .isInstanceOf(TaskNotFoundException.class)
                .satisfies(ex -> {
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                });

        then(taskTxService).should(times(1)).updateTx(id, req);
        then(taskRepository).shouldHaveNoInteractions();
    }

    /*******************
     *  일정 삭제
     *******************/
    @Test
    @DisplayName("일정 삭제 성공")
    void deleteTask_success() {
        long id = 1L;
        given(taskRepository.existsById(id)).willReturn(true);

        taskService.delete(id);

        InOrder inOrder = inOrder(taskRepository);
        inOrder.verify(taskRepository).existsById(id);
        inOrder.verify(taskRepository).deleteById(id);

        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void deleteTask_notFound() {
        long id = 999L;
        given(taskRepository.existsById(id)).willReturn(false);

        assertThatThrownBy(() -> taskService.delete(id))
                .isInstanceOf(TaskNotFoundException.class)
                .satisfies(ex -> {
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                });

        then(taskRepository).should(times(1)).existsById(id);
        then(taskRepository).should(never()).deleteById(id);
        then(taskRepository).shouldHaveNoMoreInteractions();
        then(taskTxService).shouldHaveNoInteractions();
    }
}