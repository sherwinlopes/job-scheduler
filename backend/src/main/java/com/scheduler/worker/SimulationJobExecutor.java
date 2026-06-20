package com.scheduler.worker;

import com.scheduler.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(SimulationJobExecutor.class);

    @Override
    public String execute(Job job) throws Exception {
        Thread.sleep(1000 + (long) (Math.random() * 2000));
        if (Math.random() < 0.1) {
            throw new RuntimeException("Simulated failure for job: " + job.getName());
        }
        log.info("Simulation job completed: {}", job.getName());
        return "OK";
    }
}
