import { expect, Page, test } from '@playwright/test';

const PASSWORD = 'ResolveIQ2026!';

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

async function logout(page: Page) {
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible();
}

test('customer ticket reaches an authorized support queue and requires human approval', async ({ page }) => {
  const unique = Date.now().toString();
  const subject = `E2E duplicate authorization ${unique}`;

  await login(page, 'alex.morgan@acme.com');
  await page.getByPlaceholder(/For example, I can’t access/).fill(subject);
  await page.locator('select').filter({ has: page.locator('option[value="BILLING"]') }).selectOption('BILLING');
  await page.getByPlaceholder(/What happened\? When did it start/).fill(
    `Two fictional payment authorizations appeared for E2E-${unique}. No card data is included.`,
  );
  await page.getByRole('button', { name: 'Submit request' }).click();
  await expect(page.getByRole('heading', { name: 'Your request is with us' })).toBeVisible({ timeout: 30_000 });
  const confirmation = await page.locator('main').innerText();
  const ticketNumber = confirmation.match(/RIQ-\d{4}-\d+/)?.[0];
  expect(ticketNumber, 'a real ticket number should be shown').toBeTruthy();
  await logout(page);

  await login(page, 'sarah.chen@resolveiq.local');
  await page.getByRole('button', { name: 'Team queue', exact: true }).click();
  await page.getByPlaceholder('Search tickets').fill(ticketNumber!);

  const ticketRow = page.getByRole('button', { name: new RegExp(ticketNumber!) });
  await expect(async () => {
    await page.getByRole('button', { name: 'Refresh queue' }).click();
    await expect(ticketRow).toBeVisible();
    await expect(ticketRow).toContainText('READY FOR AGENT');
  }).toPass({ timeout: 45_000, intervals: [1_000, 2_000, 3_000] });
  await ticketRow.click();
  await expect(page.getByRole('heading', { name: subject })).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText('Human approval is enforced. AI cannot send this response.')).toBeVisible();

  const attachmentName = `e2e-evidence-${unique}.txt`;
  await page.locator('input[type="file"]').setInputFiles({
    name: attachmentName,
    mimeType: 'text/plain',
    buffer: Buffer.from(`ResolveIQ clean browser evidence ${unique}`),
  });
  await expect(page.getByText(attachmentName)).toBeVisible({ timeout: 20_000 });
  await expect(page.getByText('CLEAN').last()).toBeVisible();

  const approve = page.getByRole('button', { name: 'Approve & send' });
  await expect(approve).toBeEnabled({ timeout: 45_000 });
  await approve.click();
  await expect(page.getByText('Response sent. Ticket moved to WAITING_ON_CUSTOMER.')).toBeVisible({ timeout: 20_000 });
  await logout(page);

  await login(page, 'alex.morgan@acme.com');
  await page.getByRole('button', { name: 'My tickets', exact: true }).click();
  const customerTicket = page.getByRole('button', { name: new RegExp(`${ticketNumber}.*${subject}`, 's') });
  await expect(customerTicket).toBeVisible({ timeout: 20_000 });
  await customerTicket.click();
  await expect(page.getByText('WAITING ON CUSTOMER')).toBeVisible();
  await expect(page.getByText(attachmentName)).toBeVisible();
});

test('knowledge manager publishes, supersedes, rolls back and archives indexed knowledge', async ({ page }) => {
  const unique = Date.now().toString();
  const title = `E2E lifecycle guide ${unique}`;
  const firstToken = `reconcile-e2e-${unique}`;
  const secondToken = `replacement-e2e-${unique}`;

  await login(page, 'elena.rostova@resolveiq.local');
  await page.getByRole('button', { name: 'New article' }).click();
  await page.getByLabel('Article title').fill(title);
  await page.getByLabel('Category').selectOption('BILLING');
  await page.getByLabel('Product').fill('E2E Billing');
  await page.getByLabel('Short summary').fill('Browser lifecycle acceptance');
  await page.getByLabel('Article content').fill(`Approved fictional process for ${firstToken}.`);
  await page.getByRole('button', { name: 'Save draft' }).click();
  await expect(page.getByText('Draft article created. It is not searchable until reviewed and published.')).toBeVisible();
  await expect(page.getByText('DRAFT').last()).toBeVisible();
  await page.getByRole('button', { name: 'Submit for review' }).click();
  await expect(page.getByText('IN REVIEW').last()).toBeVisible();
  await page.getByRole('button', { name: 'Publish', exact: true }).click();
  await expect(page.getByText('PUBLISHED').last()).toBeVisible({ timeout: 30_000 });

  await page.getByRole('button', { name: 'Vector indexes' }).click();
  await page.getByLabel('Search approved knowledge').fill(firstToken);
  await page.getByRole('button', { name: 'Search index' }).click();
  await expect(page.getByText(title)).toBeVisible({ timeout: 20_000 });

  await page.getByRole('button', { name: 'Articles & chunks' }).click();
  await page.getByRole('button', { name: new RegExp(title) }).click();
  await page.getByRole('button', { name: 'New version' }).click();
  await page.getByLabel('Short summary').fill('Replacement browser version');
  await page.getByLabel('Article content').fill(`Approved replacement process for ${secondToken}.`);
  await page.getByRole('button', { name: 'Save draft' }).click();
  await page.getByRole('button', { name: 'Submit for review' }).click();
  await page.getByRole('button', { name: 'Publish', exact: true }).click();
  await expect(page.getByText('SUPERSEDED').last()).toBeVisible({ timeout: 30_000 });
  await page.getByRole('button', { name: 'Rollback' }).click();
  await expect(page.getByText(/Version 1 rollback completed/)).toBeVisible({ timeout: 30_000 });

  await page.getByRole('button', { name: 'Archive', exact: true }).click();
  await expect(page.getByText('ARCHIVED').first()).toBeVisible({ timeout: 20_000 });
  await page.getByRole('button', { name: 'Vector indexes' }).click();
  await page.getByLabel('Search approved knowledge').fill(firstToken);
  await page.getByRole('button', { name: 'Search index' }).click();
  await expect(page.getByText(title)).toHaveCount(0);
});
