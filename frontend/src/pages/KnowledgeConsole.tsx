import React from 'react';
import { Search, Plus, CheckCircle2 } from 'lucide-react';

export const KnowledgeConsole: React.FC = () => {
  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-DEFAULT">Knowledge & RAG Console</h1>
          <p className="text-sm text-muted mt-1">Manage approved knowledge articles, view chunking, and inspect vector indexes.</p>
        </div>
        <button className="inline-flex items-center space-x-2 px-4 py-2 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors">
          <Plus className="w-4 h-4" />
          <span>New Article</span>
        </button>
      </div>

      <div className="flex items-center space-x-4 bg-surface p-3 rounded-card border border-border">
        <Search className="w-4 h-4 text-muted" />
        <input
          type="text"
          placeholder="Search articles, chunks, or sanitized case records..."
          className="w-full bg-transparent text-sm focus:outline-none text-DEFAULT"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="bg-surface border border-border rounded-card p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-DEFAULT text-sm">KB-104: Payment Reconciliation Guidelines</span>
            <span className="text-xs bg-success/10 text-success font-semibold px-2 py-0.5 rounded-full border border-success/20">
              PUBLISHED (v2.1)
            </span>
          </div>
          <p className="text-xs text-muted leading-relaxed">
            Standard operating procedure for resolving duplicate charges, gateway timeout reconciliation, and invoice updates.
          </p>
          <div className="flex items-center justify-between pt-2 border-t border-border-subtle text-xs text-muted">
            <span>Chunks: 4 (pgvector 1536d)</span>
            <span className="flex items-center space-x-1 text-success">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Indexed</span>
            </span>
          </div>
        </div>

        <div className="bg-surface border border-border rounded-card p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-DEFAULT text-sm">KB-105: SSO and MFA Troubleshooting</span>
            <span className="text-xs bg-success/10 text-success font-semibold px-2 py-0.5 rounded-full border border-success/20">
              PUBLISHED (v1.0)
            </span>
          </div>
          <p className="text-xs text-muted leading-relaxed">
            Troubleshooting SAML 2.0 assertions, Okta/Azure AD callback timeouts, and administrator recovery keys.
          </p>
          <div className="flex items-center justify-between pt-2 border-t border-border-subtle text-xs text-muted">
            <span>Chunks: 6 (pgvector 1536d)</span>
            <span className="flex items-center space-x-1 text-success">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Indexed</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
