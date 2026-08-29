import { render, screen, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { App } from '../App';
import { AuthProvider } from '../context/AuthContext';

describe('ResolveIQ App Smoke Test', () => {
  it('renders navigation bar with ResolveIQ branding', async () => {
    await act(async () => {
      render(<AuthProvider><App /></AuthProvider>);
    });
    expect(await screen.findByText('ResolveIQ')).toBeInTheDocument();
  });
});
