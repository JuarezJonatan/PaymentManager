import { SegmentedControl } from '@mantine/core';
import { isPaymentStatus, PAYMENT_STATUSES, type PaymentStatus } from '@/api/payments';
import { PAYMENT_STATUS_LABELS } from '../paymentStatus';

const ALL_STATUSES = 'ALL';

const OPTIONS = [
  { value: ALL_STATUSES, label: 'All' },
  ...PAYMENT_STATUSES.map((status) => ({ value: status, label: PAYMENT_STATUS_LABELS[status] })),
];

type PaymentStatusFilterProps = {
  readonly value: PaymentStatus | undefined;
  readonly onChange: (status: PaymentStatus | undefined) => void;
};

export function PaymentStatusFilter({ value, onChange }: PaymentStatusFilterProps) {
  return (
    <SegmentedControl
      aria-label="Filter by status"
      data={OPTIONS}
      value={value ?? ALL_STATUSES}
      onChange={(selected) => onChange(isPaymentStatus(selected) ? selected : undefined)}
    />
  );
}
