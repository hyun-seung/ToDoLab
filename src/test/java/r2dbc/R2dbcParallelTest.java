package r2dbc;

import com.todolab.ToDoALabApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ToDoALabApplication.class
)
public class R2dbcParallelTest {

    @Autowired
    DatabaseClient databaseClient;

    @Test
    void r2dbc_DB_지연_병렬처리_테스트() {
        int totalRequests = 20;
        int sleepSeconds = 2;

        Mono<Void> work = Flux.range(1, totalRequests)
                .flatMap(i ->
                                databaseClient.sql("SELECT SLEEP(:sec)")
                                        .bind("sec", sleepSeconds)
                                        .fetch()
                                        .rowsUpdated()
                                        .doFirst(() ->
                                                System.out.println(now() + " [SLEEP START] req=" + i +
                                                        " thread=" + Thread.currentThread().getName())
                                        )
                                        .doOnSuccess(v ->
                                                System.out.println(now() + " [SLEEP END]   req=" + i +
                                                        " thread=" + Thread.currentThread().getName())
                                        ),
                        totalRequests   // 병렬성 힌트
                )
                .then();

        long start = System.currentTimeMillis();

        StepVerifier.create(work.then())
                .verifyComplete();

        long total = System.currentTimeMillis() - start;
        System.out.println("총 실행 시간 = " + total + " ms");
    }

    private String now() {
        return java.time.LocalTime.now().toString();
    }
}
