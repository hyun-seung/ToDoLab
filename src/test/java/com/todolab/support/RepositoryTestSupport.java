package com.todolab.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@Transactional
public class RepositoryTestSupport {

    @PersistenceContext
    protected EntityManager em;

    @BeforeEach
    void clearBeforeEach() {
        // 매 테스트 시작마다 영속성 컨텍스트 초기화
        em.clear();
    }

    protected void flushAndClear() {
        em.flush();
        em.clear();
    }

}
