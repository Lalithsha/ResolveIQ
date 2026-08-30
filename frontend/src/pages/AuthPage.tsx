import React, { useState } from 'react';
import { Sparkles, ShieldCheck, UserCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const AuthPage: React.FC = () => {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const demoAccounts = [
    { label: 'Customer', email: 'alex.morgan@acme.com', pass: 'ResolveIQ2026!' },
    { label: 'Agent', email: 'sarah.chen@resolveiq.local', pass: 'ResolveIQ2026!' },
    { label: 'Lead', email: 'marcus.vance@resolveiq.local', pass: 'ResolveIQ2026!' },
    { label: 'Admin', email: 'admin@resolveiq.local', pass: 'ResolveIQ2026!' },
  ];

  const setDemoCreds = (demoEmail: string, demoPass: string) => {
    setMode('login');
    setEmail(demoEmail);
    setPassword(demoPass);
    setError('');
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register(email, password, fullName);
      }
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Authentication failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-background grid lg:grid-cols-2">
      <section className="hidden lg:flex bg-primary text-white p-16 flex-col justify-between">
        <div className="flex items-center gap-3 text-xl font-bold">
          <Sparkles /> ResolveIQ
        </div>
        <div>
          <h1 className="text-4xl font-bold leading-tight">Evidence-grounded support, with humans in control.</h1>
          <p className="mt-5 text-white/75 max-w-lg leading-relaxed">
            Classify requests, retrieve approved knowledge, and review cited AI drafts from one secure workspace.
          </p>
        </div>
        <div className="flex gap-2 text-sm text-white/75 items-center">
          <ShieldCheck className="w-5 h-5 flex-shrink-0" />
          <span>Tenant-isolated · Auditable · BCrypt Hashed · TLS Encrypted in Transit</span>
        </div>
      </section>

      <section className="flex items-center justify-center p-6">
        <div className="w-full max-w-md space-y-4">
          <form onSubmit={submit} className="bg-surface border border-border rounded-card p-8 shadow-sm space-y-5">
            <div>
              <h2 className="text-2xl font-bold text-DEFAULT">
                {mode === 'login' ? 'Welcome back' : 'Create customer account'}
              </h2>
              <p className="text-sm text-muted mt-1">
                {mode === 'login'
                  ? 'Sign in to your support workspace.'
                  : 'Register a new customer account to create and track support requests.'}
              </p>
            </div>

            {mode === 'register' && (
              <label className="block text-sm font-medium text-DEFAULT">
                Full name
                <input
                  type="text"
                  required
                  placeholder="e.g. Lalith Sharma"
                  className="mt-1 w-full h-11 px-3 bg-surface text-DEFAULT border border-border rounded-input focus:outline-none focus:ring-2 focus:ring-primary"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                />
              </label>
            )}

            <label className="block text-sm font-medium text-DEFAULT">
              Email
              <input
                type="email"
                required
                placeholder="name@example.com"
                className="mt-1 w-full h-11 px-3 bg-surface text-DEFAULT border border-border rounded-input focus:outline-none focus:ring-2 focus:ring-primary"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </label>

            <label className="block text-sm font-medium text-DEFAULT">
              Password
              <input
                type="password"
                required
                minLength={mode === 'register' ? 12 : 1}
                maxLength={128}
                placeholder="••••••••••••"
                className="mt-1 w-full h-11 px-3 bg-surface text-DEFAULT border border-border rounded-input focus:outline-none focus:ring-2 focus:ring-primary"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              {mode === 'register' && (
                <span className="block text-[11px] text-muted mt-1">
                  Must be at least 12 characters with uppercase, lowercase, number, and special symbol.
                </span>
              )}
            </label>

            {error && (
              <div role="alert" className="text-xs text-danger bg-danger/10 p-3 rounded-input border border-danger/20">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full h-11 bg-primary hover:bg-primary-hover text-white font-semibold rounded-btn transition-colors disabled:opacity-60 text-sm"
            >
              {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>

            <button
              type="button"
              onClick={() => {
                setMode(mode === 'login' ? 'register' : 'login');
                setError('');
              }}
              className="w-full text-xs text-primary font-medium hover:underline text-center"
            >
              {mode === 'login' ? 'Need a customer account? Register' : 'Already registered? Sign in'}
            </button>
          </form>

          {/* Quick Demo Credentials */}
          {mode === 'login' && (
            <div className="bg-surface border border-border rounded-card p-4 space-y-2 text-xs">
              <div className="flex items-center space-x-1.5 text-muted font-semibold uppercase tracking-wider text-[10px]">
                <UserCheck className="w-3.5 h-3.5 text-primary" />
                <span>Demo Accounts (Click to autofill)</span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5">
                {demoAccounts.map((acc) => (
                  <button
                    key={acc.label}
                    type="button"
                    onClick={() => setDemoCreds(acc.email, acc.pass)}
                    className="p-2 rounded-btn bg-surface-muted hover:bg-primary/10 hover:text-primary border border-border text-center transition-colors font-medium text-xs"
                  >
                    {acc.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>
    </main>
  );
};

export default AuthPage;
