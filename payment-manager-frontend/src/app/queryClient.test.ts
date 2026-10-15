import { ApiError } from '@/api/apiError';
import { shouldRetryQuery } from './queryClient';

function apiErrorWithStatus(status: number | null): ApiError {
  return new ApiError({ status, title: 'Error', detail: 'Something failed' });
}

describe('shouldRetryQuery', () => {
  it('given a client error, when deciding whether to retry, then does not retry', () => {
    const retry = shouldRetryQuery(0, apiErrorWithStatus(404));

    expect(retry).toBe(false);
  });

  it('given a server error on the first failure, when deciding whether to retry, then retries', () => {
    const retry = shouldRetryQuery(0, apiErrorWithStatus(503));

    expect(retry).toBe(true);
  });

  it('given a network error on the first failure, when deciding whether to retry, then retries', () => {
    const retry = shouldRetryQuery(0, apiErrorWithStatus(null));

    expect(retry).toBe(true);
  });

  it('given the retry limit was reached, when deciding whether to retry, then does not retry', () => {
    const retry = shouldRetryQuery(2, apiErrorWithStatus(503));

    expect(retry).toBe(false);
  });
});
