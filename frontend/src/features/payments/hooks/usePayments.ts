import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listPayments, type ListPaymentsParams } from '@/api/payments';
import { paymentKeys } from '../queryKeys';

export function usePayments(params: ListPaymentsParams) {
  return useQuery({
    queryKey: paymentKeys.list(params),
    queryFn: ({ signal }) => listPayments(params, signal),
    placeholderData: keepPreviousData,
  });
}
