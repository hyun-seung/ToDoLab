package com.todolab.task.repository;

import com.todolab.task.domain.Task;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface TaskRepository extends ReactiveCrudRepository<Task, Long> {

    @Query("""
        SELECT * FROM task
        WHERE date >= :start
          AND date <= :end
    """)
    Flux<Task> findByDateRange(LocalDate start, LocalDate end);
}
