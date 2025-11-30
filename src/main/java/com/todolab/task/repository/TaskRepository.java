package com.todolab.task.repository;

import com.todolab.task.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("""
        SELECT t
          FROM Task t
         WHERE t.taskDate >= :start
           AND t.taskDate <= :end
    """)
    List<Task> findByDateRange(LocalDate start, LocalDate end);
}
