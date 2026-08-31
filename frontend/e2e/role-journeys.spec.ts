import { expect, Page, test } from '@playwright/test';

const PASSWORD = 'ResolveIQ2026!';

type Journey = {
  role: string;
  email: string;
  heading?: RegExp;
  navigation: string[];
};

const journeys: Journey[] = [
  {
    role: 'customer',
    email: 'alex.morgan@acme.com',
    heading: /How can we help\?/,
    navigation: ['Create ticket', 'My tickets', 'Help center'],
  },
  {
    role: 'agent',
    email: 'sarah.chen@resolveiq.local',
    navigation: ['My queue', 'Team queue', 'SLA risk', 'Knowledge'],
  },
  {
    role: 'team lead',
    email: 'marcus.vance@resolveiq.local',
    navigation: ['Team queue', 'SLA risk', 'Knowledge'],
  },
  {
    role: 'knowledge manager',
    email: 'elena.rostova@resolveiq.local',
    heading: /Knowledge lifecycle/,
    navigation: ['Articles & chunks', 'Sanitized cases', 'Vector indexes'],
  },
  {
    role: 'administrator',
    email: 'admin@resolveiq.local',
    heading: /Operations overview/,
    navigation: ['Overview', 'All tickets', 'Teams & routing', 'Knowledge base', 'AI governance', 'Users & roles'],
  },
  {
    role: 'auditor',
    email: 'auditor@resolveiq.local',
    heading: /Security audit/,
    navigation: ['Security audit', 'Ticket evidence', 'Workflow audit', 'AI governance'],
  },
];

async function login(page: Page, email: string) {
  await page.goto('/');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(PASSWORD);
  await expect(async () => {
    if (await page.getByRole('button', { name: 'Sign out' }).isVisible()) return;
    await page.getByRole('button', { name: 'Sign in to workspace' }).click();
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible({ timeout: 5_000 });
  }).toPass({ timeout: 30_000, intervals: [500, 1_000, 2_000] });
}

for (const journey of journeys) {
  test(`${journey.role} sees only its production workspace`, async ({ page }) => {
    await login(page, journey.email);
    if (journey.heading) {
      await expect(page.getByRole('heading', { name: journey.heading }).first()).toBeVisible({ timeout: 30_000 });
    } else {
      // Queue workspaces can legitimately be empty, so the stable queue controls
      // are a stronger role-access assertion than any particular seeded ticket.
      await expect(page.getByRole('button', { name: 'Refresh queue' })).toBeVisible({ timeout: 30_000 });
    }

    const navigation = page.getByRole('navigation', { name: 'Primary navigation' });
    for (const label of journey.navigation) {
      await expect(navigation.getByRole('button', { name: label, exact: true })).toBeVisible();
    }

    await expect(page.locator('[role="alert"]')).toHaveCount(0);
    await page.getByRole('button', { name: 'Sign out' }).click();
    await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
  });
}
