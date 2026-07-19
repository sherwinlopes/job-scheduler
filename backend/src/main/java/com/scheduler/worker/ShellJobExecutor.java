package com.scheduler.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scheduler.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Executes shell commands from job payloads.
 *
 * <p><strong>SECURITY WARNING:</strong> This executor passes user-provided command strings
 * directly to the system shell ({@code /bin/sh -c} on Unix, {@code cmd.exe /c} on Windows).
 * Any user who can create a job can execute arbitrary commands on the host.
 *
 * <p>In production, consider:
 * <ul>
 *   <li>Running workers in isolated containers with minimal privileges</li>
 *   <li>Maintaining an allowlist of permitted commands</li>
 *   <li>Restricting job creation to authenticated/authorized users</li>
 *   <li>Disabling this executor entirely if not needed</li>
 * </ul>
 */
public class ShellJobExecutor implements JobExecutor {

    private static final Logger log = LoggerFactory.getLogger(ShellJobExecutor.class);
    private static final long TIMEOUT_SECONDS = 60;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String execute(Job job) throws Exception {
        JsonNode config = objectMapper.readTree(job.getPayload());
        String command = config.get("command").asText();

        long timeout = config.has("timeoutSeconds")
                ? config.get("timeoutSeconds").asLong()
                : TIMEOUT_SECONDS;

        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        ProcessBuilder processBuilder = isWindows
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("/bin/sh", "-c", command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Command timed out after " + timeout + "s: " + command);
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Command exited with code " + exitCode + ": " + output);
        }

        log.info("Shell job completed: '{}' -> exit 0", command);
        return output;
    }
}
