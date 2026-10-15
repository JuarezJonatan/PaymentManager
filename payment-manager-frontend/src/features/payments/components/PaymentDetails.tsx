import { Card, Table } from '@mantine/core';
import type { Payment } from '@/api/payments';
import { formatAmount, formatDateTime } from '@/shared/lib/format';
import { PaymentStatusBadge } from './PaymentStatusBadge';

type PaymentDetailsProps = {
  readonly payment: Payment;
};

export function PaymentDetails({ payment }: PaymentDetailsProps) {
  return (
    <Card withBorder padding="lg">
      <Table variant="vertical" layout="fixed" withRowBorders={false}>
        <Table.Tbody>
          <Table.Tr>
            <Table.Th w={160}>ID</Table.Th>
            <Table.Td ff="monospace">{payment.id}</Table.Td>
          </Table.Tr>
          <Table.Tr>
            <Table.Th>Merchant</Table.Th>
            <Table.Td>{payment.merchantId}</Table.Td>
          </Table.Tr>
          <Table.Tr>
            <Table.Th>Amount</Table.Th>
            <Table.Td>{formatAmount(payment.amount, payment.currency)}</Table.Td>
          </Table.Tr>
          <Table.Tr>
            <Table.Th>Currency</Table.Th>
            <Table.Td>{payment.currency}</Table.Td>
          </Table.Tr>
          <Table.Tr>
            <Table.Th>Status</Table.Th>
            <Table.Td>
              <PaymentStatusBadge status={payment.status} />
            </Table.Td>
          </Table.Tr>
          <Table.Tr>
            <Table.Th>Created at</Table.Th>
            <Table.Td>{formatDateTime(payment.createdAt)}</Table.Td>
          </Table.Tr>
        </Table.Tbody>
      </Table>
    </Card>
  );
}
