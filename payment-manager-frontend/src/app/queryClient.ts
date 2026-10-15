import { QueryClient } from '@tanstack/react-query';
import { isApiError } from '@/api/apiError';

const MAX_QUERY_RETRIES = 2;
const DEFAULT_STALE_TIME_MS = 30_000;

export function shouldRetryQuery(failureCount: number, error: unknown): boolean {
  if (failureCount >= MAX_QUERY_RETRIES) {
    return false;
  }
  if (isApiError(error) && error.status !== null && error.status < 500) {
    return false;
  }
  return true;
}

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: shouldRetryQuery,
        staleTime: DEFAULT_STALE_TIME_MS,
        refetchOnWindowFocus: false,
      },
      mutations: {
        retry: false,
      },
    },
  });
}
