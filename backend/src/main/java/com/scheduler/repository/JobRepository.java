package com.scheduler.repository;

import com.scheduler.model.Job;
import com.scheduler.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);

    @Modifying
    @Query("""
        UPDATE Job j SET j.status = 'RUNNING', j.lockedBy = :workerId, j.lockedAt = :now
        WHERE j.id = :jobId AND j.status = 'SCHEDULED' AND j.nextRunAt <= :now
        AND (j.lockedBy IS NULL OR j.lockedAt < :staleThreshold)
    """)
    int tryLockJob(@Param("jobId") Long jobId,
                   @Param("workerId") String workerId,
                   @Param("now") Instant now,
                   @Param("staleThreshold") Instant staleThreshold);

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'SCHEDULED' AND j.nextRunAt <= :now
        AND (j.lockedBy IS NULL OR j.lockedAt < :staleThreshold)
        ORDER BY j.nextRunAt ASC
        LIMIT :limit
    """)
    List<Job> findReadyJobs(@Param("now") Instant now,
                            @Param("staleThreshold") Instant staleThreshold,
                            @Param("limit") int limit);
}
