import { formatAmount, formatDateTime } from './format';

describe('formatAmount', () => {
  it('given an amount and a currency, when formatted, then returns the amount with the currency symbol and two decimals', () => {
    expect(formatAmount(1234.5, 'USD')).toBe('$1,234.50');
  });

  it('given a currency without minor units, when formatted, then still shows two decimals', () => {
    expect(formatAmount(1500, 'CLP')).toMatch(/^CLP\s1,500\.00$/);
  });
});

describe('formatDateTime', () => {
  it('given an ISO instant, when formatted, then returns the medium date and short time', () => {
    expect(formatDateTime('2026-10-15T13:05:00Z')).toMatch(/^Oct 15, 2026, 1:05\sPM$/);
  });
});
