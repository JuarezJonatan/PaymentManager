import { delay, http, HttpResponse } from 'msw';
import { aPayment } from '../../../../tests/mocks/payments';
import { server } from '../../../../tests/mocks/server';
import { renderApp, screen, within } from '../../../../tests/render';

function captureCreateRequests(): unknown[] {
  const bodies: unknown[] = [];
  server.events.on('request:start', async ({ request }) => {
    if (request.method === 'POST' && new URL(request.url).pathname === '/api/payments') {
      bodies.push(await request.clone().json());
    }
  });
  return bodies;
}

async function fillValidForm(user: ReturnType<typeof renderApp>['user']) {
  await user.type(await screen.findByRole('textbox', { name: 'Merchant ID' }), 'merchant-001');
  await user.type(screen.getByRole('textbox', { name: 'Amount' }), '1500.5');
}

describe('CreatePaymentPage', () => {
  afterEach(() => {
    server.events.removeAllListeners();
  });

  it('given the payments list, when the user clicks new payment, then shows the create form', async () => {
    const { user, router } = renderApp('/payments');

    await user.click(await screen.findByRole('link', { name: 'New payment' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'New payment' }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments/new');
  });

  it('given valid values, when the form is submitted, then sends the payment and opens its detail page', async () => {
    const requests = captureCreateRequests();
    server.use(
      http.post('/api/payments', () =>
        HttpResponse.json(aPayment({ id: 'pay-new' }), { status: 201 }),
      ),
    );
    const { user, router } = renderApp('/payments/new');
    await user.type(await screen.findByRole('textbox', { name: 'Merchant ID' }), '  merchant-001 ');
    await user.type(screen.getByRole('textbox', { name: 'Amount' }), '1500.5');
    await user.click(screen.getByRole('combobox', { name: 'Currency' }));
    await user.click(await screen.findByRole('option', { name: 'USD' }));
    await user.click(screen.getByRole('radio', { name: 'Rejected' }));

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Payment details' }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments/pay-new');
    expect(requests).toEqual([
      { merchantId: 'merchant-001', amount: 1500.5, currency: 'USD', simulationResult: 'REJECTED' },
    ]);
  });

  it('given an approved payment, when it is created, then notifies the approval', async () => {
    const { user } = renderApp('/payments/new');
    await fillValidForm(user);

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    expect(await screen.findByText('Payment approved')).toBeInTheDocument();
  });

  it('given a rejected simulation, when the payment is created, then notifies the rejection', async () => {
    const { user } = renderApp('/payments/new');
    await fillValidForm(user);
    await user.click(screen.getByRole('radio', { name: 'Rejected' }));

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    expect(await screen.findByText('Payment rejected')).toBeInTheDocument();
  });

  it('given empty required fields, when the form is submitted, then shows validation errors and sends no request', async () => {
    const requests = captureCreateRequests();
    const { user, router } = renderApp('/payments/new');

    await user.click(await screen.findByRole('button', { name: 'Create payment' }));

    expect(await screen.findByText('Merchant ID is required')).toBeInTheDocument();
    expect(screen.getByText('Amount is required')).toBeInTheDocument();
    expect(requests).toHaveLength(0);
    expect(router.state.location.pathname).toBe('/payments/new');
  });

  it('given an amount of zero, when the form is submitted, then shows the amount error and sends no request', async () => {
    const requests = captureCreateRequests();
    const { user } = renderApp('/payments/new');
    await user.type(await screen.findByRole('textbox', { name: 'Merchant ID' }), 'merchant-001');
    await user.type(screen.getByRole('textbox', { name: 'Amount' }), '0');

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    expect(await screen.findByText('Amount must be greater than zero')).toBeInTheDocument();
    expect(requests).toHaveLength(0);
  });

  it('given the server rejects a field, when the form is submitted, then shows the error next to the field and keeps the form', async () => {
    server.use(
      http.post('/api/payments', () =>
        HttpResponse.json(
          {
            title: 'Validation failed',
            detail: 'Request has invalid fields',
            errors: { merchantId: 'merchantId must not be blank' },
          },
          { status: 400 },
        ),
      ),
    );
    const { user, router } = renderApp('/payments/new');
    await fillValidForm(user);

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    const merchantInput = screen.getByRole('textbox', { name: 'Merchant ID' });
    expect(await screen.findByText('merchantId must not be blank')).toBeInTheDocument();
    expect(merchantInput).toHaveAttribute('aria-invalid', 'true');
    expect(merchantInput).toHaveValue('merchant-001');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments/new');
  });

  it('given the server fails without field errors, when the form is submitted, then shows a form error and keeps the values', async () => {
    server.use(
      http.post('/api/payments', () =>
        HttpResponse.json({ detail: 'Database unavailable' }, { status: 500 }),
      ),
    );
    const { user, router } = renderApp('/payments/new');
    await fillValidForm(user);

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Database unavailable')).toBeInTheDocument();
    expect(screen.getByRole('textbox', { name: 'Amount' })).toHaveValue('1,500.5');
    expect(router.state.location.pathname).toBe('/payments/new');
  });

  it('given a pending request, when the form is submitted, then disables the submit button', async () => {
    server.use(
      http.post('/api/payments', async () => {
        await delay('infinite');
        return HttpResponse.json(aPayment(), { status: 201 });
      }),
    );
    const { user } = renderApp('/payments/new');
    await fillValidForm(user);

    await user.click(screen.getByRole('button', { name: 'Create payment' }));

    expect(await screen.findByRole('button', { name: 'Create payment' })).toBeDisabled();
  });

  it('given the create form, when the user clicks cancel, then returns to the list', async () => {
    const { user, router } = renderApp('/payments/new');

    await user.click(await screen.findByRole('button', { name: 'Cancel' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Payments' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments');
  });
});
