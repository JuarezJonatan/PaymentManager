import { delay, http, HttpResponse } from 'msw';
import { aPayment } from '../../../../tests/mocks/payments';
import { server } from '../../../../tests/mocks/server';
import { renderApp, screen, waitFor, within } from '../../../../tests/render';

const PAYMENT_ID = 'pay-42';

function givenPaymentWithStatus(status: 'APPROVED' | 'REJECTED' | 'REFUNDED') {
  server.use(
    http.get('/api/payments/:paymentId', () =>
      HttpResponse.json(aPayment({ id: PAYMENT_ID, amount: 250, currency: 'USD', status })),
    ),
  );
}

function captureRefundRequests(): string[] {
  const paths: string[] = [];
  server.events.on('request:start', ({ request }) => {
    if (request.method === 'POST') {
      paths.push(new URL(request.url).pathname);
    }
  });
  return paths;
}

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

  describe('refund', () => {
    afterEach(() => {
      server.events.removeAllListeners();
    });

    it('given an approved payment, when the page loads, then offers the refund action', async () => {
      givenPaymentWithStatus('APPROVED');

      renderApp(`/payments/${PAYMENT_ID}`);

      expect(await screen.findByRole('button', { name: 'Refund' })).toBeInTheDocument();
    });

    it.each(['REJECTED', 'REFUNDED'] as const)(
      'given a %s payment, when the page loads, then does not offer the refund action',
      async (status) => {
        givenPaymentWithStatus(status);

        renderApp(`/payments/${PAYMENT_ID}`);

        expect(await screen.findByText(PAYMENT_ID)).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Refund' })).not.toBeInTheDocument();
      },
    );

    it('given an approved payment, when the user clicks refund, then asks for confirmation showing the amount', async () => {
      givenPaymentWithStatus('APPROVED');
      const { user } = renderApp(`/payments/${PAYMENT_ID}`);

      await user.click(await screen.findByRole('button', { name: 'Refund' }));

      const dialog = await screen.findByRole('dialog', { name: 'Refund payment' });
      expect(
        within(dialog).getByText(/The full amount of \$250\.00 will be refunded/),
      ).toBeInTheDocument();
    });

    it('given the refund confirmation, when the user cancels, then sends no request and keeps the payment approved', async () => {
      givenPaymentWithStatus('APPROVED');
      const requests = captureRefundRequests();
      const { user } = renderApp(`/payments/${PAYMENT_ID}`);
      await user.click(await screen.findByRole('button', { name: 'Refund' }));

      await user.click(await screen.findByRole('button', { name: 'Cancel' }));

      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(requests).toHaveLength(0);
      expect(screen.getByRole('row', { name: 'Status Approved' })).toBeInTheDocument();
    });

    it('given the refund confirmation, when the user confirms, then refunds the payment and notifies it', async () => {
      givenPaymentWithStatus('APPROVED');
      const requests = captureRefundRequests();
      const { user } = renderApp(`/payments/${PAYMENT_ID}`);
      await user.click(await screen.findByRole('button', { name: 'Refund' }));

      await user.click(await screen.findByRole('button', { name: 'Confirm refund' }));

      expect(await screen.findByText('Payment refunded')).toBeInTheDocument();
      expect(screen.getByRole('row', { name: 'Status Refunded' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Refund' })).not.toBeInTheDocument();
      expect(requests).toEqual([`/api/payments/${PAYMENT_ID}/refund`]);
    });

    it('given the payment can no longer be refunded, when the user confirms, then shows the error and keeps the payment approved', async () => {
      givenPaymentWithStatus('APPROVED');
      server.use(
        http.post('/api/payments/:paymentId/refund', () =>
          HttpResponse.json(
            {
              title: 'Payment cannot be refunded',
              detail: `Payment with id '${PAYMENT_ID}' cannot be refunded because its status is REJECTED`,
            },
            { status: 409 },
          ),
        ),
      );
      const { user } = renderApp(`/payments/${PAYMENT_ID}`);
      await user.click(await screen.findByRole('button', { name: 'Refund' }));

      await user.click(await screen.findByRole('button', { name: 'Confirm refund' }));

      expect(await screen.findByText('Could not refund the payment')).toBeInTheDocument();
      expect(
        screen.getByText(/cannot be refunded because its status is REJECTED/),
      ).toBeInTheDocument();
      await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
      expect(screen.getByRole('row', { name: 'Status Approved' })).toBeInTheDocument();
    });

    it('given a pending refund, when the user confirms, then disables the confirm button', async () => {
      givenPaymentWithStatus('APPROVED');
      server.use(
        http.post('/api/payments/:paymentId/refund', async () => {
          await delay('infinite');
          return HttpResponse.json(aPayment());
        }),
      );
      const { user } = renderApp(`/payments/${PAYMENT_ID}`);
      await user.click(await screen.findByRole('button', { name: 'Refund' }));

      await user.click(await screen.findByRole('button', { name: 'Confirm refund' }));

      expect(await screen.findByRole('button', { name: 'Confirm refund' })).toBeDisabled();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
    });
  });
});
