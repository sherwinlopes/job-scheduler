package com.scheduler.service;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CronParserTest {

    private final CronParser cronParser = new CronParser();

    @Test
    void validCronExpression() {
        assertTrue(cronParser.isValid("0 */5 * * * *"));
    }

    @Test
    void invalidCronExpression() {
        assertFalse(cronParser.isValid("not a cron"));
    }

    @Test
    void emptyExpressionIsInvalid() {
        assertFalse(cronParser.isValid(""));
    }

    @Test
    void nextExecutionReturnsInstantInTheFuture() {
        Instant next = cronParser.nextExecution("0 */5 * * * *");
        assertNotNull(next);
        assertTrue(next.isAfter(Instant.now()));
    }

    @Test
    void everySecondCronIsValid() {
        assertTrue(cronParser.isValid("* * * * * *"));
    }

    @Test
    void fiveFieldCronIsInvalid() {
        // Spring CronExpression requires 6 fields
        assertFalse(cronParser.isValid("*/5 * * * *"));
    }
}
