import { isPaymentStatus, type PaymentStatus } from '@/api/payments';

export type PaymentListFilters = {
  readonly status: PaymentStatus | undefined;
  readonly page: number;
};

const FIRST_PAGE = 1;

function parsePage(value: string | null): number {
  const page = Number(value);
  return Number.isInteger(page) && page >= FIRST_PAGE ? page : FIRST_PAGE;
}

export function parsePaymentListSearchParams(searchParams: URLSearchParams): PaymentListFilters {
  const status = searchParams.get('status');
  return {
    status: isPaymentStatus(status) ? status : undefined,
    page: parsePage(searchParams.get('page')),
  };
}

export function toPaymentListSearchParams({ status, page }: PaymentListFilters): URLSearchParams {
  const searchParams = new URLSearchParams();
  if (status !== undefined) {
    searchParams.set('status', status);
  }
  if (page > FIRST_PAGE) {
    searchParams.set('page', String(page));
  }
  return searchParams;
}
