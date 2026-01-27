package com.todolab.task.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todolab.task.domain.QTask;
import com.todolab.task.domain.Task;
import jakarta.persistence.EntityManager;

import java.util.List;

public class TaskRepositoryImpl implements TaskRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public TaskRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Task> findUnscheduledTask() {
        QTask t = QTask.task;

        return queryFactory
                .selectFrom(t)
                .where(
                        t.startAt.isNull(),
                        t.endAt.isNull()
                )
                .orderBy(t.id.asc())
                .fetch();
    }
}
