import { http, HttpResponse } from 'msw';
import { aPage, aPayment } from '../../../../tests/mocks/payments';
import { server } from '../../../../tests/mocks/server';
import { renderApp, screen, within } from '../../../../tests/render';

function captureListRequests(responsePage = aPage([aPayment()])): URLSearchParams[] {
  const requests: URLSearchParams[] = [];
  server.use(
    http.get('/api/payments', ({ request }) => {
      requests.push(new URL(request.url).searchParams);
      return HttpResponse.json(responsePage);
    }),
  );
  return requests;
}

describe('PaymentsListPage', () => {
  it('given existing payments, when the page loads, then shows each payment in the table', async () => {
    server.use(
      http.get('/api/payments', () =>
        HttpResponse.json(
          aPage([
            aPayment({ id: 'pay-1', merchantId: 'merchant-a', amount: 10, status: 'APPROVED' }),
            aPayment({ id: 'pay-2', merchantId: 'merchant-b', amount: 20, status: 'REFUNDED' }),
          ]),
        ),
      ),
    );

    renderApp('/payments');

    const firstRow = await screen.findByRole('row', { name: /pay-1/ });
    const secondRow = screen.getByRole('row', { name: /pay-2/ });
    expect(within(firstRow).getByRole('link', { name: 'pay-1' })).toBeInTheDocument();
    expect(within(firstRow).getByText('$10.00')).toBeInTheDocument();
    expect(within(secondRow).getByText('merchant-b')).toBeInTheDocument();
    expect(within(secondRow).getByText('Refunded')).toBeInTheDocument();
    expect(screen.getByText('2 payments')).toBeInTheDocument();
  });

  it('given no payments, when the page loads, then shows the empty state', async () => {
    server.use(http.get('/api/payments', () => HttpResponse.json(aPage([]))));

    renderApp('/payments');

    expect(await screen.findByText('No payments found.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('given the server fails, when the page loads, then shows the error detail and no table', async () => {
    server.use(
      http.get('/api/payments', () =>
        HttpResponse.json(
          { title: 'Internal Server Error', detail: 'Database unavailable' },
          { status: 500 },
        ),
      ),
    );

    renderApp('/payments');

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('Could not load payments')).toBeInTheDocument();
    expect(within(alert).getByText('Database unavailable')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('given a failed load, when the user clicks retry and the server recovers, then shows the payments', async () => {
    server.use(
      http.get(
        '/api/payments',
        () => HttpResponse.json({ detail: 'Database unavailable' }, { status: 500 }),
        {
          once: true,
        },
      ),
    );
    const { user } = renderApp('/payments');

    await user.click(await screen.findByRole('button', { name: 'Retry' }));

    expect(await screen.findByRole('table')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('given filters in the url, when the page loads, then requests the matching status and zero-based page', async () => {
    const requests = captureListRequests(aPage([aPayment()], { page: 1, totalPages: 3 }));

    renderApp('/payments?status=REJECTED&page=2');

    expect(await screen.findByRole('table')).toBeInTheDocument();
    expect(requests[0]?.get('status')).toBe('REJECTED');
    expect(requests[0]?.get('page')).toBe('1');
    expect(requests[0]?.get('size')).toBe('20');
    expect(screen.getByRole('radio', { name: 'Rejected' })).toBeChecked();
  });

  it('given a page after the first, when the user selects a status, then filters by it and resets to the first page', async () => {
    const requests = captureListRequests(aPage([aPayment()], { totalPages: 3 }));
    const { user, router } = renderApp('/payments?page=3');
    await screen.findByRole('table');

    await user.click(screen.getByRole('radio', { name: 'Approved' }));

    expect(router.state.location.search).toBe('?status=APPROVED');
    await vi.waitFor(() => expect(requests.at(-1)?.get('status')).toBe('APPROVED'));
    expect(requests.at(-1)?.get('page')).toBe('0');
  });

  it('given several pages, when the user clicks the next page number, then requests that page and stores it in the url', async () => {
    const requests = captureListRequests(aPage([aPayment()], { totalElements: 45, totalPages: 3 }));
    const { user, router } = renderApp('/payments');
    await screen.findByRole('table');

    await user.click(screen.getByRole('button', { name: '2' }));

    expect(router.state.location.search).toBe('?page=2');
    await vi.waitFor(() => expect(requests.at(-1)?.get('page')).toBe('1'));
  });

  it('given a single page, when the page loads, then hides the pagination', async () => {
    renderApp('/payments');

    await screen.findByRole('table');

    expect(screen.queryByRole('button', { name: '1' })).not.toBeInTheDocument();
  });

  it('given a payment in the table, when the user clicks its id, then navigates to its detail page', async () => {
    const { user, router } = renderApp('/payments');

    await user.click(await screen.findByRole('link', { name: aPayment().id }));

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Payment details' }),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/payments/${aPayment().id}`);
  });
});
