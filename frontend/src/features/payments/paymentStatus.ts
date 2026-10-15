import type { PaymentStatus } from '@/api/payments';

export const PAYMENT_STATUS_LABELS: Readonly<Record<PaymentStatus, string>> = {
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  REFUNDED: 'Refunded',
};

export const PAYMENT_STATUS_COLORS: Readonly<Record<PaymentStatus, string>> = {
  APPROVED: 'green',
  REJECTED: 'red',
  REFUNDED: 'gray',
};

export function isRefundable(status: PaymentStatus): boolean {
  return status === 'APPROVED';
}
