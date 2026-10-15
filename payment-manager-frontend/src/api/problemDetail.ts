import { ApiError, type FieldErrors } from './apiError';

const UNEXPECTED_ERROR_DETAIL = 'An unexpected error occurred';

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function readString(source: Record<string, unknown>, key: string): string | undefined {
  const value = source[key];
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

function readFieldErrors(source: Record<string, unknown>): FieldErrors {
  const errors = source.errors;
  if (!isRecord(errors)) {
    return {};
  }
  return Object.fromEntries(
    Object.entries(errors).filter(
      (entry): entry is [string, string] => typeof entry[1] === 'string',
    ),
  );
}

export function toApiError(status: number, statusText: string, body: unknown): ApiError {
  const fallbackTitle = statusText || `HTTP ${status}`;

  if (!isRecord(body)) {
    return new ApiError({ status, title: fallbackTitle, detail: UNEXPECTED_ERROR_DETAIL });
  }

  return new ApiError({
    status,
    title: readString(body, 'title') ?? fallbackTitle,
    detail: readString(body, 'detail') ?? UNEXPECTED_ERROR_DETAIL,
    fieldErrors: readFieldErrors(body),
  });
}
