import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

export interface Job {
  id: number;
  name: string;
  payload: string;
  cronExpression: string;
  status: 'SCHEDULED' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'PAUSED';
  maxRetries: number;
  retryCount: number;
  nextRunAt: string;
  lastRunAt: string | null;
  lastError: string | null;
  createdAt: string;
}

export interface JobExecution {
  id: number;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  workerNode: string;
  startedAt: string;
  completedAt: string | null;
  durationMs: number;
  errorMessage: string | null;
  attemptNumber: number;
}

export interface MetricsSummary {
  total: number;
  scheduled: number;
  running: number;
  failed: number;
  completed: number;
}

export const getJobs = () => api.get<Job[]>('/jobs');
export const getJob = (id: number) => api.get<Job>(`/jobs/${id}`);
export const createJob = (job: Partial<Job>) => api.post<Job>('/jobs', job);
export const pauseJob = (id: number) => api.post<Job>(`/jobs/${id}/pause`);
export const resumeJob = (id: number) => api.post<Job>(`/jobs/${id}/resume`);
export const retryJob = (id: number) => api.post<Job>(`/jobs/${id}/retry`);
export const deleteJob = (id: number) => api.delete(`/jobs/${id}`);
export const getExecutions = (id: number) => api.get<JobExecution[]>(`/jobs/${id}/executions`);
export const getMetricsSummary = () => api.get<MetricsSummary>('/metrics/summary');
