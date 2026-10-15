import type { APIRequestContext } from '@playwright/test';
import type { CreatePaymentRequest, Payment } from '../../src/api/payments';

export function uniqueMerchantId(): string {
  return `E2E_${crypto.randomUUID()}`;
}

export async function createPayment(
  request: APIRequestContext,
  overrides: Partial<CreatePaymentRequest> = {},
): Promise<Payment> {
  const response = await request.post('/api/payments', {
    data: {
      merchantId: uniqueMerchantId(),
      amount: 1500.5,
      currency: 'USD',
      simulationResult: 'APPROVED',
      ...overrides,
    } satisfies CreatePaymentRequest,
  });
  if (!response.ok()) {
    throw new Error(`Could not create the payment: ${response.status()}`);
  }
  return (await response.json()) as Payment;
}
