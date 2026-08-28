import React from 'react';
import { ShieldCheck, Activity, BarChart3, Database } from 'lucide-react';

export const AdminGovernance: React.FC = () => {
  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-DEFAULT">AI Governance & Operations</h1>
        <p className="text-sm text-muted mt-1">Audit model invocations, retrieval accuracy, SLA risk rules, and event pipeline metrics.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Recall@5 Target</span>
            <BarChart3 className="w-4 h-4 text-primary" />
          </div>
          <p className="text-2xl font-bold text-DEFAULT">84.2%</p>
          <p className="text-[11px] text-success font-medium">Target: ≥80%</p>
        </div>

        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Mean Recip. Rank</span>
            <Activity className="w-4 h-4 text-primary" />
          </div>
          <p className="text-2xl font-bold text-DEFAULT">76.8%</p>
          <p className="text-[11px] text-success font-medium">Target: ≥70%</p>
        </div>

        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Auto-Send Rate</span>
            <ShieldCheck className="w-4 h-4 text-success" />
          </div>
          <p className="text-2xl font-bold text-success">0.0%</p>
          <p className="text-[11px] text-muted font-medium">Zero auto-sends enforced</p>
        </div>

        <div className="bg-surface border border-border rounded-card p-4 space-y-1">
          <div className="flex items-center justify-between text-muted text-xs">
            <span>Event Lag (Kafka)</span>
            <Database className="w-4 h-4 text-primary" />
          </div>
          <p className="text-2xl font-bold text-DEFAULT">0 msgs</p>
          <p className="text-[11px] text-success font-medium">Transactional outbox healthy</p>
        </div>
      </div>

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
                <th className="py-2.5">Outcome</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border-subtle text-DEFAULT">
              <tr>
                <td className="py-2.5 font-mono">RIQ-2026-000412</td>
                <td>mock-chat-v1</td>
                <td className="font-mono">triage-agent-v1</td>
                <td>342</td>
                <td>420ms</td>
                <td><span className="text-success font-semibold">VALID_GROUNDED</span></td>
              </tr>
              <tr>
                <td className="py-2.5 font-mono">RIQ-2026-000411</td>
                <td>mock-chat-v1</td>
                <td className="font-mono">triage-agent-v1</td>
                <td>290</td>
                <td>380ms</td>
                <td><span className="text-success font-semibold">VALID_GROUNDED</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
