package com.scheduler.worker;

import com.scheduler.model.Job;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobExecutorResolverTest {

    @Test
    void simulationJobCompletes() throws Exception {
        SimulationJobExecutor executor = new SimulationJobExecutor();
        Job job = new Job();
        job.setName("sim-test");

        // Run multiple times — at most 10% fail so most should pass
        int successes = 0;
        for (int i = 0; i < 20; i++) {
            try {
                executor.execute(job);
                successes++;
            } catch (RuntimeException ignored) {
            }
        }
        assertTrue(successes > 10, "Expected most simulation runs to succeed");
    }

    @Test
    void shellExecutorRunsEcho() throws Exception {
        ShellJobExecutor executor = new ShellJobExecutor();
        Job job = new Job();
        job.setName("shell-test");
        job.setPayload("{\"type\": \"SHELL\", \"command\": \"echo hello\"}");

        String output = executor.execute(job);
        assertEquals("hello", output.trim());
    }

    @Test
    void shellExecutorFailsOnBadExitCode() {
        ShellJobExecutor executor = new ShellJobExecutor();
        Job job = new Job();
        job.setName("fail-test");
        job.setPayload("{\"type\": \"SHELL\", \"command\": \"exit 1\"}");

        assertThrows(RuntimeException.class, () -> executor.execute(job));
    }
}
