package com.todolab.task.repository;

import com.todolab.task.domain.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, TaskRepositoryCustom {

//    @Query("""
//        SELECT t
//          FROM Task t
//         WHERE t.startAt >= :start
//           AND t.startAt < :end
//    """)
    @Query("""
        SELECT t
          FROM Task t
         WHERE t.startAt IS NOT NULL
           AND (
                (t.endAt IS NULL AND t.startAt >= :start AND t.startAt < :end)
             OR (t.endAt IS NOT NULL AND t.startAt < :end AND t.endAt > :start)
           )
         ORDER BY t.startAt ASC, t.id ASC
    """)
    List<Task> findByDateRange(LocalDateTime start, LocalDateTime end);
}
