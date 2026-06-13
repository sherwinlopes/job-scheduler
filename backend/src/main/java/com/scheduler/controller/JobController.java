package com.scheduler.controller;

import com.scheduler.model.Job;
import com.scheduler.model.JobExecution;
import com.scheduler.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job createJob(@Valid @RequestBody Job job) {
        return jobService.createJob(job);
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public Job getJob(@PathVariable Long id) {
        return jobService.getJob(id);
    }

    @PostMapping("/{id}/pause")
    public Job pauseJob(@PathVariable Long id) {
        return jobService.pauseJob(id);
    }

    @PostMapping("/{id}/resume")
    public Job resumeJob(@PathVariable Long id) {
        return jobService.resumeJob(id);
    }

    @PostMapping("/{id}/retry")
    public Job retryJob(@PathVariable Long id) {
        return jobService.retryJob(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
    }

    @GetMapping("/{id}/executions")
    public List<JobExecution> getExecutions(@PathVariable Long id) {
        return jobService.getExecutions(id);
    }
}
