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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class JobExecutionService {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionService.class);

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final CronParser cronParser;
    private final Counter jobsExecuted;
    private final Counter jobsFailed;
    private final Timer executionTimer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpJobExecutor httpExecutor = new HttpJobExecutor();
    private final ShellJobExecutor shellExecutor = new ShellJobExecutor();
    private final SimulationJobExecutor simulationExecutor = new SimulationJobExecutor();

    public JobExecutionService(JobRepository jobRepository,
                               JobExecutionRepository executionRepository,
                               CronParser cronParser,
                               MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.cronParser = cronParser;
        this.jobsExecuted = Counter.builder("scheduler.jobs.executed").register(meterRegistry);
        this.jobsFailed = Counter.builder("scheduler.jobs.failed").register(meterRegistry);
        this.executionTimer = Timer.builder("scheduler.jobs.duration").register(meterRegistry);
    }

    @Transactional
    public void executeJob(Job job, String workerId) {
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

            job.setStatus(JobStatus.SCHEDULED);
            job.setLastRunAt(end);
            job.setRetryCount(0);
            job.setLockedBy(null);
            job.setLockedAt(null);
            job.setNextRunAt(cronParser.nextExecution(job.getCronExpression()));

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
