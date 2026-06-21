import React, { useState, useEffect, useCallback } from 'react';
import { Job, JobExecution, MetricsSummary, getJobs, createJob, pauseJob, resumeJob, retryJob, deleteJob, getExecutions, getMetricsSummary } from './api';

function App() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [metrics, setMetrics] = useState<MetricsSummary | null>(null);
  const [selectedJob, setSelectedJob] = useState<number | null>(null);
  const [executions, setExecutions] = useState<JobExecution[]>([]);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [jobType, setJobType] = useState<'SIMULATION' | 'HTTP' | 'SHELL'>('SIMULATION');
  const [newJob, setNewJob] = useState({ name: '', cronExpression: '', maxRetries: 3, httpUrl: '', httpMethod: 'POST', httpBody: '', shellCommand: '' });

  const refresh = useCallback(async () => {
    const [jobsRes, metricsRes] = await Promise.all([getJobs(), getMetricsSummary()]);
    setJobs(jobsRes.data);
    setMetrics(metricsRes.data);
  }, []);

  useEffect(() => {
    refresh();
    const interval = setInterval(refresh, 5000);
    return () => clearInterval(interval);
  }, [refresh]);

  useEffect(() => {
    if (selectedJob) {
      getExecutions(selectedJob).then(res => setExecutions(res.data));
    }
  }, [selectedJob]);

  const buildPayload = (): string => {
    switch (jobType) {
      case 'HTTP':
        return JSON.stringify({ type: 'HTTP', url: newJob.httpUrl, method: newJob.httpMethod, body: newJob.httpBody });
      case 'SHELL':
        return JSON.stringify({ type: 'SHELL', command: newJob.shellCommand });
      default:
        return '';
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    await createJob({ name: newJob.name, cronExpression: newJob.cronExpression, maxRetries: newJob.maxRetries, payload: buildPayload() });
    setNewJob({ name: '', cronExpression: '', maxRetries: 3, httpUrl: '', httpMethod: 'POST', httpBody: '', shellCommand: '' });
    setJobType('SIMULATION');
    setShowCreateForm(false);
    refresh();
  };

  const statusColor = (status: string) => {
    switch (status) {
      case 'RUNNING': return '#2196f3';
      case 'COMPLETED': return '#4caf50';
      case 'FAILED': return '#f44336';
      case 'PAUSED': return '#ff9800';
      default: return '#9e9e9e';
    }
  };

  return (
    <div style={{ fontFamily: 'system-ui, sans-serif', maxWidth: 1200, margin: '0 auto', padding: 24 }}>
      <h1 style={{ marginBottom: 8 }}>Distributed Job Scheduler</h1>

      {metrics && (
        <div style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
          <MetricCard label="Total" value={metrics.total} color="#333" />
          <MetricCard label="Scheduled" value={metrics.scheduled} color="#9e9e9e" />
          <MetricCard label="Running" value={metrics.running} color="#2196f3" />
          <MetricCard label="Completed" value={metrics.completed} color="#4caf50" />
          <MetricCard label="Failed" value={metrics.failed} color="#f44336" />
        </div>
      )}

      <div style={{ marginBottom: 16 }}>
        <button onClick={() => setShowCreateForm(!showCreateForm)}
                style={{ padding: '8px 16px', background: '#1976d2', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
          + Create Job
        </button>
      </div>

      {showCreateForm && (
        <form onSubmit={handleCreate} style={{ background: '#f5f5f5', padding: 16, borderRadius: 8, marginBottom: 24 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <input placeholder="Job Name" value={newJob.name} onChange={e => setNewJob({...newJob, name: e.target.value})}
                   style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4 }} required />
            <input placeholder="Cron (e.g. 0 */5 * * * *)" value={newJob.cronExpression}
                   onChange={e => setNewJob({...newJob, cronExpression: e.target.value})}
                   style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4 }} required />
            <select value={jobType} onChange={e => setJobType(e.target.value as any)}
                    style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4 }}>
              <option value="SIMULATION">Simulation</option>
              <option value="HTTP">HTTP Webhook</option>
              <option value="SHELL">Shell Command</option>
            </select>
            <input type="number" placeholder="Max Retries" value={newJob.maxRetries}
                   onChange={e => setNewJob({...newJob, maxRetries: parseInt(e.target.value)})}
                   style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4 }} />
          </div>
          {jobType === 'HTTP' && (
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 12, marginTop: 12 }}>
              <input placeholder="URL (https://...)" value={newJob.httpUrl}
                     onChange={e => setNewJob({...newJob, httpUrl: e.target.value})}
                     style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4 }} required />
              <select value={newJob.httpMethod} onChange={e => setNewJob({...newJob, httpMethod: e.target.value})}
                      style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4 }}>
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
              </select>
              <input placeholder="Request body (optional)" value={newJob.httpBody}
                     onChange={e => setNewJob({...newJob, httpBody: e.target.value})}
                     style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4, gridColumn: '1 / -1' }} />
            </div>
          )}
          {jobType === 'SHELL' && (
            <div style={{ marginTop: 12 }}>
              <input placeholder="Shell command (e.g. curl https://...)" value={newJob.shellCommand}
                     onChange={e => setNewJob({...newJob, shellCommand: e.target.value})}
                     style={{ padding: 8, border: '1px solid #ddd', borderRadius: 4, width: '100%' }} required />
            </div>
          )}
          <button type="submit" style={{ marginTop: 12, padding: '8px 24px', background: '#4caf50', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
            Create
          </button>
        </form>
      )}

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
            <th style={{ padding: 8 }}>Name</th>
            <th style={{ padding: 8 }}>Status</th>
            <th style={{ padding: 8 }}>Cron</th>
            <th style={{ padding: 8 }}>Next Run</th>
            <th style={{ padding: 8 }}>Retries</th>
            <th style={{ padding: 8 }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {jobs.map(job => (
            <tr key={job.id} style={{ borderBottom: '1px solid #eee' }}
                onClick={() => setSelectedJob(job.id === selectedJob ? null : job.id)}>
              <td style={{ padding: 8 }}>{job.name}</td>
              <td style={{ padding: 8 }}>
                <span style={{ background: statusColor(job.status), color: '#fff', padding: '2px 8px', borderRadius: 12, fontSize: 12 }}>
                  {job.status}
                </span>
              </td>
              <td style={{ padding: 8, fontFamily: 'monospace', fontSize: 13 }}>{job.cronExpression}</td>
              <td style={{ padding: 8, fontSize: 13 }}>{job.nextRunAt ? new Date(job.nextRunAt).toLocaleString() : '-'}</td>
              <td style={{ padding: 8 }}>{job.retryCount}/{job.maxRetries}</td>
              <td style={{ padding: 8 }}>
                <div style={{ display: 'flex', gap: 4 }}>
                  {job.status === 'SCHEDULED' && (
                    <ActionBtn label="Pause" onClick={async () => { await pauseJob(job.id); refresh(); }} />
                  )}
                  {job.status === 'PAUSED' && (
                    <ActionBtn label="Resume" onClick={async () => { await resumeJob(job.id); refresh(); }} />
                  )}
                  {job.status === 'FAILED' && (
                    <ActionBtn label="Retry" onClick={async () => { await retryJob(job.id); refresh(); }} />
                  )}
                  <ActionBtn label="Delete" onClick={async () => { await deleteJob(job.id); refresh(); }} color="#f44336" />
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selectedJob && executions.length > 0 && (
        <div style={{ marginTop: 24 }}>
          <h3>Execution History (Job #{selectedJob})</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #ddd', textAlign: 'left' }}>
                <th style={{ padding: 8 }}>Attempt</th>
                <th style={{ padding: 8 }}>Status</th>
                <th style={{ padding: 8 }}>Worker</th>
                <th style={{ padding: 8 }}>Duration</th>
                <th style={{ padding: 8 }}>Started</th>
                <th style={{ padding: 8 }}>Error</th>
              </tr>
            </thead>
            <tbody>
              {executions.map(exec => (
                <tr key={exec.id} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={{ padding: 8 }}>#{exec.attemptNumber}</td>
                  <td style={{ padding: 8 }}>
                    <span style={{ background: statusColor(exec.status), color: '#fff', padding: '2px 8px', borderRadius: 12, fontSize: 12 }}>
                      {exec.status}
                    </span>
                  </td>
                  <td style={{ padding: 8, fontFamily: 'monospace' }}>{exec.workerNode}</td>
                  <td style={{ padding: 8 }}>{exec.durationMs}ms</td>
                  <td style={{ padding: 8, fontSize: 13 }}>{new Date(exec.startedAt).toLocaleString()}</td>
                  <td style={{ padding: 8, fontSize: 12, color: '#f44336' }}>{exec.errorMessage || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function MetricCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div style={{ background: '#fff', border: '1px solid #e0e0e0', borderRadius: 8, padding: '12px 24px', textAlign: 'center' }}>
      <div style={{ fontSize: 28, fontWeight: 'bold', color }}>{value}</div>
      <div style={{ fontSize: 12, color: '#666' }}>{label}</div>
    </div>
  );
}

function ActionBtn({ label, onClick, color = '#1976d2' }: { label: string; onClick: () => void; color?: string }) {
  return (
    <button onClick={(e) => { e.stopPropagation(); onClick(); }}
            style={{ padding: '4px 8px', background: 'none', border: `1px solid ${color}`, color, borderRadius: 4, cursor: 'pointer', fontSize: 12 }}>
      {label}
    </button>
  );
}

export default App;
