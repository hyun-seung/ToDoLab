package com.todolab.support;

import com.todolab.task.service.TaskService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestMockConfig {

    @Bean
    public TaskService taskService() {
        return Mockito.mock(TaskService.class);
    }
}
