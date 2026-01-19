package com.todolab.task.service;

import com.todolab.common.api.ErrorCode;
import com.todolab.task.domain.Task;
import com.todolab.task.domain.query.DateRange;
import com.todolab.task.domain.query.TaskQueryType;
import com.todolab.task.dto.TaskRequest;
import com.todolab.task.dto.TaskQueryRequest;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    TaskTxService taskTxService;

    TaskService taskService;

    @BeforeEach
    void setUp() {
        // Schedulers.trampoline() 변경?
        taskService = new TaskService(taskTxService, taskRepository, Schedulers.immediate());
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

        Mockito.when(taskRepository.save(any()))
                .thenReturn(saved);

        TaskRequest request = new TaskRequest("title", null, null, null);

        StepVerifier.create(taskService.create(request))
                .assertNext(res -> {
                    assertThat(res.title()).isEqualTo("title");
                })
                .verifyComplete();
    }

    /*******************
     *  일정 조회 (단건)
     *******************/

    @Test
    @DisplayName("일정 조회_단건 성공")
    void getTask_success() {
        Long taskId = 1L;

        Task task = Task.builder()
                .title("테스트 일정")
                .description("설명")
                .taskDate(LocalDate.of(2025, 12, 16))
                .taskTime(LocalTime.of(10, 0))
                .build();

        given(taskRepository.findById(taskId))
                .willReturn(Optional.of(task));

        StepVerifier.create(taskService.getTask(taskId))
                .assertNext(response -> {
                    assertThat(response.title()).isEqualTo("테스트 일정");
                    assertThat(response.date()).isEqualTo(task.getTaskDate());
                    assertThat(response.time()).isEqualTo(task.getTaskTime());
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("일정 조회_단건 - 존재하지 않는 ID면 TaskNotFoundException 발생")
    void getTask_notFound() {
        Long taskId = 999L;

        given(taskRepository.findById(taskId))
                .willReturn(Optional.empty());

        StepVerifier.create(taskService.getTask(taskId))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(TaskNotFoundException.class);
                    TaskNotFoundException e = (TaskNotFoundException) ex;
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TASK_NOT_FOUND);
                })
                .verify();
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

        Mockito.when(taskRepository.findByDateRange(day, day))
                .thenReturn(dummy);

        StepVerifier.create(taskService.getTasks(request))
                .assertNext(list -> {
                    assertThat(list.size()).isEqualTo(3);
                    assertThat(list.getFirst().title()).isEqualTo("일정1");
                    assertThat(list.getFirst().description()).isEqualTo("desc1");
                    assertThat(list.get(1).title()).isEqualTo("일정3");
                    assertThat(list.get(1).description()).isEqualTo("desc2");
                    assertThat(list.get(2).title()).isEqualTo("일정2");
                    assertThat(list.get(2).description()).isEqualTo("desc3");
                })
                .verifyComplete();

        Mockito.verify(taskRepository, Mockito.times(1))
                .findByDateRange(day, day);
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

        Mockito.when(taskRepository.findByDateRange(expectedRange.getStart(), expectedRange.getEnd()))
                .thenReturn(dummy);

        StepVerifier.create(taskService.getTasks(request))
                .assertNext(list -> {
                    assertThat(list.size()).isEqualTo(3);
                    assertThat(list.getFirst().title()).isEqualTo("월요일");
                    assertThat(list.getFirst().date()).isEqualTo(expectedRange.getStart());
                    assertThat(list.getFirst().time()).isEqualTo(LocalTime.of(12, 0));
                    assertThat(list.get(1).title()).isEqualTo("일요일");
                    assertThat(list.get(1).date()).isEqualTo(expectedRange.getEnd());
                    assertThat(list.get(1).time()).isEqualTo(LocalTime.of(23, 30));
                    assertThat(list.get(2).title()).isEqualTo("수요일");
                    assertThat(list.get(2).date()).isEqualTo(expectedRange.getEnd());
                    assertThat(list.get(2).time()).isEqualTo(LocalTime.of(22, 30));
                })
                .verifyComplete();

        Mockito.verify(taskRepository).findByDateRange(
                expectedRange.getStart(), expectedRange.getEnd()
        );
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

        Mockito.when(taskRepository.findByDateRange(expectedRange.getStart(), expectedRange.getEnd()))
                .thenReturn(dummy);

        StepVerifier.create(taskService.getTasks(request))
                .assertNext(list -> {
                    assertThat(list.size()).isEqualTo(4);
                    assertThat(list.getFirst().title()).isEqualTo("1일 - 1");
                    assertThat(list.getFirst().date()).isEqualTo(expectedRange.getStart());
                    assertThat(list.get(1).title()).isEqualTo("1일 - 3");
                    assertThat(list.get(1).date()).isEqualTo(expectedRange.getStart());
                    assertThat(list.get(2).title()).isEqualTo("1일 - 2");
                    assertThat(list.get(2).date()).isEqualTo(expectedRange.getStart());
                    assertThat(list.get(3).title()).isEqualTo("30일");
                    assertThat(list.get(3).date()).isEqualTo(expectedRange.getEnd());
                })
                .verifyComplete();

        Mockito.verify(taskRepository).findByDateRange(
                expectedRange.getStart(), expectedRange.getEnd()
        );
    }
}