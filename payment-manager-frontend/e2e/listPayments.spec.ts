import { expect, test } from '@playwright/test';
import { createPayment } from './support/payments';

test.describe('list payments', () => {
  test('given payments in several statuses, when filtering by rejected, then lists only rejected payments', async ({
    page,
    request,
  }) => {
    const rejected = await createPayment(request, { simulationResult: 'REJECTED' });
    const approved = await createPayment(request, { simulationResult: 'APPROVED' });
    await page.goto('/payments');

    await page.getByRole('radiogroup', { name: 'Filter by status' }).getByText('Rejected').click();

    await expect(page).toHaveURL('/payments?status=REJECTED');
    await expect(page.getByRole('link', { name: rejected.id })).toBeVisible();
    await expect(page.getByRole('link', { name: approved.id })).toHaveCount(0);
    const statuses = await page.getByRole('row').locator('td:nth-child(4)').allTextContents();
    expect(statuses.every((status) => status === 'Rejected')).toBe(true);
  });

  test('given a listed payment, when its ID is clicked, then shows its details', async ({
    page,
    request,
  }) => {
    const payment = await createPayment(request);
    await page.goto('/payments');

    await page.getByRole('link', { name: payment.id }).click();

    await expect(page).toHaveURL(`/payments/${payment.id}`);
    await expect(page.getByRole('row', { name: /^ID/ })).toContainText(payment.id);
  });

  test('given an unknown payment ID, when its detail is opened, then shows the not found state', async ({
    page,
  }) => {
    await page.goto('/payments/pay_unknown');

    await expect(page.getByText('Payment not found.')).toBeVisible();
  });
});
