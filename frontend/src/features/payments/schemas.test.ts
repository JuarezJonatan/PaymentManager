import { createPaymentSchema } from './schemas';

const validValues = {
  merchantId: 'merchant-001',
  amount: 1500.5,
  currency: 'ARS',
  simulationResult: 'APPROVED',
};

function errorMessagesOf(values: Record<string, unknown>): string[] {
  const result = createPaymentSchema.safeParse(values);
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('createPaymentSchema', () => {
  it('given valid values, when parsed, then succeeds with the merchant id trimmed', () => {
    const result = createPaymentSchema.parse({ ...validValues, merchantId: '  merchant-001  ' });

    expect(result.merchantId).toBe('merchant-001');
  });

  it('given a blank merchant id, when parsed, then reports it as required', () => {
    expect(errorMessagesOf({ ...validValues, merchantId: '   ' })).toEqual([
      'Merchant ID is required',
    ]);
  });

  it('given a merchant id longer than 100 characters, when parsed, then reports the maximum length', () => {
    expect(errorMessagesOf({ ...validValues, merchantId: 'm'.repeat(101) })).toEqual([
      'Merchant ID must be at most 100 characters',
    ]);
  });

  it('given a missing amount, when parsed, then reports it as required', () => {
    expect(errorMessagesOf({ ...validValues, amount: undefined })).toEqual(['Amount is required']);
  });

  it('given an incomplete amount, when parsed, then reports it must be a number', () => {
    expect(errorMessagesOf({ ...validValues, amount: '15.' })).toEqual(['Amount must be a number']);
  });

  it.each([0, -10])(
    'given the amount %d, when parsed, then reports it must be positive',
    (amount) => {
      expect(errorMessagesOf({ ...validValues, amount })).toEqual([
        'Amount must be greater than zero',
      ]);
    },
  );

  it('given an amount with three decimals, when parsed, then reports the decimal limit', () => {
    expect(errorMessagesOf({ ...validValues, amount: 10.123 })).toEqual([
      'Amount must have at most 2 decimals',
    ]);
  });

  it('given an unsupported currency, when parsed, then reports the currency as required', () => {
    expect(errorMessagesOf({ ...validValues, currency: 'JPY' })).toEqual(['Currency is required']);
  });
});
