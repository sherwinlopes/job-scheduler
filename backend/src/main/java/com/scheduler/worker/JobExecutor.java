package com.scheduler.worker;

import com.scheduler.model.Job;

public interface JobExecutor {
    String execute(Job job) throws Exception;
}
