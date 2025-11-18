package com.todolab.task.repository;

import com.todolab.task.domain.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataR2dbcTest
class TaskRepositoryTest {

    @Autowired
    TaskRepository taskRepository;

    @Test
    void save_성공() {
        Task task = Task.builder()
                .title("hello")
                .build();

        StepVerifier.create(taskRepository.save(task))
                .assertNext(t -> {
                    assertThat(t.getId()).isNotNull();
                    assertThat(t.getTitle()).isEqualTo("hello");
                })
                .verifyComplete();
    }
}