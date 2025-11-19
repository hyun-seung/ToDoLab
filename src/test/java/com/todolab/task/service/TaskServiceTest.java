package com.todolab.task.service;

import com.todolab.support.ServiceTestSupport;
import com.todolab.task.dto.TaskCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TaskServiceTest extends ServiceTestSupport {

    @Autowired
    TaskService taskService;

    @Test
    void createTask() {
        TaskCreateRequest request = new TaskCreateRequest("title", null, null, null);

        StepVerifier.create(taskService.create(request))
                .assertNext(saved -> {
                    assertThat(saved.id()).isNotNull();
                    assertThat(saved.title()).isEqualTo("title");
                    assertThat(saved.createdAt()).isNotNull();
                })
                .verifyComplete();
    }
}