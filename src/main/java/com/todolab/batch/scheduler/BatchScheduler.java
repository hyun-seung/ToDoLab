package com.todolab.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobOperator jobOperator;
    private final Job dailyScheduleMailJob;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void runDailyScheduleMailJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("baseDate", LocalDate.now(ZoneId.of("Asia/Seoul")).toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobOperator.start(dailyScheduleMailJob, jobParameters);
        } catch (Exception e) {
            log.error("[BATCH] dailyScheduleMailJob 실행 실패", e);
        }
    }
}
