package com.scheduler.service;

import com.scheduler.model.Job;
import com.scheduler.model.JobStatus;
import com.scheduler.repository.JobExecutionRepository;
import com.scheduler.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobExecutionRepository executionRepository;

    @Mock
    private CronParser cronParser;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, executionRepository, cronParser);
    }

    @Test
    void createJobWithValidCron() {
        Job job = new Job();
        job.setName("test-job");
        job.setCronExpression("0 */5 * * * *");

        when(cronParser.isValid("0 */5 * * * *")).thenReturn(true);
        when(cronParser.nextExecution("0 */5 * * * *")).thenReturn(Instant.now().plusSeconds(300));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job created = jobService.createJob(job);

        assertEquals(JobStatus.SCHEDULED, created.getStatus());
        verify(jobRepository).save(job);
    }

    @Test
    void createJobWithInvalidCronThrows() {
        Job job = new Job();
        job.setName("bad-cron");
        job.setCronExpression("invalid");

        when(cronParser.isValid("invalid")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> jobService.createJob(job));
        verify(jobRepository, never()).save(any());
    }

    @Test
    void pauseJobSetsStatusToPaused() {
        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.SCHEDULED);

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job paused = jobService.pauseJob(1L);

        assertEquals(JobStatus.PAUSED, paused.getStatus());
    }

    @Test
    void resumeJobSetsStatusToScheduled() {
        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.PAUSED);
        job.setCronExpression("0 */5 * * * *");

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(cronParser.nextExecution("0 */5 * * * *")).thenReturn(Instant.now().plusSeconds(300));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job resumed = jobService.resumeJob(1L);

        assertEquals(JobStatus.SCHEDULED, resumed.getStatus());
        assertNotNull(resumed.getNextRunAt());
    }

    @Test
    void retryOnlyWorksForFailedJobs() {
        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.SCHEDULED);

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThrows(IllegalStateException.class, () -> jobService.retryJob(1L));
    }

    @Test
    void retryResetsCountAndSchedules() {
        Job job = new Job();
        job.setId(1L);
        job.setStatus(JobStatus.FAILED);
        job.setRetryCount(3);
        job.setCronExpression("0 */5 * * * *");

        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(cronParser.nextExecution("0 */5 * * * *")).thenReturn(Instant.now().plusSeconds(300));
        when(jobRepository.save(any(Job.class))).thenReturn(job);

        Job retried = jobService.retryJob(1L);

        assertEquals(JobStatus.SCHEDULED, retried.getStatus());
        assertEquals(0, retried.getRetryCount());
    }

    @Test
    void getJobThrowsWhenNotFound() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> jobService.getJob(99L));
    }
}
