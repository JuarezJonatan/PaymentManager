import { expect, test } from '@playwright/test';
import { createPayment } from './support/payments';

test.describe('refund payment', () => {
  test('given an approved payment, when the refund is confirmed, then shows it refunded without the refund action', async ({
    page,
    request,
  }) => {
    const payment = await createPayment(request, { simulationResult: 'APPROVED' });
    await page.goto(`/payments/${payment.id}`);

    await page.getByRole('button', { name: 'Refund' }).click();
    await page
      .getByRole('dialog', { name: 'Refund payment' })
      .getByRole('button', { name: 'Confirm refund' })
      .click();

    await expect(page.getByText('Payment refunded')).toBeVisible();
    await expect(page.getByRole('row', { name: /Status/ })).toContainText('Refunded');
    await expect(page.getByRole('button', { name: 'Refund' })).toHaveCount(0);
  });

  test('given an approved payment, when the refund is cancelled, then keeps it approved', async ({
    page,
    request,
  }) => {
    const payment = await createPayment(request, { simulationResult: 'APPROVED' });
    await page.goto(`/payments/${payment.id}`);

    await page.getByRole('button', { name: 'Refund' }).click();
    await page
      .getByRole('dialog', { name: 'Refund payment' })
      .getByRole('button', { name: 'Cancel' })
      .click();

    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.getByRole('row', { name: /Status/ })).toContainText('Approved');
  });
});
