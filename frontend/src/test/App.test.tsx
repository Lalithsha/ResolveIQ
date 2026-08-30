import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { App } from '../App';
import { CustomerPortal } from '../pages/CustomerPortal';
import { AuthProvider } from '../context/AuthContext';

describe('ResolveIQ Frontend Tests', () => {
  it('renders navigation bar with ResolveIQ branding', async () => {
    await act(async () => {
      render(<AuthProvider><App /></AuthProvider>);
    });
    expect(await screen.findByText('ResolveIQ')).toBeInTheDocument();
  });

  it('renders CustomerPortal tabs and switches between Create, My Tickets, and Help Center', async () => {
    const onSelectTab = vi.fn();
    let renderResult: any;

    await act(async () => {
      renderResult = render(<CustomerPortal activeTab="create" onSelectTab={onSelectTab} />);
    });
    expect(screen.getByText('Submit a Support Ticket')).toBeInTheDocument();

    await act(async () => {
      renderResult.rerender(<CustomerPortal activeTab="my-tickets" onSelectTab={onSelectTab} />);
    });
    expect(screen.getByText('My Support History')).toBeInTheDocument();

    await act(async () => {
      renderResult.rerender(<CustomerPortal activeTab="help" onSelectTab={onSelectTab} />);
    });
    expect(screen.getByText('ResolveIQ Self-Service Knowledge Base')).toBeInTheDocument();
  });
});
