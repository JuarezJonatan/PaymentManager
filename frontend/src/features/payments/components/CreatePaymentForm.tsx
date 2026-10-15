import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Button, Group, NumberInput, Radio, Select, Stack, TextInput } from '@mantine/core';
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { CURRENCIES, SIMULATION_RESULTS } from '@/api/payments';
import { PAYMENT_STATUS_LABELS } from '../paymentStatus';
import { createPaymentSchema, type CreatePaymentFormValues } from '../schemas';

const FORM_FIELDS = ['merchantId', 'amount', 'currency', 'simulationResult'] as const;

type CreatePaymentFormProps = {
  readonly isSubmitting: boolean;
  readonly serverErrorMessage?: string;
  readonly serverFieldErrors?: Readonly<Record<string, string>>;
  readonly onSubmit: (values: CreatePaymentFormValues) => void;
  readonly onCancel: () => void;
};

export function CreatePaymentForm({
  isSubmitting,
  serverErrorMessage,
  serverFieldErrors,
  onSubmit,
  onCancel,
}: CreatePaymentFormProps) {
  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CreatePaymentFormValues>({
    resolver: zodResolver(createPaymentSchema),
    defaultValues: { merchantId: '', currency: 'ARS', simulationResult: 'APPROVED' },
  });

  useEffect(() => {
    if (serverErrorMessage === undefined) {
      return;
    }
    const matchingFields = FORM_FIELDS.filter((field) => serverFieldErrors?.[field] !== undefined);
    matchingFields.forEach((field) => {
      setError(field, { type: 'server', message: serverFieldErrors?.[field] });
    });
    if (matchingFields.length === 0) {
      setError('root.server', { type: 'server', message: serverErrorMessage });
    }
  }, [serverErrorMessage, serverFieldErrors, setError]);

  return (
    <form noValidate onSubmit={(event) => void handleSubmit(onSubmit)(event)}>
      <Stack gap="md">
        {errors.root?.server && (
          <Alert color="red" title="Could not create the payment" role="alert">
            {errors.root.server.message}
          </Alert>
        )}
        <TextInput
          label="Merchant ID"
          withAsterisk
          error={errors.merchantId?.message}
          {...register('merchantId')}
        />
        <Group grow align="flex-start">
          <Controller
            control={control}
            name="amount"
            render={({ field }) => (
              <NumberInput
                label="Amount"
                withAsterisk
                hideControls
                decimalScale={2}
                thousandSeparator=","
                name={field.name}
                ref={field.ref}
                value={field.value ?? ''}
                onBlur={field.onBlur}
                onChange={(value) => field.onChange(value === '' ? undefined : value)}
                error={errors.amount?.message}
              />
            )}
          />
          <Controller
            control={control}
            name="currency"
            render={({ field }) => (
              <Select
                label="Currency"
                withAsterisk
                data={CURRENCIES}
                allowDeselect={false}
                name={field.name}
                ref={field.ref}
                value={field.value}
                onBlur={field.onBlur}
                onChange={field.onChange}
                error={errors.currency?.message}
              />
            )}
          />
        </Group>
        <Controller
          control={control}
          name="simulationResult"
          render={({ field }) => (
            <Radio.Group
              label="Simulated authorization result"
              withAsterisk
              name={field.name}
              value={field.value}
              onChange={field.onChange}
              error={errors.simulationResult?.message}
            >
              <Group mt="xs">
                {SIMULATION_RESULTS.map((result) => (
                  <Radio key={result} value={result} label={PAYMENT_STATUS_LABELS[result]} />
                ))}
              </Group>
            </Radio.Group>
          )}
        />
        <Group justify="flex-end">
          <Button variant="default" onClick={onCancel} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button type="submit" loading={isSubmitting}>
            Create payment
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
