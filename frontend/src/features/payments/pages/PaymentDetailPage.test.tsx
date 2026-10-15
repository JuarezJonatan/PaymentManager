import { http, HttpResponse } from 'msw';
import { aPayment } from '../../../../tests/mocks/payments';
import { server } from '../../../../tests/mocks/server';
import { renderApp, screen, within } from '../../../../tests/render';

const PAYMENT_ID = 'pay-42';

describe('PaymentDetailPage', () => {
  it('given an existing payment, when the page loads, then shows its details', async () => {
    server.use(
      http.get('/api/payments/:paymentId', () =>
        HttpResponse.json(
          aPayment({
            id: PAYMENT_ID,
            merchantId: 'merchant-x',
            amount: 99.9,
            currency: 'EUR',
            status: 'REJECTED',
          }),
        ),
      ),
    );

    renderApp(`/payments/${PAYMENT_ID}`);

    expect(await screen.findByText(PAYMENT_ID)).toBeInTheDocument();
    expect(screen.getByRole('row', { name: 'Merchant merchant-x' })).toBeInTheDocument();
    expect(screen.getByRole('row', { name: 'Amount €99.90' })).toBeInTheDocument();
    expect(screen.getByRole('row', { name: 'Currency EUR' })).toBeInTheDocument();
    expect(screen.getByRole('row', { name: 'Status Rejected' })).toBeInTheDocument();
  });

  it('given an unknown payment id, when the page loads, then shows the not found state and no details', async () => {
    server.use(
      http.get('/api/payments/:paymentId', () =>
        HttpResponse.json(
          { title: 'Resource not found', detail: `Payment with id '${PAYMENT_ID}' was not found` },
          { status: 404 },
        ),
      ),
    );

    renderApp(`/payments/${PAYMENT_ID}`);

    expect(await screen.findByText('Payment not found.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Retry' })).not.toBeInTheDocument();
  });

  it('given the server fails, when the page loads, then shows the error detail with a retry action', async () => {
    server.use(
      http.get('/api/payments/:paymentId', () =>
        HttpResponse.json({ detail: 'Database unavailable' }, { status: 500 }),
      ),
    );

    renderApp(`/payments/${PAYMENT_ID}`);

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Database unavailable')).toBeInTheDocument();
    expect(within(alert).getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('given the detail page, when the user clicks back to payments, then navigates to the list', async () => {
    const { user, router } = renderApp(`/payments/${PAYMENT_ID}`);

    await user.click(await screen.findByRole('link', { name: 'Back to payments' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Payments' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/payments');
  });
});
