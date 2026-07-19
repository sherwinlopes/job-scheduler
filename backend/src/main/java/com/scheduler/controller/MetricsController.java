package com.scheduler.controller;

import com.scheduler.model.JobStatus;
import com.scheduler.repository.JobRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "${scheduler.cors.allowed-origins:http://localhost:3000}")
public class MetricsController {

    private final JobRepository jobRepository;

    public MetricsController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        long total = jobRepository.count();
        long scheduled = jobRepository.countByStatus(JobStatus.SCHEDULED);
        long running = jobRepository.countByStatus(JobStatus.RUNNING);
        long failed = jobRepository.countByStatus(JobStatus.FAILED);
        long completed = jobRepository.countByStatus(JobStatus.COMPLETED);

        return Map.of(
                "total", total,
                "scheduled", scheduled,
                "running", running,
                "failed", failed,
                "completed", completed
        );
    }
}
