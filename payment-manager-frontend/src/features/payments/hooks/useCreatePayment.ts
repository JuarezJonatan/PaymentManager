import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import { createPayment, type CreatePaymentRequest } from '@/api/payments';
import { paymentKeys } from '../queryKeys';

type CreatePaymentAttempt = {
  readonly payment: CreatePaymentRequest;
  readonly idempotencyKey: string;
};

function isSamePayment(first: CreatePaymentRequest, second: CreatePaymentRequest): boolean {
  return (
    first.merchantId === second.merchantId &&
    first.amount === second.amount &&
    first.currency === second.currency &&
    first.simulationResult === second.simulationResult
  );
}

export function useCreatePayment() {
  const queryClient = useQueryClient();
  const lastAttempt = useRef<CreatePaymentAttempt | null>(null);

  return useMutation({
    mutationFn: (payment: CreatePaymentRequest) => {
      const previous = lastAttempt.current;
      const idempotencyKey =
        previous && isSamePayment(previous.payment, payment)
          ? previous.idempotencyKey
          : crypto.randomUUID();
      lastAttempt.current = { payment, idempotencyKey };
      return createPayment(payment, idempotencyKey);
    },
    onSuccess: (payment) => {
      lastAttempt.current = null;
      queryClient.setQueryData(paymentKeys.detail(payment.id), payment);
      return queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
  });
}
