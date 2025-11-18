package com.todolab.task.domain;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Table("task")
@Getter
public class Task {

    @Id
    private Long id;

    private String title;
    private String description;
    private LocalDate taskDate;
    private LocalTime taskTime;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder
    public Task(String title, String description, LocalDate taskDate, LocalTime taskTime) {
        this.title = title;
        this.description = description;
        this.taskDate = taskDate;
        this.taskTime = taskTime;
    }
}
