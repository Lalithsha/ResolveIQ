import { render, screen, act } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { KnowledgeReleaseFlywheelCard } from '../components/knowledge/KnowledgeReleaseFlywheelCard';
import { Sidebar } from '../components/layout/Sidebar';
import { api } from '../api/client';

describe('Knowledge Release Flywheel & Metrics Tests', () => {
  it('renders flywheel header, quality queue, and governance metrics', async () => {
    vi.spyOn(api, 'getResolutionMetrics').mockResolvedValue({
      eligibleAttempts: 50,
      respondedAttempts: 40,
      feedbackCoverage: 0.80,
      verifiedSuccessRate: 0.92,
      firstContactResolutionRate: 0.85,
      averageScore: 84,
    });

    vi.spyOn(api, 'listKnowledgeCandidates').mockResolvedValue([
      {
        id: 'cand-1',
        title: 'Okta SAML 2.0 InResponseTo Resolution',
        contentDraft: 'Draft steps for SAML signature repair',
        sanitizedContent: 'Sanitized steps for SAML signature repair',
        category: 'AUTHENTICATION',
        eligibilityStatus: 'APPROVED',
        verifiedOutcomeScore: 95,
        distinctSourceCustomers: 7,
        sanitizationStatus: 'SANITIZED',
        contentHash: 'abc123hash',
        createdAt: new Date().toISOString(),
      },
    ]);

    await act(async () => {
      render(<KnowledgeReleaseFlywheelCard role="ADMIN" />);
    });

    expect(screen.getByText('Verified Knowledge Release Flywheel')).toBeInTheDocument();
    expect(screen.getByText('92.0%')).toBeInTheDocument();
    expect(screen.getByText('80.0%')).toBeInTheDocument();
    expect(screen.getByText('84 / 100')).toBeInTheDocument();
    expect(screen.getByText('Okta SAML 2.0 InResponseTo Resolution')).toBeInTheDocument();
    expect(screen.getByText('Approve & Release')).toBeInTheDocument();
  });

  it('renders Release flywheel navigation item for KNOWLEDGE_MANAGER and ADMIN', () => {
    const select = vi.fn();
    const km = render(<Sidebar currentRole="KNOWLEDGE_MANAGER" activeTab="release-flywheel" onSelectTab={select} />);
    expect(screen.getAllByText('Release flywheel').length).toBeGreaterThan(0);
    km.unmount();

    render(<Sidebar currentRole="ADMIN" activeTab="release-flywheel" onSelectTab={select} />);
    expect(screen.getAllByText('Release flywheel').length).toBeGreaterThan(0);
  });
});
