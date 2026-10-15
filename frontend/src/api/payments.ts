import { request } from './httpClient';
import type { Page } from './page';

export const PAYMENT_STATUSES = ['APPROVED', 'REJECTED', 'REFUNDED'] as const;
export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];

export const CURRENCIES = [
  'ARS',
  'USD',
  'EUR',
  'BRL',
  'CLP',
  'UYU',
  'PYG',
  'BOB',
  'PEN',
  'COP',
  'MXN',
  'GBP',
] as const;
export type Currency = (typeof CURRENCIES)[number];

export type Payment = {
  readonly id: string;
  readonly merchantId: string;
  readonly amount: number;
  readonly currency: Currency;
  readonly status: PaymentStatus;
  readonly createdAt: string;
};

export type ListPaymentsParams = {
  readonly status?: PaymentStatus;
  readonly page: number;
  readonly size: number;
};

export function isPaymentStatus(value: unknown): value is PaymentStatus {
  return PAYMENT_STATUSES.some((status) => status === value);
}

export function listPayments(
  { status, page, size }: ListPaymentsParams,
  signal?: AbortSignal,
): Promise<Page<Payment>> {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status !== undefined) {
    query.set('status', status);
  }
  return request<Page<Payment>>(`/payments?${query.toString()}`, { signal });
}

export function getPayment(id: string, signal?: AbortSignal): Promise<Payment> {
  return request<Payment>(`/payments/${encodeURIComponent(id)}`, { signal });
}
