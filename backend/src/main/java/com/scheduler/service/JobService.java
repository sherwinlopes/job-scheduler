package com.scheduler.service;

import com.scheduler.model.Job;
import com.scheduler.model.JobExecution;
import com.scheduler.model.JobStatus;
import com.scheduler.repository.JobExecutionRepository;
import com.scheduler.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final CronParser cronParser;

    public JobService(JobRepository jobRepository,
                      JobExecutionRepository executionRepository,
                      CronParser cronParser) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.cronParser = cronParser;
    }

    @Transactional
    public Job createJob(Job job) {
        if (!cronParser.isValid(job.getCronExpression())) {
            throw new IllegalArgumentException("Invalid cron expression: " + job.getCronExpression());
        }
        job.setNextRunAt(cronParser.nextExecution(job.getCronExpression()));
        job.setStatus(JobStatus.SCHEDULED);
        return jobRepository.save(job);
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Job getJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    @Transactional
    public Job pauseJob(Long id) {
        Job job = getJob(id);
        job.setStatus(JobStatus.PAUSED);
        return jobRepository.save(job);
    }

    @Transactional
    public Job resumeJob(Long id) {
        Job job = getJob(id);
        job.setStatus(JobStatus.SCHEDULED);
        job.setNextRunAt(cronParser.nextExecution(job.getCronExpression()));
        return jobRepository.save(job);
    }

    @Transactional
    public Job retryJob(Long id) {
        Job job = getJob(id);
        if (job.getStatus() != JobStatus.FAILED) {
            throw new IllegalStateException("Can only retry failed jobs");
        }
        job.setStatus(JobStatus.SCHEDULED);
        job.setRetryCount(0);
        job.setNextRunAt(cronParser.nextExecution(job.getCronExpression()));
        return jobRepository.save(job);
    }

    @Transactional
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }

    public List<JobExecution> getExecutions(Long jobId) {
        return executionRepository.findByJobIdOrderByStartedAtDesc(jobId);
    }
}
