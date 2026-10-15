import { Anchor, Button, Group, Stack, Text, Title } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { notifications } from '@mantine/notifications';
import { Link, useParams } from 'react-router';
import { getErrorMessage, isNotFoundError } from '@/api/apiError';
import { ErrorState } from '@/shared/components/ErrorState';
import { LoadingState } from '@/shared/components/LoadingState';
import { formatAmount } from '@/shared/lib/format';
import { PaymentDetails } from '../components/PaymentDetails';
import { RefundPaymentModal } from '../components/RefundPaymentModal';
import { usePayment } from '../hooks/usePayment';
import { useRefundPayment } from '../hooks/useRefundPayment';
import { isRefundable } from '../paymentStatus';

export function PaymentDetailPage() {
  const { paymentId = '' } = useParams();
  const paymentQuery = usePayment(paymentId);
  const refundPayment = useRefundPayment();
  const [isRefundModalOpen, refundModal] = useDisclosure(false);

  const confirmRefund = () => {
    refundPayment.mutate(paymentId, {
      onSuccess: () => {
        notifications.show({
          color: 'green',
          title: 'Payment refunded',
          message: 'The full amount was refunded.',
        });
      },
      onError: (error) => {
        notifications.show({
          color: 'red',
          title: 'Could not refund the payment',
          message: getErrorMessage(error),
        });
      },
      onSettled: refundModal.close,
    });
  };

  return (
    <Stack gap="lg">
      <Anchor component={Link} to="/payments" size="sm">
        Back to payments
      </Anchor>
      <Group justify="space-between">
        <Title order={1}>Payment details</Title>
        {paymentQuery.data && isRefundable(paymentQuery.data.status) && (
          <Button color="red" variant="light" onClick={refundModal.open}>
            Refund
          </Button>
        )}
      </Group>
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
        <>
          <PaymentDetails payment={paymentQuery.data} />
          <RefundPaymentModal
            opened={isRefundModalOpen}
            formattedAmount={formatAmount(paymentQuery.data.amount, paymentQuery.data.currency)}
            isRefunding={refundPayment.isPending}
            onConfirm={confirmRefund}
            onClose={refundModal.close}
          />
        </>
      )}
    </Stack>
  );
}
