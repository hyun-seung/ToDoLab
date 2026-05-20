package com.todolab.task.service;

import com.todolab.task.domain.Task;
import com.todolab.task.domain.TaskStatus;
import com.todolab.task.exception.TaskNotFoundException;
import com.todolab.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class TaskTxServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Test
    @DisplayName("moveToTodayTx는 Task를 Today 상태로 변경하고 저장한다")
    void moveToTodayTx_success() {
        // given
        long id = 1L;
        LocalDate targetDate = LocalDate.of(2026, 5, 21);
        Task task = Task.builder()
                .title("task")
                .build();
        TaskTxService service = new TaskTxService(taskRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.moveToTodayTx(id, targetDate);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getTargetDate()).isEqualTo(targetDate);
        assertThat(result.getCompletedAt()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("completeTx는 Task를 Done 상태로 변경하고 저장한다")
    void completeTx_success() {
        // given
        long id = 1L;
        LocalDateTime completedAt = LocalDateTime.of(2026, 5, 21, 22, 0);
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 21))
                .build();
        TaskTxService service = new TaskTxService(taskRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.completeTx(id, completedAt);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(result.getCompletedAt()).isEqualTo(completedAt);

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("carryOverTx는 Task를 다음 날짜의 Today 상태로 변경하고 저장한다")
    void carryOverTx_success() {
        // given
        long id = 1L;
        LocalDate nextDate = LocalDate.of(2026, 5, 22);
        Task task = Task.builder()
                .title("task")
                .status(TaskStatus.TODAY)
                .targetDate(LocalDate.of(2026, 5, 21))
                .build();
        TaskTxService service = new TaskTxService(taskRepository);

        given(taskRepository.findById(id)).willReturn(Optional.of(task));
        given(taskRepository.save(task)).willReturn(task);

        // when
        Task result = service.carryOverTx(id, nextDate);

        // then
        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODAY);
        assertThat(result.getTargetDate()).isEqualTo(nextDate);
        assertThat(result.getCompletedAt()).isNull();

        then(taskRepository).should(times(1)).findById(id);
        then(taskRepository).should(times(1)).save(task);
    }

    @Test
    @DisplayName("상태 변경 대상 Task가 없으면 TaskNotFoundException이 발생한다")
    void changeStatus_notFound() {
        // given
        long id = 999L;
        TaskTxService service = new TaskTxService(taskRepository);
        given(taskRepository.findById(id)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.moveToTodayTx(id, LocalDate.of(2026, 5, 21)))
                .isInstanceOf(TaskNotFoundException.class);

        then(taskRepository).should(times(1)).findById(id);
    }
}
