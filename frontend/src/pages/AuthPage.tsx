import React, { useState } from 'react';
import { Sparkles, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const AuthPage: React.FC = () => {
  const { login, register } = useAuth();
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(''); setSubmitting(true);
    try {
      if (mode === 'login') await login(email, password);
      else await register(email, password, fullName);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Authentication failed');
    } finally { setSubmitting(false); }
  };

  return <main className="min-h-screen bg-background grid lg:grid-cols-2">
    <section className="hidden lg:flex bg-primary text-white p-16 flex-col justify-between">
      <div className="flex items-center gap-3 text-xl font-bold"><Sparkles /> ResolveIQ</div>
      <div><h1 className="text-4xl font-bold leading-tight">Evidence-grounded support, with humans in control.</h1>
        <p className="mt-5 text-white/75 max-w-lg">Classify requests, retrieve approved knowledge, and review cited AI drafts from one secure workspace.</p></div>
      <div className="flex gap-2 text-sm text-white/75"><ShieldCheck className="w-5" /> Tenant-isolated · Auditable · Approval-first</div>
    </section>
    <section className="flex items-center justify-center p-6">
      <form onSubmit={submit} className="w-full max-w-md bg-surface border border-border rounded-card p-8 shadow-sm space-y-5">
        <div><h2 className="text-2xl font-bold">{mode === 'login' ? 'Welcome back' : 'Create customer account'}</h2>
          <p className="text-sm text-muted mt-1">{mode === 'login' ? 'Sign in to your support workspace.' : 'Staff accounts are created by an administrator.'}</p></div>
        {mode === 'register' && <label className="block text-sm font-medium">Full name<input className="mt-1 w-full h-11 px-3 border border-border rounded-input" value={fullName} onChange={e => setFullName(e.target.value)} required /></label>}
        <label className="block text-sm font-medium">Email<input type="email" className="mt-1 w-full h-11 px-3 border border-border rounded-input" value={email} onChange={e => setEmail(e.target.value)} required /></label>
        <label className="block text-sm font-medium">Password<input type="password" minLength={12} maxLength={128} className="mt-1 w-full h-11 px-3 border border-border rounded-input" value={password} onChange={e => setPassword(e.target.value)} required /></label>
        {error && <div role="alert" className="text-sm text-danger bg-danger/10 p-3 rounded-input">{error}</div>}
        <button disabled={submitting} className="w-full h-11 bg-primary hover:bg-primary-hover text-white font-semibold rounded-btn disabled:opacity-60">{submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}</button>
        <button type="button" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(''); }} className="w-full text-sm text-primary font-medium">{mode === 'login' ? 'Need a customer account? Register' : 'Already registered? Sign in'}</button>
      </form>
    </section>
  </main>;
};
