import { ApiError } from './apiError';
import { toApiError } from './problemDetail';

export const API_BASE_PATH = '/api';

const NETWORK_ERROR_TITLE = 'Network error';
const NETWORK_ERROR_DETAIL = 'Could not reach the server. Check your connection and try again.';

type RequestOptions = Omit<RequestInit, 'body'> & {
  readonly body?: unknown;
};

async function readBody(response: Response): Promise<unknown> {
  const text = await response.text();
  if (text.length === 0) {
    return undefined;
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, headers, ...init } = options;
  const hasBody = body !== undefined;

  let response: Response;
  try {
    response = await fetch(`${API_BASE_PATH}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(hasBody ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
      body: hasBody ? JSON.stringify(body) : undefined,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw new ApiError({
      status: null,
      title: NETWORK_ERROR_TITLE,
      detail: NETWORK_ERROR_DETAIL,
      cause: error,
    });
  }

  const responseBody = await readBody(response);

  if (!response.ok) {
    throw toApiError(response.status, response.statusText, responseBody);
  }

  return responseBody as T;
}
