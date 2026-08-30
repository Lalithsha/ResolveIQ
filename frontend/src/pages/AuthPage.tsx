import React, { useState } from 'react';
import { ArrowRight, Check, LockKeyhole, ShieldCheck, Sparkles, UserCheck } from 'lucide-react';
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
    { label: 'Team lead', email: 'marcus.vance@resolveiq.local', pass: 'ResolveIQ2026!' },
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

  const changeMode = (nextMode: 'login' | 'register') => {
    setMode(nextMode);
    setError('');
  };

  return (
    <main className="grid min-h-screen bg-background lg:grid-cols-[1.05fr_0.95fr]">
      <section className="relative hidden overflow-hidden bg-slate-950 p-10 text-white lg:flex lg:flex-col lg:justify-between xl:p-14">
        <div className="absolute inset-0 opacity-30 [background-image:radial-gradient(circle_at_20%_0%,rgba(91,130,246,.38),transparent_34%),radial-gradient(circle_at_95%_80%,rgba(129,96,201,.28),transparent_30%)]" />
        <div className="absolute inset-0 opacity-[0.07] [background-image:linear-gradient(rgba(255,255,255,.7)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,.7)_1px,transparent_1px)] [background-size:44px_44px]" />

        <div className="relative flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-[12px] bg-white text-slate-950">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <p className="text-[15px] font-semibold tracking-[-0.02em]">ResolveIQ</p>
            <p className="text-[11px] text-white/50">Support intelligence, grounded in evidence</p>
          </div>
        </div>

        <div className="relative max-w-xl">
          <span className="mb-5 inline-flex rounded-full border border-white/10 bg-white/[0.06] px-3 py-1 text-[11px] font-medium text-white/70">
            Human judgment stays in control
          </span>
          <h1 className="max-w-lg text-[2.65rem] font-semibold leading-[1.08] tracking-[-0.045em] xl:text-5xl">
            Resolve support work with clarity and confidence.
          </h1>
          <p className="mt-6 max-w-lg text-[15px] leading-7 text-white/58">
            Triage incoming requests, find approved knowledge, and review every AI-assisted response with its evidence attached.
          </p>

          <div className="mt-10 grid max-w-lg grid-cols-3 gap-3">
            {[
              ['Auditable', 'Every action traced'],
              ['Grounded', 'Approved sources only'],
              ['Controlled', 'Human approval required'],
            ].map(([title, description]) => (
              <div key={title} className="rounded-[14px] border border-white/10 bg-white/[0.045] p-3.5 backdrop-blur-sm">
                <Check className="mb-3 h-4 w-4 text-blue-300" />
                <p className="text-xs font-semibold">{title}</p>
                <p className="mt-1 text-[10px] leading-4 text-white/45">{description}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative flex items-center gap-2 text-[11px] text-white/45">
          <ShieldCheck className="h-4 w-4" />
          Tenant-isolated · Encrypted in transit · Role-based access
        </div>
      </section>

      <section className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-[430px]">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div className="grid h-9 w-9 place-items-center rounded-[11px] bg-slate-950 text-white dark:bg-white dark:text-slate-950">
              <Sparkles className="h-[18px] w-[18px]" />
            </div>
            <span className="font-semibold tracking-[-0.02em] text-DEFAULT">ResolveIQ</span>
          </div>

          <div className="mb-7">
            <span className="eyebrow">Secure workspace</span>
            <h2 className="text-[1.8rem] font-semibold tracking-[-0.035em] text-DEFAULT">
              {mode === 'login' ? 'Welcome back' : 'Create your account'}
            </h2>
            <p className="mt-2 text-sm leading-6 text-muted">
              {mode === 'login'
                ? 'Sign in to continue to your ResolveIQ workspace.'
                : 'Create a customer account to submit and track requests.'}
            </p>
          </div>

          <div className="mb-6 grid grid-cols-2 rounded-[12px] bg-surface-muted p-1">
            <button
              type="button"
              onClick={() => changeMode('login')}
              className={`rounded-[9px] px-3 py-2 text-xs font-semibold transition-colors ${mode === 'login' ? 'bg-surface text-DEFAULT shadow-sm' : 'text-muted hover:text-DEFAULT'}`}
            >
              Sign in
            </button>
            <button
              type="button"
              onClick={() => changeMode('register')}
              className={`rounded-[9px] px-3 py-2 text-xs font-semibold transition-colors ${mode === 'register' ? 'bg-surface text-DEFAULT shadow-sm' : 'text-muted hover:text-DEFAULT'}`}
            >
              Create account
            </button>
          </div>

          <form onSubmit={submit} className="space-y-4">
            {mode === 'register' && (
              <label className="block">
                <span className="field-label">Full name</span>
                <input
                  type="text"
                  required
                  autoComplete="name"
                  placeholder="Your full name"
                  className="form-control h-11"
                  value={fullName}
                  onChange={(event) => setFullName(event.target.value)}
                />
              </label>
            )}

            <label className="block">
              <span className="field-label">Email address</span>
              <input
                type="email"
                required
                autoComplete="email"
                placeholder="name@company.com"
                className="form-control h-11"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>

            <label className="block">
              <span className="field-label">Password</span>
              <div className="relative">
                <LockKeyhole className="pointer-events-none absolute left-3.5 top-3.5 h-4 w-4 text-muted" />
                <input
                  type="password"
                  required
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  minLength={mode === 'register' ? 12 : 1}
                  maxLength={128}
                  placeholder="Enter your password"
                  className="form-control h-11 pl-10"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </div>
              {mode === 'register' && (
                <span className="mt-2 block text-[11px] leading-4 text-muted">
                  Use 12+ characters with uppercase, lowercase, a number, and a symbol.
                </span>
              )}
            </label>

            {error && (
              <div role="alert" className="rounded-input border border-danger/20 bg-danger/10 px-3.5 py-3 text-xs text-danger">
                {error}
              </div>
            )}

            <button type="submit" disabled={submitting} className="btn-primary h-11 w-full">
              {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in to workspace' : 'Create account'}
              {!submitting && <ArrowRight className="h-4 w-4" />}
            </button>
          </form>

          {mode === 'login' && (
            <div className="mt-7 border-t border-border-subtle pt-6">
              <div className="mb-3 flex items-center gap-2">
                <UserCheck className="h-4 w-4 text-primary" />
                <p className="text-xs font-semibold text-DEFAULT">Explore a demo workspace</p>
                <span className="text-[10px] text-muted">Select a role to autofill</span>
              </div>
              <div className="grid grid-cols-2 gap-2">
                {demoAccounts.map((account) => (
                  <button
                    key={account.label}
                    type="button"
                    onClick={() => setDemoCreds(account.email, account.pass)}
                    className="rounded-input border border-border-subtle bg-surface px-3 py-2.5 text-left transition-colors hover:border-primary/40 hover:bg-primary/5"
                  >
                    <span className="block text-xs font-semibold text-DEFAULT">{account.label}</span>
                    <span className="mt-0.5 block truncate text-[10px] text-muted">{account.email}</span>
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
