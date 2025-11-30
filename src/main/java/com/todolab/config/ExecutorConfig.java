package com.todolab.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class ExecutorConfig {

    /**
     * JPA와 같은 blocking I/O 작업을 처리하기 위한 전용 Scheduler.
     * WebFlux의 event-loop를 보호하기 위해 반드시 별도 스레드로 분리해야 한다.
     */
    @Bean
    public Scheduler jpaScheduler() {
        return Schedulers.newBoundedElastic(
                50,               // max threads
                Integer.MAX_VALUE,
                "jpa-thread"
        );
    }
}
