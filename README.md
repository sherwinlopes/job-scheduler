# Distributed Job Scheduler

A scalable distributed job scheduler built with Java 21, Spring Boot, PostgreSQL, React, and Docker.

## Features

- **Cron scheduling** — standard 6-field cron expressions
- **Distributed workers** — PostgreSQL row-level locking, no Redis required
- **Job types** — HTTP webhook, shell command, or no-op (simulation)
- **Retry with backoff** — exponential: 30s * retryCount, configurable max retries
- **Stale lock recovery** — workers reclaim jobs locked for >5 minutes
- **Live dashboard** — React UI with status metrics, execution history
- **Monitoring** — Prometheus + Grafana with pre-provisioned dashboards
- **Horizontally scalable** — spin up N workers with `docker compose up --scale worker=N`

## Architecture

```
React UI  →  Spring Boot API  →  PostgreSQL
                   ↕                    ↑
              Workers (N)  ─────────────┘
                   ↓
           Prometheus → Grafana
```

## Quick Start

```bash
docker compose up --build
```

| Service    | URL                          |
|------------|------------------------------|
| Frontend   | http://localhost:3000         |
| API        | http://localhost:8080/api/jobs |
| Prometheus | http://localhost:9090         |
| Grafana    | http://localhost:3001 (admin/admin) |

Scale workers:
```bash
docker compose up --scale worker=5
```

## Job Types

When creating a job, set the `payload` field as JSON to control execution:

### HTTP Webhook
```json
{
  "type": "HTTP",
  "url": "https://example.com/webhook",
  "method": "POST",
  "headers": {"Authorization": "Bearer token123"},
  "body": "{\"event\": \"scheduled\"}"
}
```

### Shell Command
```json
{
  "type": "SHELL",
  "command": "echo 'Hello from job scheduler'"
}
```

### Simulation (default)
If `type` is missing or unrecognized, the job runs a 1-3s simulated execution with a 10% random failure rate — useful for testing.

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/jobs | Create a job |
| GET | /api/jobs | List all jobs |
| GET | /api/jobs/{id} | Get job details |
| POST | /api/jobs/{id}/pause | Pause a scheduled job |
| POST | /api/jobs/{id}/resume | Resume a paused job |
| POST | /api/jobs/{id}/retry | Retry a failed job |
| DELETE | /api/jobs/{id} | Delete a job |
| GET | /api/jobs/{id}/executions | Execution history |
| GET | /api/metrics/summary | Status counts |

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2, Spring Data JPA
- **Database**: PostgreSQL 16
- **Frontend**: React 18, TypeScript, Axios
- **Monitoring**: Prometheus, Grafana
- **Containerization**: Docker, Docker Compose

## Development

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local dev without Docker)
- Node.js 18+ (for frontend dev)

### Run backend locally
```bash
cd backend
# Linux/Mac
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

### Run tests
```bash
cd backend
# Linux/Mac
./mvnw test
# Windows
mvnw.cmd test
```

### Run frontend locally
```bash
cd frontend
npm install
npm start
```

## License

MIT
