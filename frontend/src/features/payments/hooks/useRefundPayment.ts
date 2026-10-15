import { useMutation, useQueryClient } from '@tanstack/react-query';
import { refundPayment } from '@/api/payments';
import { paymentKeys } from '../queryKeys';

export function useRefundPayment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: refundPayment,
    onSuccess: (payment) => {
      queryClient.setQueryData(paymentKeys.detail(payment.id), payment);
      return queryClient.invalidateQueries({ queryKey: paymentKeys.lists() });
    },
    onError: (_error, id) => queryClient.invalidateQueries({ queryKey: paymentKeys.detail(id) }),
  });
}
