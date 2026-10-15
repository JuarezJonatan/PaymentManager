import { useQuery } from '@tanstack/react-query';
import { getPayment } from '@/api/payments';
import { paymentKeys } from '../queryKeys';

export function usePayment(id: string) {
  return useQuery({
    queryKey: paymentKeys.detail(id),
    queryFn: ({ signal }) => getPayment(id, signal),
  });
}
