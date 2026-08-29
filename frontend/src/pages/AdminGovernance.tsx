import React, { useState } from 'react';
import { ShieldCheck, Activity, BarChart3, Database, RefreshCw, AlertTriangle, CheckCircle } from 'lucide-react';
import { api } from '../api/client';

export const AdminGovernance: React.FC = () => {
  const [retryingId, setRetryingId] = useState<string | null>(null);
  const [replayedWorkflows, setReplayedWorkflows] = useState<Record<string, boolean>>({});
  const [replayMessage, setReplayMessage] = useState<string | null>(null);

  const handleRetryWorkflow = async (workflowId: string) => {
    setRetryingId(workflowId);
    setReplayMessage(null);
    try {
      await api.retryWorkflow(workflowId, "Operator manual retry via Governance Console");
      setReplayedWorkflows(prev => ({ ...prev, [workflowId]: true }));
      setReplayMessage(`Workflow ${workflowId.substring(0, 8)} successfully requeued for execution.`);
    } catch (failure) {
      setReplayMessage(failure instanceof Error ? failure.message : 'Workflow retry failed');
    } finally {
      setRetryingId(null);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-DEFAULT">AI Governance & Operations</h1>
        <p className="text-sm text-muted mt-1">Audit model invocations, retrieval accuracy, SLA risk rules, and event pipeline metrics.</p>
      </div>
      <div className="p-3 border border-warning/30 bg-warning/10 text-warning rounded-card text-xs">Operational metric cards below are demonstration targets until the live metrics endpoint is connected.</div>

      {replayMessage && (
        <div className="p-3 bg-success/10 border border-success/20 rounded-card text-success text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-4 h-4" />
            <span>{replayMessage}</span>
          </div>
          <button onClick={() => setReplayMessage(null)} className="text-success font-bold hover:underline">
            Dismiss
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Recall@5 (Measured)</span>
            <BarChart3 className="w-4 h-4 text-primary" />
          </div>
          <p className="text-2xl font-bold text-DEFAULT">97.0%</p>
          <p className="text-[11px] text-success font-medium">Target: ≥85% (Passed)</p>
        </div>

        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Mean Reciprocal Rank</span>
            <Activity className="w-4 h-4 text-primary" />
          </div>
          <p className="text-2xl font-bold text-DEFAULT">0.814</p>
          <p className="text-[11px] text-success font-medium">Target: ≥0.75 (Passed)</p>
        </div>

        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Auto-Send Rate</span>
            <ShieldCheck className="w-4 h-4 text-success" />
          </div>
          <p className="text-2xl font-bold text-success">0.0%</p>
          <p className="text-[11px] text-muted font-medium">Strict human approval enforced</p>
        </div>

        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Kafka Outbox Lag</span>
            <Database className="w-4 h-4 text-primary" />
          </div>
          <p className="text-2xl font-bold text-DEFAULT">0 msgs</p>
          <p className="text-[11px] text-success font-medium">Transactional outbox healthy</p>
        </div>
      </div>

      {/* Dead-Letter Queue & Workflow Operations */}
      <div className="bg-surface border border-border rounded-card p-5 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-4 h-4 text-warning" />
            <h2 className="text-sm font-semibold text-DEFAULT">Dead-Letter Queue (DLQ) & Failed Workflows</h2>
          </div>
          <span className="text-xs text-muted">Audited Operator Replay Enabled</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-border text-muted font-semibold">
                <th className="py-2.5">Workflow ID</th>
                <th className="py-2.5">Ticket</th>
                <th className="py-2.5">Failed Step</th>
                <th className="py-2.5">Error Class</th>
                <th className="py-2.5">Status</th>
                <th className="py-2.5 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-subtle">
              <tr>
                <td className="py-3 font-mono text-DEFAULT">wf-8a9b2c1d</td>
                <td className="font-mono text-DEFAULT">RIQ-2026-000410</td>
                <td>STEP_RAG</td>
                <td className="text-danger font-mono text-[11px]">PostgreSqlTimeoutException (408)</td>
                <td>
                  {replayedWorkflows['wf-8a9b2c1d'] ? (
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-primary/10 text-primary border border-primary/20">
                      RETRY_QUEUED
                    </span>
                  ) : (
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-danger/10 text-danger border border-danger/20">
                      DEAD_LETTER
                    </span>
                  )}
                </td>
                <td className="text-right">
                  <button
                    disabled={retryingId === 'wf-8a9b2c1d' || replayedWorkflows['wf-8a9b2c1d']}
                    onClick={() => handleRetryWorkflow('wf-8a9b2c1d')}
                    className="inline-flex items-center space-x-1 px-3 py-1 bg-surface border border-border text-DEFAULT hover:bg-surface-muted rounded-btn text-xs font-semibold disabled:opacity-50"
                  >
                    <RefreshCw className={`w-3 h-3 ${retryingId === 'wf-8a9b2c1d' ? 'animate-spin' : ''}`} />
                    <span>{replayedWorkflows['wf-8a9b2c1d'] ? 'Requeued' : 'Retry Workflow'}</span>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Model Invocations Table */}
      <div className="bg-surface border border-border rounded-card p-5 space-y-4">
        <h2 className="text-sm font-semibold text-DEFAULT">Recent Model Invocations & Trace Audit</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-border text-muted font-semibold">
                <th className="py-2.5">Ticket</th>
                <th className="py-2.5">Model</th>
                <th className="py-2.5">Prompt Version</th>
                <th className="py-2.5">Tokens</th>
                <th className="py-2.5">Latency</th>
                <th className="py-2.5">Validation</th>
                <th className="py-2.5">Confidence</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-subtle">
              <tr>
                <td className="py-3 font-mono text-DEFAULT">RIQ-2026-000412</td>
                <td className="text-muted">demo-provider</td>
                <td className="font-mono text-muted">triage-v1.0</td>
                <td>342</td>
                <td>124ms</td>
                <td>
                  <span className="inline-flex items-center space-x-1 text-success">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>VALID</span>
                  </span>
                </td>
                <td className="font-mono font-semibold text-DEFAULT">94.0%</td>
              </tr>
              <tr>
                <td className="py-3 font-mono text-DEFAULT">RIQ-2026-000413</td>
                <td className="text-muted">demo-provider</td>
                <td className="font-mono text-muted">triage-v1.0</td>
                <td>289</td>
                <td>110ms</td>
                <td>
                  <span className="inline-flex items-center space-x-1 text-success">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    <span>VALID</span>
                  </span>
                </td>
                <td className="font-mono font-semibold text-DEFAULT">91.5%</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
