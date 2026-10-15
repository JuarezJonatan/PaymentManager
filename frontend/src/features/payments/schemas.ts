import { z } from 'zod';
import { CURRENCIES, SIMULATION_RESULTS } from '@/api/payments';

const MAX_MERCHANT_ID_LENGTH = 100;

export const createPaymentSchema = z.object({
  merchantId: z
    .string()
    .trim()
    .min(1, 'Merchant ID is required')
    .max(
      MAX_MERCHANT_ID_LENGTH,
      `Merchant ID must be at most ${MAX_MERCHANT_ID_LENGTH} characters`,
    ),
  amount: z
    .number({
      error: (issue) =>
        issue.input === undefined ? 'Amount is required' : 'Amount must be a number',
    })
    .positive('Amount must be greater than zero')
    .multipleOf(0.01, 'Amount must have at most 2 decimals'),
  currency: z.enum(CURRENCIES, { error: 'Currency is required' }),
  simulationResult: z.enum(SIMULATION_RESULTS, { error: 'Simulation result is required' }),
});

export type CreatePaymentFormValues = z.infer<typeof createPaymentSchema>;
