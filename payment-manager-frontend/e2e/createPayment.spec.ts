import { expect, test } from '@playwright/test';
import { uniqueMerchantId } from './support/payments';

test.describe('create payment', () => {
  test('given a valid approved payment, when it is submitted, then shows its details and lists it first', async ({
    page,
  }) => {
    const merchantId = uniqueMerchantId();
    await page.goto('/payments');
    await page.getByRole('link', { name: 'New payment' }).click();

    await page.getByLabel('Merchant ID').fill(merchantId);
    await page.getByLabel('Amount').fill('1500.50');
    await page.getByRole('combobox', { name: 'Currency' }).click();
    await page.getByRole('option', { name: 'USD' }).click();
    await page.getByRole('button', { name: 'Create payment' }).click();

    await expect(page.getByText('Payment approved')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Payment details' })).toBeVisible();
    await expect(page.getByRole('row', { name: /Merchant/ })).toContainText(merchantId);
    await expect(page.getByRole('row', { name: /Amount/ })).toContainText('$1,500.50');
    await expect(page.getByRole('row', { name: /Status/ })).toContainText('Approved');

    await page.getByRole('link', { name: 'Back to payments' }).click();
    await expect(page.getByRole('row').nth(1)).toContainText(merchantId);
  });

  test('given a payment simulated as rejected, when it is submitted, then shows it rejected without the refund action', async ({
    page,
  }) => {
    await page.goto('/payments/new');

    await page.getByLabel('Merchant ID').fill(uniqueMerchantId());
    await page.getByLabel('Amount').fill('320');
    await page.getByRole('radio', { name: 'Rejected' }).check();
    await page.getByRole('button', { name: 'Create payment' }).click();

    await expect(page.getByText('Payment rejected')).toBeVisible();
    await expect(page.getByRole('row', { name: /Status/ })).toContainText('Rejected');
    await expect(page.getByRole('button', { name: 'Refund' })).toHaveCount(0);
  });

  test('given an empty form, when it is submitted, then shows the validation errors and stays on the form', async ({
    page,
  }) => {
    await page.goto('/payments/new');

    await page.getByRole('button', { name: 'Create payment' }).click();

    await expect(page.getByText('Merchant ID is required')).toBeVisible();
    await expect(page.getByText('Amount is required')).toBeVisible();
    await expect(page).toHaveURL('/payments/new');
  });
});
