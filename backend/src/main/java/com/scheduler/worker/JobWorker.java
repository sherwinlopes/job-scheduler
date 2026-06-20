package com.scheduler.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.*;
import com.scheduler.repository.JobExecutionRepository;
import com.scheduler.repository.JobRepository;
import com.scheduler.service.CronParser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final JobExecutionRepository executionRepository;
    private final CronParser cronParser;
    private final String workerId;
    private final Counter jobsExecuted;
    private final Counter jobsFailed;
    private final Timer executionTimer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpJobExecutor httpExecutor = new HttpJobExecutor();
    private final ShellJobExecutor shellExecutor = new ShellJobExecutor();
    private final SimulationJobExecutor simulationExecutor = new SimulationJobExecutor();

    public JobWorker(JobRepository jobRepository,
                     JobExecutionRepository executionRepository,
                     CronParser cronParser,
                     MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.cronParser = cronParser;
        this.workerId = UUID.randomUUID().toString().substring(0, 8);
        this.jobsExecuted = Counter.builder("scheduler.jobs.executed").register(meterRegistry);
        this.jobsFailed = Counter.builder("scheduler.jobs.failed").register(meterRegistry);
        this.executionTimer = Timer.builder("scheduler.jobs.duration").register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${scheduler.poll-interval-ms:5000}")
    public void pollAndExecute() {
        Instant now = Instant.now();
        Instant staleThreshold = now.minus(LOCK_STALENESS);

        List<Job> readyJobs = jobRepository.findReadyJobs(now, staleThreshold, 10);

        for (Job job : readyJobs) {
            int locked = jobRepository.tryLockJob(job.getId(), workerId, now, staleThreshold);
            if (locked > 0) {
                executeJob(job);
            }
        }
    }

    @Transactional
    protected void executeJob(Job job) {
        log.info("Worker {} executing job {}: {}", workerId, job.getId(), job.getName());
        Instant start = Instant.now();

        JobExecution execution = new JobExecution();
        execution.setJob(job);
        execution.setWorkerNode(workerId);
        execution.setStartedAt(start);
        execution.setAttemptNumber(job.getRetryCount() + 1);
        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);

        try {
            JobExecutor executor = resolveExecutor(job);
            executor.execute(job);

            Instant end = Instant.now();
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setCompletedAt(end);
            execution.setDurationMs(Duration.between(start, end).toMillis());

            job.setStatus(JobStatus.COMPLETED);
            job.setLastRunAt(end);
            job.setRetryCount(0);
            job.setLockedBy(null);
            job.setLockedAt(null);
            job.setNextRunAt(cronParser.nextExecution(job.getCronExpression()));
            job.setStatus(JobStatus.SCHEDULED);

            jobsExecuted.increment();
            executionTimer.record(Duration.between(start, end));
        } catch (Exception e) {
            Instant end = Instant.now();
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(end);
            execution.setDurationMs(Duration.between(start, end).toMillis());
            execution.setErrorMessage(e.getMessage());

            job.setRetryCount(job.getRetryCount() + 1);
            job.setLastError(e.getMessage());
            job.setLockedBy(null);
            job.setLockedAt(null);

            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus(JobStatus.FAILED);
                log.error("Job {} failed after {} retries", job.getId(), job.getMaxRetries());
            } else {
                job.setStatus(JobStatus.SCHEDULED);
                job.setNextRunAt(Instant.now().plusSeconds(30L * job.getRetryCount()));
                log.warn("Job {} failed, retry {}/{}", job.getId(), job.getRetryCount(), job.getMaxRetries());
            }

            jobsFailed.increment();
        }

        executionRepository.save(execution);
        jobRepository.save(job);
    }

    private JobExecutor resolveExecutor(Job job) {
        String payload = job.getPayload();
        if (payload == null || payload.isBlank()) {
            return simulationExecutor;
        }
        try {
            JsonNode config = objectMapper.readTree(payload);
            String type = config.has("type") ? config.get("type").asText() : "";
            return switch (type.toUpperCase()) {
                case "HTTP" -> httpExecutor;
                case "SHELL" -> shellExecutor;
                default -> simulationExecutor;
            };
        } catch (Exception e) {
            return simulationExecutor;
        }
    }
}
