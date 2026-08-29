import React, { useState } from 'react';
import { Search, Plus, CheckCircle2, BookOpen, Loader2 } from 'lucide-react';
import { api } from '../api/client';
import { Citation } from '../types';

export const KnowledgeConsole: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<Citation[] | null>(null);
  const [error, setError] = useState('');

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    setIsSearching(true);
    setError('');
    try {
      const res = await api.searchKnowledge(searchQuery.trim(), 5);
      setSearchResults(res.citations);
    } catch (failure) {
      setSearchResults(null);
      setError(failure instanceof Error ? failure.message : 'Knowledge search failed');
    } finally {
      setIsSearching(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-DEFAULT">Knowledge Base & RAG Indexing</h1>
          <p className="text-sm text-muted mt-1">Manage approved knowledge articles, inspect chunk embeddings, and test hybrid RRF retrieval.</p>
        </div>
        <button
          onClick={() => alert('New Article authoring dialog (Version draft workflow)')}
          className="inline-flex items-center space-x-2 px-4 py-2 bg-primary text-white text-sm font-semibold rounded-btn hover:bg-primary-hover shadow-sm transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>New Article</span>
        </button>
      </div>

      <form onSubmit={handleSearch} className="flex items-center space-x-3 bg-surface p-2 rounded-card border border-border">
        <Search className="w-4 h-4 text-muted ml-2" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Test Hybrid RRF Search (e.g. duplicate charge invoice, Okta SAML 401 signature error)..."
          className="flex-1 bg-transparent text-sm focus:outline-none text-DEFAULT"
        />
        <button
          type="submit"
          disabled={isSearching}
          className="px-4 py-1.5 bg-primary text-white text-xs font-semibold rounded-btn hover:bg-primary-hover transition-colors disabled:opacity-50 flex items-center space-x-1"
        >
          {isSearching && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
          <span>{isSearching ? 'Searching...' : 'Search Index'}</span>
        </button>
      </form>

      {error && <div role="alert" className="p-3 bg-danger/10 text-danger border border-danger/20 rounded-card text-sm">{error}</div>}

      {searchResults && (
        <div className="bg-surface border border-primary/20 rounded-card p-5 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-DEFAULT flex items-center space-x-2">
              <BookOpen className="w-4 h-4 text-primary" />
              <span>Hybrid Search Results (RRF k=60)</span>
            </h2>
            <button onClick={() => setSearchResults(null)} className="text-xs text-muted hover:text-DEFAULT">
              Clear Results
            </button>
          </div>

          <div className="space-y-3">
            {searchResults.length === 0 ? (
              <p className="text-xs text-muted">No matching knowledge articles found.</p>
            ) : (
              searchResults.map((c, i) => (
                <div key={i} className="p-3 bg-surface-muted rounded-btn border border-border-subtle space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-DEFAULT text-xs">{c.title}</span>
                    <span className="font-mono text-[11px] bg-primary/10 text-primary px-2 py-0.5 rounded">
                      RRF Score: {((c.confidenceScore ?? 0.9) * 100).toFixed(1)}%
                    </span>
                  </div>
                  <p className="text-xs text-muted leading-relaxed">"{c.snippet || c.citationText || ''}"</p>
                </div>
              ))
            )}
          </div>
        </div>
      )}

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
              <span>Indexed in PostgreSQL</span>
            </span>
          </div>
        </div>

        <div className="bg-surface border border-border rounded-card p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="font-semibold text-DEFAULT text-sm">KB-105: Enterprise SSO & SAML 2.0 Identity</span>
            <span className="text-xs bg-success/10 text-success font-semibold px-2 py-0.5 rounded-full border border-success/20">
              PUBLISHED (v1.0)
            </span>
          </div>
          <p className="text-xs text-muted leading-relaxed">
            Troubleshooting SAML 2.0 assertions, Okta/Azure AD certificate expiries, and entity ID validation errors.
          </p>
          <div className="flex items-center justify-between pt-2 border-t border-border-subtle text-xs text-muted">
            <span>Chunks: 6 (pgvector 1536d)</span>
            <span className="flex items-center space-x-1 text-success">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>Indexed in PostgreSQL</span>
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
