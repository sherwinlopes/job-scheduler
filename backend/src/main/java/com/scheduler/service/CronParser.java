package com.scheduler.service;

import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class CronParser {

    public Instant nextExecution(String cronExpression) {
        CronExpression cron = CronExpression.parse(cronExpression);
        LocalDateTime next = cron.next(LocalDateTime.now(ZoneOffset.UTC));
        if (next == null) {
            return null;
        }
        return next.toInstant(ZoneOffset.UTC);
    }

    public boolean isValid(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
