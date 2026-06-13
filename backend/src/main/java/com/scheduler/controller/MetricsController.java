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
@CrossOrigin(origins = "http://localhost:3000")
public class MetricsController {

    private final JobRepository jobRepository;

    public MetricsController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        long total = jobRepository.count();
        long scheduled = jobRepository.findByStatus(JobStatus.SCHEDULED).size();
        long running = jobRepository.findByStatus(JobStatus.RUNNING).size();
        long failed = jobRepository.findByStatus(JobStatus.FAILED).size();
        long completed = jobRepository.findByStatus(JobStatus.COMPLETED).size();

        return Map.of(
                "total", total,
                "scheduled", scheduled,
                "running", running,
                "failed", failed,
                "completed", completed
        );
    }
}
