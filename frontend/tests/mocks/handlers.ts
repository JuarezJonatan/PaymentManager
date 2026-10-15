import { http, HttpResponse, type RequestHandler } from 'msw';
import { aPage, aPayment } from './payments';

export const handlers: RequestHandler[] = [
  http.get('/api/payments', () => HttpResponse.json(aPage([aPayment()]))),
  http.get('/api/payments/:paymentId', ({ params }) =>
    HttpResponse.json(aPayment({ id: String(params.paymentId) })),
  ),
];
