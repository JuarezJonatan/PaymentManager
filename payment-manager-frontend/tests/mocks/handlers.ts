import { http, HttpResponse, type RequestHandler } from 'msw';
import type { CreatePaymentRequest } from '@/api/payments';
import { aPage, aPayment } from './payments';

export const handlers: RequestHandler[] = [
  http.get('/api/payments', () => HttpResponse.json(aPage([aPayment()]))),
  http.get('/api/payments/:paymentId', ({ params }) =>
    HttpResponse.json(aPayment({ id: String(params.paymentId) })),
  ),
  http.post('/api/payments', async ({ request }) => {
    const { simulationResult, ...payment } = (await request.json()) as CreatePaymentRequest;
    return HttpResponse.json(aPayment({ ...payment, status: simulationResult }), { status: 201 });
  }),
  http.post('/api/payments/:paymentId/refund', ({ params }) =>
    HttpResponse.json(aPayment({ id: String(params.paymentId), status: 'REFUNDED' })),
  ),
];
