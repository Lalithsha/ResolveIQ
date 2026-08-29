import { render, screen, act } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { App } from '../App';

describe('ResolveIQ App Smoke Test', () => {
  it('renders navigation bar with ResolveIQ branding', async () => {
    await act(async () => {
      render(<App />);
    });
    expect(screen.getByText('ResolveIQ')).toBeInTheDocument();
  });
});
