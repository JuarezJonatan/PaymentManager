import { Anchor, Table } from '@mantine/core';
import { Link } from 'react-router';
import type { Payment } from '@/api/payments';
import { formatAmount, formatDateTime } from '@/shared/lib/format';
import { PaymentStatusBadge } from './PaymentStatusBadge';

type PaymentsTableProps = {
  readonly payments: readonly Payment[];
};

export function PaymentsTable({ payments }: PaymentsTableProps) {
  return (
    <Table.ScrollContainer minWidth={720}>
      <Table striped highlightOnHover verticalSpacing="sm">
        <Table.Thead>
          <Table.Tr>
            <Table.Th>ID</Table.Th>
            <Table.Th>Merchant</Table.Th>
            <Table.Th ta="right">Amount</Table.Th>
            <Table.Th>Status</Table.Th>
            <Table.Th>Created at</Table.Th>
          </Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {payments.map((payment) => (
            <Table.Tr key={payment.id}>
              <Table.Td>
                <Anchor component={Link} to={`/payments/${payment.id}`} ff="monospace" size="sm">
                  {payment.id}
                </Anchor>
              </Table.Td>
              <Table.Td>{payment.merchantId}</Table.Td>
              <Table.Td ta="right">{formatAmount(payment.amount, payment.currency)}</Table.Td>
              <Table.Td>
                <PaymentStatusBadge status={payment.status} />
              </Table.Td>
              <Table.Td>{formatDateTime(payment.createdAt)}</Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
    </Table.ScrollContainer>
  );
}
