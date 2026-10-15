import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createPayment } from '@/api/payments';
import { paymentKeys } from '../queryKeys';

export function useCreatePayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: createPayment,
    onSuccess: (payment) => {
      queryClient.setQueryData(paymentKeys.detail(payment.id), payment);
      return queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
  });
}
