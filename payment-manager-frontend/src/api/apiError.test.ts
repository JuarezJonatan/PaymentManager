import { ApiError, getErrorMessage, isNotFoundError } from './apiError';

function anApiError(status: number | null): ApiError {
  return new ApiError({ status, title: 'Not Found', detail: 'Payment was not found' });
}

describe('getErrorMessage', () => {
  it('given an api error, when the message is read, then returns its detail', () => {
    expect(getErrorMessage(anApiError(404))).toBe('Payment was not found');
  });

  it('given an unknown error, when the message is read, then returns a generic message', () => {
    expect(getErrorMessage(new Error('boom'))).toBe('An unexpected error occurred');
  });
});

describe('isNotFoundError', () => {
  it('given an api error with status 404, when checked, then returns true', () => {
    expect(isNotFoundError(anApiError(404))).toBe(true);
  });

  it('given an api error with another status, when checked, then returns false', () => {
    expect(isNotFoundError(anApiError(500))).toBe(false);
  });
});
