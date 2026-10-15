import { Badge } from '@mantine/core';
import type { PaymentStatus } from '@/api/payments';
import { PAYMENT_STATUS_COLORS, PAYMENT_STATUS_LABELS } from '../paymentStatus';

type PaymentStatusBadgeProps = {
  readonly status: PaymentStatus;
};

export function PaymentStatusBadge({ status }: PaymentStatusBadgeProps) {
  return (
    <Badge color={PAYMENT_STATUS_COLORS[status]} variant="light">
      {PAYMENT_STATUS_LABELS[status]}
    </Badge>
  );
}
