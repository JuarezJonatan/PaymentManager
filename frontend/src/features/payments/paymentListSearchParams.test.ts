import { parsePaymentListSearchParams, toPaymentListSearchParams } from './paymentListSearchParams';

describe('parsePaymentListSearchParams', () => {
  it('given a valid status and page, when parsed, then returns them as filters', () => {
    const filters = parsePaymentListSearchParams(new URLSearchParams('status=REFUNDED&page=3'));

    expect(filters).toEqual({ status: 'REFUNDED', page: 3 });
  });

  it('given no params, when parsed, then returns no status and the first page', () => {
    const filters = parsePaymentListSearchParams(new URLSearchParams());

    expect(filters).toEqual({ status: undefined, page: 1 });
  });

  it('given an unknown status, when parsed, then ignores the status', () => {
    const filters = parsePaymentListSearchParams(new URLSearchParams('status=PENDING'));

    expect(filters.status).toBeUndefined();
  });

  it.each(['0', '-1', '1.5', 'abc'])(
    'given the invalid page "%s", when parsed, then falls back to the first page',
    (page) => {
      const filters = parsePaymentListSearchParams(new URLSearchParams({ page }));

      expect(filters.page).toBe(1);
    },
  );
});

describe('toPaymentListSearchParams', () => {
  it('given a status and a page after the first, when serialized, then includes both', () => {
    const searchParams = toPaymentListSearchParams({ status: 'APPROVED', page: 2 });

    expect(searchParams.toString()).toBe('status=APPROVED&page=2');
  });

  it('given no status and the first page, when serialized, then returns empty params', () => {
    const searchParams = toPaymentListSearchParams({ status: undefined, page: 1 });

    expect(searchParams.toString()).toBe('');
  });
});
