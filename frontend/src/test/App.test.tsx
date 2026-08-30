import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { App } from '../App';
import { CustomerPortal } from '../pages/CustomerPortal';
import { AuthProvider } from '../context/AuthContext';
import { Sidebar } from '../components/layout/Sidebar';

describe('ResolveIQ Frontend Tests', () => {
  it('renders navigation bar with ResolveIQ branding', async () => {
    await act(async () => {
      render(<AuthProvider><App /></AuthProvider>);
    });
    expect((await screen.findAllByText('ResolveIQ')).length).toBeGreaterThan(0);
  });

  it('renders CustomerPortal tabs and switches between Create, My Tickets, and Help Center', async () => {
    const onSelectTab = vi.fn();
    let renderResult: any;

    await act(async () => {
      renderResult = render(<CustomerPortal activeTab="create" onSelectTab={onSelectTab} />);
    });
    expect(screen.getByRole('heading', { name: /How can we help\?|Submit a Support Ticket/ })).toBeInTheDocument();

    await act(async () => {
      renderResult.rerender(<CustomerPortal activeTab="my-tickets" onSelectTab={onSelectTab} />);
    });
    expect(screen.getByRole('heading', { name: /My tickets|My Support History/ })).toBeInTheDocument();

    await act(async () => {
      renderResult.rerender(<CustomerPortal activeTab="help" onSelectTab={onSelectTab} />);
    });
    expect(screen.getByRole('heading', { name: /Help center|ResolveIQ Self-Service Knowledge Base/ })).toBeInTheDocument();
  });

  it('gives team leads and auditors distinct role-complete navigation', () => {
    const select = vi.fn();
    const teamLead = render(<Sidebar currentRole="TEAM_LEAD" activeTab="team-queue" onSelectTab={select} />);
    expect(screen.getAllByText('Team queue').length).toBeGreaterThan(0);
    expect(screen.getAllByText('SLA risk').length).toBeGreaterThan(0);
    teamLead.unmount();

    render(<Sidebar currentRole="AUDITOR" activeTab="audit" onSelectTab={select} />);
    expect(screen.getAllByText('Security audit').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Workflow audit').length).toBeGreaterThan(0);
  });
});
