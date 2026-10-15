import { Anchor, Card, Stack, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { Link, useNavigate } from 'react-router';
import { getErrorMessage, isApiError } from '@/api/apiError';
import type { Payment } from '@/api/payments';
import { CreatePaymentForm } from '../components/CreatePaymentForm';
import { useCreatePayment } from '../hooks/useCreatePayment';
import type { CreatePaymentFormValues } from '../schemas';

function notifyCreated(payment: Payment) {
  if (payment.status === 'APPROVED') {
    notifications.show({
      color: 'green',
      title: 'Payment approved',
      message: 'The payment was created and approved.',
    });
  } else {
    notifications.show({
      color: 'orange',
      title: 'Payment rejected',
      message: 'The payment was created, but the authorization was rejected.',
    });
  }
}

export function CreatePaymentPage() {
  const navigate = useNavigate();
  const createPayment = useCreatePayment();
  const { error } = createPayment;

  const submit = (values: CreatePaymentFormValues) => {
    createPayment.mutate(values, {
      onSuccess: (payment) => {
        notifyCreated(payment);
        void navigate(`/payments/${payment.id}`);
      },
    });
  };

  return (
    <Stack gap="lg" maw={640}>
      <Anchor component={Link} to="/payments" size="sm">
        Back to payments
      </Anchor>
      <Title order={1}>New payment</Title>
      <Card withBorder padding="lg">
        <CreatePaymentForm
          isSubmitting={createPayment.isPending}
          serverErrorMessage={error ? getErrorMessage(error) : undefined}
          serverFieldErrors={isApiError(error) ? error.fieldErrors : undefined}
          onSubmit={submit}
          onCancel={() => void navigate('/payments')}
        />
      </Card>
    </Stack>
  );
}
