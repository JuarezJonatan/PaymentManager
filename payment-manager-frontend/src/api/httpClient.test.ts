import { http, HttpResponse } from 'msw';
import { server } from '../../tests/mocks/server';
import { ApiError } from './apiError';
import { request } from './httpClient';

const RESOURCE_URL = '/api/resources';

async function captureError(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise;
  } catch (error) {
    if (error instanceof ApiError) {
      return error;
    }
    throw error;
  }
  throw new Error('Expected the request to fail');
}

describe('request', () => {
  it('given a successful response, when the request is sent, then returns the parsed JSON body', async () => {
    server.use(http.get(RESOURCE_URL, () => HttpResponse.json({ id: '1', name: 'first' })));

    const result = await request<{ id: string; name: string }>('/resources');

    expect(result).toEqual({ id: '1', name: 'first' });
  });

  it('given a request body, when the request is sent, then sends it as JSON', async () => {
    let receivedBody: unknown;
    let receivedContentType: string | null = null;
    server.use(
      http.post(RESOURCE_URL, async ({ request: received }) => {
        receivedBody = await received.json();
        receivedContentType = received.headers.get('Content-Type');
        return HttpResponse.json({ id: '1' }, { status: 201 });
      }),
    );

    await request('/resources', { method: 'POST', body: { name: 'first' } });

    expect(receivedBody).toEqual({ name: 'first' });
    expect(receivedContentType).toBe('application/json');
  });

  it('given an empty successful response, when the request is sent, then resolves with undefined', async () => {
    server.use(http.delete(`${RESOURCE_URL}/1`, () => new HttpResponse(null, { status: 204 })));

    const result = await request<undefined>('/resources/1', { method: 'DELETE' });

    expect(result).toBeUndefined();
  });

  it('given a problem detail response, when the request is sent, then rejects with an ApiError carrying its status, title and detail', async () => {
    server.use(
      http.get(`${RESOURCE_URL}/missing`, () =>
        HttpResponse.json(
          {
            status: 404,
            title: 'Resource not found',
            detail: "Resource with id 'missing' was not found",
          },
          { status: 404, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const error = await captureError(request('/resources/missing'));

    expect(error.status).toBe(404);
    expect(error.title).toBe('Resource not found');
    expect(error.detail).toBe("Resource with id 'missing' was not found");
    expect(error.isNetworkError).toBe(false);
  });

  it('given a validation problem detail, when the request is sent, then exposes the field errors', async () => {
    server.use(
      http.post(RESOURCE_URL, () =>
        HttpResponse.json(
          {
            status: 400,
            title: 'Validation failed',
            detail: 'Request has invalid fields',
            errors: { name: 'name must not be blank' },
          },
          { status: 400 },
        ),
      ),
    );

    const error = await captureError(request('/resources', { method: 'POST', body: {} }));

    expect(error.status).toBe(400);
    expect(error.fieldErrors).toEqual({ name: 'name must not be blank' });
  });

  it('given an error response without a JSON body, when the request is sent, then rejects with a generic ApiError', async () => {
    server.use(
      http.get(RESOURCE_URL, () =>
        HttpResponse.text('Bad Gateway', { status: 502, statusText: 'Bad Gateway' }),
      ),
    );

    const error = await captureError(request('/resources'));

    expect(error.status).toBe(502);
    expect(error.title).toBe('Bad Gateway');
    expect(error.detail).toBe('An unexpected error occurred');
    expect(error.fieldErrors).toEqual({});
  });

  it('given the server cannot be reached, when the request is sent, then rejects with a network ApiError', async () => {
    server.use(http.get(RESOURCE_URL, () => HttpResponse.error()));

    const error = await captureError(request('/resources'));

    expect(error.isNetworkError).toBe(true);
    expect(error.status).toBeNull();
    expect(error.title).toBe('Network error');
  });
});
