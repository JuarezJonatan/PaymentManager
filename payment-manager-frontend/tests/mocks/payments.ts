import type { Page } from '@/api/page';
import type { Payment } from '@/api/payments';

export function aPayment(overrides: Partial<Payment> = {}): Payment {
  return {
    id: '7f3c2a1e-4b5d-4e6f-8a9b-0c1d2e3f4a5b',
    merchantId: 'merchant-001',
    amount: 1500.5,
    currency: 'USD',
    status: 'APPROVED',
    createdAt: '2026-10-15T13:05:00Z',
    ...overrides,
  };
}

export function aPage<T>(content: readonly T[], overrides: Partial<Page<T>> = {}): Page<T> {
  return {
    content,
    page: 0,
    size: 20,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    ...overrides,
  };
}
