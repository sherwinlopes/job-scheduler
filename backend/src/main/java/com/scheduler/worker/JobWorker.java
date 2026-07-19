package com.scheduler.worker;

import com.scheduler.model.Job;
import com.scheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "scheduler.worker.enabled", havingValue = "true", matchIfMissing = true)
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final Duration LOCK_STALENESS = Duration.ofMinutes(5);

    private final JobRepository jobRepository;
    private final JobExecutionService executionService;
    private final String workerId;

    public JobWorker(JobRepository jobRepository, JobExecutionService executionService) {
        this.jobRepository = jobRepository;
        this.executionService = executionService;
        this.workerId = UUID.randomUUID().toString().substring(0, 8);
    }

    @Scheduled(fixedDelayString = "${scheduler.poll-interval-ms:5000}")
    public void pollAndExecute() {
        Instant now = Instant.now();
        Instant staleThreshold = now.minus(LOCK_STALENESS);

        List<Job> readyJobs = jobRepository.findReadyJobs(now, staleThreshold, 10);

        for (Job job : readyJobs) {
            int locked = jobRepository.tryLockJob(job.getId(), workerId, now, staleThreshold);
            if (locked > 0) {
                executionService.executeJob(job, workerId);
            }
        }
    }
}
