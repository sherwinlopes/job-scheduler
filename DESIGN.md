# Distributed Job Scheduler - Design Notes

## Requirements (from initial conversation)

- Users can create jobs, schedule jobs, retry failed jobs
- Monitor execution
- Scale workers independently
- Free/open-source stack only

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Frontend | React 18, TypeScript, Axios |
| Containerization | Docker, Docker Compose |
| Monitoring | Prometheus + Grafana |

## Architecture

```
React UI  -->  Spring Boot API  -->  PostgreSQL
                    |    |
             Workers     Prometheus --> Grafana
            (scalable)
```

## Key Design Decisions

| Feature | Approach |
|---------|----------|
| Distributed locking | PostgreSQL row-level UPDATE...WHERE (no Redis needed) |
| Retry with backoff | Exponential: 30s * retryCount |
| Scale workers | `docker compose up --scale worker=N` |
| Monitoring | Spring Actuator -> Prometheus -> Grafana (pre-configured) |
| Job execution | HTTP webhook, shell command, or simulation (auto-resolved from payload) |
| Scheduling | Spring CronExpression parser, stored per job |
| Stale lock recovery | 5-minute threshold, workers reclaim stale locks |

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/jobs | Create a new job |
| GET | /api/jobs | List all jobs |
| GET | /api/jobs/{id} | Get job details |
| POST | /api/jobs/{id}/pause | Pause a scheduled job |
| POST | /api/jobs/{id}/resume | Resume a paused job |
| POST | /api/jobs/{id}/retry | Retry a failed job |
| DELETE | /api/jobs/{id} | Delete a job |
| GET | /api/jobs/{id}/executions | Get execution history |
| GET | /api/metrics/summary | Get job status counts |

## How to Run

```bash
cd ~/personal-projects/job-scheduler
docker compose up --build
```

- Frontend: http://localhost:3000
- API: http://localhost:8080/api/jobs
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)

Scale workers: `docker compose up --scale worker=5`

## Future Enhancements

- Authentication/authorization
- Job dependencies (DAG workflows)
- Dead letter queue for permanently failed jobs
- WebSocket for real-time UI updates
