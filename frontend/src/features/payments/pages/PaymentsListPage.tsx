import { Group, Pagination, Stack, Text, Title } from '@mantine/core';
import { useSearchParams } from 'react-router';
import { getErrorMessage } from '@/api/apiError';
import type { PaymentStatus } from '@/api/payments';
import { ErrorState } from '@/shared/components/ErrorState';
import { LoadingState } from '@/shared/components/LoadingState';
import { PaymentStatusFilter } from '../components/PaymentStatusFilter';
import { PaymentsTable } from '../components/PaymentsTable';
import { usePayments } from '../hooks/usePayments';
import {
  parsePaymentListSearchParams,
  toPaymentListSearchParams,
  type PaymentListFilters,
} from '../paymentListSearchParams';

const PAGE_SIZE = 20;

export function PaymentsListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = parsePaymentListSearchParams(searchParams);
  const paymentsQuery = usePayments({
    status: filters.status,
    page: filters.page - 1,
    size: PAGE_SIZE,
  });

  const updateFilters = (nextFilters: PaymentListFilters) => {
    setSearchParams(toPaymentListSearchParams(nextFilters));
  };

  const changeStatus = (status: PaymentStatus | undefined) => {
    updateFilters({ status, page: 1 });
  };

  const changePage = (page: number) => {
    updateFilters({ ...filters, page });
  };

  return (
    <Stack gap="lg">
      <Title order={1}>Payments</Title>
      <PaymentStatusFilter value={filters.status} onChange={changeStatus} />
      {paymentsQuery.isPending ? (
        <LoadingState label="Loading payments" />
      ) : paymentsQuery.isError ? (
        <ErrorState
          title="Could not load payments"
          message={getErrorMessage(paymentsQuery.error)}
          onRetry={() => void paymentsQuery.refetch()}
        />
      ) : paymentsQuery.data.content.length === 0 ? (
        <Text c="dimmed">No payments found.</Text>
      ) : (
        <Stack gap="md">
          <PaymentsTable payments={paymentsQuery.data.content} />
          <Group justify="space-between">
            <Text size="sm" c="dimmed">
              {paymentsQuery.data.totalElements} payments
            </Text>
            {paymentsQuery.data.totalPages > 1 && (
              <Pagination
                total={paymentsQuery.data.totalPages}
                value={filters.page}
                onChange={changePage}
              />
            )}
          </Group>
        </Stack>
      )}
    </Stack>
  );
}
