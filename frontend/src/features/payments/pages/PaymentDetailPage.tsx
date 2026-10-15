import { Anchor, Stack, Text, Title } from '@mantine/core';
import { Link, useParams } from 'react-router';
import { getErrorMessage, isNotFoundError } from '@/api/apiError';
import { ErrorState } from '@/shared/components/ErrorState';
import { LoadingState } from '@/shared/components/LoadingState';
import { PaymentDetails } from '../components/PaymentDetails';
import { usePayment } from '../hooks/usePayment';

export function PaymentDetailPage() {
  const { paymentId = '' } = useParams();
  const paymentQuery = usePayment(paymentId);

  return (
    <Stack gap="lg">
      <Anchor component={Link} to="/payments" size="sm">
        Back to payments
      </Anchor>
      <Title order={1}>Payment details</Title>
      {paymentQuery.isPending ? (
        <LoadingState label="Loading payment" />
      ) : isNotFoundError(paymentQuery.error) ? (
        <Text c="dimmed">Payment not found.</Text>
      ) : paymentQuery.isError ? (
        <ErrorState
          title="Could not load the payment"
          message={getErrorMessage(paymentQuery.error)}
          onRetry={() => void paymentQuery.refetch()}
        />
      ) : (
        <PaymentDetails payment={paymentQuery.data} />
      )}
    </Stack>
  );
}
